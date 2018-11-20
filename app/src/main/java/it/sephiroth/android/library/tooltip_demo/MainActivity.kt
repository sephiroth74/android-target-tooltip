package it.sephiroth.android.library.tooltip_demo

import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import it.sephiroth.android.library.xtooltip.Tooltip.Gravity
import it.sephiroth.android.library.xtooltip.Typefaces
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    var tooltip: Tooltip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        fab.setOnClickListener { view: View ->

            if (false) {

                val dialog = AlertDialog
                    .Builder(this)
                    .setTitle("Titolo")
                    .setView(R.layout.custom_dialog)
                    .create()

                dialog.show()

                dialog.findViewById<Button>(R.id.button)?.setOnClickListener {
                    Tooltip.Builder(this@MainActivity)
                        .fadeDuration(200)
                        .text("This is a tooltip fellas!")
                        .anchor(it, 0, 0)
                        .maxWidth(400)
                        .floatingAnimation(Tooltip.Animation.DEFAULT)
                        .create()
                        .show(it, Gravity.LEFT, false)
                }

            } else {
                if (null != tooltip && tooltip!!.isShowing) {
                    tooltip!!.hide()
                } else {
                    val displayFrame = Rect()
                    fab.getWindowVisibleDisplayFrame(displayFrame)

                    tooltip = Tooltip
                        .Builder(this)
                        .closePolicy(ClosePolicy.TOUCH_NONE)
                        .typeface(Typefaces[this, "fonts/at.ttc"])
                        .fadeDuration(300)
                        .overlay(true)
                        .arrow(true)
                        .text("This is just a sample text\nfor the tooltip X!")
                        .anchor(findViewById(R.id.center_text), 0, 0)
                        .maxWidth(660)
                        .floatingAnimation(Tooltip.Animation.DEFAULT)
                        .create()
                        .doOnFailure { tooltip ->
                            tooltip.show(fab, Gravity.CENTER, false)
                        }
                        .doOnShown {
                            Timber.v("tooltip show")
                        }
                        .doOnHidden {
                            Timber.v("tooltip hidden")
                        }
                        .show(fab, Gravity.TOP, true)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
