package com.rb.someapp

import android.animation.Animator
import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.transition.Transition
import androidx.transition.TransitionValues
import kotlin.concurrent.fixedRateTimer
import kotlin.math.hypot


class CircleTransition: android.transition.Transition() {
    private val BOUNDS = "viewBounds"

    private val PROPS = arrayOf(BOUNDS)
    override fun captureStartValues(transitionValues: android.transition.TransitionValues?) {
        return captureValues(transitionValues!!)
    }

    override fun captureEndValues(transitionValues: android.transition.TransitionValues?) {
        return captureValues(transitionValues!!)
    }

    override fun getTransitionProperties(): Array<String>? {
        return PROPS
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: android.transition.TransitionValues?,
        endValues: android.transition.TransitionValues??
    ): Animator?  {

        if (startValues == null || endValues == null) {
            return null
        }

        val startRect = startValues.values[BOUNDS] as Rect?
        val endRect = endValues.values[BOUNDS] as Rect?

        val transition: Animator
        val view = endValues.view
        if (isReveal(startRect!!, endRect!!)) {
            transition = createReveal(view, startRect, endRect)
            return NoPauseAnimator(transition)
        }
        else {
            layout(view, startRect)
            transition = createConceal(view, startRect, endRect)
            transition.addListener(object : Animator.AnimatorListener{
                override fun onAnimationStart(p0: Animator) {
                }

                override fun onAnimationEnd(p0: Animator) {
                    view.outlineProvider =object : ViewOutlineProvider() {
                        override fun getOutline(view: View?, p1: Outline?) {
                            val bounds = endRect
                            bounds.left -= view!!.left
                            bounds.top -= view.top;
                            bounds.right -= view.left;
                            bounds.bottom -= view.top;
                            p1!!.setOval(bounds)
                            view.clipToOutline = true
                        }

                    }
                }

                override fun onAnimationCancel(p0: Animator) {
                }

                override fun onAnimationRepeat(p0: Animator) {
                }

            })
            return NoPauseAnimator(transition)

        }
    }

    fun captureValues(values: android.transition.TransitionValues) {
        val view = values.view
        val bounds = Rect()
        bounds.left = view.left
        bounds.right = view.right
        bounds.top = view.top
        bounds.bottom = view.bottom
        values.values.put(BOUNDS, bounds)
    }

    fun isReveal(startRect: Rect, endRect: Rect): Boolean {
        return startRect.width() < endRect.width()
    }

    fun createReveal(view: View, startRect: Rect, endRect: Rect): Animator{
        val centerX = startRect.centerX()
        val centerY = startRect.centerY()

        val finalRadius = hypot(endRect.width().toDouble(), endRect.height().toDouble()).toFloat()

        return ViewAnimationUtils.createCircularReveal(view, centerX, centerY, (startRect.width()/2f), finalRadius)

    }

    fun createConceal(view: View, startRect: Rect, endRect: Rect): Animator {
        val centerX = endRect.centerX()
        val centerY = endRect.centerY()

        val initialRadius = Math.hypot(startRect.width().toDouble(), startRect.height().toDouble()).toFloat()

        return ViewAnimationUtils.createCircularReveal(view, centerX, centerY, initialRadius, endRect.width()/2f)
    }

    fun layout(view: View, startRect: Rect) {
        view.layout(startRect.left, startRect.top, startRect.right, startRect.bottom)
    }
}