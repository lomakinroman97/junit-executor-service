package com.example.junit.service

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

fun main() {
    val logger = LoggerFactory.getLogger("Main")
    
    try {
        logger.info("Starting JUnit Executor Service...")
        
        // Load configuration
        val config = ConfigFactory.load()
        logger.info("Configuration loaded successfully")
        
        // Start HTTP server
        val server = embeddedServer(Netty, port = config.getInt("server.port"), host = config.getString("server.host")) {
            module()
        }
        
        logger.info("Server starting on ${config.getString("server.host")}:${config.getInt("server.port")}")
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown signal received, stopping server...")
            server.stop(1000, 2000)
            logger.info("Server stopped")
        })
        
        // Start server
        server.start(wait = true)
        
    } catch (e: Exception) {
        logger.error("Failed to start service", e)
        exitProcess(1)
    }
}
