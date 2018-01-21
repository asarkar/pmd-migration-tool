package org.abhijitsarkar.kotlin.pmd

import net.sourceforge.pmd.Ruleset
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.bind.JAXBContext
import kotlin.test.Test

/**
 * @author Abhijit Sarkar
 */
class MainTest {
    @Test
    fun `should write migrated ruleset to given file`() {
        val out = Files.createTempFile(null, "xml").apply {
            Runtime.getRuntime().addShutdownHook(Thread {
                Files.delete(this)
            })
        }

        val args = arrayOf(
                "-f",
                "-o",
                out.toAbsolutePath().toString(),
                Paths.get(javaClass.getResource("/ruleset.xml").toURI()).toAbsolutePath().toString()
        )
        main(args)

        JAXBContext.newInstance(Ruleset::class.java)
                .createUnmarshaller()
                .unmarshal(out.toFile()).apply {
            RulesetVerifier.verify(this as Ruleset)
        }
    }
}