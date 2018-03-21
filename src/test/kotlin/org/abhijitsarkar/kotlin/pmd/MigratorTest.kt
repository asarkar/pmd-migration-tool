package org.abhijitsarkar.kotlin.pmd

/**
 * @author Abhijit Sarkar
 */

import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MigratorTest {
    @Test
    fun `should migrate ruleset`() {
        val ruleset = unmarshal(Paths.get(javaClass.getResource("/ruleset.xml").toURI()))
        Migrator.migrate(ruleset)
                .apply {
                    RulesetVerifier.verify(this)
                    RulesetValidator.validate(ruleset, this)
                }
    }

    @Test
    fun `should migrate rule without ref`() {
        val ruleset = unmarshal(Paths.get(javaClass.getResource("/ruleset-issue-2.xml").toURI()))
        val migrated = Migrator.migrate(ruleset)

        assertNotNull(migrated)
        assertEquals(1, migrated.rule.size)

        val rule = migrated.rule.first()
        assertEquals("CheckstyleCustomShortVariable", rule.name)
        assertEquals("Avoid variables with short names that shorter than 2 symbols: {0}", rule.message)
        assertEquals("java", rule.language)
        assertEquals("net.sourceforge.pmd.lang.rule.XPathRule", rule.clazz)
        assertEquals(3, rule.priority)
        assertTrue(rule.externalInfoUrl.isEmpty())
        assertTrue(rule.description.isNotEmpty())
        assertEquals(1, rule.properties.property.size)

        RulesetValidator.validate(ruleset, migrated)
    }
}