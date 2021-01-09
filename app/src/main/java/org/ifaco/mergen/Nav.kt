package org.ifaco.mergen

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

class Nav {
    companion object {
        lateinit var locreq: LocationRequest
        var here: Location? = null
        const val reqLocPer = 508
        const val locPerm = Manifest.permission.ACCESS_COARSE_LOCATION
        const val reqLocSet = 666
        const val locInterval = 60000L * 2L


        @Suppress("unused")
        fun locationPermission(that: Panel) {
            if (ActivityCompat.checkSelfPermission(that, locPerm) == PackageManager.PERMISSION_GRANTED
            ) locationSettings(that)
            else ActivityCompat.requestPermissions(that, arrayOf(locPerm), reqLocPer)
        }

        fun locationSettings(that: Panel) {
            locreq = LocationRequest.create()
            locreq.interval = locInterval
            locreq.fastestInterval = locInterval / 2L
            locreq.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            val lrBuilder = LocationSettingsRequest.Builder().addLocationRequest(locreq)
            val lrTask = LocationServices.getSettingsClient(that)
                .checkLocationSettings(lrBuilder.build())
            lrTask.addOnSuccessListener(that) { locate(that) }
            lrTask.addOnFailureListener(that) { e ->
                if (e is ResolvableApiException) try {
                    e.startResolutionForResult(that, reqLocSet)
                } catch (ignored: IntentSender.SendIntentException) {
                }
            }
        }

        @SuppressLint("MissingPermission")
        fun locate(that: Panel) {
            var flpc = LocationServices.getFusedLocationProviderClient(Fun.c)
            flpc.requestLocationUpdates(
                locreq, object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        if (locationResult != null)
                            for (location in locationResult.locations)
                                if (location.time >= here?.time ?: 0) here = location
                    }
                }, Looper.getMainLooper()
            )
            flpc.lastLocation.addOnSuccessListener { location -> if (location != null) here = location }
                .addOnFailureListener { locationSettings(that) }
        }
    }
}