package com.example.junit.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import java.io.File

class TestExecutionServiceTest {
    
    private lateinit var testExecutionService: TestExecutionService
    
    @BeforeEach
    fun setUp() {
        testExecutionService = TestExecutionService()
    }
    
    @AfterEach
    fun tearDown() {
        testExecutionService.cleanup()
    }
    
    @Test
    fun `should execute simple Kotlin code and tests`() {
        // Given
        val kotlinCode = """
            fun add(a: Int, b: Int): Int {
                return a + b
            }
            
            fun multiply(a: Int, b: Int): Int {
                return a * b
            }
        """.trimIndent()
        
        val testCode = """
            import org.junit.jupiter.api.Test
            import org.junit.jupiter.api.Assertions.*
            
            class GeneratedTests {
                @Test
                fun testAdd() {
                    assertEquals(5, add(2, 3))
                    assertEquals(0, add(-1, 1))
                }
                
                @Test
                fun testMultiply() {
                    assertEquals(6, multiply(2, 3))
                    assertEquals(0, multiply(0, 5))
                }
            }
        """.trimIndent()
        
        // When
        val result = testExecutionService.executeTests(kotlinCode, testCode)
        
        // Then
        assertTrue(result.success, "Test execution should succeed")
        assertNotNull(result.testResults, "Test results should not be null")
        assertTrue(result.testResults!!.isNotEmpty(), "Should have test results")
        
        // Check that we have test summary
        val summaryResult = result.testResults!!.find { it.testName == "Test Summary" }
        assertNotNull(summaryResult, "Should have test summary")
        assertEquals("PASSED", summaryResult!!.status, "Test summary should be PASSED")
    }
    
    @Test
    fun `should handle compilation errors gracefully`() {
        // Given
        val invalidKotlinCode = """
            fun add(a: Int, b: Int): Int {
                return a + b + // Missing closing parenthesis
            }
        """.trimIndent()
        
        val testCode = """
            import org.junit.jupiter.api.Test
            import org.junit.jupiter.api.Assertions.*
            
            class GeneratedTests {
                @Test
                fun testAdd() {
                    assertEquals(5, add(2, 3))
                }
            }
        """.trimIndent()
        
        // When
        val result = testExecutionService.executeTests(invalidKotlinCode, testCode)
        
        // Then
        assertFalse(result.success, "Test execution should fail with invalid code")
        assertNotNull(result.error, "Should have error information")
        assertTrue(result.error!!.contains("COMPILATION_ERROR"), "Should indicate compilation error")
    }
}
