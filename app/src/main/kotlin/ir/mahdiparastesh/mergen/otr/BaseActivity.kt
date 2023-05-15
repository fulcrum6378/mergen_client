package ir.mahdiparastesh.mergen.otr

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ir.mahdiparastesh.mergen.Model
import ir.mahdiparastesh.mergen.R

abstract class BaseActivity : ComponentActivity() {
    lateinit var c: Context
    lateinit var m: Model
    lateinit var sp: SharedPreferences
    lateinit var fRegular: Typeface
    lateinit var fBold: Typeface
    var dirLtr = true
    var dm = DisplayMetrics()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        c = applicationContext
        m = ViewModelProvider(this, Model.Factory()).get("Model", Model::class.java)

        dm = resources.displayMetrics
        dirLtr = resources.getBoolean(R.bool.dirLtr)
        sp = getPreferences(Context.MODE_PRIVATE)

        // Fonts
        fRegular = fonts(UiTools.Fonts.REGULAR)
        fBold = fonts(UiTools.Fonts.BOLD)
    }

    override fun setContentView(root: View) {
        super.setContentView(root)
        if (!dirLtr) root.layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    fun color(res: Int) = ContextCompat.getColor(c, res)

    fun drawable(res: Int) = ContextCompat.getDrawable(c, res)

    fun cf(res: Int = R.color.CS) = PorterDuffColorFilter(color(res), PorterDuff.Mode.SRC_IN)

    fun fonts(which: UiTools.Fonts): Typeface = Typeface.createFromAsset(c.assets, which.path)

    fun dp(px: Int = 0) = (dm.density * px.toFloat()).toInt()
}
