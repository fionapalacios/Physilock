package com.example.physi_lock.data

import com.google.firebase.firestore.Exclude

// Firestore-backed account profile, stored at users/{uid} (uid = Firebase Auth UID).
// `id` mirrors the document ID and is excluded from serialization — always repopulate
// it via `.copy(id = document.id)` after reading, never trust a stored `id` field.
// Firebase Auth owns email/password verification; this doc holds everything Auth doesn't.
data class Account(
    @get:Exclude val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val occupation: String? = null,
    val role: String = Role.USER,        // Role.USER or Role.ADMIN
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
