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
                append("You are a senior Kotlin developer. Generate a JUnit 5 test class for the following code. ")
                append("Return ONLY the Kotlin code without any markdown formatting, explanations, or additional text. ")
                append("The test class must be named 'GeneratedTests' and use org.junit.jupiter.api.Assertions for assertions. ")
                append("Use Kotlin syntax (fun instead of public static, etc.). Here is the code to test: ")
                append(kotlinCode)
            }

            val requestBody = buildJsonObject {
                put("modelUri", "gpt://${config.getString("yandex-gpt.folder")}/yandexgpt-lite")
                put("completionOptions", buildJsonObject {
                    put("temperature", 0.1)
                    put("maxTokens", 2000)
                })
                put("messages", buildJsonArray {
                    addJsonObject {
                        put("role", "user")
                        put("text", prompt)
                    }
                })
            }

            try {
                logger.info("Sending request to Yandex GPT for test generation")
                
                val response = client.post(config.getString("yandex-gpt.url")) {
                    header("Authorization", "Api-Key ${config.getString("yandex-gpt.api-key")}")
                    header("Content-Type", "application/json")
                    setBody(requestBody)
                }

                if (response.status.isSuccess()) {
                    val responseBody = response.body<JsonObject>()
                    val choices = responseBody["result"]?.jsonObject?.get("alternatives")?.jsonArray
                    
                    if (choices != null && choices.isNotEmpty()) {
                        val generatedCode = choices[0].jsonObject["message"]?.jsonObject?.get("text")?.jsonPrimitive?.content
                        if (generatedCode != null && generatedCode.isNotBlank()) {
                            logger.info("Successfully generated tests from Yandex GPT")
                            return@runBlocking generatedCode.trim()
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
