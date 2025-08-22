package com.example.junit.service

import kotlinx.serialization.Serializable

@Serializable
data class ExecuteRequest(
    val code: String
)

@Serializable
data class ExecuteResponse(
    val success: Boolean,
    val testResults: List<TestResult>? = null,
    val originalCode: String? = null,
    val generatedTestCode: String? = null,
    val codeExecutionResult: CodeExecutionResult? = null,
    val error: String? = null,
    val details: String? = null
)

@Serializable
data class CodeExecutionResult(
    val success: Boolean,
    val output: String? = null,
    val error: String? = null,
    val details: String? = null,
    val executionTime: Long? = null
)

@Serializable
data class TestResult(
    val testName: String,
    val status: String, // "PASSED", "FAILED", "SKIPPED", or "WARNING"
    val assertions: List<String>,
    val errorMessage: String? = null
)
