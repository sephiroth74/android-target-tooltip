package it.sephiroth.android.library.xtooltip

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.annotation.*
import android.text.Html
import android.text.Spannable
import android.view.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED
import android.widget.TextView
import it.sephiroth.android.library.tooltip.R
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by alessandro crugnola on 12/12/15.
 * alessandro.crugnola@gmail.com
 *
 *
 * LICENSE
 * Copyright 2015 Alessandro Crugnola
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
class XTooltip private constructor(private val context: Context, builder: Builder) {

    private val windowManager: WindowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var isShowing = false
        private set

    private val mGravities = Gravity.values().filter { it != Gravity.CENTER }
    private var isVisible = false
    private val mSizeTolerance = context.resources.displayMetrics.density * 10

    private val mLayoutInsetDecor = true
    private val mWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
    private val mSoftInputMode = INPUT_METHOD_NOT_NEEDED
    private val mHandler = Handler()

    private var mPopupView: TooltipViewContainer? = null
    private var mText: CharSequence?
    private var mAnchorPoint: Point
    private var mShowArrow: Boolean
    private var mPadding: Int = 0
    private var mActivateDelay: Long
    private var mClosePolicy: ClosePolicy
    private var mFadeDuration: Long
    private var mShowDuration: Long
    private var mMaxWidth: Int? = null
    private var mTextAppearance: Int
    private var mTextGravity: Int
    private var mTextViewElevation: Float
    private var mTypeface: Typeface? = null
    private var mIsCustomView: Boolean = false
    private var mTooltipLayoutIdRes = R.layout.textview
    private var mTextViewIdRes = android.R.id.text1
    private var mFloatingAnimation: Animation?
    private var mAnimator: ValueAnimator? = null
    private var mShowOverlay: Boolean
    private var mOverlayStyle: Int
    private var mActivated = false
    private var mHasAnchorView = false
    private var mFollowAnchor = false

    private var mViewOverlay: TooltipOverlay? = null
    private var mDrawable: TooltipTextDrawable? = null
    private var mAnchorView: WeakReference<View>? = null
    private lateinit var mContentView: View
    private lateinit var mTextView: TextView

    private val hideRunnable = Runnable { hide() }
    private val activateRunnable = Runnable { mActivated = true }

    var contentView: View? = null
        get() = mContentView
        private set

    private var predrawListener = ViewTreeObserver.OnPreDrawListener {
        if (mHasAnchorView && null != mAnchorView?.get()) {
            val view = mAnchorView?.get()!!
            if (!view.viewTreeObserver.isAlive) {
                removeListeners(view)
            } else {
                if (isShowing && null != mPopupView) {
                    view.getLocationOnScreen(mNewLocation)

                    if (mOldLocation == null) {
                        mOldLocation = intArrayOf(mNewLocation[0], mNewLocation[1])
                    }

                    if (mOldLocation!![0] != mNewLocation[1] || mOldLocation!![1] != mNewLocation[1]) {
                        offsetBy(
                                mNewLocation[0] - mOldLocation!![0],
                                mNewLocation[1] - mOldLocation!![1]
                        )
                    }
                }
            }
        }
        true
    }

    init {
        val theme = context.theme
                .obtainStyledAttributes(
                        null,
                        R.styleable.TooltipLayout,
                        builder.defStyleAttr,
                        builder.defStyleRes
                )
        this.mPadding = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_padding, 30)
        this.mTextAppearance =
                theme.getResourceId(R.styleable.TooltipLayout_android_textAppearance, 0)
        this.mTextGravity = theme
                .getInt(
                        R.styleable.TooltipLayout_android_gravity,
                        android.view.Gravity.TOP or android.view.Gravity.START
                )
        this.mTextViewElevation = theme.getDimension(R.styleable.TooltipLayout_ttlm_elevation, 0f)
        mOverlayStyle =
                theme.getResourceId(
                        R.styleable.TooltipLayout_ttlm_overlayStyle,
                        R.style.ToolTipOverlayDefaultStyle
                )
        val font = theme.getString(R.styleable.TooltipLayout_ttlm_font)
        theme.recycle()

