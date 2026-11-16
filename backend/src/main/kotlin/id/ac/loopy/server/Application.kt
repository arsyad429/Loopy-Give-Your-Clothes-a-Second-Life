package id.ac.loopy.server

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    routing {
        get("/health") {
            // Liveness saja: koneksi database dan fitur PRD belum diimplementasikan.
            call.respondText("""{"status":"ok","mode":"template"}""", ContentType.Application.Json)
        }
    }
}
