package ir.mahdiparastesh.mergen.man

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class NetworkDiscovery(
    val manager: WifiP2pManager,
    val channel: WifiP2pManager.Channel,
    val that: AppCompatActivity
) : BroadcastReceiver() {

    companion object {
        var registered = false
        val filters = IntentFilter().apply {
            //addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        /*fun canDiscover(that: AppCompatActivity) = ActivityCompat.checkSelfPermission(
            that, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED*/
    }

    override fun onReceive(c: Context?, intent: Intent?) {
        if (intent?.action == null) return
        Toast.makeText(c, intent.action, Toast.LENGTH_SHORT).show()
        @SuppressLint("MissingPermission")
        when (intent.action) {
            /*WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION ->
                if (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) ==
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED
                ) Toast.makeText(c, "Wifi P2P is enabled", Toast.LENGTH_SHORT).show()
                else Toast.makeText(c, "Wifi P2P is not enabled", Toast.LENGTH_SHORT).show()*/
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                    Toast.makeText(c, peers.toString(), Toast.LENGTH_LONG).show()
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
            }
        }
    }
}