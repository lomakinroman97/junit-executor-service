package com.example.junit.service

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    
    // Content negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    
    // Routing
    routing {
        route("/api") {
            post("/execute") {
                try {
                    val request = call.receive<ExecuteRequest>()
                    logger.info("Received execution request for code length: ${request.code.length}")
                    
                    val executor = CodeExecutor()
                    val result = executor.execute(request.code)
                    
                    call.respond(HttpStatusCode.OK, result)
                    
                } catch (e: Exception) {
                    logger.error("Error processing execution request", e)
                    val errorResponse = ExecuteResponse(
                        success = false,
                        error = "EXECUTION_ERROR",
                        details = e.message ?: "Unknown error occurred"
                    )
                    call.respond(HttpStatusCode.InternalServerError, errorResponse)
                }
            }
        }
        
        // Health check
        get("/health") {
            call.respondText("OK", ContentType.Text.Plain)
        }
    }
}
