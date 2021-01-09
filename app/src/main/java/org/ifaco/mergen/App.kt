package org.ifaco.mergen

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class App : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig() = Camera2Config.defaultConfig()
}
