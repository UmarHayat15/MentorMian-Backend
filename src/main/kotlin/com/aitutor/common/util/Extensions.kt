package com.aitutor.common.util

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction { block() }

fun String.toUUID(): UUID = UUID.fromString(this)
