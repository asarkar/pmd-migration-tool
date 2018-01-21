package org.abhijitsarkar.kotlin.pmd

import net.sourceforge.pmd.Ruleset

/**
 * @author Abhijit Sarkar
 */
object RulesetValidator {
    fun validate(old: Ruleset, new: Ruleset) {
        (old.rule to new.rule).apply {
            second.partition { it.ref.isNotMigrated() }.first.apply {
                assert(this.isEmpty(), {
                    "Some rules have not been migrated: $this"
                })
            }

            val ex1 = first.flatMap { it.exclude.map { it.name } }.toSet()
            val ex2 = second.flatMap { it.exclude.map { it.name } }
            (ex1 to ex2).apply {
                assert(first.size == second.size, {
                    "Old ruleset had ${first.size} exclusions, new one has ${second.size}. Diff: ${first - second}"
                })
            }
        }
    }
}