package org.ifaco.mergen.ear

import android.Manifest
import androidx.core.app.ActivityCompat
import org.ifaco.mergen.Fun
import org.ifaco.mergen.Panel

class Recorder {
    companion object {
        const val reqRecPer = 786
        const val recPerm = Manifest.permission.RECORD_AUDIO

        fun recordPermission(that: Panel) {
            if (Fun.permGranted(recPerm))
                Panel.handler?.obtainMessage(Panel.Action.HEAR.ordinal)?.sendToTarget()
            else ActivityCompat.requestPermissions(that, arrayOf(recPerm), reqRecPer)
        }
    }

    // init {}
}
