package ir.mahdiparastesh.mergen.otr

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import ir.mahdiparastesh.mergen.R

class UiTools {
    companion object {
        fun View.vis(b: Boolean = true) {
            visibility = if (b) View.VISIBLE else View.GONE
        }

        fun View.vish(b: Boolean = true) {
            visibility = if (b) View.VISIBLE else View.INVISIBLE
        }

        fun shift(v: ImageView, newRes: Int) {
            /*AnimatorSet().apply {
                duration = 192
                interpolator = LinearInterpolator()
                playTogether(
                    ObjectAnimator.ofFloat(v, "scaleX", 0f),
                    ObjectAnimator.ofFloat(v, "scaleY", 0f),
                    ObjectAnimator.ofFloat(v, "rotation", -360f)
                )
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {*/
            v.setImageResource(newRes)
            /*reverse()
            start()
        }
    })
    start()
}*/
        }

        fun drown(v: View, bb: Boolean = false) {
            if (bb) v.vish()
            AnimatorSet().apply {
                duration = 192
                interpolator = LinearInterpolator()
                playTogether(
                    ObjectAnimator.ofFloat(v, "scaleX", if (bb) 1f else 0f),
                    ObjectAnimator.ofFloat(v, "scaleY", if (bb) 1f else 0f),
                    ObjectAnimator.ofFloat(v, "rotation", if (bb) 0f else -360f)
                )
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        if (!bb) v.vish(false)
                    }
                })
                start()
            }
        }

        fun whirl(v: View, oa: ObjectAnimator? = null): ObjectAnimator? {
            drown(v, oa == null)
            return if (oa != null) {
                oa.cancel()
                null
            } else ObjectAnimator.ofFloat(v, "rotation", 0f, 360f).apply {
                duration = 343
                interpolator = LinearInterpolator()
                repeatCount = Animation.INFINITE
                start()
            }
        }

        fun fade(v: View, bb: Boolean = true) {
            if (bb) v.vis()
            ObjectAnimator.ofFloat(v, "alpha", if (bb) 1f else 0f).apply {
                duration = 192
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        if (!bb) v.vis(false)
                    }
                })
                start()
            }
        }

        fun pieChart(c: BaseActivity, percent: Int = 0, col: Int = c.color(R.color.CS)) =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.SWEEP_GRADIENT
                colors = intArrayOf(
                    col, col, col, col, col, col, col, col, col, col, Color.TRANSPARENT
                )
                useLevel = true
                level = percent * 100
            }

        /*fun shake(dur: Long = 78L) {
            //<uses-permission android:name="android.permission.VIBRATE" />
            val v = c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(dur)
        }*/

        fun permGranted(c: Context, perm: String) =
            ActivityCompat.checkSelfPermission(c, perm) == PackageManager.PERMISSION_GRANTED

        fun z(s: String): String {
            var add = ""
            for (x in 0..(9 - s.length)) add += "0"
            return add + s
        }
    }

    enum class Fonts(val path: String) {
        REGULAR("trebuchet.ttf"),
        BOLD("trebuchet_bold.ttf")
    }
}
