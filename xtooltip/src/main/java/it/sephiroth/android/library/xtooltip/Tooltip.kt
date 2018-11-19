package it.sephiroth.android.library.xtooltip

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.text.Html
import android.view.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.PopupWindow.INPUT_METHOD_NOT_NEEDED
import android.widget.TextView
import androidx.annotation.*
import androidx.core.view.setPadding
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*


class Tooltip private constructor(private val context: Context, builder: Builder) {

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var isShowing = false
        private set

    private val mGravities = Gravity.values()
    private var isVisible = false
    private val mSizeTolerance = context.resources.displayMetrics.density * 10

    private val mLayoutInsetDecor = true
    private val mWindowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
    private val mSoftInputMode = INPUT_METHOD_NOT_NEEDED
    private val mHandler = Handler()

    private var mPopupView: TooltipViewContainer? = null
    private var mText: CharSequence?
    private var mAnchorPoint: Point
    private var mGravity: Gravity
    private var mShowArrow: Boolean
    private var mPadding: Int = 0
    private var mActivateDelay: Long
    private var mClosePolicy: ClosePolicy

    private var mFitToScreen: Boolean
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

    private var mViewOverlay: TooltipOverlay? = null
    private var mDrawable: TooltipTextDrawable? = null
    private var mAnchorView: WeakReference<View>? = null
    private lateinit var mContentView: View
    private lateinit var mTextView: TextView

    private val hideRunnable = Runnable { close(false, false, false) }
    private val activateRunnable = Runnable { mActivated = true }

    init {
        val theme = context.theme
            .obtainStyledAttributes(null, R.styleable.TooltipLayout, builder.defStyleAttr, builder.defStyleRes)
        this.mPadding = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_padding, 30)
        this.mTextAppearance = theme.getResourceId(R.styleable.TooltipLayout_android_textAppearance, 0)
        this.mTextGravity = theme
            .getInt(R.styleable.TooltipLayout_android_gravity, android.view.Gravity.TOP or android.view.Gravity.START)
        this.mTextViewElevation = theme.getDimension(R.styleable.TooltipLayout_ttlm_elevation, 0f)
        mOverlayStyle =
                theme.getResourceId(R.styleable.TooltipLayout_ttlm_overlayStyle, R.style.ToolTipOverlayDefaultStyle)
        val font = theme.getString(R.styleable.TooltipLayout_ttlm_font)
        theme.recycle()

        this.mText = builder.text
        this.mActivateDelay = builder.activateDelay
        this.mGravity = builder.gravity
        this.mAnchorPoint = builder.point!!
        this.mClosePolicy = builder.closePolicy
        this.mMaxWidth = builder.maxWidth
        this.mFloatingAnimation = builder.floatingAnimation
        this.mShowDuration = builder.showDuration
        this.mFadeDuration = builder.fadeDuration
        this.mShowOverlay = builder.overlay
        this.mShowArrow = builder.showArrow && builder.layoutId == null
        this.mFitToScreen = builder.fitToScreen
        builder.anchorView?.let {
            this.mAnchorView = WeakReference(it)
            this.mHasAnchorView = true
        }

        builder.layoutId?.let {
            mTextViewIdRes = builder.textId!!
            mTooltipLayoutIdRes = builder.layoutId!!
            mIsCustomView = true
        } ?: run {
            mDrawable = TooltipTextDrawable(context, builder)
        }

        if (builder.typeface != null) {
            mTypeface = builder.typeface
        } else if (!font.isNullOrEmpty()) {
            mTypeface = Typefaces[context, font]
        }

        Timber.i("mAnchorPoint: $mAnchorPoint")
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
        Timber.d("curFlags: $curFlags")

        var curFlags1 = curFlags
        curFlags1 = curFlags1 or
////                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES or
////                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
////                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
////                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
////                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
//
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
//
//        if (true) {
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
//        }
//
//        if (true) {
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//        }
//
//        if (true) {
        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//        }
//        if (mLayoutInsetDecor) {
//        curFlags1 = curFlags1 or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
//        }
//
//        Timber.v("curFlags1: $curFlags1")

