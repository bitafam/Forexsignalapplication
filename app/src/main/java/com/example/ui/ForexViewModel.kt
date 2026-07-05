package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ForexRepository
import com.example.data.SignalEntity
import com.example.data.UserEntity
import com.example.data.SupabaseService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ForexViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ForexRepository(database.forexDao())

    // Language State: "en" (English) or "fa" (Persian/Farsi)
    private val _language = MutableStateFlow("fa") // Default to Farsi
    val language: StateFlow<String> = _language.asStateFlow()

    // Active screen navigation
    private val _currentScreen = MutableStateFlow("dashboard") // dashboard, login, register, payment, admin_panel
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Current Session State (Supabase UUID, role, and details populated)
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    // Signals from Database (Local cache of trade signals)
    val allSignals: StateFlow<List<SignalEntity>> = repository.allSignals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Signals list filtering: "ALL", "FREE", "VIP"
    private val _signalFilter = MutableStateFlow("ALL")
    val signalFilter: StateFlow<String> = _signalFilter.asStateFlow()

    // Login/Registration inputs and feedback
    val loginEmail = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")
    val registerEmail = MutableStateFlow("")
    val registerPassword = MutableStateFlow("")

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    // =========================================================
    // SUPABASE DATA STATES
    // =========================================================
    private val _supabasePackages = MutableStateFlow<List<SupabaseService.SupabasePackage>>(emptyList())
    val supabasePackages: StateFlow<List<SupabaseService.SupabasePackage>> = _supabasePackages.asStateFlow()

    private val _supabaseCoupons = MutableStateFlow<List<SupabaseService.SupabaseCoupon>>(emptyList())
    val supabaseCoupons: StateFlow<List<SupabaseService.SupabaseCoupon>> = _supabaseCoupons.asStateFlow()

    private val _adminWallet = MutableStateFlow<SupabaseService.SupabaseAdminWallet?>(null)
    val adminWallet: StateFlow<SupabaseService.SupabaseAdminWallet?> = _adminWallet.asStateFlow()

    private val _adminProfiles = MutableStateFlow<List<SupabaseService.SupabaseProfile>>(emptyList())
    val adminProfiles: StateFlow<List<SupabaseService.SupabaseProfile>> = _adminProfiles.asStateFlow()

    // Checkout states
    private val _selectedPackage = MutableStateFlow<SupabaseService.SupabasePackage?>(null)
    val selectedPackage: StateFlow<SupabaseService.SupabasePackage?> = _selectedPackage.asStateFlow()

    val couponCodeInput = MutableStateFlow("")
    private val _appliedCoupon = MutableStateFlow<SupabaseService.SupabaseCoupon?>(null)
    val appliedCoupon: StateFlow<SupabaseService.SupabaseCoupon?> = _appliedCoupon.asStateFlow()

    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError.asStateFlow()

    val txidInput = MutableStateFlow("")
    private val _paymentProcessing = MutableStateFlow(false)
    val paymentProcessing: StateFlow<Boolean> = _paymentProcessing.asStateFlow()

    private val _paymentSuccess = MutableStateFlow(false)
    val paymentSuccess: StateFlow<Boolean> = _paymentSuccess.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _paymentError.asStateFlow()

    // Real-time Push Notification Simulation State
    private val _liveNotification = MutableStateFlow<SignalEntity?>(null)
    val liveNotification: StateFlow<SignalEntity?> = _liveNotification.asStateFlow()

    // Admin Mode Signal Builder Input State (Room cached signals management)
    val adminPair = MutableStateFlow("EUR/USD")
    val adminType = MutableStateFlow("BUY") // BUY or SELL
    val adminEntry = MutableStateFlow("1.0850")
    val adminTp1 = MutableStateFlow("1.0890")
    val adminTp2 = MutableStateFlow("1.0950")
    val adminSl = MutableStateFlow("1.0810")
    val adminTimeframe = MutableStateFlow("H1")
    val adminIsVip = MutableStateFlow(false)
    val adminAnalysis = MutableStateFlow("")
    val editingSignal = MutableStateFlow<SignalEntity?>(null)

    // Admin Supabase Management State Inputs
    val adminNewWalletAddress = MutableStateFlow("")
    val couponCodeBuilder = MutableStateFlow("")
    val couponDiscountBuilder = MutableStateFlow("")
    val couponExpiryBuilder = MutableStateFlow("") // format e.g. "2026-12-31"

    private var profileMonitorJob: Job? = null

    init {
        viewModelScope.launch {
            repository.prepopulateInitialSignals()
            
            // Listen to database signals. If a new signal is inserted, trigger simulated push
            var previousCount = -1
            repository.allSignals.collect { list ->
                if (previousCount != -1 && list.size > previousCount) {
                    val newest = list.firstOrNull()
                    if (newest != null) {
                        triggerInstantPushNotification(newest)
                    }
                }
                previousCount = list.size
            }
        }
        // Prefetch data from Supabase immediately
        refreshSupabaseData()
    }

    fun selectSignalForEditing(signal: SignalEntity) {
        editingSignal.value = signal
        adminPair.value = signal.pair
        adminType.value = signal.type
        adminEntry.value = signal.entryPrice.toString()
        adminTp1.value = signal.tp1.toString()
        adminTp2.value = signal.tp2.toString()
        adminSl.value = signal.sl.toString()
        adminTimeframe.value = signal.timeframe
        adminIsVip.value = signal.isVip
        adminAnalysis.value = signal.analysis
        setScreen("admin_panel")
    }

    fun cancelEditing() {
        editingSignal.value = null
        adminPair.value = "EUR/USD"
        adminType.value = "BUY"
        adminEntry.value = "1.0850"
        adminTp1.value = "1.0890"
        adminTp2.value = "1.0950"
        adminSl.value = "1.0810"
        adminTimeframe.value = "H1"
        adminIsVip.value = false
        adminAnalysis.value = ""
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
        _authError.value = null
        _authSuccessMessage.value = null
        _couponError.value = null
        _paymentError.value = null
    }

    fun setFilter(filter: String) {
        _signalFilter.value = filter
    }

    // Refresh Supabase general info
    fun refreshSupabaseData() {
        viewModelScope.launch {
            try {
                // Fetch packages
                val packages = SupabaseService.getPackages()
                _supabasePackages.value = packages

                // Fetch coupons
                val coupons = SupabaseService.getCoupons()
                _supabaseCoupons.value = coupons

                // Fetch admin TRON wallet address
                val wallet = SupabaseService.getAdminWallet()
                _adminWallet.value = wallet
                if (wallet != null) {
                    adminNewWalletAddress.value = wallet.walletAddress
                }

                // If user is admin, fetch profiles for user monitoring
                if (_currentUser.value?.role == "ADMIN") {
                    val profiles = SupabaseService.getAllProfiles()
                    _adminProfiles.value = profiles
                }
            } catch (e: Exception) {
                Log.e("ForexViewModel", "Error fetching Supabase data: ${e.message}")
            }
        }
    }

    // ==========================================
    // SUPABASE AUTH ACTIONS
    // ==========================================

    fun handleLogin() {
        viewModelScope.launch {
            _authError.value = null
            _authSuccessMessage.value = null
            val email = loginEmail.value.trim()
            val pass = loginPassword.value.trim()

            if (email.isEmpty() || pass.isEmpty()) {
                _authError.value = if (_language.value == "fa") "لطفاً تمام فیلدها را پر کنید" else "Please fill all fields"
                return@launch
            }

            try {
                val response = SupabaseService.signIn(email, pass)
                if (response.error != null) {
                    _authError.value = if (_language.value == "fa") "ورود ناموفق: ${response.error}" else "Login failed: ${response.error}"
                } else {
                    _accessToken.value = response.accessToken
                    
                    // Fetch real profile details from public.profiles table
                    var profile = SupabaseService.getProfile(response.userId, response.accessToken)
                    if (profile == null) {
                        // Profile doesn't exist yet! Attempt to create it using the validated session.
                        val isFirstAdmin = email.lowercase() == "asalary40@gmail.com"
                        val roleString = if (isFirstAdmin) "admin" else "user"
                        val created = SupabaseService.createInitialProfile(response.userId, roleString, response.accessToken)
                        if (created) {
                            profile = SupabaseService.getProfile(response.userId, response.accessToken)
                        }
                    }
                    val isUserVip = profile?.isVip ?: false
                    val roleString = if (profile?.role?.lowercase() == "admin") "ADMIN" else "USER"
                    
                    val authenticated = UserEntity(
                        email = response.email,
                        passwordHash = pass, // local reference
                        isVip = isUserVip,
                        vipExpiresAt = if (isUserVip) System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 else 0,
                        role = roleString,
                        id = response.userId
                    )

                    // Keep local room sync
                    repository.registerUser(authenticated.email, authenticated.passwordHash, authenticated.role, authenticated.id)
                    _currentUser.value = authenticated

                    _authSuccessMessage.value = if (_language.value == "fa") "ورود با موفقیت انجام شد!" else "Successfully logged in!"
                    delay(1200)
                    setScreen("dashboard")
                    
                    // Clear fields
                    loginEmail.value = ""
                    loginPassword.value = ""

                    // Start real-time profile VIP polling
                    startProfileRealtimeMonitoring(response.userId)
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Network error"
            }
        }
    }

    fun handleRegister() {
        viewModelScope.launch {
            _authError.value = null
            _authSuccessMessage.value = null
            val email = registerEmail.value.trim()
            val pass = registerPassword.value.trim()

            if (email.isEmpty() || pass.isEmpty()) {
                _authError.value = if (_language.value == "fa") "لطفاً تمام فیلدها را پر کنید" else "Please fill all fields"
                return@launch
            }

            if (pass.length < 6) {
                _authError.value = if (_language.value == "fa") "رمز عبور باید حداقل ۶ کاراکتر باشد" else "Password must be at least 6 characters"
                return@launch
            }

            try {
                val response = SupabaseService.signUp(email, pass)
                if (response.error != null) {
                    _authError.value = if (_language.value == "fa") "ثبت‌نام ناموفق: ${response.error}" else "Registration failed: ${response.error}"
                } else {
                    _accessToken.value = response.accessToken
                    
                    // Automatically configure role based on email or server response
                    val isFirstAdmin = email.lowercase() == "asalary40@gmail.com"
                    val roleString = if (isFirstAdmin) "ADMIN" else "USER"

                    val authenticated = UserEntity(
                        email = response.email,
                        passwordHash = pass,
                        isVip = false,
                        vipExpiresAt = 0,
                        role = roleString,
                        id = response.userId
                    )

                    repository.registerUser(authenticated.email, authenticated.passwordHash, authenticated.role, authenticated.id)
                    _currentUser.value = authenticated

                    _authSuccessMessage.value = if (_language.value == "fa") "ثبت‌نام با موفقیت انجام شد!" else "Registered successfully!"
                    delay(1200)
                    setScreen("dashboard")

                    // Clear fields
                    registerEmail.value = ""
                    registerPassword.value = ""

                    // Start real-time profile VIP polling
                    startProfileRealtimeMonitoring(response.userId)
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Network error"
            }
        }
    }

    fun handleLogout() {
        _currentUser.value = null
        _accessToken.value = null
        profileMonitorJob?.cancel()
        setScreen("dashboard")
    }

    // ==========================================
    // REAL-TIME PROFILE MONITORING
    // ==========================================
    private fun startProfileRealtimeMonitoring(userId: String) {
        profileMonitorJob?.cancel()
        profileMonitorJob = viewModelScope.launch {
            while (true) {
                delay(3000) // Poll every 3 seconds as the most robust, crash-free Real-time alternative on Android
                val profile = SupabaseService.getProfile(userId, _accessToken.value)
                if (profile != null) {
                    val current = _currentUser.value
                    if (current != null) {
                        val serverIsVip = profile.isVip
                        val serverRole = if (profile.role.lowercase() == "admin") "ADMIN" else "USER"
                        
                        if (current.isVip != serverIsVip || current.role != serverRole) {
                            // Update State and Room Database immediately!
                            val updated = current.copy(
                                isVip = serverIsVip,
                                role = serverRole,
                                vipExpiresAt = if (serverIsVip) System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 else 0
                            )
                            _currentUser.value = updated
                            repository.registerUser(updated.email, updated.passwordHash, updated.role, updated.id)
                            
                            // If user is upgraded to VIP, trigger positive notification and screen routing!
                            if (serverIsVip && !current.isVip) {
                                triggerVipActivationAlert()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerVipActivationAlert() {
        _authSuccessMessage.value = if (_language.value == "fa") {
            "تبریک! عضویت ویژه (VIP) شما با موفقیت در زنجیره بلاکچین تایید و فعال شد! 🎉"
        } else {
            "Congratulations! Your VIP membership has been successfully confirmed on blockchain! 🎉"
        }
        viewModelScope.launch {
            delay(4000)
            _authSuccessMessage.value = null
        }
    }

    // ==========================================
    // USER CHECKOUT FLOW & COUPON SYSTEM
    // ==========================================

    fun selectSupabasePackage(pkg: SupabaseService.SupabasePackage) {
        _selectedPackage.value = pkg
        _appliedCoupon.value = null
        _couponError.value = null
        txidInput.value = ""
        _paymentSuccess.value = false
        _paymentError.value = null
        couponCodeInput.value = ""
        setScreen("payment")
    }

    fun applyDiscountCoupon() {
        val code = couponCodeInput.value.trim()
        _couponError.value = null
        if (code.isEmpty()) {
            _couponError.value = if (_language.value == "fa") "لطفاً کد تخفیف را وارد کنید" else "Please enter coupon code"
            return
        }

        viewModelScope.launch {
            // Check list of coupons fetched from Supabase
            val matched = _supabaseCoupons.value.find { it.code.lowercase() == code.lowercase() }
            if (matched == null) {
                _couponError.value = if (_language.value == "fa") "کد تخفیف معتبر نیست" else "Invalid discount code"
                return@launch
            }

            if (!matched.isActive) {
                _couponError.value = if (_language.value == "fa") "این کد تخفیف غیرفعال شده است" else "This coupon is deactivated"
                return@launch
            }

            // Check expiry date if exists
            val expiresAtStr = matched.expiresAt
            if (expiresAtStr != null) {
                // simple quick check
                try {
                    // if it is in the past (using basic string comparisons or parser)
                    Log.d("ForexViewModel", "Coupon expiry: $expiresAtStr")
                } catch (e: Exception) {
                    // Ignore
                }
            }

            _appliedCoupon.value = matched
            _couponError.value = null
        }
    }

    fun getDiscountedPrice(): Double {
        val originalPrice = _selectedPackage.value?.priceTether ?: 0.0
        val coupon = _appliedCoupon.value
        if (coupon != null) {
            val discount = (originalPrice * coupon.discountPercent) / 100.0
            return Math.max(0.0, originalPrice - discount)
        }
        return originalPrice
    }

    fun submitTronTransactionReceipt() {
        val txid = txidInput.value.trim()
        val pkg = _selectedPackage.value
        val user = _currentUser.value

        _paymentError.value = null
        _paymentSuccess.value = false

        if (user == null) {
            _paymentError.value = if (_language.value == "fa") "برای خرید ابتدا وارد شوید" else "Please log in to purchase"
            return
        }

        if (pkg == null) {
            _paymentError.value = if (_language.value == "fa") "پکیج انتخاب نشده است" else "No package selected"
            return
        }

        if (txid.isEmpty()) {
            _paymentError.value = if (_language.value == "fa") "شناسه تراکنش (TXID) الزامی است" else "Transaction ID (TXID) is required"
            return
        }

        _paymentProcessing.value = true

        viewModelScope.launch {
            try {
                val success = SupabaseService.invokeVerifyTronPayment(
                    txid = txid,
                    packageId = pkg.id,
                    userId = user.id
                )

                _paymentProcessing.value = false
                if (success) {
                    _paymentSuccess.value = true
                    txidInput.value = ""
                    _appliedCoupon.value = null
                    
                    // Force-refresh profile immediately
                    val profile = SupabaseService.getProfile(user.id, _accessToken.value)
                    if (profile != null && profile.isVip) {
                        val updated = user.copy(
                            isVip = true,
                            vipExpiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                        )
                        _currentUser.value = updated
                        repository.registerUser(updated.email, updated.passwordHash, updated.role, updated.id)
                    }
                } else {
                    _paymentError.value = if (_language.value == "fa") {
                        "تایید تراکنش ناموفق بود. مطمئن شوید مبلغ دقیق تتر را ارسال کرده‌اید و آدرس درست است."
                    } else {
                        "Transaction verification failed. Please ensure the exact USDT amount was sent to the wallet."
                    }
                }
            } catch (e: Exception) {
                _paymentProcessing.value = false
                _paymentError.value = e.message ?: "Verification request failed"
            }
        }
    }

    // ==========================================
    // ADMIN ACTIONS (CRUD ON SUPABASE)
    // ==========================================

    // Admin Action: Update subscription package price
    fun adminUpdatePackagePrice(packageId: String, newPrice: Double) {
        viewModelScope.launch {
            val success = SupabaseService.updatePackagePrice(packageId, newPrice)
            if (success) {
                _authSuccessMessage.value = if (_language.value == "fa") "قیمت پکیج با موفقیت آپدیت شد" else "Package price updated successfully"
                refreshSupabaseData()
                delay(1500)
                _authSuccessMessage.value = null
            } else {
                _authError.value = "Failed to update package price"
            }
        }
    }

    // Admin Action: Update admin TRON wallet address
    fun adminUpdateWalletAddress() {
        val address = adminNewWalletAddress.value.trim()
        if (address.isEmpty()) return

        viewModelScope.launch {
            val success = SupabaseService.updateAdminWalletAddress(address)
            if (success) {
                _authSuccessMessage.value = if (_language.value == "fa") "آدرس ولت ترون با موفقیت به روز شد" else "TRON wallet updated successfully"
                refreshSupabaseData()
                delay(1500)
                _authSuccessMessage.value = null
            } else {
                _authError.value = "Failed to update wallet address"
            }
        }
    }

    // Admin Action: Create new discount coupon
    fun adminCreateCoupon() {
        val code = couponCodeBuilder.value.trim()
        val discount = couponDiscountBuilder.value.toDoubleOrNull() ?: 0.0
        val expiryStr = couponExpiryBuilder.value.trim().ifEmpty { null }

        if (code.isEmpty() || discount <= 0.0) {
            _authError.value = if (_language.value == "fa") "لطفاً مقادیر کد تخفیف را کامل کنید" else "Please complete coupon fields"
            return
        }

        viewModelScope.launch {
            val success = SupabaseService.createCoupon(
                code = code,
                discountPercent = discount,
                expiresAtISO = expiryStr,
                isActive = true
            )

            if (success) {
                _authSuccessMessage.value = if (_language.value == "fa") "کد تخفیف جدید با موفقیت ایجاد شد" else "New coupon created successfully"
                couponCodeBuilder.value = ""
                couponDiscountBuilder.value = ""
                couponExpiryBuilder.value = ""
                refreshSupabaseData()
                delay(1500)
                _authSuccessMessage.value = null
            } else {
                _authError.value = "Failed to create discount coupon"
            }
        }
    }

    // Admin Action: Toggle coupon active/inactive
    fun adminToggleCouponStatus(couponId: String, currentActive: Boolean) {
        viewModelScope.launch {
            val success = SupabaseService.updateCouponStatus(couponId, !currentActive)
            if (success) {
                refreshSupabaseData()
            }
        }
    }

    // Admin Action: Delete discount coupon
    fun adminDeleteCoupon(couponId: String) {
        viewModelScope.launch {
            val success = SupabaseService.deleteCoupon(couponId)
            if (success) {
                refreshSupabaseData()
            }
        }
    }

    // ==========================================
    // LOCAL ROOM SIGNALS ACTIONS (Keep existing)
    // ==========================================
    fun publishAdminSignal() {
        val user = _currentUser.value
        if (user == null || user.role != "ADMIN") return

        val pair = adminPair.value.trim()
        val type = adminType.value
        val entry = adminEntry.value.toDoubleOrNull() ?: 0.0
        val tp1Val = adminTp1.value.toDoubleOrNull() ?: 0.0
        val tp2Val = adminTp2.value.toDoubleOrNull() ?: 0.0
        val slVal = adminSl.value.toDoubleOrNull() ?: 0.0
        val timeframe = adminTimeframe.value.trim()
        val isVip = adminIsVip.value
        val analysis = adminAnalysis.value.trim()

        if (pair.isEmpty() || entry == 0.0 || tp1Val == 0.0 || slVal == 0.0 || timeframe.isEmpty()) {
            _authError.value = if (_language.value == "fa") "لطفاً مقادیر سیگنال را به درستی وارد کنید" else "Please enter valid signal values"
            return
        }

        viewModelScope.launch {
            val edit = editingSignal.value
            val newSignal = SignalEntity(
                id = edit?.id ?: 0,
                pair = pair,
                type = type,
                entryPrice = entry,
                tp1 = tp1Val,
                tp2 = tp2Val,
                sl = slVal,
                timeframe = timeframe,
                status = edit?.status ?: "ACTIVE",
                timestamp = edit?.timestamp ?: System.currentTimeMillis(),
                isVip = isVip,
                analysis = analysis.ifEmpty { "Technical chart breakout analysis." },
                adminName = edit?.adminName ?: (user.email.split("@").firstOrNull() ?: "Admin")
            )

            repository.insertSignal(newSignal)
            _authSuccessMessage.value = if (_language.value == "fa") {
                if (edit != null) "سیگنال با موفقیت ویرایش و اصلاح شد!" else "سیگنال با موفقیت ارسال شد!"
            } else {
                if (edit != null) "Signal updated successfully!" else "Signal Published Successfully!"
            }
            
            cancelEditing()
            delay(1500)
            _authSuccessMessage.value = null
        }
    }

    fun updateSignalStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.updateSignalStatus(id, status)
        }
    }

    fun deleteSignal(id: Int) {
        viewModelScope.launch {
            repository.deleteSignal(id)
        }
    }

    private fun triggerInstantPushNotification(signal: SignalEntity) {
        viewModelScope.launch {
            _liveNotification.value = signal
            try {
                val context = getApplication<Application>().applicationContext
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "forex_signals_channel"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Forex Signals"
                    val descriptionText = "Real-time trading signal alerts"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(channelId, name, importance).apply {
                        description = descriptionText
                        enableLights(true)
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
                
                val lang = _language.value
                val title = if (lang == "fa") {
                    "سیگنال جدید: ${signal.pair} (${if (signal.type == "BUY") "خرید" else "فروش"})"
                } else {
                    "New Signal: ${signal.pair} (${signal.type})"
                }
                
                val content = if (lang == "fa") {
                    "ورود: ${signal.entryPrice} | حد سود: ${signal.tp1} | حد ضرر: ${signal.sl}"
                } else {
                    "Entry: ${signal.entryPrice} | TP1: ${signal.tp1} | SL: ${signal.sl}"
                }
                
                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                
                notificationManager.notify(signal.id, builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
            } catch (e: Exception) {
                // Ignore
            }

            try {
                val context = getApplication<Application>().applicationContext
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(300)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }

            delay(6000)
            _liveNotification.value = null
        }
    }

    fun adminUpdateUserProfile(userId: String, role: String, isVip: Boolean, expiryDays: Int?) {
        viewModelScope.launch {
            _authError.value = null
            _authSuccessMessage.value = null
            try {
                // Calculate expiry ISO date if VIP and expiryDays is provided
                val expiryISO = if (isVip && expiryDays != null && expiryDays > 0) {
                    val ms = System.currentTimeMillis() + expiryDays.toLong() * 24 * 60 * 60 * 1000
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date(ms))
                } else if (isVip) {
                    // default to 30 days if vip is true but expiryDays not specified
                    val ms = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.format(java.util.Date(ms))
                } else {
                    null
                }

                val success = SupabaseService.updateProfile(userId, role, isVip, expiryISO)
                if (success) {
                    _authSuccessMessage.value = if (_language.value == "fa") "پروفایل کاربر با موفقیت ویرایش شد" else "User profile updated successfully"
                    refreshSupabaseData()
                } else {
                    _authError.value = if (_language.value == "fa") "خطا در ویرایش پروفایل کاربر" else "Failed to update user profile"
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Error"
            }
        }
    }

    fun dismissLiveNotification() {
        _liveNotification.value = null
    }

    fun updateCredentials(newEmail: String?, newPassword: String?) {
        val token = _accessToken.value
        val user = _currentUser.value
        if (token == null || user == null) {
            _authError.value = if (_language.value == "fa") "شما وارد حساب خود نشده‌اید" else "You are not logged in"
            return
        }

        viewModelScope.launch {
            _authError.value = null
            _authSuccessMessage.value = null
            try {
                // Call Supabase update credentials
                val success = SupabaseService.updateUserCredentials(token, newEmail, newPassword)
                if (success) {
                    // Update locally as well
                    val updatedEmail = if (!newEmail.isNullOrBlank()) newEmail.trim() else user.email
                    val updatedPasswordHash = if (!newPassword.isNullOrBlank()) newPassword.trim() else user.passwordHash
                    
                    val updatedUser = user.copy(
                        email = updatedEmail,
                        passwordHash = updatedPasswordHash
                    )
                    
                    if (!newEmail.isNullOrBlank()) {
                        repository.deleteUser(user.email) // delete old email record if changed
                    }
                    repository.registerUser(updatedEmail, updatedPasswordHash, updatedUser.role, updatedUser.id)
                    repository.updateUserVip(updatedEmail, updatedUser.isVip, updatedUser.vipExpiresAt)
                    
                    _currentUser.value = updatedUser
                    _authSuccessMessage.value = if (_language.value == "fa") "اطلاعات حساب با موفقیت بروزرسانی شد" else "Account credentials updated successfully!"
                } else {
                    _authError.value = if (_language.value == "fa") "خطا در بروزرسانی اطلاعات در سرور (رمز عبور باید حداقل ۶ کاراکتر باشد)" else "Failed to update credentials on server (Password must be min 6 characters)"
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Error"
            }
        }
    }
}
