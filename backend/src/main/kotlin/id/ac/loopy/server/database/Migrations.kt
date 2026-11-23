package id.ac.loopy.server.database

import javax.sql.DataSource
import org.flywaydb.core.Flyway

fun migrateDatabase(database: DataSource) {
    Flyway.configure()
        .dataSource(database)
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate()
}
