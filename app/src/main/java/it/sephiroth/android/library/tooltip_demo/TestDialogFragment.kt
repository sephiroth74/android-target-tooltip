package it.sephiroth.android.library.tooltip_demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import it.sephiroth.android.library.xtooltip.ClosePolicy
import it.sephiroth.android.library.xtooltip.Tooltip
import kotlinx.android.synthetic.main.dialog_fragment.*

class TestDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button1.setOnClickListener { button ->
            Tooltip.Builder(context!!)
                .anchor(button, 0, 0, false)
                .closePolicy(ClosePolicy.TOUCH_ANYWHERE_CONSUME)
                .showDuration(0)
                .text("This is a dialog")
                .create()
                .show(button, Tooltip.Gravity.TOP, false)
        }
    }

    companion object {
        fun newInstance(): TestDialogFragment {
            val frag = TestDialogFragment()
            return frag
        }
    }
}