package com.example.junit.service

import com.typesafe.config.Config
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.runBlocking

class YandexGPTService(private val config: Config) {
    private val logger = LoggerFactory.getLogger("YandexGPTService")
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = true
            })
        }
    }

    fun generateTests(kotlinCode: String): String {
        return runBlocking {
            val prompt = buildString {
                append("You are a senior Kotlin developer. Generate a JUnit 4 test class for the following code. ")
                append("Return ONLY the Kotlin code without any markdown formatting, explanations, or additional text. ")
                append("The test class must be named 'GeneratedTests' and use JUnit 4 annotations (@Test from org.junit.Test, not Jupiter). ")
                append("Use org.junit.Assert for assertions (assertEquals, assertTrue, etc.). ")
                append("The code under test will be in the same file, so call functions directly (for example add(1, 2)). ")
                append("Use Kotlin syntax (fun instead of public static, etc.). Here is the code to test: ")
                append(kotlinCode)
            }

            val requestBody = buildJsonObject {
                put("modelUri", "gpt://b1gp9fidpabmov8j1rid/yandexgpt-lite")
                put("messages", buildJsonArray {
                    addJsonObject {
                        put("role", "user")
                        put("text", prompt)
                    }
                })
            }

            try {
                logger.info("Sending request to Yandex GPT for test generation")
                logger.info("API URL: ${config.getString("yandex-gpt.url")}")
                logger.info("Folder ID: ${config.getString("yandex-gpt.folder")}")
                logger.info("Request body: $requestBody")
                
                val response = client.post(config.getString("yandex-gpt.url")) {
                    header("Authorization", "Api-Key ${config.getString("yandex-gpt.api-key")}")
                    header("x-folder-id", config.getString("yandex-gpt.folder"))
                    header("Content-Type", "application/json")
                    setBody(requestBody)
                }

                logger.info("Response status: ${response.status}")
                
                if (response.status.isSuccess()) {
                    val responseBody = response.body<JsonObject>()
                    logger.info("Response body: $responseBody")
                    
                    // Используем правильную структуру ответа на основе рабочего примера
                    val result = responseBody["result"]?.jsonObject
                    if (result != null) {
                        val alternatives = result["alternatives"]?.jsonArray
                        if (alternatives != null && alternatives.isNotEmpty()) {
                            val alternative = alternatives[0].jsonObject
                            val message = alternative["message"]?.jsonObject
                            if (message != null) {
                                val generatedCode = message["text"]?.jsonPrimitive?.content
                                if (generatedCode != null && generatedCode.isNotBlank()) {
                                    logger.info("Successfully generated tests from Yandex GPT")
                                    // Убираем markdown разметку если она есть
                                    val cleanCode = generatedCode.trim()
                                        .removePrefix("```")
                                        .removePrefix("kotlin")
                                        .removeSuffix("```")
                                        .trim()
                                    return@runBlocking cleanCode
                                }
                            }
                        }
                    }
                    
                    throw RuntimeException("No valid test code generated from Yandex GPT")
                } else {
                    val errorBody = response.body<String>()
                    logger.error("Yandex GPT API error: ${response.status} - $errorBody")
                    throw RuntimeException("Yandex GPT API error: ${response.status}")
                }
                
            } catch (e: TimeoutException) {
                logger.error("Timeout while waiting for Yandex GPT response")
                throw RuntimeException("LLM_API_TIMEOUT")
            } catch (e: Exception) {
                logger.error("Error calling Yandex GPT API", e)
                throw RuntimeException("LLM_API_ERROR: ${e.message}")
            }
        }
    }

    fun close() {
        client.close()
    }
}
