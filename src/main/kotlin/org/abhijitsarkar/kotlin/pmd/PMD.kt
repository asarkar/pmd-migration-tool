package org.abhijitsarkar.kotlin.pmd

import net.sourceforge.pmd.Ruleset
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import javax.xml.bind.JAXBContext

/**
 * @author Abhijit Sarkar
 */
object PMD {
    private const val EMPTY = ""
    private val LOGGER = LoggerFactory.getLogger(PMD::class.java)
    private const val BASE_URL = "https://raw.githubusercontent.com/pmd/pmd/master/pmd-java/src/main/resources"
    private val JAXB_CTX = JAXBContext.newInstance(Ruleset::class.java)

    private val rulesets: MutableMap<String, Ruleset> = ConcurrentHashMap()

    private fun categoriesInternal(): List<String> {
        return (URL("$BASE_URL/category/java/categories.properties").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doOutput = false
            connectTimeout = 500
            readTimeout = 3000
            instanceFollowRedirects = true

            connect()
        }
                .let {
                    when (it.responseCode) {
                        in 200..399 -> {
                            val props = it.inputStream.bufferedReader().use { it.readText() }
                                    .let {
                                        Properties().apply {
                                            load(StringReader(it))
                                        }
                                    }
                            props["rulesets.filenames"]
                                    .toString()
                                    .replace("""\""", EMPTY)
                                    .replace("""\R""".toRegex(), EMPTY)
                                    .replace("""\s""".toRegex(), EMPTY)
                                    .split(",")
                        }
                        else -> {
                            it.errorStream?.bufferedReader()?.use { it.readText() }
                                    ?.run {
                                        LOGGER.error(this)
                                    }
                            throw IOException(it.responseMessage)
                        }
                    }
                }
    }

    fun categories(): Map<String, Ruleset> {
        return categoriesInternal()
                .map { it to ruleset(it) }
                .toMap()
    }

    fun ruleset(name: String): Ruleset {
        return rulesets.computeIfAbsent(name, {
            JAXB_CTX
                    .createUnmarshaller()
                    .unmarshal(URL("$BASE_URL/$it"))
                    .let { it as Ruleset }
        })
    }
}