package id.ac.loopy.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object DatabaseFactory {
    fun fromEnvironment(): HikariDataSource {
        val url = requiredEnv("DATABASE_URL")
        val user = requiredEnv("DATABASE_USER")
        val password = requiredEnv("DATABASE_PASSWORD")

        return HikariDataSource(HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = 5
            connectionTimeout = 5_000
            validationTimeout = 2_000
        })
    }

    private fun requiredEnv(name: String): String =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("$name wajib diisi untuk backend.")
}

internal suspend fun <T> DataSource.read(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        connection.use(block)
    }

internal suspend fun <T> DataSource.transaction(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        connection.use { db ->
            db.autoCommit = false
            try {
                val result = block(db)
                ensureActive()
                db.commit()
                result
            } catch (failure: Throwable) {
                try {
                    db.rollback()
                } catch (rollbackFailure: SQLException) {
                    failure.addSuppressed(rollbackFailure)
                }
                throw failure
            }
        }
    }

internal fun Connection.update(sql: String, vararg arguments: Any?): Int =
    prepareStatement(sql).use { statement ->
        arguments.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
        statement.executeUpdate()
    }

internal fun <T> Connection.queryOne(
    sql: String,
    vararg arguments: Any?,
    map: (ResultSet) -> T,
): T? = prepareStatement(sql).use { statement ->
    arguments.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
    statement.executeQuery().use { rows ->
        if (rows.next()) map(rows) else null
    }
}
