package org.ifaco.mergen

import android.content.DialogInterface
import android.widget.EditText
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.ifaco.mergen.Fun.Companion.c
import org.ifaco.mergen.Fun.Companion.dm
import org.ifaco.mergen.Fun.Companion.dp
import org.ifaco.mergen.otr.AlertDialogue.Companion.alertDialogue2

class Client(that: Panel) {
    init {
        val et = EditText(c).apply {
            setPadding(dp(18), dp(12), dp(18), dp(12))
            textSize = that.resources.getDimension(R.dimen.alert1Title) / dm.density
            setText(R.string.cliDefIP)
        }
        alertDialogue2(
            that, R.string.cliConnect, R.string.cliConnectIP, et,
            DialogInterface.OnClickListener { dialog, which ->
                Volley.newRequestQueue(c).add(
                    StringRequest(Request.Method.GET, "http://${et.text}/", { res ->
                        Toast.makeText(c, res, Toast.LENGTH_LONG).show()
                    }, {
                        Toast.makeText(c, "$it", Toast.LENGTH_LONG).show()
                    }).setShouldCache(false).setTag("client").setRetryPolicy(
                        DefaultRetryPolicy(
                            5000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                        )
                    )
                )
            })
    }
}
