package org.abhijitsarkar.kotlin.pmd

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import net.sourceforge.pmd.Ruleset
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.Properties
import javax.xml.bind.JAXBContext

/**
 * @author Abhijit Sarkar
 */
object PMD {
    private const val EMPTY = ""
    private val rulesets: MutableMap<String, Ruleset> = mutableMapOf()

    init {
        FuelManager.instance.basePath = "https://raw.githubusercontent.com/pmd/pmd/master/pmd-java/src/main/resources"
    }

    private fun categoriesInternal(): List<String> {
        val (_, _, result) = "/category/java/categories.properties".httpGet().response()
        val (data, error) = result
        return if (error == null) {
            val props = Properties().apply {
                load(ByteArrayInputStream(data))
            }
            props["rulesets.filenames"]
                    .toString()
                    .replace("""\""", EMPTY)
                    .replace("""\R""".toRegex(), EMPTY)
                    .replace("""\s""".toRegex(), EMPTY)
                    .split(",")
        } else {
            throw error
        }
    }

    fun categories(): Map<String, Ruleset> {
        return categoriesInternal()
                .map { it to ruleset(it) }
                .toMap()
    }

    fun ruleset(name: String): Ruleset {
        return rulesets.computeIfAbsent(name, {
            JAXBContext.newInstance(Ruleset::class.java)
                    .createUnmarshaller()
                    .unmarshal(URL("${FuelManager.instance.basePath}/$it"))
                    .let { it as Ruleset }
        })
    }
}