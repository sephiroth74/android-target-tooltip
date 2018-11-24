package it.sephiroth.android.library.tooltip_demo

import android.graphics.Rect
import android.widget.CheckBox
import androidx.annotation.IdRes
import androidx.core.view.doOnPreDraw
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun testTooltipCreated() {
        onView(withId(R.id.text_duration)).perform(replaceText("1000"))
        onView(withId(R.id.text_fade)).perform(replaceText("100"))
        onView(withText(R.string.click_me)).perform(click())
        onView(withText("Lorem ipsum dolor")).check(matches(isDisplayed()))

        Thread.sleep(1400)

        onView(withText("Lorem ipsum dolor")).check(doesNotExist())
    }

    @Test
    fun test_close() {
        onView(withId(R.id.text_duration)).perform(replaceText("0"))
        onView(withId(R.id.text_fade)).perform(replaceText("100"))

        check(R.id.switch1)
        check(R.id.switch2)
        uncheck(R.id.switch3)

        onView(withId(R.id.spinner_gravities)).perform(click())
        onData(allOf(isA(String::class.java), `is`("TOP"))).perform(click())
        onView(withId(R.id.spinner_gravities)).check(matches(withSpinnerText(containsString("TOP"))))
        Thread.sleep(200)

        onView(withId(R.id.button1)).perform(click())
        Thread.sleep(200)

        onView(withId(R.id.tooltip)).check(matches(isDisplayed()))

        val latch = CountDownLatch(1)
        val rect = Rect()
        val tooltipContent = activityRule.activity.tooltip!!.contentView!!

        tooltipContent.doOnPreDraw {
            it.getHitRect(rect)
            latch.countDown()
        }

        latch.await()

        // click outside
        device.click(rect.left - 10, rect.top - 10)
        Thread.sleep(500)

        onView(withId(R.id.tooltip)).check(matches(isDisplayed()))

        // click inside the tooltip
        onView(withId(R.id.tooltip)).perform(click())
        Thread.sleep(500)

        onView(withId(R.id.tooltip)).check(doesNotExist())
    }


    private fun check(@IdRes id: Int) {
        if (!activityRule.activity.findViewById<CheckBox>(id).isChecked)
            onView(withId(id)).perform(click())
        onView(withId(id)).check(ViewAssertions.matches(isChecked()))
    }

    private fun uncheck(@IdRes id: Int) {
        if (activityRule.activity.findViewById<CheckBox>(id).isChecked)
            onView(withId(id)).perform(click())
        onView(withId(id)).check(ViewAssertions.matches(isNotChecked()))
    }
}