        return curFlags1
    }

    private fun preparePopup(params: WindowManager.LayoutParams) {
        val viewContainer = TooltipViewContainer(context)

        if (mShowOverlay) {
            mViewOverlay = TooltipOverlay(context, 0, mOverlayStyle)
            mViewOverlay!!.adjustViewBounds = true
            mViewOverlay!!.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val contentView = LayoutInflater.from(context).inflate(mTooltipLayoutIdRes, viewContainer, false)

        mAnchorView?.get()?.addOnAttachStateChangeListener {
            onViewDetachedFromWindow {
                Timber.i("onViewDetachedFromWindow")
                dismiss()
            }
        }

        mFloatingAnimation?.let { contentView.setPadding(it.radius) }

        mTextView = contentView.findViewById(mTextViewIdRes)

        mDrawable?.let { mTextView.background = it }

        if (mShowArrow) {
            mTextView.setPadding(mPadding, mPadding, mPadding, mPadding)
        } else {
            mTextView.setPadding(mPadding / 2, mPadding / 2, mPadding / 2, mPadding / 2)
        }

        mTextView.text = Html.fromHtml(this.mText as String)
        mTextView.gravity = mTextGravity

        mMaxWidth?.let { mTextView.maxWidth = it }
        mTypeface?.let { mTextView.typeface = it }

        if (mTextAppearance != 0) {
            mTextView.setTextAppearance(context, mTextAppearance)
        }

        if (!mIsCustomView && mTextViewElevation > 0 && Build.VERSION.SDK_INT >= 21) {
            mTextView.elevation = mTextViewElevation
            mTextView.translationZ = mTextViewElevation
            mTextView.outlineProvider = ViewOutlineProvider.BACKGROUND
        }

        if (null != mViewOverlay) {
            viewContainer.addView(
                mViewOverlay,
                FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            )
        }

        viewContainer.addView(
            contentView,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        )

        viewContainer.measureAllChildren = true
        viewContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        Timber.i("viewContainer size: ${viewContainer.measuredWidth}, ${viewContainer.measuredHeight}")
        Timber.i("contentView size: ${contentView.measuredWidth}, ${contentView.measuredHeight}")

        mContentView = contentView
        mPopupView = viewContainer
    }

    private fun findPosition(
        parent: View,
        anchor: View?,
        offset: Point,
        gravities: ArrayList<Gravity>,
        params: WindowManager.LayoutParams
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

        if (mFitToScreen) {
            val finalRect = Rect(contentPosition.x, contentPosition.y, contentPosition.x + w, contentPosition.y + h)
            if (!displayFrame.rectContainsWithTolerance(finalRect, mSizeTolerance.toInt())) {
                Timber.e("content won't fit! $displayFrame, $finalRect")
                return findPosition(parent, anchor, offset, gravities, params)
            }
        }

        return Positions(arrowPosition, centerPosition, contentPosition, gravity, params)
    }

    private fun invokePopup(positions: Positions?) {
        positions?.let {
            isShowing = true

            setupAnimation(positions.gravity)

            mDrawable?.setAnchor(
                it.gravity,
                if (!mShowArrow) 0 else mPadding / 2,
                if (!mShowArrow) null else it.arrowPoint
            )

            mContentView.translationX = it.contentPoint.x.toFloat()
            mContentView.translationY = it.contentPoint.y.toFloat()

            mViewOverlay?.let { viewOverlay ->
                viewOverlay.translationX = it.centerPoint.x.toFloat() - viewOverlay.measuredWidth / 2
                viewOverlay.translationY = it.centerPoint.y.toFloat() - viewOverlay.measuredHeight / 2
            }

            it.params.packageName = context.packageName
            mPopupView?.fitsSystemWindows = mLayoutInsetDecor
            windowManager.addView(mPopupView, it.params)
            fadeIn(mFadeDuration)
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
        mAnimator = ObjectAnimator.ofFloat(mTextView, property, -endValue.toFloat(), endValue.toFloat())
        mAnimator!!.run {
            setDuration(duration)
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }

        mTextView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                mAnimator?.cancel()
                removeCallbacks()
            }

            override fun onViewAttachedToWindow(v: View?) {
                mAnimator?.start()
                if (mShowDuration > 0) {
                    mHandler.removeCallbacks(hideRunnable)
                    mHandler.removeCallbacks(activateRunnable)
                    mHandler.postDelayed(hideRunnable, mShowDuration)
                    mHandler.postDelayed(activateRunnable, mActivateDelay)
                }

            }
        })
    }

    fun show(parent: View) {
        if (isShowing) return
        if (mHasAnchorView && mAnchorView?.get() == null) return

        isVisible = false

        val params = createPopupLayoutParams(parent.windowToken)
        preparePopup(params)

        val gravities = mGravities.toCollection(ArrayList())
        gravities.remove(mGravity)
        gravities.add(0, mGravity)

        invokePopup(findPosition(parent, mAnchorView?.get(), mAnchorPoint, gravities, params))
    }

    fun hide() {
        if (!isShowing) return
        fadeOut(mFadeDuration)
    }

    private fun dismiss() {
        if (isShowing && mPopupView != null) {
            Timber.v("dismiss")
            windowManager.removeView(mPopupView)
            mPopupView = null
            isShowing = false
            isVisible = false
        }
    }

    private fun removeCallbacks() {
        mHandler.removeCallbacks(hideRunnable)
        mHandler.removeCallbacks(activateRunnable)
    }

    private fun close(fromUser: Boolean, containsTouch: Boolean, immediate: Boolean) {
        fadeOut(if (immediate) 0 else mFadeDuration)
    }

    private fun fadeIn(fadeDuration: Long) {
        if (!isShowing || isVisible) return

        isVisible = true

        if (fadeDuration > 0 && null != mPopupView) {
            mPopupView!!.alpha = 0F
            mPopupView!!.animate().setDuration(mFadeDuration).alpha(1f).start()
        }
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

        var sizeChange: ((w: Int, h: Int) -> Unit)? = null

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            sizeChange?.invoke(w, h)
        }
