package com.nana.phone

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.nana.common.Result

/**
 * System toggle utilities.
 */
object SystemToggles {
    fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    fun setWifiEnabled(context: Context, enabled: Boolean): Result<Unit> {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled = enabled
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("WIFI_TOGGLE_FAILED", "Failed to toggle WiFi", e))
        }
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        return adapter?.isEnabled == true
    }

    fun setBluetoothEnabled(context: Context, enabled: Boolean): Result<Unit> {
        return try {
            val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            adapter?.let {
                if (enabled) it.enable() else it.disable()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("BLUETOOTH_TOGGLE_FAILED", "Failed to toggle Bluetooth", e))
        }
    }

    fun isDndEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            return notificationManager.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_NONE
        }
        return false
    }

    fun setDndEnabled(context: Context, enabled: Boolean): Result<Unit> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.setInterruptionFilter(if (enabled) {
                    android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                } else {
                    android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                })
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(Result.Error("DND_TOGGLE_FAILED", "Failed to toggle DND", e))
            }
        } else {
            Result.Failure(Result.Error("UNSUPPORTED", "DND requires Android 6+"))
        }
    }

    fun getBrightness(context: Context): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            -1
        }
    }

    fun setBrightness(context: Context, level: Int): Result<Unit> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, level.coerceIn(0, 255))
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Failure(Result.Error("BRIGHTNESS_FAILED", "Failed to set brightness", e))
            }
        } else {
            Result.Failure(Result.Error("UNSUPPORTED", "Brightness requires Android 6+"))
        }
    }

    fun isFlashlightOn(context: Context): Boolean {
        // No direct API, would need camera2
        return false
    }

    fun setFlashlight(context: Context, enabled: Boolean): Result<Unit> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val c = cameraManager.getCameraCharacteristics(id)
                c.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.Failure(Result.Error("NO_FLASH", "No flashlight available"))
            
            cameraManager.setTorchMode(cameraId, enabled)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(Result.Error("FLASHLIGHT_FAILED", "Failed to toggle flashlight", e))
        }
    }
}
