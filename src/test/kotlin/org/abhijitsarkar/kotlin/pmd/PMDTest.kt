package org.abhijitsarkar.kotlin.pmd

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Abhijit Sarkar
 */
class PMDTest {
    @Test
    fun `should fetch categories`() {
        assertEquals(7, PMD.categories().size)
    }
}