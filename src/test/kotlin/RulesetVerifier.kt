package org.abhijitsarkar.pmd

import net.sourceforge.pmd.Ruleset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * @author Abhijit Sarkar
 */
object RulesetVerifier {
    fun verify(ruleset: Ruleset) {
        assertNotNull(ruleset)

        val refMap = ruleset.rule
                .groupBy { Migrator.split(it.ref)?.first ?: it.name }
                .mapValues { it.value.flatMap { it?.properties?.property ?: emptyList() }.size }
                .toMap()

        assertEquals(7, refMap.size)

        ruleset.apply {
            assertEquals(0, refMap["category/java/bestpractices.xml"])
            assertEquals(3, refMap["category/java/codestyle.xml"])
            assertEquals(0, refMap["category/java/design.xml"])
            assertEquals(0, refMap["category/java/documentation.xml"])
            assertEquals(1, refMap["category/java/errorprone.xml"])
            assertEquals(0, refMap["category/java/multithreading.xml"])
            assertEquals(0, refMap["category/java/performance.xml"])
        }
    }
}