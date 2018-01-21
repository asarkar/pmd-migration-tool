package org.abhijitsarkar.kotlin.pmd

/**
 * @author Abhijit Sarkar
 */

import java.nio.file.Paths
import kotlin.test.Test

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
}