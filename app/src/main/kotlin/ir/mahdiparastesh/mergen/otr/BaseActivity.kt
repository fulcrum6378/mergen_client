package ir.mahdiparastesh.mergen.otr

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ir.mahdiparastesh.mergen.Model
import ir.mahdiparastesh.mergen.R

abstract class BaseActivity : ComponentActivity() {
    val c: Context get() = applicationContext
    lateinit var m: Model
    lateinit var sp: SharedPreferences
    val dm: DisplayMetrics by lazy { resources.displayMetrics }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
        sp = getPreferences(Context.MODE_PRIVATE)
    }

    fun color(res: Int) = ContextCompat.getColor(c, res)

    fun cf(res: Int = R.color.CS) = PorterDuffColorFilter(color(res), PorterDuff.Mode.SRC_IN)

    fun dp(px: Int = 0) = (dm.density * px.toFloat()).toInt()

    fun font(bold: Boolean = false) =
        resources.getFont(if (!bold) R.font.trebuchet_normal else R.font.trebuchet_bold)
}
