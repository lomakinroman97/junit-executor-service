package com.example.junit.service

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class CodeExecutor {
    private val logger = LoggerFactory.getLogger("CodeExecutor")
    private val config = ConfigFactory.load()
    private val securityService = SecurityService(config)
    private val yandexGPTService = YandexGPTService(config)
    private val testExecutionService = TestExecutionService()

    fun execute(kotlinCode: String): ExecuteResponse {
        return try {
            // Apply timeout to entire execution using a separate thread
            val timeoutMs = config.getLong("execution.timeout-seconds") * 1000
            val resultRef = AtomicReference<ExecuteResponse?>()
            val exceptionRef = AtomicReference<Exception?>()
            
            val executionThread = thread {
                try {
                    resultRef.set(executeWithTimeout(kotlinCode))
                } catch (e: Exception) {
                    exceptionRef.set(e)
                }
            }
            
            executionThread.join(timeoutMs)
            
            if (executionThread.isAlive) {
                executionThread.interrupt()
                logger.error("Execution timeout after ${config.getLong("execution.timeout-seconds")} seconds")
                return ExecuteResponse(
                    success = false,
                    error = "EXECUTION_TIMEOUT",
                    details = "Execution exceeded maximum allowed time of ${config.getLong("execution.timeout-seconds")} seconds"
                )
            }
            
            val capturedException = exceptionRef.get()
            if (capturedException != null) {
                throw capturedException
            }
            
            resultRef.get() ?: throw RuntimeException("No result returned from execution")
            
        } catch (e: Exception) {
            logger.error("Unexpected error during execution", e)
            ExecuteResponse(
                success = false,
                error = "UNEXPECTED_ERROR",
                details = e.message ?: "Unknown error occurred"
            )
        } finally {
            // Cleanup resources
            try {
                testExecutionService.cleanup()
                yandexGPTService.close()
            } catch (e: Exception) {
                logger.warn("Error during cleanup", e)
            }
        }
    }

    private fun executeWithTimeout(kotlinCode: String): ExecuteResponse {
        // Step 1: Security validation
        logger.info("Validating code security")
        val securityValidation = securityService.validateCode(kotlinCode)
        if (!securityValidation.isValid) {
            return ExecuteResponse(
                success = false,
                error = securityValidation.error,
                details = securityValidation.details
            )
        }
        logger.info("Code security validation passed")

        // Step 2: Generate tests using Yandex GPT
        logger.info("Generating tests using Yandex GPT")
        val generatedTestCode = try {
            yandexGPTService.generateTests(kotlinCode)
        } catch (e: Exception) {
            logger.error("Failed to generate tests", e)
            return ExecuteResponse(
                success = false,
                error = "LLM_API_ERROR",
                details = e.message ?: "Failed to generate tests from LLM"
            )
        }

        if (generatedTestCode.isBlank()) {
            return ExecuteResponse(
                success = false,
                error = "NO_TESTS_GENERATED",
                details = "LLM failed to generate valid test code"
            )
        }
        logger.info("Tests generated successfully, length: ${generatedTestCode.length}")

        // Step 3: Execute tests
        logger.info("Executing generated tests")
        val testExecutionResult = testExecutionService.executeTests(kotlinCode, generatedTestCode)
        
        if (!testExecutionResult.success) {
            return ExecuteResponse(
                success = false,
                error = testExecutionResult.error,
                details = testExecutionResult.details
            )
        }

        // Step 4: Return results
        logger.info("Test execution completed successfully")
        return ExecuteResponse(
            success = true,
            testResults = testExecutionResult.testResults,
            originalCode = kotlinCode,
            generatedTestCode = generatedTestCode
        )
    }
}
