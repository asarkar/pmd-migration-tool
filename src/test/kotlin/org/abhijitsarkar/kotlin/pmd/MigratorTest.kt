package org.abhijitsarkar.kotlin.pmd

/**
 * @author Abhijit Sarkar
 */

import java.nio.file.Paths
import kotlin.test.Test

class MigratorTest {
    @Test
    fun `should migrate ruleset`() {
        Migrator.migrate(Paths.get(javaClass.getResource("/ruleset.xml").toURI()))
                .apply { RulesetVerifier.verify(this) }
    }
}