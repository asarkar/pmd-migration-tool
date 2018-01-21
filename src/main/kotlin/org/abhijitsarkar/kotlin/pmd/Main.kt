package org.abhijitsarkar.kotlin.pmd

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import net.sourceforge.pmd.Ruleset
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * @author Abhijit Sarkar
 */

class CmdLineArgs(parser: ArgParser) {
    val output by parser.storing(
            "-o", "--output",
            help = "output file path"
    )
            .default {
                null
            }
    val force by parser.flagging(
            "-f", "--force",
            help = "overwrite output file if exists"
    )
    val ruleset by parser.positional("ruleset path")
}

fun main(args: Array<String>): Unit = mainBody {
    val cmdLineArgs = ArgParser(args).parseInto(::CmdLineArgs)
    val out = cmdLineArgs.output?.let {
        Paths.get(it).let {
            if (Files.exists(it) && !cmdLineArgs.force) {
                throw IllegalArgumentException("Output file already exists.")
            }

            if (Files.isDirectory(it)) {
                throw IllegalArgumentException("Output file is a directory.")
            }

            Files.newBufferedWriter(it, StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING, WRITE)
        }
    } ?: PrintWriter(System.out)

    val rulset = cmdLineArgs.ruleset.let {
        Paths.get(it).apply {
            if (!Files.exists(this) || !Files.isReadable(this)) {
                throw IllegalArgumentException("Input file doesn't exist or isn't readable.")
            }

            if (Files.isDirectory(this)) {
                throw IllegalArgumentException("Input file is a directory.")
            }
        }
    }

    try {
        val ruleset = Migrator.migrate(rulset)

        val doc = DocumentBuilderFactory.newInstance()
                .apply { isNamespaceAware = true }
                .run { newDocumentBuilder() }
                .run { newDocument() }

        JAXBContext.newInstance(Ruleset::class.java)
                .createMarshaller()
                .apply {
                    setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd")
                }
                .marshal(ruleset, doc)

        TransformerFactory.newInstance()
                .run {
                    newTransformer().apply {
                        setOutputProperty(OutputKeys.INDENT, "yes")
                        setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                        setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name())
                    }
                }
                .also {
                    val source = DOMSource(doc)
                    val result = StreamResult(out)
                    it.transform(source, result)
                }
    } finally {
        cmdLineArgs.output?.also {
            out.close()
        }
    }
}