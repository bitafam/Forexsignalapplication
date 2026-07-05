package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pair: String,
    val type: String, // "BUY" or "SELL"
    val entryPrice: Double,
    val tp1: Double,
    val tp2: Double,
    val sl: Double,
    val timeframe: String, // e.g., "M15", "H1", "H4"
    val status: String = "ACTIVE", // "ACTIVE", "TP1_HIT", "TP2_HIT", "SL_HIT"
    val timestamp: Long = System.currentTimeMillis(),
    val isVip: Boolean = false,
    val analysis: String = "",
    val adminName: String = "Nexis Core"
)
