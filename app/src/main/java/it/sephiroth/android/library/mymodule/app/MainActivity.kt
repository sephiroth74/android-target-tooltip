package it.sephiroth.android.library.mymodule.app;

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.XTooltip
import it.sephiroth.android.library.xtooltip.Typefaces
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    var tooltip: XTooltip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val metrics = resources.displayMetrics

        button1.setOnClickListener { button ->

            val gravity = XTooltip.Gravity.valueOf(spinner_gravities.selectedItem.toString())
            val closePolicy = getClosePolicy()
            val typeface = if (checkbox_font.isChecked) Typefaces[this, "fonts/at.ttc"] else null
            val animation = if (checkbox_animation.isChecked) XTooltip.Animation.DEFAULT else null
            val showDuration = if (text_duration.text.isNullOrEmpty()) 0 else text_duration.text.toString().toLong()
            val fadeDuration = if (text_fade.text.isNullOrEmpty()) 0 else text_fade.text.toString().toLong()
            val arrow = checkbox_arrow.isChecked
            val overlay = checkbox_overlay.isChecked
            val style = if (checkbox_style.isChecked) R.style.ToolTipAltStyle else null
            val text =
                    if (text_tooltip.text.isNullOrEmpty()) "Lorem ipsum dolor" else text_tooltip.text!!.toString()

            Timber.v("gravity: $gravity")
            Timber.v("closePolicy: $closePolicy")

            tooltip = XTooltip.Builder(this)
                .anchor(button, 0, 0, false)
                .text(text)
                .styleId(style)
                .typeface(typeface)
                .maxWidth(metrics.widthPixels / 2)
                .arrow(arrow)
                .floatingAnimation(animation)
                .closePolicy(closePolicy)
                .showDuration(showDuration)
                .fadeDuration(fadeDuration)
                .overlay(overlay)
                .create()

            tooltip
                ?.doOnHidden {
                    tooltip = null
                }
                ?.doOnFailure { }
                ?.doOnShown {}
                ?.show(button, gravity, true)
        }

        button2.setOnClickListener {
            val fragment = TestDialogFragment.newInstance()
            fragment.show(supportFragmentManager, "test_dialog_fragment")
        }
    }

    private fun getClosePolicy(): ClosePolicy {
        val builder = ClosePolicy.Builder()
        builder.inside(switch1.isChecked)
        builder.outside(switch3.isChecked)
        builder.consume(switch2.isChecked)
        return builder.build()
    }

    override fun onDestroy() {
        Timber.i("onDestroy")
        super.onDestroy()
        tooltip?.dismiss()
    }

}
