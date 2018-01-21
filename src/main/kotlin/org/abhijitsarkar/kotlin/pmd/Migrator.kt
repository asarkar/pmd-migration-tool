package org.abhijitsarkar.kotlin.pmd

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
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.xml.bind.JAXBContext


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

    private fun split(rule: String): Pair<String, String>? {
        val m = "(.*\\.xml)/(.*)".toRegex().matchEntire(rule)
        if (m?.groups?.isNotEmpty() == true && m.groups.size >= 3) {
            return m.groups.let {
                it[1]!!.value to it[2]!!.value
            }
        } else if (rule.contains('-')) {
            return rule.split('-', limit = 2).let {
                "rulesets/${it[0]}/${it[1]}/.xml" to ""
            }
        }
        LOGGER.warn("No match found for rule: $rule")
        return null
    }

    private fun unmarshal(ruleset: Path): Ruleset {
        return JAXBContext.newInstance(Ruleset::class.java)
                .createUnmarshaller()
                .unmarshal(ruleset.toFile())
                .let { it as Ruleset }
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
                    if (exclude != null && exclude.isNotEmpty()) {
                        exclude.sortedBy { it.name }.apply {
                            rule.exclude.addAll(this)
                        }
                    }
                    if (properties != null && properties.property != null && properties.property.isNotEmpty()) {
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
                    .map { it to (categoryMap.getOrDefault(it, emptyList()) + excludeMap.getOrDefault(it, emptyList())) }
                    .map { (c, ex) ->
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

    private fun String.isNotMigrated() = this.startsWith("rulesets")
    private fun String.isRuleset() = this.isNotMigrated() && this.endsWith(".xml")

    private fun awaitRules(deferred: Deferred<List<Rule>>): List<Rule> {
        return runBlocking {
            withTimeout(1L, TimeUnit.SECONDS) {
                deferred.await()
            }
        }
    }

    fun migrate(ruleset: Path): Ruleset {
        val objectFactory = ObjectFactory()
        val r = unmarshal(ruleset)

        return r
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
                                        first to (done + second)
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
                        name = r.name
                        description = r.description
                    }
                }
    }
}