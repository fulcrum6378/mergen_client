package org.ifaco.mergen

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

@Suppress("unused")
class App : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig() = Camera2Config.defaultConfig()
}

// adb tcpip 5555
// adb connect 192.168.1.4:5555
// adb kill-server
