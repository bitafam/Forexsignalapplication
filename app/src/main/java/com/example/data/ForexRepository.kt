package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForexRepository(private val forexDao: ForexDao) {

    val allSignals: Flow<List<SignalEntity>> = forexDao.getAllSignals()

    suspend fun insertSignal(signal: SignalEntity): Long {
        return withContext(Dispatchers.IO) {
            forexDao.insertSignal(signal)
        }
    }

    suspend fun updateSignalStatus(id: Int, status: String) {
        withContext(Dispatchers.IO) {
            forexDao.updateSignalStatus(id, status)
        }
    }

    suspend fun deleteSignal(id: Int) {
        withContext(Dispatchers.IO) {
            forexDao.deleteSignal(id)
        }
    }

    suspend fun getUser(email: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            forexDao.getUser(email)
        }
    }

    suspend fun registerUser(email: String, passwordHash: String, role: String = "USER", id: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            val existing = forexDao.getUser(email)
            if (existing != null) {
                // Update existing user with new ID and role
                forexDao.insertUser(UserEntity(email, passwordHash, isVip = existing.isVip, vipExpiresAt = existing.vipExpiresAt, role = role, id = id))
                true
            } else {
                forexDao.insertUser(UserEntity(email, passwordHash, isVip = false, vipExpiresAt = 0, role = role, id = id))
                true
            }
        }
    }

    suspend fun authenticateUser(email: String, passwordHash: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            val user = forexDao.getUser(email)
            if (user != null && user.passwordHash == passwordHash) {
                user
            } else {
                null
            }
        }
    }

    suspend fun subscribeVip(email: String, durationDays: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val user = forexDao.getUser(email)
            if (user != null) {
                val expiresAt = System.currentTimeMillis() + (durationDays * 24L * 60 * 60 * 1000)
                forexDao.updateUserVip(email, isVip = true, expiresAt = expiresAt)
                true
            } else {
                false
            }
        }
    }

    suspend fun setAdminRole(email: String, isAdmin: Boolean) {
        withContext(Dispatchers.IO) {
            val role = if (isAdmin) "ADMIN" else "USER"
            forexDao.updateUserRole(email, role)
        }
    }

    suspend fun prepopulateInitialSignals() {
        withContext(Dispatchers.IO) {
            // Check if database already has signals
            val current = allSignals.firstOrNull() ?: emptyList()
            if (current.isEmpty()) {
                // Populate default Free and VIP signals
                val initial = listOf(
                    SignalEntity(
                        pair = "EUR/USD",
                        type = "BUY",
                        entryPrice = 1.0850,
                        tp1 = 1.0890,
                        tp2 = 1.0950,
                        sl = 1.0810,
                        timeframe = "H1",
                        status = "ACTIVE",
                        isVip = false,
                        analysis = "Double bottom formation spotted on the H1 support zone. RSI is displaying a strong bullish divergence. Target 1 expects clean breakout.",
                        adminName = "Nexis Core"
                    ),
                    SignalEntity(
                        pair = "GBP/USD",
                        type = "SELL",
                        entryPrice = 1.2640,
                        tp1 = 1.2600,
                        tp2 = 1.2540,
                        sl = 1.2690,
                        timeframe = "M30",
                        status = "ACTIVE",
                        isVip = false,
                        analysis = "Ascending wedge resistance breakout failed. 30-minute EMA 50 crossed below EMA 200 indicating high-probability downside acceleration.",
                        adminName = "Alpha Algo"
                    ),
                    SignalEntity(
                        pair = "XAU/USD (GOLD)",
                        type = "BUY",
                        entryPrice = 2345.50,
                        tp1 = 2360.00,
                        tp2 = 2385.00,
                        sl = 2330.00,
                        timeframe = "H4",
                        status = "ACTIVE",
                        isVip = true,
                        analysis = "Strong ascending channel support retest. Geopolitical tensions spiking volume on Spot Gold. Volume profile suggests minimal resistance above 2355.",
                        adminName = "Vip Quant"
                    ),
                    SignalEntity(
                        pair = "BTC/USD (BITCOIN)",
                        type = "SELL",
                        entryPrice = 67200.00,
                        tp1 = 66000.00,
                        tp2 = 64500.00,
                        sl = 68500.00,
                        timeframe = "D1",
                        status = "ACTIVE",
                        isVip = true,
                        analysis = "D1 Head & Shoulders pattern completed right at the key weekly resistance. High exchange inflows point to imminent retail distributions.",
                        adminName = "Vip Quant"
                    )
                )
                for (sig in initial) {
                    forexDao.insertSignal(sig)
                }

                // Create a default admin user for quick testing: admin@nexis.com / admin123
                if (forexDao.getUser("admin@nexis.com") == null) {
                    forexDao.insertUser(
                        UserEntity(
                            email = "admin@nexis.com",
                            passwordHash = "admin123",
                            isVip = true,
                            vipExpiresAt = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000,
                            role = "ADMIN"
                        )
                    )
                }

                // Preseed requested admin: asalary40@gmail.com / admin123
                if (forexDao.getUser("asalary40@gmail.com") == null) {
                    forexDao.insertUser(
                        UserEntity(
                            email = "asalary40@gmail.com",
                            passwordHash = "admin123",
                            isVip = true,
                            vipExpiresAt = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000,
                            role = "ADMIN"
                        )
                    )
                }
                
                // Create a default regular VIP user: user@vip.com / vip123
                if (forexDao.getUser("user@vip.com") == null) {
                    forexDao.insertUser(
                        UserEntity(
                            email = "user@vip.com",
                            passwordHash = "vip123",
                            isVip = true,
                            vipExpiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
                            role = "USER"
                        )
                    )
                }
            }
        }
    }
}
