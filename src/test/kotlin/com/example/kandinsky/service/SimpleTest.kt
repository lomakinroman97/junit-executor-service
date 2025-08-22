package com.example.junit.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SimpleTest {
    
    @Test
    fun `should pass basic test`() {
        // Given
        val expected = 2
        
        // When
        val actual = 1 + 1
        
        // Then
        assertEquals(expected, actual, "1 + 1 should equal 2")
    }
    
    @Test
    fun `should handle string operations`() {
        // Given
        val input = "Hello"
        
        // When
        val result = input + " World"
        
        // Then
        assertEquals("Hello World", result)
    }
}