        this.mText = builder.text
        this.mActivateDelay = builder.activateDelay
        this.mAnchorPoint = builder.point!!
        this.mClosePolicy = builder.closePolicy
        this.mMaxWidth = builder.maxWidth
        this.mFloatingAnimation = builder.floatingAnimation
        this.mShowDuration = builder.showDuration
        this.mFadeDuration = builder.fadeDuration
        this.mShowOverlay = builder.overlay
        this.mShowArrow = builder.showArrow && builder.layoutId == null
        builder.anchorView?.let {
            this.mAnchorView = WeakReference(it)
            this.mHasAnchorView = true
            this.mFollowAnchor = builder.followAnchor
        }

        builder.layoutId?.let {
            mTextViewIdRes = builder.textId!!
            mTooltipLayoutIdRes = builder.layoutId!!
            mIsCustomView = true
        } ?: run {
            mDrawable = TooltipTextDrawable(context, builder)
        }

        builder.typeface?.let {
            mTypeface = it
        } ?: run {
            font?.let { mTypeface = Typefaces[context, it] }
        }
    }

    private var mFailureFunc: ((tooltip: XTooltip) -> Unit)? = null
    private var mShownFunc: ((tooltip: XTooltip) -> Unit)? = null
    private var mHiddenFunc: ((tooltip: XTooltip) -> Unit)? = null

    @Suppress("UNUSED")
    fun doOnFailure(func: ((tooltip: XTooltip) -> Unit)?): XTooltip {
        mFailureFunc = func
        return this
    }

    @Suppress("UNUSED")
    fun doOnShown(func: ((tooltip: XTooltip) -> Unit)?): XTooltip {
        mShownFunc = func
        return this
    }

    @Suppress("UNUSED")
    fun doOnHidden(func: ((tooltip: XTooltip) -> Unit)?): XTooltip {
        mHiddenFunc = func
        return this
    }

    @SuppressLint("RtlHardcoded")
    private fun createPopupLayoutParams(token: IBinder): WindowManager.LayoutParams {
        val p = WindowManager.LayoutParams()
        p.gravity = android.view.Gravity.LEFT or android.view.Gravity.TOP
        p.width = WindowManager.LayoutParams.MATCH_PARENT
        p.height = WindowManager.LayoutParams.MATCH_PARENT
        p.format = PixelFormat.TRANSLUCENT
        p.flags = computeFlags(p.flags)
        p.type = mWindowLayoutType
        p.token = token
        p.softInputMode = mSoftInputMode
        p.title = "ToolTip:" + Integer.toHexString(hashCode())
        return p
    }


    private fun computeFlags(curFlags: Int): Int {
        var curFlags1 = curFlags
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        curFlags1 = if (mClosePolicy.inside() || mClosePolicy.outside()) {
            curFlags1 and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            curFlags1 or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        if (!mClosePolicy.consume()) {
            curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        // curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
        return curFlags1
    }

    @Suppress("UNUSED_PARAMETER")
    private fun preparePopup(params: WindowManager.LayoutParams, gravity: Gravity) {
        mPopupView?.let {
            if (mViewOverlay != null && gravity == Gravity.CENTER) {
                it.removeView(mViewOverlay)
                mViewOverlay = null
            }
        } ?: run {
            val viewContainer = TooltipViewContainer(context)

            if (mShowOverlay && mViewOverlay == null) {
                mViewOverlay = TooltipOverlay(context, 0, mOverlayStyle)
                with(mViewOverlay!!) {
                    adjustViewBounds = true
                    layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }

            val contentView =
                    LayoutInflater.from(context).inflate(mTooltipLayoutIdRes, viewContainer, false)

            mFloatingAnimation?.let { contentView.setPadding(it.radius, it.radius, it.radius, it.radius) }

            mTextView = contentView.findViewById(mTextViewIdRes)

            with(mTextView) {
                mDrawable?.let { background = it }

                if (mShowArrow)
                    setPadding(mPadding, mPadding, mPadding, mPadding)
                else
                    setPadding(mPadding / 2, mPadding / 2, mPadding / 2, mPadding / 2)

                if (mTextAppearance != 0) {
                    @Suppress("DEPRECATION")
                    setTextAppearance(context, mTextAppearance)
                }

                if (!mIsCustomView && mTextViewElevation > 0 && Build.VERSION.SDK_INT >= 21) {
                    elevation = mTextViewElevation
                    translationZ = mTextViewElevation
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                }
                this.gravity = mTextGravity

                text = if (mText is Spannable) {
                    mText
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(this@XTooltip.mText as String)
                }

                mMaxWidth?.let { maxWidth = it }
                mTypeface?.let { typeface = it }
            }

            if (null != mViewOverlay) {
                viewContainer.addView(
                        mViewOverlay,
                        FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                )
            }

            viewContainer.addView(contentView, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
            viewContainer.measureAllChildren = true
            viewContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

            Timber.i("viewContainer size: ${viewContainer.measuredWidth}, ${viewContainer.measuredHeight}")
            Timber.i("contentView size: ${contentView.measuredWidth}, ${contentView.measuredHeight}")

            mTextView.addOnAttachStateChangeListener {
                onViewAttachedToWindow { _: View?, _: View.OnAttachStateChangeListener ->
                    mAnimator?.start()

                    if (mShowDuration > 0) {
                        mHandler.removeCallbacks(hideRunnable)
                        mHandler.postDelayed(hideRunnable, mShowDuration)
                    }

                    mHandler.removeCallbacks(activateRunnable)
                    mHandler.postDelayed(activateRunnable, mActivateDelay)
                }

                onViewDetachedFromWindow { view: View?, listener: View.OnAttachStateChangeListener ->
                    view?.removeOnAttachStateChangeListener(listener)
                    mAnimator?.cancel()
                    removeCallbacks()
                }
            }

            mContentView = contentView
            mPopupView = viewContainer
        }
    }

    private fun findPosition(
            parent: View,
            anchor: View?,
            offset: Point,
            gravities: ArrayList<Gravity>,
            params: WindowManager.LayoutParams,
            fitToScreen: Boolean = false
    ): Positions? {

        if (null == mPopupView) return null
        if (gravities.isEmpty()) return null

        val gravity = gravities.removeAt(0)

        Timber.i("findPosition. $gravity, offset: $offset")

        val displayFrame = Rect()
        val anchorPosition = intArrayOf(0, 0)
        val centerPosition = Point(offset)

        parent.getWindowVisibleDisplayFrame(displayFrame)

        anchor?.let {
            anchor.getLocationOnScreen(anchorPosition)
            centerPosition.x += anchorPosition[0] + anchor.width / 2
            centerPosition.y += anchorPosition[1] + anchor.height / 2

            when (gravity) {
                Gravity.LEFT -> {
                    anchorPosition[1] += anchor.height / 2
                }
                Gravity.RIGHT -> {
                    anchorPosition[0] += anchor.width
                    anchorPosition[1] += anchor.height / 2
                }
                Gravity.TOP -> {
                    anchorPosition[0] += anchor.width / 2
                }
                Gravity.BOTTOM -> {
                    anchorPosition[0] += anchor.width / 2
                    anchorPosition[1] += anchor.height
                }
                Gravity.CENTER -> {
                    anchorPosition[0] += anchor.width / 2
                    anchorPosition[1] += anchor.height / 2
                }
            }
        }

        anchorPosition[0] += offset.x
        anchorPosition[1] += offset.y

        Timber.d("anchorPosition: ${anchorPosition[0]}, ${anchorPosition[1]}")
        Timber.d("centerPosition: $centerPosition")
        Timber.d("displayFrame: $displayFrame")

        val w: Int = mContentView.measuredWidth
        val h: Int = mContentView.measuredHeight

        Timber.v("contentView size: $w, $h")

        val contentPosition = Point()
        val arrowPosition = Point()
        val radius = (mFloatingAnimation?.radius ?: run { 0 })

        when (gravity) {
            Gravity.LEFT -> {
                contentPosition.x = anchorPosition[0] - w
                contentPosition.y = anchorPosition[1] - h / 2
                arrowPosition.y = h / 2 - mPadding / 2 - radius
            }
            Gravity.TOP -> {
                contentPosition.x = anchorPosition[0] - w / 2
                contentPosition.y = anchorPosition[1] - h
                arrowPosition.x = w / 2 - mPadding / 2 - radius
            }
            Gravity.RIGHT -> {
                contentPosition.x = anchorPosition[0]
                contentPosition.y = anchorPosition[1] - h / 2
                arrowPosition.y = h / 2 - mPadding / 2 - radius
            }
            Gravity.BOTTOM -> {
                contentPosition.x = anchorPosition[0] - w / 2
                contentPosition.y = anchorPosition[1]
                arrowPosition.x = w / 2 - mPadding / 2 - radius
            }
            Gravity.CENTER -> {
                contentPosition.x = anchorPosition[0] - w / 2
                contentPosition.y = anchorPosition[1] - h / 2
            }
        }

        anchor?.let {
            // pass
        } ?: run {
            mViewOverlay?.let {
                when (gravity) {
                    Gravity.LEFT -> contentPosition.x -= it.measuredWidth / 2
                    Gravity.RIGHT -> contentPosition.x += it.measuredWidth / 2

                    Gravity.TOP -> contentPosition.y -= it.measuredHeight / 2
                    Gravity.BOTTOM -> contentPosition.y += it.measuredHeight / 2
                    Gravity.CENTER -> {
                    }
                }
            }
        }

        Timber.d("arrowPosition: $arrowPosition")
        Timber.d("centerPosition: $centerPosition")
        Timber.d("contentPosition: $contentPosition")

        if (fitToScreen) {
            val finalRect = Rect(
                    contentPosition.x,
                    contentPosition.y,
                    contentPosition.x + w,
                    contentPosition.y + h
            )
            if (!displayFrame.rectContainsWithTolerance(finalRect, mSizeTolerance.toInt())) {
                Timber.e("content won't fit! $displayFrame, $finalRect")
                return findPosition(parent, anchor, offset, gravities, params, fitToScreen)
            }
        }

        return Positions(arrowPosition, centerPosition, contentPosition, gravity, params)
    }

    private var mCurrentPosition: Positions? = null
    private var mOldLocation: IntArray? = null
    private var mNewLocation: IntArray = intArrayOf(0, 0)

    private fun invokePopup(positions: Positions?): XTooltip? {
        positions?.let {
            isShowing = true
            mCurrentPosition = positions

            setupAnimation(positions.gravity)

            if (mHasAnchorView && mAnchorView?.get() != null) {
                setupListeners(mAnchorView!!.get()!!)
            }

            mDrawable?.setAnchor(
                    it.gravity,
                    if (!mShowArrow) 0 else mPadding / 2,
                    if (!mShowArrow) null else it.arrowPoint
            )

            offsetBy(0, 0)

            it.params.packageName = context.packageName
            mPopupView?.fitsSystemWindows = mLayoutInsetDecor
            windowManager.addView(mPopupView, it.params)
            Timber.v("windowManager.addView: $mPopupView")
            fadeIn(mFadeDuration)
            return this
        } ?: run {
            mFailureFunc?.invoke(this)
            return null
        }
    }

    private fun offsetBy(xoff: Int, yoff: Int) {
        if (isShowing && mPopupView != null && mCurrentPosition != null) {
            Timber.i("offsetBy($xoff, $yoff)")
            mContentView.translationX = mCurrentPosition!!.contentPoint.x.toFloat() + xoff
            mContentView.translationY = mCurrentPosition!!.contentPoint.y.toFloat() + yoff

            mViewOverlay?.let { viewOverlay ->
                viewOverlay.translationX = mCurrentPosition!!.centerPoint.x.toFloat() - viewOverlay.measuredWidth /
                        2 + xoff
                viewOverlay.translationY = mCurrentPosition!!.centerPoint.y.toFloat() - viewOverlay.measuredHeight /
                        2 + yoff
            }
        }
    }

    private fun setupListeners(anchorView: View) {
        anchorView.addOnAttachStateChangeListener {
            onViewDetachedFromWindow { view: View?, listener: View.OnAttachStateChangeListener ->
                Timber.i("anchorView detached from parent")
                view?.removeOnAttachStateChangeListener(listener)
                dismiss()
            }
        }

        if (mFollowAnchor) {
            anchorView.viewTreeObserver.addOnPreDrawListener(predrawListener)
        }
    }

    private fun removeListeners(anchorView: View?) {
        if (mFollowAnchor) {
            anchorView?.viewTreeObserver?.removeOnPreDrawListener(predrawListener)
        }
    }

    private fun setupAnimation(gravity: Gravity) {
        if (mTextView === mContentView || null == mFloatingAnimation) {
            return
        }

        val endValue = mFloatingAnimation!!.radius
        val duration = mFloatingAnimation!!.duration

        val direction: Int = if (mFloatingAnimation!!.direction == 0) {
            if (gravity === Gravity.TOP || gravity === Gravity.BOTTOM) 2 else 1
        } else {
            mFloatingAnimation!!.direction
        }

        val property = if (direction == 2) "translationY" else "translationX"
        mAnimator =
                ObjectAnimator.ofFloat(mTextView, property, -endValue.toFloat(), endValue.toFloat())
        mAnimator!!.run {
            setDuration(duration)
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
    }

    fun show(parent: View, gravity: Gravity, fitToScreen: Boolean = false) {
        if (isShowing || (mHasAnchorView && mAnchorView?.get() == null)) return

        isVisible = false

        val params = createPopupLayoutParams(parent.windowToken)
        preparePopup(params, gravity)

        val gravities = mGravities.toCollection(ArrayList())
        gravities.remove(gravity)
        gravities.add(0, gravity)

        invokePopup(
                findPosition(
                        parent,
                        mAnchorView?.get(),
                        mAnchorPoint,
                        gravities,
                        params,
                        fitToScreen
                )
        )
    }

    fun hide() {
        Timber.i("hide")
        if (!isShowing) return
        fadeOut(mFadeDuration)
    }

    fun dismiss() {
        if (isShowing && mPopupView != null) {
            removeListeners(mAnchorView?.get())
            removeCallbacks()
            windowManager.removeView(mPopupView)
            Timber.v("dismiss: $mPopupView")
            mPopupView = null
            isShowing = false
            isVisible = false

            mHiddenFunc?.invoke(this)
        }
    }

    private fun removeCallbacks() {
        mHandler.removeCallbacks(hideRunnable)
        mHandler.removeCallbacks(activateRunnable)
    }

    private fun fadeIn(fadeDuration: Long) {
        if (!isShowing || isVisible) return

        isVisible = true

        if (fadeDuration > 0 && null != mPopupView) {
            mPopupView!!.alpha = 0F
            mPopupView!!.animate()
                    .setDuration(mFadeDuration)
                    .alpha(1f).start()
        }
        mShownFunc?.invoke(this)
    }

    private fun fadeOut(fadeDuration: Long) {
        if (!isShowing || !isVisible) return

        isVisible = false
        removeCallbacks()

        Timber.i("fadeOut($fadeDuration)")

        if (fadeDuration > 0) {
            mPopupView?.let { popupView ->
                popupView.clearAnimation()
                popupView.animate()
                        .alpha(0f)
                        .setDuration(fadeDuration)
                        .setListener {
                            onAnimationEnd {
                                popupView.visibility = View.INVISIBLE
                                dismiss()
                            }
                        }
                        .start()
            }
        } else {
            dismiss()
        }
    }

    inner class TooltipViewContainer(context: Context) : FrameLayout(context) {

        init {
            clipChildren = false
            clipToPadding = false
        }

        private var sizeChange: ((w: Int, h: Int) -> Unit)? = null

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            sizeChange?.invoke(w, h)
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (!isShowing || !isVisible || !mActivated) return super.dispatchKeyEvent(event)
            Timber.i("dispatchKeyEvent: $event")

            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (keyDispatcherState == null) {
                    return super.dispatchKeyEvent(event)
                }

                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    keyDispatcherState?.startTracking(event, this)
                    return true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    val state = keyDispatcherState
                    if (state != null && state.isTracking(event) && !event.isCanceled) {
                        Timber.v("Back pressed, close the tooltip")
                        hide()
                        return true
                    }
                }
                return super.dispatchKeyEvent(event)
            } else {
                return super.dispatchKeyEvent(event)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!isShowing || !isVisible || !mActivated) return false

            Timber.i("onTouchEvent: $event")
            Timber.d("event position: ${event.x}, ${event.y}")

            val r1 = Rect()
            mTextView.getGlobalVisibleRect(r1)
            val containsTouch = r1.contains(event.x.toInt(), event.y.toInt())

            if (mClosePolicy.anywhere()) {
                hide()
            } else if (mClosePolicy.inside() && containsTouch) {
                hide()
            } else if (mClosePolicy.outside() && !containsTouch) {
                hide()
            }

            return mClosePolicy.consume()
        }
    }

    private data class Positions(
            val arrowPoint: Point,
            val centerPoint: Point,
            val contentPoint: Point,
            val gravity: Gravity,
            val params: WindowManager.LayoutParams
    )

    enum class Gravity {
        LEFT, RIGHT, TOP, BOTTOM, CENTER
    }

    data class Animation(val radius: Int, val direction: Int, val duration: Long) {

        @Suppress("unused")
        companion object {
            val DEFAULT = Animation(8, 0, 400)
            val SLOW = Animation(4, 0, 600)
        }
    }

    @Suppress("unused")
    class Builder(private val context: Context) {
        internal var point: Point? = null
        internal var closePolicy = ClosePolicy.TOUCH_INSIDE_CONSUME
        internal var text: CharSequence? = null
        internal var anchorView: View? = null
        internal var maxWidth: Int? = null
        internal var defStyleRes = R.style.ToolTipLayoutDefaultStyle
        internal var defStyleAttr = R.attr.ttlm_defaultStyle
        internal var typeface: Typeface? = null
        internal var overlay = true
        internal var floatingAnimation: Animation? = null
        internal var showDuration: Long = 0
        internal var fadeDuration: Long = 100
        internal var showArrow = true
        internal var activateDelay = 0L
        internal var followAnchor = false

        @LayoutRes
        internal var layoutId: Int? = null

        @IdRes
        internal var textId: Int? = null

        fun typeface(value: Typeface?): Builder {
            this.typeface = value
            return this
        }

        fun styleId(@StyleRes styleId: Int?): Builder {
            styleId?.let {
                this.defStyleAttr = 0
                this.defStyleRes = it
            } ?: run {
                this.defStyleRes = R.style.ToolTipLayoutDefaultStyle
                this.defStyleAttr = R.attr.ttlm_defaultStyle
            }
            return this
        }

        fun customView(@LayoutRes layoutId: Int, @IdRes textId: Int): Builder {
            this.layoutId = layoutId
            this.textId = textId
            return this
        }

        fun activateDelay(value: Long): Builder {
            this.activateDelay = value
            return this
        }

        fun arrow(value: Boolean): Builder {
            this.showArrow = value
            return this
        }

        fun fadeDuration(value: Long): Builder {
            this.fadeDuration = value
            return this
        }

        fun showDuration(value: Long): Builder {
            this.showDuration = value
            return this
        }

        fun floatingAnimation(value: Animation?): Builder {
            this.floatingAnimation = value
            return this
        }

        fun maxWidth(w: Int): Builder {
            this.maxWidth = w
            return this
        }

        fun maxWidth(res: Resources, @DimenRes dimension: Int): Builder {
            return maxWidth(res.getDimensionPixelSize(dimension))
        }

        fun overlay(value: Boolean): Builder {
            this.overlay = value
            return this
        }

        fun anchor(x: Int, y: Int): Builder {
            this.anchorView = null
            this.point = Point(x, y)
            return this
        }

        fun anchor(view: View, xoff: Int = 0, yoff: Int = 0, follow: Boolean = false): Builder {
            this.anchorView = view
            this.followAnchor = follow
            this.point = Point(xoff, yoff)
            return this
        }

        fun text(text: CharSequence): Builder {
            this.text = text
            return this
        }

        fun text(@StringRes text: Int): Builder {
            this.text = context.getString(text)
            return this
        }

        fun text(@StringRes text: Int, vararg args: Any): Builder {
            this.text = context.getString(text, args)
            return this
        }

        @JvmOverloads
        fun closePolicy(policy: ClosePolicy, milliseconds:Long = 0): Builder {
            this.closePolicy = policy
            this.showDuration = milliseconds
            Timber.v("closePolicy: $policy")
            return this
        }

        fun create(): XTooltip {
            if (null == anchorView && null == point) {
                throw IllegalArgumentException("missing anchor point or anchor view")
            }
            return XTooltip(context, this)
        }
    }
}

class ClosePolicy internal constructor(private val policy: Int) {

    fun consume() = policy and CONSUME == CONSUME

    fun inside(): Boolean {
        return policy and TOUCH_INSIDE == TOUCH_INSIDE
    }

    fun outside(): Boolean {
        return policy and TOUCH_OUTSIDE == TOUCH_OUTSIDE
    }

    fun anywhere() = inside() and outside()

    override fun toString(): String {
        return "ClosePolicy{policy: $policy, inside:${inside()}, outside: ${outside()}, anywhere: ${anywhere()}, consume: ${consume()}}"
    }

    @Suppress("unused")
    class Builder {
        private var policy = NONE

        fun consume(value: Boolean): Builder {
            policy = if (value) policy or CONSUME else policy and CONSUME.inv()
            return this
        }

        fun inside(value: Boolean): Builder {
            policy = if (value) policy or TOUCH_INSIDE else policy and TOUCH_INSIDE.inv()
            return this
        }

        fun outside(value: Boolean): Builder {
            policy = if (value) policy or TOUCH_OUTSIDE else policy and TOUCH_OUTSIDE.inv()
            return this
        }

        fun clear() {
            policy = NONE
        }

        fun build() = ClosePolicy(policy)
    }

    @Suppress("unused")
    companion object {
        private const val NONE = 0
        private const val TOUCH_INSIDE = 1 shl 1
        private const val TOUCH_OUTSIDE = 1 shl 2
        private const val CONSUME = 1 shl 3

        @JvmField
        val TOUCH_NONE = ClosePolicy(NONE)
        @JvmField
        val TOUCH_INSIDE_CONSUME = ClosePolicy(TOUCH_INSIDE or CONSUME)
        @JvmField
        val TOUCH_INSIDE_NO_CONSUME = ClosePolicy(TOUCH_INSIDE)
        @JvmField
        val TOUCH_OUTSIDE_CONSUME = ClosePolicy(TOUCH_OUTSIDE or CONSUME)
        @JvmField
        val TOUCH_OUTSIDE_NO_CONSUME = ClosePolicy(TOUCH_OUTSIDE)
        @JvmField
        val TOUCH_ANYWHERE_NO_CONSUME = ClosePolicy(TOUCH_INSIDE or TOUCH_OUTSIDE)
        @JvmField
        val TOUCH_ANYWHERE_CONSUME = ClosePolicy(TOUCH_INSIDE or TOUCH_OUTSIDE or CONSUME)
    }

}