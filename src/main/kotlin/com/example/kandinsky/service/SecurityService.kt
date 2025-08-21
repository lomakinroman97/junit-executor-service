package com.example.junit.service

import com.typesafe.config.Config
import org.slf4j.LoggerFactory

class SecurityService(private val config: Config) {
    private val logger = LoggerFactory.getLogger("SecurityService")
    private val blacklistedPatterns = config.getStringList("security.blacklisted-patterns")
    private val maxCodeLength = config.getInt("execution.max-code-length")

    fun validateCode(code: String): SecurityValidationResult {
        // Check code length
        if (code.length > maxCodeLength) {
            return SecurityValidationResult(
                isValid = false,
                error = "CODE_TOO_LONG",
                details = "Code length ${code.length} exceeds maximum allowed length $maxCodeLength"
            )
        }

        // Check for blacklisted patterns
        val foundBlacklistedPatterns = blacklistedPatterns.filter { pattern ->
            code.contains(pattern, ignoreCase = true)
        }

        if (foundBlacklistedPatterns.isNotEmpty()) {
            logger.warn("Blacklisted patterns found in code: $foundBlacklistedPatterns")
            return SecurityValidationResult(
                isValid = false,
                error = "BLACKLISTED_PATTERNS",
                details = "Code contains forbidden patterns: ${foundBlacklistedPatterns.joinToString(", ")}"
            )
        }

        // Check for potentially dangerous imports
        val dangerousImports = listOf(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.System",
            "java.io.File",
            "java.nio.file.Files",
            "java.net.URL",
            "java.net.URLConnection"
        )

        val foundDangerousImports = dangerousImports.filter { import ->
            code.contains("import $import") || code.contains("import $import.*")
        }

        if (foundDangerousImports.isNotEmpty()) {
            logger.warn("Potentially dangerous imports found: $foundDangerousImports")
            return SecurityValidationResult(
                isValid = false,
                error = "DANGEROUS_IMPORTS",
                details = "Code contains potentially dangerous imports: ${foundDangerousImports.joinToString(", ")}"
            )
        }

        return SecurityValidationResult(isValid = true)
    }
}

data class SecurityValidationResult(
    val isValid: Boolean,
    val error: String? = null,
    val details: String? = null
)
