package it.sephiroth.android.library.xtooltip

import android.animation.Animator
import android.graphics.Rect
import android.view.View
import android.view.ViewPropertyAnimator


internal inline fun Rect.rectContainsWithTolerance(childRect: Rect, t: Int): Boolean {
    return this.contains(childRect.left + t, childRect.top + t, childRect.right - t, childRect.bottom - t)
}

internal inline fun Rect.intersection(other: Rect): Rect {
    val intersect = Rect()
    intersect.left = Math.max(left, other.left)
    intersect.top = Math.max(top, other.top)
    intersect.right = Math.min(right, other.right)
    intersect.bottom = Math.min(bottom, other.bottom)
    return intersect
}

internal inline fun View.addOnAttachStateChangeListener(func: AttachStateChangeListener.() -> Unit): View {
    val listener = AttachStateChangeListener()
    listener.func()
    addOnAttachStateChangeListener(listener)
    return this
}

internal class AttachStateChangeListener : View.OnAttachStateChangeListener {

    private var _onViewAttachedToWindow: ((view: View?, listener: View.OnAttachStateChangeListener) -> Unit)? = null
    private var _onViewDetachedFromWindow: ((view: View?, listener: View.OnAttachStateChangeListener) -> Unit)? = null

    fun onViewDetachedFromWindow(func: (view: View?, listener: View.OnAttachStateChangeListener) -> Unit) {
        _onViewDetachedFromWindow = func
    }

    fun onViewAttachedToWindow(func: (view: View?, listener: View.OnAttachStateChangeListener) -> Unit) {
        _onViewAttachedToWindow = func
    }

    override fun onViewDetachedFromWindow(v: View?) {
        _onViewDetachedFromWindow?.invoke(v, this)
    }

    override fun onViewAttachedToWindow(v: View?) {
        _onViewAttachedToWindow?.invoke(v, this)
    }
}

internal inline fun ViewPropertyAnimator.setListener(
        func: AnimationListener.() -> Unit
                                           ): ViewPropertyAnimator {
    val listener = AnimationListener()
    listener.func()
    setListener(listener)
    return this
}

internal class AnimationListener : Animator.AnimatorListener {
    private var _onAnimationRepeat: ((animation: Animator) -> Unit)? = null
    private var _onAnimationEnd: ((animation: Animator) -> Unit)? = null
    private var _onAnimationStart: ((animation: Animator) -> Unit)? = null
    private var _onAnimationCancel: ((animation: Animator) -> Unit)? = null

    override fun onAnimationRepeat(animation: Animator) {
        _onAnimationRepeat?.invoke(animation)
    }

    override fun onAnimationCancel(animation: Animator) {
        _onAnimationCancel?.invoke(animation)
    }

    override fun onAnimationEnd(animation: Animator) {
        _onAnimationEnd?.invoke(animation)
    }

    override fun onAnimationStart(animation: Animator) {
        _onAnimationStart?.invoke(animation)
    }

    fun onAnimationRepeat(func: (animation: Animator) -> Unit) {
        _onAnimationRepeat = func
    }

    fun onAnimationCancel(func: (animation: Animator) -> Unit) {
        _onAnimationCancel = func
    }

    fun onAnimationEnd(func: (animation: Animator) -> Unit) {
        _onAnimationEnd = func
    }


    fun onAnimationStart(func: (animation: Animator) -> Unit) {
        _onAnimationStart = func
    }

}