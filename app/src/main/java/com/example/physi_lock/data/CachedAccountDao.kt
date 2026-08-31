package com.example.physi_lock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: CachedAccount)

    // Admin Console's Manage User Accounts tab (demo mode) -- see AdminViewModel.accounts.
    @Query("SELECT * FROM cached_accounts ORDER BY username")
    fun getAll(): Flow<List<CachedAccount>>

    @Query("DELETE FROM cached_accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM cached_accounts WHERE username = :identifier OR email = :identifier LIMIT 1")
    suspend fun findByIdentifier(identifier: String): CachedAccount?

    // Auto-resume-session lookup (see HybridAccountRepository.getCurrentAccount) — keyed
    // by uid rather than username/email since that's all Firebase Auth's locally-persisted
    // session gives us when Firestore is unreachable.
    @Query("SELECT * FROM cached_accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CachedAccount?
}
