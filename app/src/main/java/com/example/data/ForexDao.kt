package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ForexDao {

    // --- Signal Operations ---
    @Query("SELECT * FROM signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity): Long

    @Query("UPDATE signals SET status = :status WHERE id = :id")
    suspend fun updateSignalStatus(id: Int, status: String)

    @Query("DELETE FROM signals WHERE id = :id")
    suspend fun deleteSignal(id: Int)

    // --- User Operations ---
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUser(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isVip = :isVip, vipExpiresAt = :expiresAt WHERE email = :email")
    suspend fun updateUserVip(email: String, isVip: Boolean, expiresAt: Long)

    @Query("UPDATE users SET role = :role WHERE email = :email")
    suspend fun updateUserRole(email: String, role: String)
}
