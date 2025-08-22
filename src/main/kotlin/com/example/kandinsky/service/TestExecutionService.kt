package com.example.junit.service

import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
// Removed Kotlin compiler embeddable imports - using external kotlinc process instead
// Using JUnit Core for simpler programmatic test execution instead of JUnit Platform
import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URL

class TestExecutionService {
    private val logger = LoggerFactory.getLogger("TestExecutionService")
    private val tempDir = createTempDirectory("kotlin-test-executor")
    private val classLoader: URLClassLoader

    init {
        logger.info("Test execution service initialized with temp directory: $tempDir")
        
        // Create class loader for compiled classes
        val urls = arrayOf(tempDir.toUri().toURL())
        classLoader = URLClassLoader(urls, this.javaClass.classLoader)
    }

    fun executeTests(kotlinCode: String, kotlinTestCode: String): TestExecutionResult {
        try {
            logger.info("Starting test execution for Kotlin code")
            
            // Step 1: Compile the main Kotlin code and test code together
            // Create a proper Kotlin file with imports first, then code, then tests
            // Remove duplicate imports from test code and ensure proper class structure
            val cleanTestCode = kotlinTestCode
                .lines()
                .filterNot { it.trim().startsWith("import ") }
                .joinToString("\n")
            
            val combinedCode = buildString {
                appendLine("// Generated combined file")
                appendLine("import org.junit.Test")
                appendLine("import org.junit.Assert.*")
                appendLine()
                appendLine(kotlinCode)
                appendLine()
                appendLine(cleanTestCode)
            }
            val testClassFile = compileKotlinCode(combinedCode, "CombinedTests")
            if (testClassFile == null) {
                return TestExecutionResult(
                    success = false,
                    error = "COMPILATION_ERROR",
                    details = "Failed to compile code and tests together"
                )
            }
            
            // Step 3: Execute the tests using JUnit Platform
            val testResults = executeJUnitTests(testClassFile)
            
            logger.info("Test execution completed successfully")
            return TestExecutionResult(
                success = true,
                testResults = testResults
            )

        } catch (e: Exception) {
            logger.error("Error during test execution", e)
            return TestExecutionResult(
                success = false,
                error = "TEST_EXECUTION_ERROR",
                details = e.message ?: "Unknown error during test execution"
            )
        }
    }

