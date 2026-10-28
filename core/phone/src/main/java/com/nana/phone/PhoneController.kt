package com.nana.phone

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telecom.TelecomManager
import com.nana.common.ActionLog
import com.nana.common.Result
import com.nana.logging.NanaLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phone basics: calls, SMS, alarms, timers, toggles, system info.
 */
class PhoneController(private val context: Context) {

    // Calls
    suspend fun call(number: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("CALL_FAILED", "Failed to make call", e))
        }
    }

    // SMS
    suspend fun sendSms(number: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("SMS_FAILED", "Failed to send SMS", e))
        }
    }

    // Alarms
    suspend fun setAlarm(hour: Int, minute: Int, label: String? = null, days: List<Int>? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
                putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
                label?.let { putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, it) }
                days?.let { putExtra(android.provider.AlarmClock.EXTRA_DAYS, it.toIntArray()) }
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("ALARM_FAILED", "Failed to set alarm", e))
        }
    }

    // Timers
    suspend fun setTimer(seconds: Long, label: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
                label?.let { putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, it) }
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("TIMER_FAILED", "Failed to set timer", e))
        }
    }

    // Flashlight
    suspend fun toggleFlashlight(): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult(false, "No flashlight available")

            val isOn = isFlashlightOn(cameraManager, cameraId)
            cameraManager.setTorchMode(cameraId, !isOn)
            ActionResult(true, if (isOn) "Flashlight off" else "Flashlight on")
        } catch (e: Exception) {
            NanaLogger.e("PhoneController", "Flashlight toggle failed", e)
            ActionResult(false, "Flashlight toggle failed: ${e.message}")
        }
    }

    private fun isFlashlightOn(cameraManager: android.hardware.camera2.CameraManager, cameraId: String): Boolean {
        try {
            return cameraManager.getCameraCharacteristics(cameraId)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                // Note: No direct API to query torch state, track internally or use camera
                false // Default to off
        } catch (e: Exception) {
            return false
        }
    }

    // WiFi
    suspend fun toggleWifi(): ActionResult {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val enabled = wifiManager.isWifiEnabled
            wifiManager.isWifiEnabled = !enabled
            ActionResult(true, if (enabled) "Wi-Fi off" else "Wi-Fi on")
        } catch (e: Exception) {
            ActionResult(false, "Wi-Fi toggle failed: ${e.message}")
        }
    }

    // Bluetooth
    suspend fun toggleBluetooth(): ActionResult {
        return try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) return ActionResult(false, "Bluetooth not supported")
            
            val enabled = bluetoothAdapter.isEnabled
            if (enabled) {
                bluetoothAdapter.disable()
            } else {
                bluetoothAdapter.enable()
            }
            ActionResult(true, if (enabled) "Bluetooth off" else "Bluetooth on")
        } catch (e: Exception) {
            ActionResult(false, "Bluetooth toggle failed: ${e.message}")
        }
    }

    // Do Not Disturb
    suspend fun toggleDnd(): ActionResult {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isDnd = notificationManager.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                val mode = if (isDnd) android.app.NotificationManager.INTERRUPTION_FILTER_ALL else android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                notificationManager.setInterruptionFilter(mode)
                ActionResult(true, if (isDnd) "Do Not Disturb off" else "Do Not Disturb on")
            } else {
                ActionResult(false, "DND toggle requires Android 6+")
            }
        } catch (e: Exception) {
            ActionResult(false, "DND toggle failed: ${e.message}")
        }
    }

    // Brightness
    suspend fun setBrightness(level: Int): ActionResult { // 0-255
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val contentResolver = context.contentResolver
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, level.coerceIn(0, 255))
                ActionResult(true, "Brightness set to ${(level * 100 / 255)}%")
            } else {
                ActionResult(false, "Brightness control requires Android 6+")
            }
        } catch (e: Exception) {
            ActionResult(false, "Brightness set failed: ${e.message}")
        }
    }

    // Battery info
    fun getBatteryInfo(): ActionResult {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
        val charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
        val statusStr = when (status) {
            android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            android.os.BatteryManager.BATTERY_STATUS_FULL -> "full"
            android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not charging"
            else -> "unknown"
        }
        
        return ActionResult(true, "Battery: $level% $statusStr${if (charging) " ⚡" else ""}")
    }

    // Storage info
    fun getStorageInfo(): ActionResult {
        return try {
            val file = context.filesDir
            val total = file.totalSpace
            val free = file.freeSpace
            val used = total - free
            
            ActionResult(true, "Storage: ${formatBytes(free)} free of ${formatBytes(total)} (${used * 100 / total}% used)")
        } catch (e: Exception) {
            ActionResult(false, "Storage info failed: ${e.message}")
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes > 1_000_000_000_000L -> "${bytes / 1_000_000_000_000L} TB"
            bytes > 1_000_000_000L -> "${bytes / 1_000_000_000L} GB"
            bytes > 1_000_000L -> "${bytes / 1_000_000L} MB"
            else -> "${bytes / 1000} KB"
        }
    }

    // Calendar events (requires READ_CALENDAR permission)
    suspend fun getCalendarEvents(days: Int = 7): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        // TODO: Implement calendar query via CalendarContract
        Result.Success(emptyList())
    }

    // Contacts lookup (requires READ_CONTACTS permission)
    suspend fun lookupContact(name: String): Result<List<Contact>> = withContext(Dispatchers.IO) {
        // TODO: Implement contacts query via ContactsContract
        Result.Success(emptyList())
    }

    data class ActionResult(val success: Boolean, val message: String)
    data class CalendarEvent(val title: String, val startTime: Long, val endTime: Long, val location: String?)
    data class Contact(val name: String, val phoneNumbers: List<String>, val emails: List<String>)
}
