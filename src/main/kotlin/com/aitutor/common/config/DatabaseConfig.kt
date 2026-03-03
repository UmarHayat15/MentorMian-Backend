package com.aitutor.common.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

object DatabaseFactory {

    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun init(config: AppConfig) {
        val dataSource = createHikariDataSource(config.database)
        runMigrations(dataSource)
        Database.connect(dataSource)
        logger.info("Database connected and migrations applied")
    }

    private fun createHikariDataSource(dbConfig: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.url
            username = dbConfig.user
            password = dbConfig.password
            driverClassName = dbConfig.driver
            maximumPoolSize = dbConfig.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    private fun runMigrations(dataSource: HikariDataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        logger.info("Flyway migrations completed")
    }
}
