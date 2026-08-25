package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: CachedAccount)

    @Query("SELECT * FROM cached_accounts WHERE username = :identifier OR email = :identifier LIMIT 1")
    suspend fun findByIdentifier(identifier: String): CachedAccount?

    // Auto-resume-session lookup (see HybridAccountRepository.getCurrentAccount) — keyed
    // by uid rather than username/email since that's all Firebase Auth's locally-persisted
    // session gives us when Firestore is unreachable.
    @Query("SELECT * FROM cached_accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CachedAccount?
}
