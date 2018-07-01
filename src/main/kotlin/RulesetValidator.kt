package org.abhijitsarkar.pmd

import net.sourceforge.pmd.Ruleset

/**
 * @author Abhijit Sarkar
 */
object RulesetValidator {
    fun validate(old: Ruleset, new: Ruleset) {
        (old.rule to new.rule).apply {
            second.partition { it.ref?.isNotMigrated() == true }.first.apply {
                assert(this.isEmpty(), {
                    "Some rules have not been migrated: $this"
                })
            }

            val excludes = first
                    .flatMap { it.exclude.map { it.name } }
                    .toSet()
            val oldNames = first
                    .mapNotNull { it.name ?: it.ref?.let { Migrator.split(it) }?.second }
                    .toSet()
            val newNames = second.mapNotNull { it.name }
                    .toSet()
            // excluded rules shouldn't be in new ruleset unless they'd been redefined
            val exclusions = excludes.intersect(newNames) - oldNames
            assert(exclusions.isEmpty(), { "Found rules that're supposed to be excluded: $exclusions" })
        }
    }
}