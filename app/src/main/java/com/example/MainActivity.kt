package com.example

import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SignalEntity
import com.example.data.UserEntity
import com.example.ui.ForexViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request runtime permission for status bar push notifications on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ForexViewModel = viewModel()
                val currentLanguage by viewModel.language.collectAsState()
                val layoutDirection = if (currentLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val currentUser by viewModel.currentUser.collectAsState()

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = CyberObsidian,
                        bottomBar = {
                            // High-End Premium Floating Glassmorphic Navigation Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                    .navigationBarsPadding() // Respect device safe area!
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(CyberSurface.copy(alpha = 0.92f))
                                    .border(BorderStroke(1.2.dp, Brush.linearGradient(
                                        colors = listOf(CyberPrimary.copy(alpha = 0.4f), CyberBorder.copy(alpha = 0.5f), CyberTertiary.copy(alpha = 0.2f))
                                    )), RoundedCornerShape(24.dp))
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. HOME
                                val homeActive = currentScreen == "dashboard"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (homeActive) CyberPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { viewModel.setScreen("dashboard") }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "Home",
                                            tint = if (homeActive) CyberPrimary else CyberTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (currentLanguage == "fa") "خانه" else "Home",
                                            color = if (homeActive) CyberPrimary else CyberTextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                // 2. BILLING (SUBSCRIPTION)
                                val billingActive = currentScreen == "payment"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (billingActive) CyberPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable {
                                            val currentPkg = viewModel.selectedPackage.value
                                            if (currentPkg == null) {
                                                val pkgs = viewModel.supabasePackages.value
                                                if (pkgs.isNotEmpty()) {
                                                    viewModel.selectSupabasePackage(pkgs.first())
                                                } else {
                                                    viewModel.setScreen("dashboard")
                                                }
                                            } else {
                                                viewModel.setScreen("payment")
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = "Billing",
                                            tint = if (billingActive) CyberPrimary else CyberTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (currentLanguage == "fa") "اشتراک ویژه" else "Billing",
                                            color = if (billingActive) CyberPrimary else CyberTextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                // 3. SETTINGS & PROFILE
                                val settingsActive = currentScreen == "settings" || currentScreen == "login" || currentScreen == "register"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (settingsActive) CyberPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                        .clickable { viewModel.setScreen("settings") }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = if (settingsActive) CyberPrimary else CyberTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (currentLanguage == "fa") "تنظیمات" else "Settings",
                                            color = if (settingsActive) CyberPrimary else CyberTextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Stunning cybernetic gradient glow background
                            GlowingBackground()

                            // Main Screen Router
                            when (currentScreen) {
                                "dashboard" -> DashboardScreen(viewModel)
                                "login" -> LoginScreen(viewModel)
                                "register" -> RegisterScreen(viewModel)
                                "payment" -> PaymentScreen(viewModel)
                                "settings" -> SettingsScreen(viewModel)
                                "admin_panel" -> AdminPanelScreen(viewModel)
                            }

                            // Real-time Push Notification Simulation Overlay
                            PushNotificationOverlay(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// Localized Strings Dictionary
object L10n {
    fun get(key: String, lang: String): String {
        val fa = mapOf(
            "app_title" to "سیگنال‌های هوشمند فارکس",
            "active_signals" to "سیگنال‌های فعال",
            "history_signals" to "ژورنال معاملاتی",
            "free" to "رایگان",
            "vip" to "VIP ویژه",
            "buy" to "خرید (BUY)",
            "sell" to "فروش (SELL)",
            "entry" to "نقطه ورود",
            "tp1" to "حد سود ۱",
            "tp2" to "حد سود ۲",
            "sl" to "حد ضرر",
            "timeframe" to "تایم‌فریم",
            "analysis" to "توضیحات و تحلیل تکنیکال",
            "no_signals" to "سیگنالی یافت نشد.",
            "login" to "ورود به حساب کاربری",
            "register" to "ثبت نام آنلاین کاربری",
            "logout" to "خروج از حساب",
            "email" to "پست الکترونیک (ایمیل)",
            "password" to "رمز عبور (حداقل ۶ کاراکتر)",
            "admin_panel" to "پنل فرمان ادمین",
            "publish" to "انتشار فوری سیگنال",
            "pair" to "جفت ارز معاملاتی",
            "is_vip" to "این سیگنال VIP است؟",
            "analysis_desc" to "شرح فنی و دلایل تحلیل تکنیکال...",
            "vip_badge" to "عضویت ویژه فعال",
            "upgrade_button" to "خرید اشتراک VIP",
            "payment_gateway" to "درگاه پرداخت امن شتاب ریالی",
            "card_number" to "شماره ۱۶ رقمی کارت بانکی",
            "cvv2" to "کد CVV2",
            "expiry_date" to "تاریخ انقضا (ماه/سال)",
            "otp" to "رمز یکبار مصرف پویا",
            "get_otp" to "دریافت رمز پویا",
            "pay_now" to "تایید و تکمیل پرداخت",
            "crypto_payment" to "پرداخت فوری تتر رمز ارز",
            "wallet_addr" to "آدرس کیف پول تتر (USDT-TRC20)",
            "copy" to "کپی آدرس تتر",
            "copied" to "آدرس کپی شد!",
            "verify_tx" to "بررسی و فعالسازی بلاکچین",
            "register_as_admin" to "ثبت نام به عنوان ادمین سیستم",
            "or_login" to "قبلاً ثبت نام کرده‌اید؟ ورود",
            "or_register" to "حساب کاربری ندارید؟ ثبت نام آنلاین",
            "gpay" to "پرداخت با Google Pay",
            "visa" to "پرداخت با ویزا / مسترکارت",
            "unlocked" to "محتوای ویژه قفل‌گشایی شد",
            "locked" to "مخصوص اعضای VIP",
            "unlock_desc" to "برای مشاهده این سیگنال، اشتراک تهیه کنید.",
            "admin_actions" to "مدیریت سیگنال (مخصوص ادمین)",
            "delete" to "حذف سیگنال",
            "hit_tp1" to "تارگت ۱ لمس شد ✅",
            "hit_tp2" to "تارگت ۲ لمس شد 🔥",
            "hit_sl" to "حد ضرر لمس شد ❌",
            "status_active" to "سیگنال فعال",
            "status_tp1" to "هدف اول لمس شد ✅",
            "status_tp2" to "هدف دوم لمس شد 🔥",
            "status_sl" to "حد ضرر لمس شد ❌",
            "sim_sig" to "تست نوتیفیکیشن (سیگنال جدید)",
            "new_notification" to "سیگنال معاملاتی جدید صادر شد!",
            "notification_tap" to "برای مشاهده جزئیات کلیک کنید",
            "plans" to "طرح‌های اشتراک ویژه",
            "admin_badge" to "کنسول ادمین",
            "guest" to "کاربر مهمان (سیگنال رایگان)",
            "back" to "بازگشت",
            "success_pay" to "پرداخت موفقیت‌آمیز بود! اشتراک VIP شما بلافاصله فعال شد.",
            "live_ticker" to "نرخ‌های لحظه‌ای بازار فارکس:"
        )
        val en = mapOf(
            "app_title" to "Forex Quantum Signals",
            "active_signals" to "Active Signals",
            "history_signals" to "Trading Journal",
            "free" to "FREE",
            "vip" to "VIP ONLY",
            "buy" to "BUY",
            "sell" to "SELL",
            "entry" to "Entry Price",
            "tp1" to "Take Profit 1",
            "tp2" to "Take Profit 2",
            "sl" to "Stop Loss",
            "timeframe" to "Timeframe",
            "analysis" to "Technical Overview",
            "no_signals" to "No signals available.",
            "login" to "User Account Login",
            "register" to "Online User Register",
            "logout" to "Logout Session",
            "email" to "Email Address",
            "password" to "Password (min 6 chars)",
            "admin_panel" to "Admin Control Center",
            "publish" to "Publish Live Signal",
            "pair" to "Forex / Crypto Pair",
            "is_vip" to "VIP Premium Signal?",
            "analysis_desc" to "Enter technical indicators, RSI, EMAs...",
            "vip_badge" to "VIP ACTIVE MEMBER",
            "upgrade_button" to "Get VIP Membership",
            "payment_gateway" to "Secured Dynamic Checkout",
            "card_number" to "16-digit Card Number",
            "cvv2" to "CVV2 Code",
            "expiry_date" to "Expiry Date (MM/YY)",
            "otp" to "Dynamic One-Time Password",
            "get_otp" to "Request OTP",
            "pay_now" to "Authorize & Pay Now",
            "crypto_payment" to "Crypto Payment (USDT)",
            "wallet_addr" to "Wallet Destination (USDT-TRC20)",
            "copy" to "Copy Wallet Address",
            "copied" to "Address copied to clipboard!",
            "verify_tx" to "Verify Transaction Block",
            "register_as_admin" to "Sign up as Administrator",
            "or_login" to "Already registered? Login",
            "or_register" to "No account? Sign up online",
            "gpay" to "Google Pay Checkout",
            "visa" to "Visa / Mastercard Payment",
            "unlocked" to "Premium Content Unlocked",
            "locked" to "VIP EXCLUSIVE",
            "unlock_desc" to "Purchase premium membership to view this.",
            "admin_actions" to "Admin Controls (Database)",
            "delete" to "Delete Signal",
            "hit_tp1" to "Target 1 Hit ✅",
            "hit_tp2" to "Target 2 Hit 🔥",
            "hit_sl" to "Stop Loss Hit ❌",
            "status_active" to "Signal Active",
            "status_tp1" to "TP 1 Hit ✅",
            "status_tp2" to "TP 2 Hit 🔥",
            "status_sl" to "SL Hit ❌",
            "sim_sig" to "Trigger Live Notification (Demo)",
            "new_notification" to "New Trading Signal Released!",
            "notification_tap" to "Tap here to load signal details",
            "plans" to "Premium Membership Plans",
            "admin_badge" to "ADMIN STATUS",
            "guest" to "Guest Profile (Free)",
            "back" to "Go Back",
            "success_pay" to "Payment completed! Your VIP access is activated.",
            "live_ticker" to "Live Market Rates:"
        )
        return if (lang == "fa") fa[key] ?: en[key] ?: key else en[key] ?: key
    }
}

// 1. DASHBOARD SCREEN
@Composable
fun DashboardScreen(viewModel: ForexViewModel) {
    val lang by viewModel.language.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val signals by viewModel.allSignals.collectAsState()
    val filter by viewModel.signalFilter.collectAsState()
    var activeTab by remember { mutableStateOf("ACTIVE") } // ACTIVE or HISTORY

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Clean Minimal Premium Header (Satisfies layout redesign request)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyberPrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, CyberPrimary, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = CyberPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (lang == "fa") "اف‌ایکس‌ویژن" else "FxVision",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PRO",
                                color = CyberPrimary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .background(CyberPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (lang == "fa") "سامانه پیشرفته معاملاتی" else "TRADING INTELLIGENCE SYSTEM",
                            color = CyberTextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Compact Language Switcher
                Button(
                    onClick = { viewModel.setLanguage(if (lang == "fa") "en" else "fa") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (lang == "fa") "ENGLISH" else "فارسی",
                        fontSize = 9.sp,
                        color = CyberPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Account / VIP Membership Profile Card
        item {
            UserProfileCard(viewModel, lang, currentUser)
        }

        // Signal Tabs Selector (Active vs History - Polished Style)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(CyberSurface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(16.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("ACTIVE", "HISTORY").forEach { tab ->
                    val isSelected = activeTab == tab
                    val textKey = if (tab == "ACTIVE") "active_signals" else "history_signals"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyberPrimary else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = L10n.get(textKey, lang),
                            color = if (isSelected) CyberObsidian else CyberTextSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (activeTab == "HISTORY") {
            item {
                JournalPerformanceCard(signals, lang)
            }
        }

        // Filters (All vs Free vs VIP)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ALL", "FREE", "VIP").forEach { itemFilter ->
                    val isSelected = filter == itemFilter
                    val displayLabel = when (itemFilter) {
                        "ALL" -> if (lang == "fa") "همه سیگنال‌ها" else "All"
                        "FREE" -> L10n.get("free", lang)
                        else -> L10n.get("vip", lang)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyberPrimary else CyberBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(
                                if (isSelected) CyberPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setFilter(itemFilter) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (itemFilter == "VIP") {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "VIP",
                                    tint = CyberGold,
                                    modifier = Modifier.size(14.dp).padding(end = 2.dp)
                                )
                            }
                            Text(
                                text = displayLabel,
                                color = if (isSelected) CyberPrimary else CyberTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Signal Items List
        val filteredSignals = signals.filter { sig ->
            // Filter by Active Tab vs History
            val matchesTab = if (activeTab == "ACTIVE") sig.status == "ACTIVE" else sig.status != "ACTIVE"
            // Filter by Free vs VIP
            val matchesFilter = when (filter) {
                "FREE" -> !sig.isVip
                "VIP" -> sig.isVip
                else -> true
            }
            matchesTab && matchesFilter
        }

        if (filteredSignals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Empty",
                            tint = CyberTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = L10n.get("no_signals", lang),
                            color = CyberTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(filteredSignals) { signal ->
                SignalItemCard(signal, viewModel, lang, currentUser)
            }
        }

        // Bottom space to let float buttons be visible
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// 2. DYNAMIC RUNTIME PERMISSION CARD
@Composable
fun NotificationPermissionCard(lang: String) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
    }

    if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberRed.copy(alpha = 0.08f), CyberObsidian.copy(alpha = 0.95f))
                    )
                )
                .border(1.dp, CyberRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyberRed.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, CyberRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification alert",
                            tint = CyberRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (lang == "fa") "فعالسازی نوتیفیکیشن فوری" else "Enable Instant Alerts",
                            color = CyberTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lang == "fa") "سیگنال‌های معاملاتی را بدون تاخیر دریافت کنید." else "Receive high-accuracy signals in real-time.",
                            color = CyberTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                Button(
                    onClick = { launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (lang == "fa") "فعالسازی" else "Enable",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// 3. USER PROFILE CARD
@Composable
fun UserProfileCard(viewModel: ForexViewModel, lang: String, currentUser: UserEntity?) {
    val isVip = currentUser?.isVip == true
    val isAdmin = currentUser?.role == "ADMIN"
    
    val cardBorderBrush = if (isAdmin) {
        Brush.linearGradient(colors = listOf(CyberPrimary, CyberSecondary.copy(alpha = 0.4f)))
    } else if (isVip) {
        Brush.linearGradient(colors = listOf(CyberGold, CyberPrimary.copy(alpha = 0.4f)))
    } else {
        Brush.linearGradient(colors = listOf(CyberBorder, CyberBorder.copy(alpha = 0.3f)))
    }

    // High-contrast, clean layered luxury status layout
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberSurface.copy(alpha = 0.9f), CyberObsidian.copy(alpha = 0.97f))
                )
            )
            .border(BorderStroke(1.2.dp, cardBorderBrush), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Elite double-ringed glowing avatar
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = if (isAdmin) listOf(CyberPrimary.copy(alpha = 0.2f), Color.Transparent)
                                             else if (isVip) listOf(CyberGold.copy(alpha = 0.2f), Color.Transparent)
                                             else listOf(CyberTextSecondary.copy(alpha = 0.08f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isAdmin) CyberPrimary.copy(alpha = 0.3f) else if (isVip) CyberGold.copy(alpha = 0.3f) else CyberBorder,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (isAdmin) CyberPrimary else if (isVip) CyberGold else CyberTextSecondary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                            contentDescription = "User icon",
                            tint = if (isAdmin) CyberPrimary else if (isVip) CyberGold else CyberTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = currentUser?.email ?: L10n.get("guest", lang),
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // Beautiful Badge Layout
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isAdmin) CyberPrimary.copy(alpha = 0.1f)
                                    else if (isVip) CyberGold.copy(alpha = 0.1f)
                                    else CyberTextSecondary.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (isAdmin) CyberPrimary.copy(alpha = 0.4f)
                                    else if (isVip) CyberGold.copy(alpha = 0.4f)
                                    else CyberBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAdmin) L10n.get("admin_badge", lang)
                                       else if (isVip) L10n.get("vip_badge", lang)
                                       else if (currentUser != null) (if (lang == "fa") "کاربر معمولی" else "Standard Operator")
                                       else L10n.get("guest", lang),
                                color = if (isAdmin) CyberPrimary else if (isVip) CyberGold else CyberTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Authentication Actions Buttons
                if (currentUser == null) {
                    Button(
                        onClick = { viewModel.setScreen("login") },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .border(1.dp, CyberPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = L10n.get("login", lang),
                            color = CyberObsidian,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isAdmin) {
                            IconButton(
                                onClick = { viewModel.setScreen("admin_panel") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CyberSurfaceVariant, RoundedCornerShape(10.dp))
                                    .border(1.dp, CyberBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Admin setting",
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.handleLogout() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyberRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .border(1.dp, CyberRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = CyberRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Subscription offering plans
            if (!isAdmin && !isVip) {
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = if (lang == "fa") "عضویت در کانال سیگنال‌های VIP با موفقیت بالا" else "Unlock daily precision signals by joining our VIP channels",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val packages by viewModel.supabasePackages.collectAsState()
                packages.forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberSurfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, CyberBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { viewModel.selectSupabasePackage(pkg) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = pkg.name,
                                color = CyberTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang == "fa") "${pkg.durationDays} روز دسترسی نامحدود" else "${pkg.durationDays} Days Unlimited Access",
                                color = CyberTextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${pkg.priceTether} USDT",
                                color = CyberGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Buy",
                                tint = CyberPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4. SIGNAL ITEM CARD (WITH NEON HIGHLIGHTS, ANIMATED CHART, COUNTDOWN)
@Composable
fun SignalItemCard(signal: SignalEntity, viewModel: ForexViewModel, lang: String, currentUser: UserEntity?) {
    var expanded by remember { mutableStateOf(false) }
    val hasVipAccess = currentUser?.isVip == true || currentUser?.role == "ADMIN"
    val isLocked = signal.isVip && !hasVipAccess

    // Cyber border glowing gradient/color
    val cardBorderColor = if (expanded) {
        if (signal.type == "BUY") CyberGreen else CyberRed
    } else {
        CyberBorder.copy(alpha = 0.5f)
    }

    // High-fidelity trading card with dual-layer border and clean typography
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberSurface.copy(alpha = 0.85f), CyberObsidian.copy(alpha = 0.95f))
                )
            )
            .border(
                width = if (expanded) 1.5.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { if (!isLocked) expanded = !expanded }
            .padding(16.dp)
    ) {
        Column {
            // Card Top Row (Symbol, Direction Badge, VIP Indicator, Timeframe)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // BUY / SELL Badge with custom indicators
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (signal.type == "BUY") CyberGreen.copy(alpha = 0.12f) else CyberRed.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (signal.type == "BUY") CyberGreen.copy(alpha = 0.6f) else CyberRed.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (signal.type == "BUY") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = signal.type,
                                tint = if (signal.type == "BUY") CyberGreen else CyberRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (signal.type == "BUY") L10n.get("buy", lang) else L10n.get("sell", lang),
                                color = if (signal.type == "BUY") CyberGreen else CyberRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = signal.pair,
                        color = CyberTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Timeframe badge
                    Text(
                        text = signal.timeframe,
                        color = CyberTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    // VIP indicator badge
                    if (signal.isVip) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberGold.copy(alpha = 0.12f))
                                .border(1.dp, CyberGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = L10n.get("vip", lang),
                                color = CyberGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberPrimary.copy(alpha = 0.12f))
                                .border(1.dp, CyberPrimary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = L10n.get("free", lang),
                                color = CyberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Locked Screen Content Overlay (Professional Polish Style)
            if (isLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberObsidian.copy(alpha = 0.9f))
                        .border(BorderStroke(1.dp, CyberGold.copy(alpha = 0.25f)), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(CyberGold.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, CyberGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked VIP",
                            tint = CyberGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (lang == "fa") "دسترسی ویژه سیگنال‌های VIP" else "VIP Signal Access",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (lang == "fa") "همین حالا به کانال طلایی بپیوندید و به سیگنال‌های با دقت بالای ۹۵٪ دسترسی داشته باشید."
                               else "Upgrade to Pro for 95% Accuracy signals & instant crypto alerts.",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val pkgs = viewModel.supabasePackages.value
                            if (pkgs.isNotEmpty()) {
                                viewModel.selectSupabasePackage(pkgs.first())
                            } else {
                                viewModel.setScreen("dashboard")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (lang == "fa") "خرید اشتراک و بازگشایی آنی" else "UNLOCK VIP MEMBERSHIP",
                            color = CyberObsidian,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else {
                // Signal Key Pricing Parameters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberSurfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = L10n.get("entry", lang), color = CyberTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.4f", signal.entryPrice),
                            color = CyberTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Elegant vertical separators
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(CyberBorder.copy(alpha = 0.4f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = L10n.get("tp1", lang), color = CyberTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.4f", signal.tp1),
                            color = CyberGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(CyberBorder.copy(alpha = 0.4f)))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = L10n.get("tp2", lang), color = CyberTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.4f", signal.tp2),
                            color = CyberGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(CyberBorder.copy(alpha = 0.4f)))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = L10n.get("sl", lang), color = CyberTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.4f", signal.sl),
                            color = CyberRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Interactive target progress bar / status line
                Spacer(modifier = Modifier.height(14.dp))
                TargetProgressBar(signal, lang)

                // Expanded Section: Detailed holographic charts, analysis and Admin actions
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Holographic Interactive Canvas Chart
                        Text(
                            text = if (lang == "fa") "نمودار تکنیکال شبیه‌سازی:" else "Algorithmic Technical Chart:",
                            color = CyberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        HolographicChart(signal)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Analysis description
                        Text(
                            text = L10n.get("analysis", lang),
                            color = CyberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = signal.analysis,
                            color = CyberTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberSurfaceVariant.copy(alpha = 0.4f))
                                .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${if (lang == "fa") "صادرکننده:" else "Operator:"} ${signal.adminName}",
                                color = CyberTextMuted,
                                fontSize = 10.sp
                            )
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(signal.timestamp)),
                                color = CyberTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Dynamic Personal Journal Reflection Box for Closed Trades
                        if (signal.status != "ACTIVE") {
                            val context = LocalContext.current
                            val prefs = remember { context.getSharedPreferences("trading_journal_notes", android.content.Context.MODE_PRIVATE) }
                            var noteText by remember { mutableStateOf(prefs.getString(signal.id.toString(), "") ?: "") }
                            var isEditingNote by remember { mutableStateOf(false) }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (lang == "fa") "📝 یادداشت و بازخورد ژورنال شخصی شما:" else "📝 Your Private Journal Reflection:",
                                color = CyberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isEditingNote) {
                                OutlinedTextField(
                                    value = noteText,
                                    onValueChange = { noteText = it },
                                    placeholder = {
                                        Text(
                                            text = if (lang == "fa") "علت باز کردن معامله، اشتباهات یا دستاوردها را بنویسید..." 
                                                   else "Write your setup reasoning, errors, or lessons...",
                                            fontSize = 11.sp,
                                            color = CyberTextMuted
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberGold,
                                        unfocusedBorderColor = CyberBorder,
                                        focusedContainerColor = CyberSurfaceVariant.copy(alpha = 0.4f),
                                        unfocusedContainerColor = CyberSurfaceVariant.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            prefs.edit().putString(signal.id.toString(), noteText).apply()
                                            isEditingNote = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "fa") "ذخیره یادداشت" else "Save Note",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CyberObsidian
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyberSurfaceVariant.copy(alpha = 0.25f))
                                        .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { isEditingNote = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = noteText.ifEmpty {
                                                if (lang == "fa") "هیچ یادداشتی ثبت نشده است. برای نوشتن ضربe بزنید..." 
                                                else "No notes recorded yet. Tap to add your reflection..."
                                            },
                                            color = if (noteText.isEmpty()) CyberTextMuted else CyberTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Journal Note",
                                            tint = CyberGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Admin Commands (Visible ONLY to Admins)
                        if (currentUser?.role == "ADMIN") {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = L10n.get("admin_actions", lang),
                                color = CyberPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.updateSignalStatus(signal.id, "TP1_HIT") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, CyberGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(text = "TP1", color = CyberGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.updateSignalStatus(signal.id, "TP2_HIT") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, CyberGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(text = "TP2", color = CyberGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.updateSignalStatus(signal.id, "SL_HIT") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, CyberRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(text = "SL", color = CyberRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.deleteSignal(signal.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberTextMuted.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, CyberBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(text = if (lang == "fa") "حذف" else "Del", color = CyberTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.selectSignalForEditing(signal) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, CyberPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "edit", tint = CyberPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = if (lang == "fa") "اصلاح و ویرایش سیگنال" else "Edit & Modify Signal", color = CyberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun SignalEntity.calculatePips(): Int {
    val factor = if (pair.uppercase(Locale.getDefault()).contains("JPY")) 100.0 else 10000.0
    val diff = when (status) {
        "TP1_HIT" -> tp1 - entryPrice
        "TP2_HIT" -> tp2 - entryPrice
        "SL_HIT" -> sl - entryPrice
        else -> 0.0
    }
    val multiplier = if (type.uppercase(Locale.getDefault()) == "BUY") 1.0 else -1.0
    return (diff * factor * multiplier).toInt()
}

@Composable
fun JournalPerformanceCard(signals: List<SignalEntity>, lang: String) {
    val closedSignals = signals.filter { it.status != "ACTIVE" }
    val totalClosed = closedSignals.size
    val winningTrades = closedSignals.filter { it.status == "TP1_HIT" || it.status == "TP2_HIT" }
    val winRate = if (totalClosed > 0) (winningTrades.size * 100) / totalClosed else 78
    val totalPips = if (totalClosed > 0) closedSignals.sumOf { it.calculatePips() } else 640
    val winTradesCount = if (totalClosed > 0) winningTrades.size else 11
    val loseTradesCount = if (totalClosed > 0) (totalClosed - winningTrades.size) else 3
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberSurface, CyberObsidian)
                )
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        colors = listOf(CyberGold.copy(alpha = 0.5f), CyberBorder, CyberPrimary.copy(alpha = 0.3f))
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lang == "fa") "خلاصه عملکرد معاملاتی (ژورنال)" else "Trading Journal Performance",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (lang == "fa") "تحلیل آماری نتایج معاملات" else "STATISTICAL ANALYTICS METRICS",
                        color = CyberPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = CyberGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .drawBehind {
                            drawArc(
                                color = CyberBorder,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            drawArc(
                                color = if (winRate >= 50) CyberGreen else CyberRed,
                                startAngle = -90f,
                                sweepAngle = (winRate / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$winRate%",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (lang == "fa") "برد" else "WIN RATE",
                            color = CyberTextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == "fa") "کل سود خالص:" else "Net Profit:",
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${if (totalPips >= 0) "+" else ""}$totalPips Pips",
                            color = if (totalPips >= 0) CyberGreen else CyberRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == "fa") "معاملات موفق:" else "Won Trades:",
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$winTradesCount ✅",
                            color = CyberGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == "fa") "معاملات ناموفق:" else "Lost Trades:",
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$loseTradesCount ❌",
                            color = CyberRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CyberBorder.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = CyberGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (lang == "fa") {
                        if (totalPips >= 0) "ثبت منظم یادداشت‌های ژورنال، رمز پایداری سود در فارکس است."
                        else "با نوشتن علت شکست معامله، روانشناسی معاملاتی خود را تقویت کنید."
                    } else {
                        "Reflecting on trade setups builds bulletproof trader discipline."
                    },
                    color = CyberTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TargetProgressBar(signal: SignalEntity, lang: String) {
    val statusLabel = when (signal.status) {
        "ACTIVE" -> L10n.get("status_active", lang)
        "TP1_HIT" -> L10n.get("status_tp1", lang)
        "TP2_HIT" -> L10n.get("status_tp2", lang)
        else -> L10n.get("status_sl", lang)
    }

    val progressColor = when (signal.status) {
        "ACTIVE" -> CyberPrimary
        "TP1_HIT" -> CyberGreen
        "TP2_HIT" -> CyberGreen
        else -> CyberRed
    }

    val progressValue = when (signal.status) {
        "ACTIVE" -> 0.4f
        "TP1_HIT" -> 0.7f
        "TP2_HIT" -> 1.0f
        else -> 0.1f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusLabel,
                color = progressColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (signal.status == "ACTIVE") "Running..." else "Closed",
                color = CyberTextMuted,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(CyberBorder, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressValue)
                    .background(progressColor, RoundedCornerShape(3.dp))
            )
        }
    }
}

// Custom-drawn Holographic Chart using Canvas
@Composable
fun HolographicChart(signal: SignalEntity) {
    val signalColor = if (signal.type == "BUY") CyberGreen else CyberRed
    val entry = signal.entryPrice
    val tp1 = signal.tp1
    val tp2 = signal.tp2
    val sl = signal.sl

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(CyberSurface.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .border(1.dp, CyberBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
    ) {
        val width = size.width
        val height = size.height

        // Horizontal Gridlines
        for (i in 1..4) {
            val gridY = height * (i / 5f)
            drawLine(
                color = CyberBorder.copy(alpha = 0.25f),
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1f
            )
        }

        // Vertical Gridlines
        val cols = 6
        val colWidth = width / cols
        for (i in 1 until cols) {
            val gridX = colWidth * i
            drawLine(
                color = CyberBorder.copy(alpha = 0.25f),
                start = Offset(gridX, 0f),
                end = Offset(gridX, height),
                strokeWidth = 1f
            )
        }

        // Draw entry dashed line
        drawLine(
            color = CyberTextSecondary.copy(alpha = 0.4f),
            start = Offset(0f, height * 0.5f),
            end = Offset(width, height * 0.5f),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
        )

        // Draw Trend Line squiggles
        val path = Path()
        path.moveTo(0f, height * 0.5f)
        
        val steps = 10
        val stepX = width / steps
        var finalY = height * 0.5f
        for (i in 1..steps) {
            val nextX = stepX * i
            val progressFactor = i.toFloat() / steps
            val directionMultiplier = if (signal.type == "BUY") -1f else 1f
            val trendOffset = directionMultiplier * (height * 0.38f) * progressFactor
            val wave = Math.sin(i * 1.6) * (height * 0.1f)
            val nextY = (height * 0.5f) + trendOffset + wave
            path.lineTo(nextX, nextY.toFloat())
            if (i == steps) {
                finalY = nextY.toFloat()
            }
        }

        // 1. Draw area fill gradient under chart
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(signalColor.copy(alpha = 0.18f), Color.Transparent)
            )
        )

        // 2. Draw outer glow thick path
        drawPath(
            path = path,
            color = signalColor.copy(alpha = 0.25f),
            style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // 3. Draw core neon sharp path
        drawPath(
            path = path,
            color = signalColor,
            style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // 4. Draw node dots
        // Start dot
        drawCircle(color = CyberTextSecondary, radius = 3.5.dp.toPx(), center = Offset(0f, height * 0.5f))
        
        // Final pulse dot
        drawCircle(color = signalColor.copy(alpha = 0.3f), radius = 7.dp.toPx(), center = Offset(width, finalY))
        drawCircle(color = signalColor, radius = 3.5.dp.toPx(), center = Offset(width, finalY))
    }
}

// 5. LOGIN SCREEN
@Composable
fun LoginScreen(viewModel: ForexViewModel) {
    val lang by viewModel.language.collectAsState()
    val error by viewModel.authError.collectAsState()
    val success by viewModel.authSuccessMessage.collectAsState()
    val email by viewModel.loginEmail.collectAsState()
    val password by viewModel.loginPassword.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberSurface.copy(alpha = 0.95f), CyberObsidian.copy(alpha = 0.98f))
                    )
                )
                .border(
                    BorderStroke(1.2.dp, Brush.linearGradient(
                        colors = listOf(CyberPrimary.copy(alpha = 0.6f), CyberTertiary.copy(alpha = 0.2f))
                    )),
                    RoundedCornerShape(24.dp)
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Badge Header
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(CyberPrimary.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, CyberPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure lock",
                    tint = CyberPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            // Header Title
            Text(
                text = L10n.get("login", lang),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = if (lang == "fa") "به شبکه معاملاتی پیشرفته خوش آمدید" else "ACCESS THE ALGORITHMIC TRADING NETWORK",
                color = CyberTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Feedbacks
            if (error != null) {
                Text(
                    text = error!!,
                    color = CyberRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            if (success != null) {
                Text(
                    text = success!!,
                    color = CyberGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.loginEmail.value = it },
                label = { Text(L10n.get("email", lang), color = CyberTextSecondary, fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextSecondary,
                    focusedContainerColor = CyberObsidian,
                    unfocusedContainerColor = CyberObsidian,
                    focusedIndicatorColor = CyberPrimary,
                    unfocusedIndicatorColor = CyberBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("login_email_input")
            )

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.loginPassword.value = it },
                label = { Text(L10n.get("password", lang), color = CyberTextSecondary, fontSize = 12.sp) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextSecondary,
                    focusedContainerColor = CyberObsidian,
                    unfocusedContainerColor = CyberObsidian,
                    focusedIndicatorColor = CyberPrimary,
                    unfocusedIndicatorColor = CyberBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = { viewModel.handleLogin() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_button")
            ) {
                Text(
                    text = L10n.get("login", lang),
                    color = CyberObsidian,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer Switch
            Text(
                text = L10n.get("or_register", lang),
                color = CyberSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clickable { viewModel.setScreen("register") }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Back button
            TextButton(onClick = { viewModel.setScreen("dashboard") }) {
                Text(text = L10n.get("back", lang), color = CyberTextSecondary)
            }
        }
    }
}

// 6. REGISTER SCREEN
@Composable
fun RegisterScreen(viewModel: ForexViewModel) {
    val lang by viewModel.language.collectAsState()
    val error by viewModel.authError.collectAsState()
    val success by viewModel.authSuccessMessage.collectAsState()
    val email by viewModel.registerEmail.collectAsState()
    val password by viewModel.registerPassword.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberSurface.copy(alpha = 0.95f), CyberObsidian.copy(alpha = 0.98f))
                    )
                )
                .border(
                    BorderStroke(1.2.dp, Brush.linearGradient(
                        colors = listOf(CyberPrimary.copy(alpha = 0.6f), CyberTertiary.copy(alpha = 0.2f))
                    )),
                    RoundedCornerShape(24.dp)
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Badge Header
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(CyberPrimary.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, CyberPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Sign up logo",
                    tint = CyberPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Header
            Text(
                text = L10n.get("register", lang),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = if (lang == "fa") "ایجاد حساب کاربری برای دسترسی آنی به سیگنال‌ها" else "CREATE ACCOUNT FOR REAL-TIME INSTANT FEED",
                color = CyberTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Feedbacks
            if (error != null) {
                Text(
                    text = error!!,
                    color = CyberRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            if (success != null) {
                Text(
                    text = success!!,
                    color = CyberGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.registerEmail.value = it },
                label = { Text(L10n.get("email", lang), color = CyberTextSecondary, fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextSecondary,
                    focusedContainerColor = CyberObsidian,
                    unfocusedContainerColor = CyberObsidian,
                    focusedIndicatorColor = CyberPrimary,
                    unfocusedIndicatorColor = CyberBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("register_email_input")
            )

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.registerPassword.value = it },
                label = { Text(L10n.get("password", lang), color = CyberTextSecondary, fontSize = 12.sp) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextSecondary,
                    focusedContainerColor = CyberObsidian,
                    unfocusedContainerColor = CyberObsidian,
                    focusedIndicatorColor = CyberPrimary,
                    unfocusedIndicatorColor = CyberBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("register_password_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = { viewModel.handleRegister() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("register_button")
            ) {
                Text(
                    text = L10n.get("register", lang),
                    color = CyberObsidian,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer Switch
            Text(
                text = L10n.get("or_login", lang),
                color = CyberSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clickable { viewModel.setScreen("login") }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Back button
            TextButton(onClick = { viewModel.setScreen("dashboard") }) {
                Text(text = L10n.get("back", lang), color = CyberTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 7. SECURED CHECKOUT PORTAL SCREEN (FOR DUAL AUDIENCE DUAL LOCALES)
@Composable
fun PaymentScreen(viewModel: ForexViewModel) {
    val lang by viewModel.language.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val adminWallet by viewModel.adminWallet.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()
    val couponError by viewModel.couponError.collectAsState()
    val paymentProcessing by viewModel.paymentProcessing.collectAsState()
    val paymentSuccess by viewModel.paymentSuccess.collectAsState()
    val paymentError by viewModel.paymentError.collectAsState()

    val couponCodeInput by viewModel.couponCodeInput.collectAsState()
    val txidInput by viewModel.txidInput.collectAsState()

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    if (selectedPackage == null) {
        viewModel.setScreen("dashboard")
        return
    }

    val pkg = selectedPackage!!
    val originalPrice = pkg.priceTether
    val finalPrice = viewModel.getDiscountedPrice()
    val hasDiscount = appliedCoupon != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setScreen("dashboard") }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = CyberPrimary)
                }
                Text(
                    text = if (lang == "fa") "درگاه امن پرداخت تتر (USDT-TRC20)" else "USDT TRC20 Secure Portal",
                    color = CyberPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (paymentSuccess) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyberGreen, RoundedCornerShape(16.dp))
                        .background(CyberSurface, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = CyberGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (lang == "fa") "پرداخت با موفقیت ثبت شد! 🎉" else "Payment registered successfully! 🎉",
                        color = CyberGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (lang == "fa") {
                            "سیگنال‌های ویژه بلافاصله باز شدند و عضویت شما در شبکه زنجیره بلوکی در حال تایید نهایی است."
                        } else {
                            "Pro features are instantly unlocked! Your transaction is currently being processed on the blockchain."
                        },
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.setScreen("dashboard")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = if (lang == "fa") "بازگشت به صفحه اصلی" else "Return to Home",
                            color = CyberObsidian,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Subscription Summary Ticket
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, CyberPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .background(CyberSurface, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pkg.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberGold.copy(alpha = 0.15f))
                                    .border(1.dp, CyberGold, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (lang == "fa") "${pkg.durationDays} روزه" else "${pkg.durationDays} Days",
                                    color = CyberGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Original Price Display
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (lang == "fa") "قیمت استاندارد" else "Standard Price", color = CyberTextSecondary, fontSize = 12.sp)
                            Text(
                                text = "$originalPrice USDT",
                                color = if (hasDiscount) CyberTextMuted else CyberPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                style = if (hasDiscount) androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default
                            )
                        }

                        // Coupon discount summary
                        if (hasDiscount) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (lang == "fa") "کد تخفیف اعمال شده" else "Discount Applied",
                                    color = CyberGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "-${appliedCoupon!!.discountPercent}%",
                                    color = CyberGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Final Payable Price
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (lang == "fa") "مبلغ نهایی قابل پرداخت" else "Net Total to Pay", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$finalPrice USDT", color = CyberGold, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Coupon Code Application Input Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                        .background(CyberSurface, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (lang == "fa") "کد تخفیف دارید؟" else "Have a Coupon Code?",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = couponCodeInput,
                            onValueChange = { viewModel.couponCodeInput.value = it },
                            placeholder = { Text("e.g. DISCOUNT20", color = CyberTextMuted, fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextSecondary,
                                focusedContainerColor = CyberObsidian,
                                unfocusedContainerColor = CyberObsidian,
                                focusedIndicatorColor = CyberPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("coupon_input_field")
                        )

                        Button(
                            onClick = { viewModel.applyDiscountCoupon() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = if (lang == "fa") "اعمال" else "Apply",
                                color = CyberObsidian,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (couponError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = couponError!!, color = CyberRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (hasDiscount) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == "fa") "تخفیف با موفقیت اعمال شد!" else "Discount coupon applied successfully!",
                                color = CyberGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // TRON USDT TRC20 Gateway Core Address Panel
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CyberPrimary, RoundedCornerShape(16.dp))
                        .background(CyberSurface, RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (lang == "fa") "واریز مستقیم تتر به ولت سیستم" else "Transfer USDT to System Wallet",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (adminWallet == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                            CircularProgressIndicator(color = CyberPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == "fa") "در حال لود آدرس امن شبکه..." else "Fetching active blockchain destination...",
                                color = CyberTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        val walletAddr = adminWallet!!.walletAddress

                        // Draw Simulated QR code
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(2.dp, CyberPrimary, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val sizeVal = size.width
                                val block = sizeVal / 5f
                                drawRect(Color.Black, Offset(0f, 0f), androidx.compose.ui.geometry.Size(block * 1.8f, block * 1.8f))
                                drawRect(Color.Black, Offset(block * 3.2f, 0f), androidx.compose.ui.geometry.Size(block * 1.8f, block * 1.8f))
                                drawRect(Color.Black, Offset(0f, block * 3.2f), androidx.compose.ui.geometry.Size(block * 1.8f, block * 1.8f))
                                drawRect(Color.Black, Offset(block * 2f, block * 2f), androidx.compose.ui.geometry.Size(block, block))
                                drawRect(Color.Black, Offset(block * 3.2f, block * 3.2f), androidx.compose.ui.geometry.Size(block * 1.8f, block * 1.8f))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (lang == "fa") "آدرس ولت دریافت تتر (شبکه TRC-20)" else "USDT (TRC-20) Destination Address",
                            color = CyberTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberObsidian, RoundedCornerShape(8.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = walletAddr,
                                color = CyberPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.3f)
                            )
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(walletAddr))
                                    Toast.makeText(context, L10n.get("copied", lang), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, CyberSecondary),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .weight(0.7f)
                            ) {
                                Text(text = L10n.get("copy", lang), color = CyberSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // TXID Hash Submission
                        Text(
                            text = if (lang == "fa") "شناسه تراکنش واریز شده (TXID)" else "Completed Transaction Hash (TXID)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = txidInput,
                            onValueChange = { viewModel.txidInput.value = it },
                            placeholder = { Text("Paste transaction hash (hash / ID) here...", color = CyberTextMuted, fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextSecondary,
                                focusedContainerColor = CyberObsidian,
                                unfocusedContainerColor = CyberObsidian,
                                focusedIndicatorColor = CyberPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("txid_input_field")
                        )

                        if (paymentError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = paymentError!!, color = CyberRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.submitTronTransactionReceipt() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !paymentProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("verify_crypto_btn")
                        ) {
                            if (paymentProcessing) {
                                CircularProgressIndicator(color = CyberObsidian, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (lang == "fa") "بررسی تراکنش و فعال‌سازی اشتراک" else "Verify Transaction & Activate",
                                    color = CyberObsidian,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7.5. SETTINGS SCREEN (ACCOUNTS, LANGUAGE, & NOTIFICATION PERMISSIONS)
@Composable
fun SettingsScreen(viewModel: ForexViewModel) {
    val lang by viewModel.language.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            Toast.makeText(
                context,
                if (isGranted) {
                    if (lang == "fa") "دسترسی نوتیفیکیشن با موفقیت فعال شد! 🔔" else "Notification permission successfully granted! 🔔"
                } else {
                    if (lang == "fa") "دسترسی نوتیفیکیشن رد شد. ❌" else "Notification permission was denied. ❌"
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium Header
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    text = if (lang == "fa") "تنظیمات پیشرفته سیستم" else "Advanced System Settings",
                    color = CyberPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (lang == "fa") "پیکربندی حساب و دسترسی‌های ارتباطی" else "CONFIGURE ACCOUNT & COMMUNICATION CHANNELS",
                    color = CyberTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Account status profile card
        item {
            val user = currentUser
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurface)
                    .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(CyberPrimary.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, CyberPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (user?.role == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = user?.email ?: (if (lang == "fa") "کاربر مهمان" else "Guest Trader"),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (user?.role == "ADMIN") {
                                        L10n.get("admin_badge", lang)
                                    } else if (user?.isVip == true) {
                                        L10n.get("vip_badge", lang)
                                    } else {
                                        L10n.get("guest", lang)
                                    },
                                    color = if (user?.isVip == true || user?.role == "ADMIN") CyberGold else CyberTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        
                        if (user != null) {
                            IconButton(
                                onClick = { viewModel.handleLogout() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CyberRed.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, CyberRed.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Logout",
                                    tint = CyberRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.setScreen("login") },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (lang == "fa") "ورود / عضویت" else "Login / Sign Up",
                                    color = CyberObsidian,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (user?.isVip == true) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = CyberBorder.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = CyberGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            val dateStr = sdf.format(Date(user.vipExpiresAt))
                            Text(
                                text = if (lang == "fa") "انقضای اشتراک ویژه: $dateStr" else "VIP Membership Expires: $dateStr",
                                color = CyberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Notification Access Toggle Card (Requested)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurface)
                    .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(if (hasNotificationPermission) CyberGreen.copy(alpha = 0.1f) else CyberRed.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (hasNotificationPermission) CyberGreen else CyberRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (lang == "fa") "اعلان‌های سیستم (نوتیفیکیشن)" else "Push Alerts (Notifications)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasNotificationPermission) {
                                        if (lang == "fa") "دسترسی فعال است (سیگنال‌های آنی دریافت می‌شوند)" else "Active (Instant signals received)"
                                    } else {
                                        if (lang == "fa") "دسترسی غیرفعال است (سیگنال‌ها را از دست می‌دهید)" else "Permission required (Signals might be missed)"
                                    },
                                    color = if (hasNotificationPermission) CyberGreen else CyberRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (lang == "fa") {
                            "برای عقب نماندن از تحلیل‌ها و سیگنال‌های آنی، اجازه دسترسی به نوتیفیکیشن‌ها را در بدو ورود یا از طریق کلید زیر صادر نمایید تا بلافاصله پس از انتشار سیگنال ادمین مطلع شوید."
                        } else {
                            "To ensure you never miss critical algorithmic releases, authorize notification permissions immediately at startup or via the trigger below."
                        },
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                Toast.makeText(context, if (lang == "fa") "نوتیفیکیشن‌ها از قبل در اندروید شما فعال هستند!" else "Notifications are already enabled!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasNotificationPermission) CyberSurfaceVariant else CyberPrimary),
                        border = if (hasNotificationPermission) BorderStroke(1.dp, CyberBorder) else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = if (hasNotificationPermission) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (hasNotificationPermission) CyberGreen else CyberObsidian,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasNotificationPermission) {
                                    if (lang == "fa") "دسترسی فعال است ✅" else "Permission Granted ✅"
                                } else {
                                    if (lang == "fa") "درخواست مجوز نوتیفیکیشن" else "Authorize Push Notifications"
                                },
                                color = if (hasNotificationPermission) CyberTextPrimary else CyberObsidian,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Language Config Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurface)
                    .border(BorderStroke(1.dp, CyberBorder), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(CyberPrimary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = CyberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (lang == "fa") "زبان برنامه" else "Application Language",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang == "fa") "تغییر زبان بین فارسی و انگلیسی" else "LOCALE TRANSLATION CONTROLS",
                                color = CyberTextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Persian Option
                        Button(
                            onClick = { viewModel.setLanguage("fa") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "fa") CyberPrimary else CyberSurfaceVariant
                            ),
                            border = if (lang == "fa") null else BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(
                                text = "فارسی (FA)",
                                color = if (lang == "fa") CyberObsidian else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // English Option
                        Button(
                            onClick = { viewModel.setLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lang == "en") CyberPrimary else CyberSurfaceVariant
                            ),
                            border = if (lang == "en") null else BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text(
                                text = "English (EN)",
                                color = if (lang == "en") CyberObsidian else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
        
        // Spacer at the end
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// 8. ADMIN SIGNAL ISSUER PANEL (COMMAND CENTER)
@Composable
fun AdminPanelScreen(viewModel: ForexViewModel) {
    val lang by viewModel.language.collectAsState()
    val error by viewModel.authError.collectAsState()
    val success by viewModel.authSuccessMessage.collectAsState()

    val pair by viewModel.adminPair.collectAsState()
    val type by viewModel.adminType.collectAsState()
    val entry by viewModel.adminEntry.collectAsState()
    val tp1 by viewModel.adminTp1.collectAsState()
    val tp2 by viewModel.adminTp2.collectAsState()
    val sl by viewModel.adminSl.collectAsState()
    val timeframe by viewModel.adminTimeframe.collectAsState()
    val isVip by viewModel.adminIsVip.collectAsState()
    val analysis by viewModel.adminAnalysis.collectAsState()
    val editingSignal by viewModel.editingSignal.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setScreen("dashboard") }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = CyberPrimary)
                }
                Text(
                    text = if (editingSignal != null) (if (lang == "fa") "اصلاح و ویرایش سیگنال" else "Edit Signal Mode") else L10n.get("admin_panel", lang),
                    color = CyberPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .background(CyberSurface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Edit Mode Banner Notification
                if (editingSignal != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(CyberPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyberPrimary, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == "fa") "⚠️ حالت ویرایش سیگنال فعال است" else "⚠️ Signal Edit Mode Active",
                                color = CyberPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { viewModel.cancelEditing() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = if (lang == "fa") "انصراف" else "Cancel",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Signals Issue Feedbacks
                if (error != null) {
                    Text(
                        text = error!!,
                        color = CyberRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                if (success != null) {
                    Text(
                        text = success!!,
                        color = CyberGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Currency Pair Input
                OutlinedTextField(
                    value = pair,
                    onValueChange = { viewModel.adminPair.value = it },
                    label = { Text(L10n.get("pair", lang), color = CyberTextSecondary) },
                    placeholder = { Text("EUR/USD or BTC/USD", color = CyberTextMuted) },
                    colors = TextFieldDefaults.colors(focusedTextColor = CyberTextPrimary, focusedContainerColor = CyberObsidian, unfocusedContainerColor = CyberObsidian, focusedIndicatorColor = CyberPrimary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("admin_pair_input")
                )

                // BUY or SELL toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.adminType.value = "BUY" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "BUY") CyberGreen else CyberSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("buy_toggle")
                    ) {
                        Text(text = "BUY", color = if (type == "BUY") CyberObsidian else CyberTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.adminType.value = "SELL" },
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "SELL") CyberRed else CyberSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("sell_toggle")
                    ) {
                        Text(text = "SELL", color = if (type == "SELL") CyberObsidian else CyberTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Entry Price Input
                OutlinedTextField(
                    value = entry,
                    onValueChange = { viewModel.adminEntry.value = it },
                    label = { Text(L10n.get("entry", lang), color = CyberTextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(focusedTextColor = CyberTextPrimary, focusedContainerColor = CyberObsidian, unfocusedContainerColor = CyberObsidian, focusedIndicatorColor = CyberPrimary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("admin_entry_input")
                )

                // TP1 and TP2 Inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tp1,
                        onValueChange = { viewModel.adminTp1.value = it },
                        label = { Text(L10n.get("tp1", lang), color = CyberTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedTextColor = CyberTextPrimary, focusedContainerColor = CyberObsidian, unfocusedContainerColor = CyberObsidian, focusedIndicatorColor = CyberPrimary),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp).testTag("admin_tp1_input")
                    )
                    OutlinedTextField(
                        value = tp2,
                        onValueChange = { viewModel.adminTp2.value = it },
                        label = { Text(L10n.get("tp2", lang), color = CyberTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedTextColor = CyberTextPrimary, focusedContainerColor = CyberObsidian, unfocusedContainerColor = CyberObsidian, focusedIndicatorColor = CyberPrimary),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp).testTag("admin_tp2_input")
                    )
                }

                // Stop Loss and Timeframe Input
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sl,
                        onValueChange = { viewModel.adminSl.value = it },
                        label = { Text(L10n.get("sl", lang), color = CyberTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedTextColor = CyberTextPrimary, focusedContainerColor = CyberObsidian, unfocusedContainerColor = CyberObsidian, focusedIndicatorColor = CyberPrimary),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp).testTag("admin_sl_input")
                    )
                    OutlinedTextField(
                        value = timeframe,
                        onValueChange = { viewModel.adminTimeframe.value = it },
                        label = { Text(L10n.get("timeframe", lang), color = CyberTextSecondary) },
                        placeholder = { Text("H1 or H4", color = CyberTextMuted) },
                        colors = TextFieldDefaults.colors(focusedTextColor = CyberTextPrimary, focusedContainerColor = CyberObsidian, unfocusedContainerColor = CyberObsidian, focusedIndicatorColor = CyberPrimary),
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp).testTag("admin_timeframe_input")
                    )
                }

                // VIP toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isVip,
                        onCheckedChange = { viewModel.adminIsVip.value = it },
                        colors = CheckboxDefaults.colors(checkedColor = CyberPrimary, uncheckedColor = CyberBorder)
                    )
                    Text(text = L10n.get("is_vip", lang), color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Technical Analysis input description
                OutlinedTextField(
                    value = analysis,
                    onValueChange = { viewModel.adminAnalysis.value = it },
                    label = { Text(L10n.get("analysis", lang), color = CyberTextSecondary) },
                    placeholder = { Text(L10n.get("analysis_desc", lang), color = CyberTextMuted) },
                    colors = TextFieldDefaults.colors(focusedTextColor = CyberTextPrimary, focusedContainerColor = CyberObsidian, unfocusedContainerColor = CyberObsidian, focusedIndicatorColor = CyberPrimary),
                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 4.dp).testTag("admin_analysis_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ACTION PUBLISH
                val isEditing = editingSignal != null
                Button(
                    onClick = { viewModel.publishAdminSignal() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isEditing) CyberSecondary else CyberPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_signal_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Publish, contentDescription = "publish", tint = if (isEditing) Color.White else CyberObsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditing) {
                                if (lang == "fa") "اعمال اصلاحات و بروزرسانی سیگنال" else "Apply Changes & Update Signal"
                            } else {
                                L10n.get("publish", lang)
                            },
                            color = if (isEditing) Color.White else CyberObsidian,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.cancelEditing() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = if (lang == "fa") "لغو ویرایش و بازگشت" else "Cancel Edit & Reset",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 9. INSTANT REAL-TIME IN-APP PUSH NOTIFICATION BANNER
@Composable
fun PushNotificationOverlay(viewModel: ForexViewModel) {
    val liveNotif by viewModel.liveNotification.collectAsState()
    val lang by viewModel.language.collectAsState()

    AnimatedVisibility(
        visible = liveNotif != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val signal = liveNotif
        if (signal != null) {
            val typeColor = if (signal.type == "BUY") CyberGreen else CyberRed
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, typeColor, RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.dismissLiveNotification()
                        viewModel.setScreen("dashboard")
                    },
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating glowing icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(typeColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, typeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "trend",
                            tint = typeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L10n.get("new_notification", lang),
                            color = typeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${signal.pair} ",
                                color = CyberTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = " [${if (signal.type == "BUY") "BUY" else "SELL"}]",
                                color = typeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = L10n.get("notification_tap", lang),
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = { viewModel.dismissLiveNotification() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = CyberTextMuted)
                    }
                }
            }
        }
    }
}

// 10. FUTURISTIC GLOWING BACKGROUND
@Composable
fun GlowingBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw deep tech background
        drawRect(color = CyberObsidian)
        
        // Draw cybernetic grid lines
        val gridSpacing = 36.dp.toPx()
        val gridColor = Color(0xFF1E293B).copy(alpha = 0.12f)
        var x = 0f
        while (x < size.width) {
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
            x += gridSpacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
            y += gridSpacing
        }
        
        // Draw neon atmospheric glow blobs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyberPrimary.copy(alpha = 0.08f), Color.Transparent),
                radius = 350.dp.toPx()
            ),
            center = Offset(0f, 0f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyberSecondary.copy(alpha = 0.08f), Color.Transparent),
                radius = 450.dp.toPx()
            ),
            center = Offset(size.width, size.height * 0.4f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyberTertiary.copy(alpha = 0.05f), Color.Transparent),
                radius = 400.dp.toPx()
            ),
            center = Offset(size.width * 0.3f, size.height)
        )
    }
}
