package it.sephiroth.android.library.xtooltip

import android.animation.Animator
import android.graphics.Rect
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.Animation

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
internal inline fun Rect.rectContainsWithTolerance(childRect: Rect, t: Int): Boolean {
    return this.contains(childRect.left + t, childRect.top + t, childRect.right - t, childRect.bottom - t)
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
        func: ViewPropertyAnimatorListener.() -> Unit
): ViewPropertyAnimator {
    val listener = ViewPropertyAnimatorListener()
    listener.func()
    setListener(listener)
    return this
}

internal inline fun Animation.setListener(func: AnimationListener.() -> Unit): Animation {
    val listener = AnimationListener()
    listener.func()
    setAnimationListener(listener)
    return this
}

internal class AnimationListener : Animation.AnimationListener {
    private var _onAnimationRepeat: ((animation: Animation?) -> Unit)? = null
    private var _onAnimationEnd: ((animation: Animation?) -> Unit)? = null
    private var _onAnimationStart: ((animation: Animation?) -> Unit)? = null

    override fun onAnimationRepeat(animation: Animation?) {
        _onAnimationRepeat?.invoke(animation)
    }

    override fun onAnimationEnd(animation: Animation?) {
        _onAnimationEnd?.invoke(animation)
    }

    override fun onAnimationStart(animation: Animation?) {
        _onAnimationStart?.invoke(animation)
    }

    fun onAnimationEnd(func: (animation: Animation?) -> Unit) {
        _onAnimationEnd = func
    }

    fun onAnimationRepeat(func: (animation: Animation?) -> Unit) {
        _onAnimationRepeat = func
    }

    fun onAnimationStart(func: (animation: Animation?) -> Unit) {
        _onAnimationStart = func
    }

}

@Suppress("unused")
internal class ViewPropertyAnimatorListener : Animator.AnimatorListener {
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