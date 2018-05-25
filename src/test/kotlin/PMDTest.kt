package org.abhijitsarkar.pmd

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Abhijit Sarkar
 */
class PMDTest {
    @Test
    fun `should fetch categories`() {
        assertEquals(8, PMD.categories().size)
    }
}