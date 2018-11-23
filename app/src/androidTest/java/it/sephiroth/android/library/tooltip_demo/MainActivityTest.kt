package it.sephiroth.android.library.tooltip_demo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testTooltipCreated() {
        onView(withId(R.id.text_duration)).perform(replaceText("1000"))
        onView(withId(R.id.text_fade)).perform(replaceText("100"))
        onView(withText(R.string.click_me)).perform(click())
        onView(withText("Lorem ipsum dolor")).check(matches(isDisplayed()))

        Thread.sleep(1400)

        onView(withText("Lorem ipsum dolor")).check(doesNotExist())
    }
}
