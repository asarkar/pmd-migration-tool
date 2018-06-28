package org.abhijitsarkar.pmd

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
    private val LOGGER = LoggerFactory.getLogger(Migrator::class.java)
    private val RULE_MAP: Map<String, String>

    init {
        RULE_MAP = PMD.categories()
                .flatMap { c ->
                    c.value.rule.map { it.name to c.key }
                }
                .toMap()
    }

    internal fun split(rule: String): Pair<String, String>? {
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
            ref: String?, exclude:
            List<Exclude>? = null,
            properties: Properties? = null
    ): Rule {
        return ObjectFactory().createRule()
                .also { rule ->
                    rule.ref = ref
                    rule.name = name
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
        val name = this.ref?.let { split(it) }?.second ?: return this

        if (this.name != null && this.name != name) {
            LOGGER.warn("Rule: {} has been renamed to: {}", this.name, name)
        }

        return createRule(
                name = name,
                ref = "${RULE_MAP[name]}/$name",
                properties = this.properties
        ).also {
            it.description = description
            it.priority = priority
            it.language = language
            it.minimumLanguageVersion = minimumLanguageVersion
            it.maximumLanguageVersion = maximumLanguageVersion
            it.since = since
            it.message = message
            it.externalInfoUrl = externalInfoUrl
            it.clazz = clazz
            it.isDfa = isDfa
            it.isTypeResolution = isTypeResolution
            it.isDeprecated = isDeprecated
        }
    }

    private fun Rule.toRules(): Deferred<List<Rule>> = async {
        val outer = this@toRules

        if (outer.ref?.isRuleset() == true) {
            LOGGER.debug("Found ruleset: {}", outer.ref)
            PMD.ruleset(outer.ref)
                    .rule.mapNotNull { it.toRule() }
                    .filter { outer.exclude.none { e -> e.name == it.name } }
        } else {
            LOGGER.debug("Found rule: {}", outer.ref)
            outer.toRule()?.let {
                listOf(it)
            } ?: emptyList()
        }
    }

    private fun String.isRuleset(): Boolean = isNotMigrated() && this.endsWith(".xml")

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
                                    .partition { it.ref?.isNotMigrated() == true }
                                    .run {
                                        first to (second + done)
                                    }
                        }
                    }
                            .toList()
                            .flatMap { it.second }
                }
                .let {
                    objectFactory.createRuleset().apply {
                        rule.addAll(it.sortedWith(compareBy(nullsLast<String>()) { it.ref }))
                        name = ruleset.name
                        description = ruleset.description
                    }
                }
    }
}