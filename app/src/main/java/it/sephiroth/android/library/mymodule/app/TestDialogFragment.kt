package it.sephiroth.android.library.mymodule.app;

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.XTooltip
import kotlinx.android.synthetic.main.dialog_fragment.*

class TestDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button1.setOnClickListener { button ->
            XTooltip.Builder(context!!)
                .anchor(button, 0, 0, false)
                .closePolicy(ClosePolicy.TOUCH_ANYWHERE_CONSUME)
                .fadeDuration(200)
                .showDuration(0)
                .text("This is a dialog")
                .create()
                .show(button, XTooltip.Gravity.TOP, false)
        }
    }

    companion object {
        fun newInstance(): TestDialogFragment {
            val frag = TestDialogFragment()
            return frag
        }
    }
}