//
//        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
//            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
//                if (keyDispatcherState == null) {
//                    return super.dispatchKeyEvent(event)
//                }
//                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
//                    val state = keyDispatcherState
//                    state?.startTracking(event, this)
//                    return true
//                } else if (event.action == KeyEvent.ACTION_UP) {
//                    val state = keyDispatcherState
//                    if (state != null && state.isTracking(event) && !event.isCanceled) {
//                        dismiss()
//                        return true
//                    }
//                }
//                return super.dispatchKeyEvent(event)
//            } else {
//                return super.dispatchKeyEvent(event)
//            }
//        }

        override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
            Timber.i("onInterceptTouchEvent: $ev")
//            return super.onInterceptTouchEvent(ev)
            return true
        }

        //
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            Timber.i("dispatchTouchEvent: $ev")
//            return if (mTouchInterceptor != null && mTouchInterceptor.onTouch(this, ev)) {
//                true
//            } else
            super.dispatchTouchEvent(ev)
            return false
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            Timber.i("onTouchEvent: $event")
//            super.onTouchEvent(event)
            return true
//            val x = event.x.toInt()
//            val y = event.y.toInt()
//
//            if (event.action == MotionEvent.ACTION_DOWN && (x < 0 || x >= width || y < 0 || y >= height)) {
//                dismiss()
//                return true
//            } else if (event.action == MotionEvent.ACTION_OUTSIDE) {
//                dismiss()
//                return true
//            } else {
//                return super.onTouchEvent(event)
//            }
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

        companion object {
            val DEFAULT = Animation(8, 0, 400)
            val SLOW = Animation(4, 0, 600)
        }
    }

    class Builder(val context: Context) {
        internal var point: Point? = null
        internal var closePolicy = ClosePolicy.TOUCH_INSIDE_CONSUME
        internal var gravity = Gravity.RIGHT
        internal var text: CharSequence? = null
        internal var anchorView: View? = null
        internal var maxWidth: Int? = null
        internal var defStyleRes = R.style.ToolTipLayoutDefaultStyle
        internal var defStyleAttr = R.attr.ttlm_defaultStyle
        internal var typeface: Typeface? = null
        internal var overlay = true
        internal var floatingAnimation: Animation? = null
        internal var showDuration: Long = 0
        internal var fadeDuration: Long = 0
        internal var showArrow = true
        internal var activateDelay = 0L
        internal var fitToScreen = false

        @LayoutRes
        internal var layoutId: Int? = null

        @IdRes
        internal var textId: Int? = null

        fun fitToScreen(value: Boolean): Builder {
            this.fitToScreen = value
            return this
        }

        fun styleId(@StyleRes styleId: Int): Builder {
            this.defStyleAttr = 0
            this.defStyleRes = styleId
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

        fun floatingAnimation(value: Animation): Builder {
            this.floatingAnimation = value
            return this
        }

        fun maxWidth(w: Int): Builder {
            this.maxWidth = w
            return this
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

        fun anchor(view: View, xoff: Int = 0, yoff: Int = 0): Builder {
            this.anchorView = view
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

        fun closePolicy(policy: ClosePolicy): Builder {
            this.closePolicy = policy
            return this
        }

        fun gravity(gravity: Gravity): Builder {
            this.gravity = gravity
            return this
        }

        fun create(): Tooltip {
            if (gravity == Gravity.CENTER) overlay = false
            if (null == anchorView && null == point) {
                throw IllegalArgumentException("missing anchor point or anchor view")
            }
            return Tooltip(context, this)
        }
    }
}

class ClosePolicy {
    var policy: Int = 0
        private set

    constructor() {
        policy = NONE
    }

    internal constructor(policy: Int) {
        this.policy = policy
    }

    @SuppressWarnings("unused")
    fun insidePolicy(close: Boolean, consume: Boolean): ClosePolicy {
        policy = if (close) policy or TOUCH_INSIDE else policy and TOUCH_INSIDE.inv()
        policy = if (consume) policy or CONSUME_INSIDE else policy and CONSUME_INSIDE.inv()
        return this
    }

    fun outsidePolicy(close: Boolean, consume: Boolean): ClosePolicy {
        policy = if (close) policy or TOUCH_OUTSIDE else policy and TOUCH_OUTSIDE.inv()
        policy = if (consume) policy or CONSUME_OUTSIDE else policy and CONSUME_OUTSIDE.inv()
        return this
    }

    fun clear(): ClosePolicy {
        policy = NONE
        return this
    }

    fun build(): Int {
        return policy
    }

    companion object {
        internal val NONE = 0
        internal val TOUCH_INSIDE = 1 shl 1
        internal val TOUCH_OUTSIDE = 1 shl 2
        internal val CONSUME_INSIDE = 1 shl 3
        internal val CONSUME_OUTSIDE = 1 shl 4
        val TOUCH_NONE = ClosePolicy(NONE)
        val TOUCH_INSIDE_CONSUME = ClosePolicy(TOUCH_INSIDE or CONSUME_INSIDE)
        val TOUCH_INSIDE_NO_CONSUME = ClosePolicy(TOUCH_INSIDE)
        val TOUCH_OUTSIDE_CONSUME = ClosePolicy(TOUCH_OUTSIDE or CONSUME_OUTSIDE)
        val TOUCH_OUTSIDE_NO_CONSUME = ClosePolicy(TOUCH_OUTSIDE)
        val TOUCH_ANYWHERE_NO_CONSUME = ClosePolicy(TOUCH_INSIDE or TOUCH_OUTSIDE)
        val TOUCH_ANYWHERE_CONSUME = ClosePolicy(TOUCH_INSIDE or TOUCH_OUTSIDE or CONSUME_INSIDE or CONSUME_OUTSIDE)

        fun touchInside(value: Int): Boolean {
            return value and TOUCH_INSIDE == TOUCH_INSIDE
        }

        fun touchOutside(value: Int): Boolean {
            return value and TOUCH_OUTSIDE == TOUCH_OUTSIDE
        }

        fun consumeInside(value: Int): Boolean {
            return value and CONSUME_INSIDE == CONSUME_INSIDE
        }

        fun consumeOutside(value: Int): Boolean {
            return value and CONSUME_OUTSIDE == CONSUME_OUTSIDE
        }
    }

}