package org.abhijitsarkar.kotlin.pmd

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import net.sourceforge.pmd.Exclude
import net.sourceforge.pmd.ObjectFactory
import net.sourceforge.pmd.Properties
import net.sourceforge.pmd.Rule
import net.sourceforge.pmd.Ruleset
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


/**
 * @author Abhijit Sarkar
 */
object Migrator {
    private val LOGGER = LoggerFactory.getLogger(Migrator::class.java).apply {
        if (isVerbose) {
            (this as Logger).level = Level.DEBUG
        }
    }
    private val RULE_MAP: Map<String, String>

    init {
        RULE_MAP = PMD.categories()
                .flatMap { c ->
                    c.value.rule.map { it.name to c.key }
                }
                .toMap()
    }

    private fun split(rule: String): Pair<String, String>? {
        return "(.*\\.xml)(?:/)?(.*)?".toRegex().matchEntire(rule).let {
            when (it?.groups?.size) {
                3 -> it.groups[1]!!.value to it.groups[2]!!.value
                else -> {
                    LOGGER.warn("No match found for rule: $rule")
                    null
                }
            }
        }
    }

    private fun createRule(
            name: String? = null,
            ref: String, exclude:
            List<Exclude>? = null,
            properties: Properties? = null
    ): Rule {
        return ObjectFactory().createRule()
                .also { rule ->
                    rule.ref = ref
                    if (name != null) {
                        rule.name = name
                    }
                    if (exclude?.isNotEmpty() == true) {
                        exclude.sortedBy { it.name }.apply {
                            rule.exclude.addAll(this)
                        }
                    }
                    if (properties?.property?.isNotEmpty() == true) {
                        rule.properties = properties.apply {
                            property.sortBy { it.name }
                        }
                    }
                }
    }

    private fun Rule.toRule(): Rule? {
        val name = split(this.ref)?.second ?: return null

        return createRule(
                name = name,
                ref = "${RULE_MAP[name]}/$name",
                properties = this.properties
        )
    }

    private fun Rule.toRules() = async {
        val objectFactory = ObjectFactory()
        val outer = this@toRules

        if (outer.ref.isRuleset()) {
            LOGGER.debug("Found ruleset: {}", outer.ref)

            val categoryMap = PMD.ruleset(outer.ref)
                    .rule
                    .map {
                        split(it.ref)?.first
                    }
                    .filter { it != null }
                    .distinct()
                    .map { it to emptyList<String>() }
                    .toMap()

            val excludeMap = (outer.exclude ?: emptyList())
                    .map { it.name }
                    .groupBy { RULE_MAP[it] }

            (categoryMap.keys + excludeMap.keys)
                    .map { it to ((categoryMap[it] ?: emptyList()) + (excludeMap[it] ?: emptyList())) }
                    .map { (c, ex) ->
                        if (ex.isNotEmpty()) {
                            LOGGER.debug("Found exclusions: $ex")
                        }
                        createRule(
                                ref = c as String,
                                exclude = ex
                                        .map { objectFactory.createExclude().apply { this.name = it } }
                        )
                    }
        } else {
            LOGGER.debug("Found rule: {}", outer.ref)
            outer.toRule()?.let {
                listOf(it)
            } ?: emptyList()
        }
    }

    private fun String.isRuleset() = isNotMigrated() && this.endsWith(".xml")

    private fun awaitRules(deferred: Deferred<List<Rule>>): List<Rule> {
        return runBlocking {
            withTimeout(1L, TimeUnit.SECONDS) {
                deferred.await()
            }
        }
    }

    fun migrate(ruleset: Ruleset): Ruleset {
        val objectFactory = ObjectFactory()

        return ruleset
                .rule
                .flatMap { rule ->
                    generateSequence(listOf(rule) to emptyList<Rule>()) { (todo, done) ->
                        if (todo.isEmpty()) {
                            null
                        } else {
                            todo.map { it.toRules() }
                                    .flatMap { awaitRules(it) }
                                    .partition { it.ref.isNotMigrated() }
                                    .run {
                                        first to (second + done)
                                    }
                        }
                    }
                            .toList()
                            .flatMap { it.second }
                }
                .groupBy { it.ref }
                .map {
                    createRule(
                            ref = it.key,
                            exclude = it.value.flatMap { it.exclude }.distinctBy { it.name },
                            properties = it.value.flatMap { it.properties?.property ?: emptyList() }
                                    .let {
                                        objectFactory.createProperties().apply {
                                            property.addAll(it)
                                        }
                                    }
                    )
                }
                .let {
                    objectFactory.createRuleset().apply {
                        rule.addAll(it.sortedBy { it.ref })
                        name = ruleset.name
                        description = ruleset.description
                    }
                }
    }
}