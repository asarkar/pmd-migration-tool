package org.abhijitsarkar.pmd

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test

/**
 * @author Abhijit Sarkar
 */
class MainTest {
    @Test
    fun `should write migrated ruleset to given file`() {
        val out = Files.createTempFile(null, ".xml").apply {
            Runtime.getRuntime().addShutdownHook(Thread {
                Files.delete(this)
            })
        }

        arrayOf(
                "-f",
                "-o",
                out.toAbsolutePath().toString(),
                Paths.get(javaClass.getResource("/ruleset.xml").toURI()).toAbsolutePath().toString()
        )
                .apply { main(this) }

        RulesetVerifier.verify(unmarshal(out))
    }
}