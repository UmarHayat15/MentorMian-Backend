package com.aitutor.user.repository

import com.aitutor.common.util.dbQuery
import com.aitutor.user.model.UserTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

data class UserRecord(
    val id: UUID,
    val email: String?,
    val createdAt: String
)

class UserRepository {

    suspend fun create(email: String?): UserRecord = dbQuery {
        val row = UserTable.insert {
            it[UserTable.email] = email
        }.resultedValues!!.first()
        row.toUserRecord()
    }

    suspend fun findById(id: UUID): UserRecord? = dbQuery {
        UserTable.selectAll()
            .where { UserTable.id eq id }
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    private fun ResultRow.toUserRecord() = UserRecord(
        id = this[UserTable.id],
        email = this[UserTable.email],
        createdAt = this[UserTable.createdAt].toString()
    )
}
