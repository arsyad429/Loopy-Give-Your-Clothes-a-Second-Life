package id.ac.loopy.server.checks

import id.ac.loopy.server.auth.LOOPY_AUTH
import id.ac.loopy.server.auth.LoopyPrincipal
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import java.io.IOException
import java.sql.SQLException
import java.util.UUID
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val MAX_MULTIPART_BYTES = MAX_UPLOAD_BYTES.toLong() + 64 * 1024

internal fun Application.installClothingCheckErrors() {
    install(StatusPages) {
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respondProblem(status, "AUTH_REQUIRED", "Sesi tidak tersedia atau sudah berakhir.")
        }
        exception<CheckProblem> { call, cause ->
            call.respondProblem(cause.status, cause.code, cause.message.orEmpty(), cause.retryAfterSeconds)
        }
        exception<PayloadTooLargeException> { call, _ ->
            call.respondProblem(HttpStatusCode.PayloadTooLarge, "PHOTO_TOO_LARGE", "Ukuran foto maksimal 10 MiB.")
        }
        exception<BadRequestException> { call, _ ->
            call.respondProblem(HttpStatusCode.BadRequest, "INVALID_REQUEST", "Request tidak dapat dibaca.")
        }
        exception<SQLException> { call, _ ->
            call.respondProblem(
                HttpStatusCode.ServiceUnavailable, "STORAGE_UNAVAILABLE",
                "Penyimpanan pemeriksaan belum tersedia. Coba lagi.",
            )
        }
        exception<AiVisionException> { call, cause ->
            when (cause.failure) {
                AiVisionFailure.TIMEOUT, AiVisionFailure.RATE_LIMITED, AiVisionFailure.UNAVAILABLE ->
                    call.respondProblem(
                        HttpStatusCode.ServiceUnavailable, "AI_UNAVAILABLE",
                        "Layanan AI belum tersedia. Coba lagi.", cause.retryAfterSeconds,
                    )
                AiVisionFailure.INVALID_RESPONSE, AiVisionFailure.PROVIDER_REJECTED ->
                    call.respondProblem(
                        HttpStatusCode.BadGateway, "AI_RESPONSE_FAILED",
                        "Pemeriksaan AI tidak dapat diselesaikan. Coba lagi.",
                    )
            }
        }
    }
}

internal fun Route.clothingCheckRoutes(service: ClothingCheckService) {
    val uploadSlots = Semaphore(4)

    authenticate(LOOPY_AUTH) {
        route("/v1/checks") {
            install(RequestBodyLimit) {
                bodyLimit { MAX_MULTIPART_BYTES }
            }

            post {
                val actor = call.checkActor()
                val key = call.request.headers.getAll("Idempotency-Key")?.singleOrNull()?.trim()
                    ?: throw CheckProblem(
                        HttpStatusCode.BadRequest, "IDEMPOTENCY_KEY_REQUIRED",
                        "Sertakan satu header Idempotency-Key.",
                    )

                if (!uploadSlots.tryAcquire()) {
                    throw CheckProblem(
                        HttpStatusCode.TooManyRequests, "CHECKS_BUSY",
                        "Pemeriksaan sedang penuh. Coba lagi.", 2,
                    )
                }

                try {
                    val upload = withTimeoutOrNull(15_000) { call.readCheckUpload() }
                        ?: throw CheckProblem(
                            HttpStatusCode.RequestTimeout, "UPLOAD_TIMEOUT", "Waktu unggah foto habis.",
                        )
                    val result = service.create(actor, key, upload)
                    if (result.isNew) call.response.header(HttpHeaders.Location, "/v1/checks/${result.check.checkId}")
                    call.respondCheck(
                        result.check,
                        if (result.isNew) HttpStatusCode.Created else HttpStatusCode.OK,
                    )
                } finally {
                    uploadSlots.release()
                }
            }

            get("/{id}") {
                call.respondCheck(service.find(call.checkActor(), call.checkId()))
            }

            get("/{id}/photo") {
                val photo = service.photo(call.checkActor(), call.checkId())
                call.response.header(HttpHeaders.CacheControl, "private, no-store")
                call.response.header("X-Content-Type-Options", "nosniff")
                call.respondBytes(photo, ContentType.Image.JPEG)
            }
        }
    }
}

