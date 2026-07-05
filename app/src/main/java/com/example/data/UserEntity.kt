package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val passwordHash: String,
    val isVip: Boolean = false,
    val vipExpiresAt: Long = 0,
    val role: String = "USER", // "USER" or "ADMIN"
    val id: String = "" // Supabase UUID
)
