package com.example.physi_lock.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Local offline-login cache: mirrors the Firestore users/{uid} profile plus a salted
// password hash, written after every successful online login/registration. Read only
// when Firebase Auth/Firestore is unreachable — see HybridAccountRepository.
@Entity(tableName = "cached_accounts")
data class CachedAccount(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val occupation: String?,
    val role: String,
    val createdAt: Long,
    val isActive: Boolean,
    val passwordHash: String,
    val passwordSalt: String,
    val lastSyncedAt: Long
)

fun CachedAccount.toAccount() = Account(
    id = id,
    username = username,
    fullName = fullName,
    email = email,
    occupation = occupation,
    role = role,
    createdAt = createdAt,
    isActive = isActive
)
