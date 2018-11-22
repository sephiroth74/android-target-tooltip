package it.sephiroth.android.library.tooltip_demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import it.sephiroth.android.library.xtooltip.Typefaces
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val metrics = resources.displayMetrics

        button1.setOnClickListener { button ->

            val gravity = Tooltip.Gravity.valueOf(spinner_gravities.selectedItem.toString())
            val closePolicy = getClosePolicy()
            val typeface = if (checkbox_font.isChecked) Typefaces[this, "fonts/at.ttc"] else null
            val animation = if (checkbox_animation.isChecked) Tooltip.Animation.DEFAULT else null
            val showDuration = text_duration.text.toString().toLong()
            val fadeDuration = text_fade.text.toString().toLong()
            val arrow = checkbox_arrow.isChecked
            val overlay = checkbox_overlay.isChecked
            val text =
                    if (text_tooltip.text.isNullOrEmpty()) "Lorem ipsum dolor sit amet" else text_tooltip.text!!.toSpannable()

            Timber.v("gravity: $gravity")
            Timber.v("closePolicy: $closePolicy")

            Tooltip.Builder(this)
                .anchor(button, 0, 0, false)
                .text(text)
                .typeface(typeface)
                .maxWidth(metrics.widthPixels / 2)
                .arrow(arrow)
                .floatingAnimation(animation)
                .closePolicy(closePolicy)
                .showDuration(showDuration)
                .fadeDuration(fadeDuration)
                .overlay(overlay)
                .create()
                .show(button, gravity, true)
        }
    }

    private fun getClosePolicy(): ClosePolicy {
        val builder = ClosePolicy.Builder()
        builder.inside(switch1.isChecked)
        builder.outside(switch3.isChecked)
        builder.consume(switch2.isChecked)
        return builder.build()
    }

}
