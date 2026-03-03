package com.aitutor.user.service

import com.aitutor.user.repository.UserRecord
import com.aitutor.user.repository.UserRepository
import java.util.UUID

class UserService(private val userRepository: UserRepository) {

    suspend fun createUser(email: String?): UserRecord {
        return userRepository.create(email)
    }

    suspend fun getUser(id: UUID): UserRecord {
        return userRepository.findById(id)
            ?: throw NoSuchElementException("User not found: $id")
    }
}
