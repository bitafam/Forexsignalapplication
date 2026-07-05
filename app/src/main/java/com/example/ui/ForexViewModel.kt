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
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ForexRepository
import com.example.data.SignalEntity
import com.example.data.UserEntity
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
    private val _language = MutableStateFlow("fa") // Default to Farsi as requested for Iranian users
    val language: StateFlow<String> = _language.asStateFlow()

    // Active screen navigation
    private val _currentScreen = MutableStateFlow("dashboard") // dashboard, login, register, payment, admin_panel
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Current Session State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Signals from Database
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
    val registerRole = MutableStateFlow("USER") // USER or ADMIN

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    // Selected payment option details
    private val _selectedPaymentPlan = MutableStateFlow<PaymentPlan?>(null)
    val selectedPaymentPlan: StateFlow<PaymentPlan?> = _selectedPaymentPlan.asStateFlow()

    private val _currentPaymentMethod = MutableStateFlow<String?>(null) // "RIAL", "CRYPTO", "GOOGLE_PAY", "CARD"
    val currentPaymentMethod: StateFlow<String?> = _currentPaymentMethod.asStateFlow()

    // Real-time Push Notification Simulation State
    private val _liveNotification = MutableStateFlow<SignalEntity?>(null)
    val liveNotification: StateFlow<SignalEntity?> = _liveNotification.asStateFlow()

    // Admin Mode Signal Builder Input State
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

    init {
        viewModelScope.launch {
            repository.prepopulateInitialSignals()
            
            // Listen to database signals. If a new signal is inserted by another source, trigger simulated push
            var previousCount = -1
            repository.allSignals.collect { list ->
                if (previousCount != -1 && list.size > previousCount) {
                    // New signal added! Find the newest one and trigger notification
                    val newest = list.firstOrNull()
                    if (newest != null) {
                        triggerInstantPushNotification(newest)
                    }
                }
                previousCount = list.size
            }
        }
    }

    fun setLanguage(lang: String) {
        _language.value = lang
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
        _authError.value = null
        _authSuccessMessage.value = null
    }

    fun setFilter(filter: String) {
        _signalFilter.value = filter
    }

    fun startPaymentFlow(plan: PaymentPlan) {
        _selectedPaymentPlan.value = plan
        _currentPaymentMethod.value = if (_language.value == "fa") "RIAL" else "GOOGLE_PAY"
        setScreen("payment")
    }

    fun setPaymentMethod(method: String) {
        _currentPaymentMethod.value = method
    }

    // Auth Actions
    fun handleLogin() {
        viewModelScope.launch {
            _authError.value = null
            val email = loginEmail.value.trim()
            val pass = loginPassword.value.trim()

            if (email.isEmpty() || pass.isEmpty()) {
                _authError.value = if (_language.value == "fa") "لطفاً تمام فیلدها را پر کنید" else "Please fill all fields"
                return@launch
            }

            var authenticated = repository.authenticateUser(email, pass)
            if (authenticated != null) {
                // If it's the specific requested admin email, guarantee they are ADMIN!
                if (email.lowercase() == "asalary40@gmail.com" && authenticated.role != "ADMIN") {
                    repository.setAdminRole(email, true)
                    authenticated = repository.getUser(email)
                }
                _currentUser.value = authenticated
                _authSuccessMessage.value = if (_language.value == "fa") "ورود با موفقیت انجام شد!" else "Successfully logged in!"
                delay(1200)
                setScreen("dashboard")
                // Clear fields
                loginEmail.value = ""
                loginPassword.value = ""
            } else {
                _authError.value = if (_language.value == "fa") "ایمیل یا رمز عبور نادرست است" else "Invalid email or password"
            }
        }
    }

    fun handleRegister() {
        viewModelScope.launch {
            _authError.value = null
            val email = registerEmail.value.trim()
            val pass = registerPassword.value.trim()
            
            // Automatic role detection based on specified email:
            val role = if (email.lowercase() == "asalary40@gmail.com") "ADMIN" else "USER"

            if (email.isEmpty() || pass.isEmpty()) {
                _authError.value = if (_language.value == "fa") "لطفاً تمام فیلدها را پر کنید" else "Please fill all fields"
                return@launch
            }

            if (pass.length < 6) {
                _authError.value = if (_language.value == "fa") "رمز عبور باید حداقل ۶ کاراکتر باشد" else "Password must be at least 6 characters"
                return@launch
            }

            val success = repository.registerUser(email, pass, role)
            if (success) {
                _authSuccessMessage.value = if (_language.value == "fa") "ثبت نام با موفقیت انجام شد! در حال ورود..." else "Registered successfully! Logging in..."
                delay(1200)
                // Automatically login
                val authenticated = repository.authenticateUser(email, pass)
                _currentUser.value = authenticated
                setScreen("dashboard")
                // Clear fields
                registerEmail.value = ""
                registerPassword.value = ""
            } else {
                _authError.value = if (_language.value == "fa") "این ایمیل قبلاً ثبت شده است" else "Email is already registered"
            }
        }
    }

    fun handleLogout() {
        _currentUser.value = null
        setScreen("dashboard")
    }

    // Payment completion
    fun completeSubscription() {
        val user = _currentUser.value
        val plan = _selectedPaymentPlan.value
        if (user == null) {
            _authError.value = if (_language.value == "fa") "لطفاً ابتدا ثبت نام کنید یا وارد حساب کاربری شوید" else "Please login or register first"
            setScreen("login")
            return
        }
        if (plan == null) return

        viewModelScope.launch {
            val success = repository.subscribeVip(user.email, plan.durationDays)
            if (success) {
                // Refresh user state
                val updatedUser = repository.getUser(user.email)
                _currentUser.value = updatedUser
                _authSuccessMessage.value = if (_language.value == "fa") "اشتراک VIP فعال شد!" else "VIP Membership Activated!"
                delay(1500)
                setScreen("dashboard")
            }
        }
    }

    // Admin Signal Actions
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
            
            // Reset fields & cancel editing
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

    // Real-time Push Notification Simulation Trigger
    private fun triggerInstantPushNotification(signal: SignalEntity) {
        viewModelScope.launch {
            _liveNotification.value = signal
            
            // Post an actual Android system notification on the status bar
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
            
            // Play physical buzzer beep sound and trigger vibration
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
            } catch (e: Exception) {
                // Log and ignore if audio not supported
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
                // Ignore vibration if failed or permissions missing
            }

            // Keep notification on screen for 6 seconds, then dismiss
            delay(6000)
            _liveNotification.value = null
        }
    }

    fun dismissLiveNotification() {
        _liveNotification.value = null
    }

    // Direct trigger button for demo purposes
    fun simulateRandomSignalPublish() {
        viewModelScope.launch {
            val pairs = listOf("XAU/USD", "GBP/JPY", "USD/CAD", "ETH/USD")
            val selectedPair = pairs.random()
            val type = listOf("BUY", "SELL").random()
            val entry = if (selectedPair.contains("USD")) 1.2500 + (Math.random() * 0.1) else 2350.0 + (Math.random() * 50.0)
            val isVip = listOf(true, false).random()
            
            val formattedEntry = String.format("%.4f", entry).toDouble()
            val change = if (type == "BUY") 0.0080 else -0.0080
            val tp1 = String.format("%.4f", entry + change).toDouble()
            val tp2 = String.format("%.4f", entry + (change * 1.8)).toDouble()
            val sl = String.format("%.4f", entry - change).toDouble()

            val simulated = SignalEntity(
                pair = selectedPair,
                type = type,
                entryPrice = formattedEntry,
                tp1 = tp1,
                tp2 = tp2,
                sl = sl,
                timeframe = listOf("M15", "H1", "H4").random(),
                isVip = isVip,
                analysis = "Simulated automated algorithmic indicator breakout.",
                adminName = "Alpha AI Engine"
            )
            repository.insertSignal(simulated)
        }
    }
}

// Subscription Plans
data class PaymentPlan(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val durationDays: Int,
    val priceFa: String,
    val priceEn: String
)

val VIP_PLANS = listOf(
    PaymentPlan("1m", "اشتراک ۱ ماهه", "1 Month Access", 30, "۱,۲۰۰,۰۰۰ ریال", "$19.99"),
    PaymentPlan("3m", "اشتراک ۳ ماهه (پیشنهاد طلایی)", "3 Months Access (Gold Offer)", 90, "۳,۰۰۰,۰۰۰ ریال", "$49.99"),
    PaymentPlan("1y", "اشتراک سالانه (ویژه)", "1 Year Access (Infinite)", 365, "۹,۹۰۰,۰۰۰ ریال", "$149.99")
)
