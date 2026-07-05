package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object SupabaseService {

    private const val TAG = "SupabaseService"

    // Supabase Credentials provided by the user
    private const val SUPABASE_URL = "https://qeqazdbiabpwkxjvhzfy.supabase.co"
    private const val ANON_KEY = "sb_publishable_tv5XN-AisLWlesqjWgRtoA_19Lbzdw6"

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Helper for making standard GET request
    private suspend fun makeGetRequest(path: String, accessToken: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$SUPABASE_URL$path"
                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                    .get()

                val response = client.newCall(requestBuilder.build()).execute()
                val body = response.body?.string()
                if (response.isSuccessful) {
                    body
                } else {
                    Log.e(TAG, "GET request to $path failed with code ${response.code}: $body")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "GET request to $path exception: ${e.message}", e)
                null
            }
        }
    }

    // Helper for making standard POST request
    private suspend fun makePostRequest(path: String, jsonBody: String, accessToken: String? = null, preferReturn: Boolean = false): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$SUPABASE_URL$path"
                val requestBuilder = Request.Builder()
                    .url(url)
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toRequestBody(jsonMediaType))

                if (preferReturn) {
                    requestBuilder.addHeader("Prefer", "return=representation")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val body = response.body?.string()
                if (response.isSuccessful) {
                    body
                } else {
                    Log.e(TAG, "POST request to $path failed with code ${response.code}: $body")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "POST request to $path exception: ${e.message}", e)
                null
            }
        }
    }

    // Helper for making standard PATCH request
    private suspend fun makePatchRequest(path: String, jsonBody: String, accessToken: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$SUPABASE_URL$path"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .patch(jsonBody.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful) {
                    true
                } else {
                    Log.e(TAG, "PATCH request to $path failed with code ${response.code}: $body")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "PATCH request to $path exception: ${e.message}", e)
                false
            }
        }
    }

    // Helper for making standard DELETE request
    private suspend fun makeDeleteRequest(path: String, accessToken: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$SUPABASE_URL$path"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer ${accessToken ?: ANON_KEY}")
                    .delete()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful) {
                    true
                } else {
                    Log.e(TAG, "DELETE request to $path failed with code ${response.code}: $body")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "DELETE request to $path exception: ${e.message}", e)
                false
            }
        }
    }

    // ==========================================
    // 1. SUPABASE AUTH ACTIONS
    // ==========================================

    data class AuthResponse(
        val userId: String,
        val email: String,
        val accessToken: String?,
        val error: String? = null
    )

    suspend fun signUp(email: String, password: String): AuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }.toString()

                val responseBody = makePostRequest("/auth/v1/signup", jsonPayload)
                if (responseBody == null) {
                    return@withContext AuthResponse("", "", null, "Registration failed on server")
                }

                val json = JSONObject(responseBody)
                if (json.has("error_description")) {
                    return@withContext AuthResponse("", "", null, json.getString("error_description"))
                }
                if (json.has("error")) {
                    val errObj = json.optJSONObject("error")
                    val errMsg = errObj?.optString("message") ?: json.optString("error")
                    return@withContext AuthResponse("", "", null, errMsg)
                }

                val userObj = json.optJSONObject("user") ?: json
                val id = userObj.optString("id", "")
                val emailVal = userObj.optString("email", email)
                val token = json.optString("access_token", null)

                // Try to create profile in profiles table just in case trigger is slow or not configured
                val isFirstUser = email.lowercase() == "asalary40@gmail.com"
                val role = if (isFirstUser) "admin" else "user"
                createInitialProfile(id, role)

                AuthResponse(id, emailVal, token)
            } catch (e: Exception) {
                Log.e(TAG, "signUp exception: ${e.message}", e)
                AuthResponse("", "", null, e.message ?: "Unknown registration error")
            }
        }
    }

    suspend fun signIn(email: String, password: String): AuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }.toString()

                val responseBody = makePostRequest("/auth/v1/token?grant_type=password", jsonPayload)
                if (responseBody == null) {
                    return@withContext AuthResponse("", "", null, "Invalid credentials or connection issue")
                }

                val json = JSONObject(responseBody)
                if (json.has("error_description")) {
                    return@withContext AuthResponse("", "", null, json.getString("error_description"))
                }
                if (json.has("error")) {
                    val errMsg = json.optString("error_description", json.getString("error"))
                    return@withContext AuthResponse("", "", null, errMsg)
                }

                val accessToken = json.getString("access_token")
                val userObj = json.getJSONObject("user")
                val id = userObj.getString("id")
                val emailVal = userObj.getString("email")

                AuthResponse(id, emailVal, accessToken)
            } catch (e: Exception) {
                Log.e(TAG, "signIn exception: ${e.message}", e)
                AuthResponse("", "", null, e.message ?: "Authentication failed")
            }
        }
    }

    private suspend fun createInitialProfile(id: String, role: String): Boolean {
        val payload = JSONObject().apply {
            put("id", id)
            put("role", role)
            put("is_vip", false)
            put("vip_expiry", null)
        }.toString()
        val result = makePostRequest("/rest/v1/profiles", payload)
        return result != null
    }

    // ==========================================
    // 2. PROFILES OPERATIONS (Real-time compatible)
    // ==========================================

    data class SupabaseProfile(
        val id: String,
        val role: String, // 'admin' or 'user'
        val isVip: Boolean,
        val vipExpiry: String? // Timestamptz ISO string
    )

    suspend fun getProfile(userId: String, accessToken: String? = null): SupabaseProfile? {
        val responseBody = makeGetRequest("/rest/v1/profiles?id=eq.$userId", accessToken)
        if (responseBody == null) return null
        return try {
            val arr = JSONArray(responseBody)
            if (arr.length() > 0) {
                val obj = arr.getJSONObject(0)
                SupabaseProfile(
                    id = obj.getString("id"),
                    role = obj.optString("role", "user"),
                    isVip = obj.optBoolean("is_vip", false),
                    vipExpiry = if (obj.isNull("vip_expiry")) null else obj.getString("vip_expiry")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse profile error: ${e.message}", e)
            null
        }
    }

    // ==========================================
    // 3. SUBSCRIPTION PACKAGES CRUD (ADMIN & USER)
    // ==========================================

    data class SupabasePackage(
        val id: String,
        val name: String,
        val durationDays: Int,
        val priceTether: Double
    )

    suspend fun getPackages(): List<SupabasePackage> {
        val responseBody = makeGetRequest("/rest/v1/subscription_packages?order=duration_days.asc")
        if (responseBody == null) return emptyList()
        return try {
            val list = mutableListOf<SupabasePackage>()
            val arr = JSONArray(responseBody)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SupabasePackage(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        durationDays = obj.getInt("duration_days"),
                        priceTether = obj.optDouble("price_tether", 0.0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Parse packages error: ${e.message}", e)
            emptyList()
        }
    }

    // ADMIN Action: Edit package price
    suspend fun updatePackagePrice(packageId: String, price: Double): Boolean {
        val payload = JSONObject().apply {
            put("price_tether", price)
        }.toString()
        return makePatchRequest("/rest/v1/subscription_packages?id=eq.$packageId", payload)
    }

    // ==========================================
    // 4. DISCOUNT COUPONS CRUD (ADMIN & USER)
    // ==========================================

    data class SupabaseCoupon(
        val id: String,
        val code: String,
        val discountPercent: Double,
        val expiresAt: String?,
        val isActive: Boolean
    )

    suspend fun getCoupons(): List<SupabaseCoupon> {
        val responseBody = makeGetRequest("/rest/v1/coupons?order=code.asc")
        if (responseBody == null) return emptyList()
        return try {
            val list = mutableListOf<SupabaseCoupon>()
            val arr = JSONArray(responseBody)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SupabaseCoupon(
                        id = obj.getString("id"),
                        code = obj.getString("code"),
                        discountPercent = obj.optDouble("discount_percent", 0.0),
                        expiresAt = if (obj.isNull("expires_at")) null else obj.getString("expires_at"),
                        isActive = obj.optBoolean("is_active", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Parse coupons error: ${e.message}", e)
            emptyList()
        }
    }

    // ADMIN Action: Create discount coupon
    suspend fun createCoupon(code: String, discountPercent: Double, expiresAtISO: String?, isActive: Boolean): Boolean {
        val payload = JSONObject().apply {
            put("code", code)
            put("discount_percent", discountPercent)
            put("expires_at", expiresAtISO)
            put("is_active", isActive)
        }.toString()
        val response = makePostRequest("/rest/v1/coupons", payload)
        return response != null
    }

    // ADMIN Action: Toggle coupon status
    suspend fun updateCouponStatus(couponId: String, isActive: Boolean): Boolean {
        val payload = JSONObject().apply {
            put("is_active", isActive)
        }.toString()
        return makePatchRequest("/rest/v1/coupons?id=eq.$couponId", payload)
    }

    // ADMIN Action: Delete coupon
    suspend fun deleteCoupon(couponId: String): Boolean {
        return makeDeleteRequest("/rest/v1/coupons?id=eq.$couponId")
    }

    // ==========================================
    // 5. TRON WALLETS CRUD (ADMIN & USER)
    // ==========================================

    data class SupabaseAdminWallet(
        val id: String, // 'TRC20'
        val networkName: String,
        val walletAddress: String
    )

    suspend fun getAdminWallet(): SupabaseAdminWallet? {
        val responseBody = makeGetRequest("/rest/v1/admin_wallets?id=eq.TRC20")
        if (responseBody == null) return null
        return try {
            val arr = JSONArray(responseBody)
            if (arr.length() > 0) {
                val obj = arr.getJSONObject(0)
                SupabaseAdminWallet(
                    id = obj.getString("id"),
                    networkName = obj.optString("network_name", "TRC20"),
                    walletAddress = obj.optString("wallet_address", "")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse wallet error: ${e.message}", e)
            null
        }
    }

    // ADMIN Action: Edit admin wallet address
    suspend fun updateAdminWalletAddress(address: String): Boolean {
        val payload = JSONObject().apply {
            put("wallet_address", address)
        }.toString()
        // If wallet doesn't exist, try to insert it, otherwise patch it!
        val exists = getAdminWallet() != null
        return if (exists) {
            makePatchRequest("/rest/v1/admin_wallets?id=eq.TRC20", payload)
        } else {
            val insertPayload = JSONObject().apply {
                put("id", "TRC20")
                put("network_name", "Tron (TRC20)")
                put("wallet_address", address)
            }.toString()
            makePostRequest("/rest/v1/admin_wallets", insertPayload) != null
        }
    }

    // ==========================================
    // 6. INVOKE EDGE FUNCTION (VERIFY TRON PAYMENT)
    // ==========================================

    suspend fun invokeVerifyTronPayment(txid: String, packageId: String, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("txid", txid)
                    put("packageId", packageId)
                    put("userId", userId)
                }.toString()

                val url = "$SUPABASE_URL/functions/v1/verify-tron-payment"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Authorization", "Bearer $ANON_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful) {
                    Log.d(TAG, "verify-tron-payment success response: $body")
                    true
                } else {
                    Log.e(TAG, "verify-tron-payment failed code ${response.code}: $body")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "verify-tron-payment exception: ${e.message}", e)
                false
            }
        }
    }
}
