package it.sephiroth.android.library.mymodule.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Created by alessandro on 04/09/14.
 */
public class MyTextView extends TextView {
    public static interface OnAttachStatusListener {
        void onAttachedtoWindow(View view);

        void onDetachedFromWindow(View view);

        void onFinishTemporaryDetach(View view);
    }

    OnAttachStatusListener mListener;

    public MyTextView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnAttachStatusListener(OnAttachStatusListener listener) {
        mListener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        //		Log.i(VIEW_LOG_TAG, "onDetachedFromWindow");

        if (null != mListener) {
            mListener.onDetachedFromWindow(this);
        }
    }

    @Override
    public void onFinishTemporaryDetach() {
        super.onFinishTemporaryDetach();

        //		Log.i(VIEW_LOG_TAG, "onFinishTemporaryDetach: " + mListener);

        if (null != mListener) {
            mListener.onFinishTemporaryDetach(this);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        //		Log.i(VIEW_LOG_TAG, "onAttachedToWindow: " + mListener);

        if (null != mListener) {
            mListener.onAttachedtoWindow(this);
        }
    }
}
