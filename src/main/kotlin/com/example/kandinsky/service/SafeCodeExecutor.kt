package com.example.junit.service

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Callable

class SafeCodeExecutor {
    private val logger = LoggerFactory.getLogger("SafeCodeExecutor")
    private val tempDir = createTempDirectory("safe-code-executor")
    private val executor = Executors.newFixedThreadPool(2)

    fun executeCodeSafely(
        kotlinCode: String,
        timeoutSeconds: Long = 30
    ): CodeExecutionResult {
        try {
            // Create a temporary directory for this execution
            val executionDir = createTempDirectory("execution-${System.currentTimeMillis()}")
            
            // Save the code to a file
            val sourceFile = executionDir.resolve("UserCode.kt").toFile()
            sourceFile.writeText(kotlinCode)
            
            // Compile the code
            val compilationResult = compileKotlinCode(sourceFile, executionDir.toFile())
            if (!compilationResult.success) {
                return CodeExecutionResult(
                    success = false,
                    error = "COMPILATION_ERROR",
                    details = compilationResult.errorDetails
                )
            }
            
            // Execute the code with timeout
            val executionResult = executeWithTimeout(executionDir.toFile(), timeoutSeconds)
            
            // Cleanup
            cleanupExecutionDir(executionDir.toFile())
            
            return executionResult
            
        } catch (e: Exception) {
            logger.error("Error in safe code execution", e)
            return CodeExecutionResult(
                success = false,
                error = "EXECUTION_ERROR",
                details = e.message ?: "Unknown error"
            )
        }
    }

    private fun compileKotlinCode(sourceFile: File, outputDir: File): CompilationResult {
        return try {
            // Use the same compilation logic as TestExecutionService
            // This is a simplified version for demonstration
            val success = true // Placeholder for actual compilation
            CompilationResult(success, null)
        } catch (e: Exception) {
            CompilationResult(false, e.message)
        }
    }

    private fun executeWithTimeout(executionDir: File, timeoutSeconds: Long): CodeExecutionResult {
        val task = Callable<CodeExecutionResult> {
            try {
                // Here you would actually execute the compiled code
                // For now, return a success result
                CodeExecutionResult(
                    success = true,
                    output = "Code executed successfully",
                    executionTime = 0L
                )
            } catch (e: Exception) {
                CodeExecutionResult(
                    success = false,
                    error = "RUNTIME_ERROR",
                    details = e.message
                )
            }
        }

        val future: Future<CodeExecutionResult> = executor.submit(task)
        
        return try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: Exception) {
            future.cancel(true)
            CodeExecutionResult(
                success = false,
                error = "TIMEOUT_ERROR",
                details = "Execution timed out after $timeoutSeconds seconds"
            )
        }
    }

    private fun cleanupExecutionDir(executionDir: File) {
        try {
            Files.walk(executionDir.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach { Files.delete(it) }
        } catch (e: Exception) {
            logger.warn("Failed to cleanup execution directory", e)
        }
    }

    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}

// CodeExecutionResult is already defined in Models.kt

data class CompilationResult(
    val success: Boolean,
    val errorDetails: String? = null
)