    private fun compileKotlinCode(kotlinCode: String, className: String): File? {
        try {
            val sourceFile = tempDir.resolve("$className.kt").toFile()
            sourceFile.writeText(kotlinCode)
            
            // Use external kotlinc process to compile the code
            val kotlincCommand = findKotlincCommand()
            if (kotlincCommand == null) {
                logger.error("kotlinc command not found. Please install Kotlin compiler.")
                return null
            }
            
            // Build proper classpath for JUnit dependencies
            val junitJar = findJUnitJar()
            val hamcrestJar = findHamcrestJar()
            
            if (junitJar == null || hamcrestJar == null) {
                logger.error("Required JUnit dependencies not found")
                return null
            }
            
            val classpath = "$junitJar:$hamcrestJar"
            logger.info("Compiling $className using $kotlincCommand")
            logger.info("Source file content: ${sourceFile.readText()}")
            logger.info("Classpath: $classpath")
            
            val processBuilder = ProcessBuilder(
                kotlincCommand,
                sourceFile.absolutePath,
                "-d", tempDir.toFile().absolutePath,
                "-jvm-target", "19",
                "-classpath", classpath
            )
            
            processBuilder.directory(tempDir.toFile())
            val process = processBuilder.start()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // For top-level functions, Kotlin creates ClassName + "Kt" class
                val possibleClassFiles = listOf(
                    tempDir.resolve("$className.class").toFile(),
                    tempDir.resolve("${className}Kt.class").toFile()
                )
                
                for (classFile in possibleClassFiles) {
                    if (classFile.exists()) {
                        logger.info("Successfully compiled $className using kotlinc -> ${classFile.name}")
                        return classFile
                    }
                }
                
                logger.error("Compilation succeeded but no class file found for $className")
            } else {
                val errorOutput = process.errorStream.bufferedReader().readText()
                logger.error("Failed to compile $className, exit code: $exitCode, error: $errorOutput")
            }
            
            return null
            
        } catch (e: Exception) {
            logger.error("Error compiling $className", e)
            return null
        }
    }
    
    private fun findKotlincCommand(): String? {
        return try {
            // Try different possible locations for kotlinc
            val possibleCommands = listOf(
                "kotlinc", 
                "/usr/local/bin/kotlinc", 
                "/usr/local/kotlinc/bin/kotlinc",
                "kotlin-compiler/bin/kotlinc"
            )
            
            for (command in possibleCommands) {
                val process = ProcessBuilder("which", command).start()
                if (process.waitFor() == 0) {
                    return command
                }
            }
            
            // Try to use the one from KOTLIN_HOME
            val kotlinHome = System.getenv("KOTLIN_HOME")
            if (kotlinHome != null) {
                val kotlincPath = "$kotlinHome/bin/kotlinc"
                if (File(kotlincPath).exists()) {
                    return kotlincPath
                }
            }
            
            null
        } catch (e: Exception) {
            logger.warn("Error finding kotlinc command", e)
            null
        }
    }

    private fun executeJUnitTests(testClassFile: File): List<TestResult> {
        val testResults = mutableListOf<TestResult>()
        
        try {
            // Get the test class name from the class file
            // For our combined file, we want the GeneratedTests class, not the Kt wrapper
            val className = "GeneratedTests"
            logger.info("Executing JUnit tests for class: $className (file: ${testClassFile.name})")
            
            // List all compiled classes for debugging
            val allClassFiles = tempDir.toFile().listFiles { file -> file.name.endsWith(".class") }
            logger.info("All compiled class files: ${allClassFiles?.map { it.name } ?: "none"}")
            
            // Create a new class loader with the current classpath plus our temp directory
            val currentClasspath = System.getProperty("java.class.path")
            val tempDirUrl = tempDir.toUri().toURL()
            
            val classpathUrls = mutableListOf<URL>()
            classpathUrls.add(tempDirUrl)
            
            // Add current classpath entries
            currentClasspath?.split(File.pathSeparator)?.forEach { path ->
                if (path.isNotEmpty()) {
                    try {
                        classpathUrls.add(File(path).toURI().toURL())
                    } catch (e: Exception) {
                        logger.warn("Failed to add classpath entry: $path", e)
                    }
                }
            }
            
            val enhancedClassLoader = URLClassLoader(classpathUrls.toTypedArray(), this.javaClass.classLoader)
            
            // Load the test class
            val testClass = enhancedClassLoader.loadClass(className)
            logger.info("Successfully loaded test class: ${testClass.name}")
            
            // Create JUnit Core runner
            val junitCore = JUnitCore()
            
            logger.info("Executing JUnit tests using JUnit Core...")
            // Execute tests
            val result = junitCore.run(testClass)
            
            // Process results
            processJUnitResult(result, testResults)
            
        } catch (e: Exception) {
            logger.error("Error executing JUnit tests", e)
            testResults.add(TestResult(
                testName = "Test Execution",
                status = "FAILED",
                assertions = listOf("Failed to execute tests"),
                errorMessage = e.message ?: "Unknown error"
            ))
        }
        
        return testResults
    }

    private fun processJUnitResult(result: Result, testResults: MutableList<TestResult>) {
        // Add summary information
            testResults.add(TestResult(
            testName = "Test Summary",
            status = if (result.wasSuccessful()) "PASSED" else "FAILED",
            assertions = listOf(
                "Total tests: ${result.runCount}",
                "Tests succeeded: ${result.runCount - result.failureCount}",
                "Tests failed: ${result.failureCount}",
                "Tests ignored: ${result.ignoreCount}",
                "Execution time: ${result.runTime}ms"
            )
        ))
        
        // Add details for failed tests
        result.failures.forEach { failure ->
            testResults.add(TestResult(
                testName = failure.testHeader,
                status = "FAILED",
                assertions = listOf("Test failed during execution"),
                errorMessage = failure.message ?: failure.exception?.message ?: "Unknown failure reason"
            ))
        }
        
        // JUnit Core doesn't separate ignored/skipped tests in the same way as Platform
        // but we can log the ignore count if needed
        if (result.ignoreCount > 0) {
            testResults.add(TestResult(
                testName = "Ignored Tests",
                status = "SKIPPED",
                assertions = listOf("${result.ignoreCount} tests were ignored"),
                errorMessage = null
            ))
        }
    }
    
    private fun findJUnitJar(): String? {
        return try {
            // First, try to find in Docker dependencies directory
            val dockerDepsDir = File("/app/deps")
            if (dockerDepsDir.exists()) {
                val jarFile = dockerDepsDir.listFiles()?.find { 
                    it.name.contains("junit") && it.name.endsWith(".jar") && 
                    !it.name.contains("jupiter") && !it.name.contains("platform") && 
                    it.name.contains("junit-4.13.2")
                }
                if (jarFile != null) {
                    logger.info("Found JUnit jar in Docker deps: ${jarFile.absolutePath}")
                    return jarFile.absolutePath
                }
            }
            
            // Try to find JUnit 4 jar in gradle cache
            val gradleCache = System.getProperty("user.home") + "/.gradle/caches"
            val junitPattern = "junit/junit/4.13.2"
            
            val possiblePaths = listOf(
                "$gradleCache/modules-2/files-2.1/$junitPattern"
            )
            
            for (path in possiblePaths) {
                val dir = File(path)
                if (dir.exists()) {
                    val jarFile = dir.listFiles()?.find { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }
                    if (jarFile != null) {
                        logger.info("Found JUnit jar: ${jarFile.absolutePath}")
                        return jarFile.absolutePath
                    }
                }
            }
            
            // Fallback: try to find in current classpath
            val currentClasspath = System.getProperty("java.class.path")
            currentClasspath?.split(File.pathSeparator)?.forEach { path ->
                if (path.contains("junit") && path.endsWith(".jar") && 
                    !path.contains("jupiter") && !path.contains("platform") && 
                    path.contains("junit-4.13.2")) {
                    logger.info("Found JUnit 4 jar in classpath: $path")
                    return path
                }
            }
            
            logger.error("JUnit jar not found")
            null
        } catch (e: Exception) {
            logger.error("Error finding JUnit jar", e)
            null
        }
    }
    
    private fun findHamcrestJar(): String? {
        return try {
            // First, try to find in Docker dependencies directory
            val dockerDepsDir = File("/app/deps")
            if (dockerDepsDir.exists()) {
                val jarFile = dockerDepsDir.listFiles()?.find { 
                    it.name.contains("hamcrest") && it.name.endsWith(".jar")
                }
                if (jarFile != null) {
                    logger.info("Found hamcrest jar in Docker deps: ${jarFile.absolutePath}")
                    return jarFile.absolutePath
                }
            }
            
            // Try to find hamcrest jar in gradle cache
            val gradleCache = System.getProperty("user.home") + "/.gradle/caches"
            val hamcrestPattern = "org.hamcrest/hamcrest-core/1.3"
            
            val possiblePaths = listOf(
                "$gradleCache/modules-2/files-2.1/$hamcrestPattern"
            )
            
            for (path in possiblePaths) {
                val dir = File(path)
                if (dir.exists()) {
                    val jarFile = dir.listFiles()?.find { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }
                    if (jarFile != null) {
                        logger.info("Found hamcrest jar: ${jarFile.absolutePath}")
                        return jarFile.absolutePath
                    }
                }
            }
            
            // Fallback: try to find in current classpath
            val currentClasspath = System.getProperty("java.class.path")
            currentClasspath?.split(File.pathSeparator)?.forEach { path ->
                if (path.contains("hamcrest") && path.endsWith(".jar")) {
                    logger.info("Found hamcrest jar in classpath: $path")
                    return path
                }
            }
            
            logger.error("Hamcrest jar not found")
            null
        } catch (e: Exception) {
            logger.error("Error finding hamcrest jar", e)
            null
        }
    }

    fun cleanup() {
        try {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.delete(it) }
            logger.info("Temporary files cleaned up")
        } catch (e: Exception) {
            logger.warn("Failed to cleanup temporary files", e)
        }
    }
}

data class TestExecutionResult(
    val success: Boolean,
    val testResults: List<TestResult>? = null,
    val error: String? = null,
    val details: String? = null
)
