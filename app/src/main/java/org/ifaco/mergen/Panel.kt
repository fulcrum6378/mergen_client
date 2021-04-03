package org.ifaco.mergen

import android.Manifest
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.permResult
import org.ifaco.mergen.databinding.PanelBinding
import org.ifaco.mergen.pro.Communicator
import org.ifaco.mergen.pro.Writer

class Panel : AppCompatActivity() {
    lateinit var b: PanelBinding
    val model: Model by viewModels()
    lateinit var pro: Writer
    lateinit var com: Communicator

    companion object {
        const val reqRecord = 786
        var handler: Handler? = null
        var mp: MediaPlayer? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)
        Fun.init(this, b.root)


        // Handlers
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Action.RECORD.ordinal -> {
                    }
                    Action.WRITE.ordinal -> msg.obj?.let { b.say.setText("$it") }
                    Action.PRONOUNCE.ordinal -> {
                        mp = MediaPlayer.create(this@Panel, msg.obj as Uri)
                        mp?.setOnPreparedListener { mp?.start() }
                        mp?.setOnCompletionListener { mp?.release(); mp = null }
                    }
                    Action.SOCKET.ordinal ->
                        Toast.makeText(c, msg.obj as String, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Pro
        pro = Writer(this, model, b.body, b.response, b.resSV, b.say, b.clear)
        com = Communicator(this, b.say, model)

        permissions()
    }

    override fun onDestroy() {
        try {
            mp?.release()
        } catch (ignored: Exception) {
        }
        mp = null
        handler = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            reqRecord -> if (permResult(grantResults))
                handler?.obtainMessage(Action.RECORD.ordinal)?.sendToTarget()
        }
    }


    fun permissions() {
        if (Fun.permGranted(Manifest.permission.RECORD_AUDIO) && Fun.permGranted(Manifest.permission.CAMERA))
            handler?.obtainMessage(Action.RECORD.ordinal)?.sendToTarget()
        else ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), reqRecord
        )
    }

    enum class Action { RECORD, WRITE, PRONOUNCE, SOCKET }
}