private suspend fun ApplicationCall.readCheckUpload(): CheckUpload {
    if (!request.contentType().match(ContentType.MultiPart.FormData)) {
        throw CheckProblem(
            HttpStatusCode.UnsupportedMediaType, "MULTIPART_REQUIRED", "Gunakan multipart/form-data.",
        )
    }

    var clothingType: String? = null
    var mime: ContentType? = null
    var image: ByteArray? = null

    try {
        receiveMultipart(formFieldLimit = MAX_MULTIPART_BYTES).forEachPart { part ->
            try {
                when {
                    part is PartData.FormItem && part.name == "clothingType" && clothingType == null -> {
                        clothingType = part.value.trim()
                    }

                    part is PartData.FileItem && part.name == "image" && image == null -> {
                        val type = part.contentType?.withoutParameters()
                        if (type != ContentType.Image.JPEG && type != ContentType.Image.PNG) {
                            throw CheckProblem(
                                HttpStatusCode.UnsupportedMediaType, "UNSUPPORTED_PHOTO", "Gunakan JPEG atau PNG.",
                            )
                        }

                        val bytes = part.provider().readRemaining(MAX_UPLOAD_BYTES.toLong() + 1).readByteArray()
                        if (bytes.size > MAX_UPLOAD_BYTES) {
                            throw CheckProblem(
                                HttpStatusCode.PayloadTooLarge, "PHOTO_TOO_LARGE", "Ukuran foto maksimal 10 MiB.",
                            )
                        }
                        image = bytes
                        mime = type
                    }

                    else -> throw CheckProblem(
                        HttpStatusCode.BadRequest, "INVALID_MULTIPART",
                        "Kirim satu image dan satu clothingType.",
                    )
                }
            } finally {
                part.dispose()
            }
        }
    } catch (_: IOException) {
        throw CheckProblem(HttpStatusCode.BadRequest, "INVALID_MULTIPART", "Unggahan tidak dapat dibaca.")
    }

    val type = clothingType?.takeIf { it.isNotBlank() }
    val bytes = image
    val mediaType = mime
    if (type == null || bytes == null || mediaType == null) {
        throw CheckProblem(
            HttpStatusCode.BadRequest, "MISSING_UPLOAD_FIELDS", "Sertakan image dan clothingType.",
        )
    }
    return CheckUpload(type, mediaType, bytes)
}

private fun ApplicationCall.checkActor(): LoopyPrincipal =
    principal<LoopyPrincipal>() ?: throw CheckProblem(
        HttpStatusCode.Unauthorized, "AUTH_REQUIRED", "Sesi tidak tersedia atau sudah berakhir.",
    )

private fun ApplicationCall.checkId(): UUID {
    val value = parameters["id"].orEmpty()
    val uuid = try {
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        null
    }
    return uuid?.takeIf { it.toString().equals(value, ignoreCase = true) }
        ?: throw CheckProblem(HttpStatusCode.BadRequest, "INVALID_CHECK_ID", "ID pemeriksaan tidak valid.")
}

private suspend fun ApplicationCall.respondCheck(
    check: StoredClothingCheck,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    response.header(HttpHeaders.CacheControl, "private, no-store")
    val json = buildJsonObject {
        put("checkId", check.checkId.toString())
        put("userId", check.ownerId.toString())
        put("clothingPhoto", "/v1/checks/${check.checkId}/photo")
        put("clothingType", check.clothingType)
        put("aiResult", check.analysis.aiResult.name)
        put("aiDescription", check.analysis.aiDescription)
        put("actionRecommendation", check.analysis.actionRecommendation?.let { JsonPrimitive(it) } ?: JsonNull)
        put("createdAt", check.createdAt.toString())
    }
    respondText(json.toString(), ContentType.Application.Json, status)
}

private suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    code: String,
    message: String,
    retryAfterSeconds: Long? = null,
) {
    response.header(HttpHeaders.CacheControl, "no-store")
    if (retryAfterSeconds != null) response.header(HttpHeaders.RetryAfter, retryAfterSeconds)

    val json = buildJsonObject {
        putJsonObject("error") {
            put("code", code)
            put("message", message)
            putJsonObject("fieldErrors") { }
        }
    }
    respondText(json.toString(), ContentType.Application.Json, status)
}
