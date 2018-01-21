package org.abhijitsarkar.kotlin.pmd

/**
 * @author Abhijit Sarkar
 */

import net.sourceforge.pmd.Rule
import net.sourceforge.pmd.Ruleset
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MigratorTest {
    @Test
    fun `should migrate ruleset`() {
        val ruleset = Migrator.migrate(Paths.get(javaClass.getResource("/ruleset.xml").toURI()))
        assertNotNull(ruleset)

        assertEquals(11, ruleset.rule.size)

        ruleset.apply {
            assertRule(findByRef(this, "category/java/bestpractices.xml"))
            assertRule(findByRef(this, "category/java/codestyle.xml"), excludeCount = 9)
            assertRule(findByRef(this, "category/java/codestyle.xml/LongVariable"), propertyCount = 1)
            assertRule(findByRef(this, "category/java/codestyle.xml/ShortVariable"), propertyCount = 1)
            assertRule(findByRef(this, "category/java/codestyle.xml/TooManyStaticImports"), propertyCount = 1)
            assertRule(findByRef(this, "category/java/design.xml"), excludeCount = 5)
            assertRule(findByRef(this, "category/java/documentation.xml"), excludeCount = 1)
            assertRule(findByRef(this, "category/java/errorprone.xml"), excludeCount = 5)
            assertRule(findByRef(this, "category/java/errorprone.xml/AvoidDuplicateLiterals"), propertyCount = 1)
            assertRule(findByRef(this, "category/java/multithreading.xml"))
            assertRule(findByRef(this, "category/java/performance.xml"))
        }
    }

    private fun assertRule(rule: Rule?, excludeCount: Int = 0, propertyCount: Int = 0) {
        assertNotNull(rule)
        assertEquals(excludeCount, rule?.exclude?.size ?: 0)
        assertEquals(propertyCount, rule?.properties?.property?.size ?: 0)
    }

    private fun findByRef(ruleset: Ruleset, ref: String): Rule? {
        return ruleset.rule
                .find { it.ref == ref }
    }
}