package id.ac.loopy.server

import id.ac.loopy.server.auth.SessionStore
import id.ac.loopy.server.auth.installLoopyAuthentication
import id.ac.loopy.server.checks.AiVisionGateway
import id.ac.loopy.server.checks.ClothingCheckService
import id.ac.loopy.server.checks.ClothingCheckStore
import id.ac.loopy.server.checks.GroqVisionGateway
import id.ac.loopy.server.checks.clothingCheckRoutes
import id.ac.loopy.server.checks.createGroqHttpClient
import id.ac.loopy.server.checks.installClothingCheckErrors
import id.ac.loopy.server.database.DatabaseFactory
import id.ac.loopy.server.database.migrateDatabase
import id.ac.loopy.server.database.read
import id.ac.loopy.server.storage.PhotoStorage
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.sql.SQLException
import javax.sql.DataSource

fun main() {
    DatabaseFactory.fromEnvironment().use { database ->
        migrateDatabase(database)

        createGroqHttpClient().use { client ->
            val gateway = GroqVisionGateway.fromEnvironment(client)
            embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
                backendModule(database, gateway)
            }.start(wait = true)
        }
    }
}

fun Application.backendModule(database: DataSource, gateway: AiVisionGateway) {
    module()
    installClothingCheckErrors()
    installLoopyAuthentication(SessionStore(database))

    val checks = ClothingCheckService(gateway, ClothingCheckStore(database), PhotoStorage(database))

    routing {
        get("/ready") {
            val ready = try {
                database.read { it.isValid(2) }
            } catch (_: SQLException) {
                false
            }
            call.respondText(
                if (ready) """{"status":"ready"}""" else """{"status":"unavailable"}""",
                ContentType.Application.Json,
                if (ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
            )
        }
        clothingCheckRoutes(checks)
    }
}

fun Application.module() {
    routing {
        get("/health") {
            call.respondText("""{"status":"ok","mode":"template"}""", ContentType.Application.Json)
        }
    }
}
