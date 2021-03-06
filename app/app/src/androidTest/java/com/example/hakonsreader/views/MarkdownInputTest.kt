package com.example.hakonsreader.views

import android.content.Intent
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [MarkdownInput] that tests that the syntax buttons modify the text in the input
 * field as expected
 */
@HiltAndroidTest
class MarkdownInputTest {
    private lateinit var markdownInput: MarkdownInput

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()

        val activity = ActivityScenario.launch<HiltTestActivity>(Intent(InstrumentationRegistry.getInstrumentation().targetContext, HiltTestActivity::class.java))
        activity.onActivity {
            markdownInput = it.addOnlyOneView(MarkdownInput::class.java)
        }
    }


    /**
     * Tests that the header button adds the syntax for headers correctly when the current line is
     * empty. This should add "# ", and further clicks should only add a "#" (without a space), up to
     * 6 times
     */
    @Test
    fun header_addsSyntaxCorrectlyWhenLineIsEmpty() {
        // Ensure the text is empty by default
        assertEquals("", markdownInput.inputText)

        onView(withId(R.id.markdownHeader))
                .check(matches(isDisplayed()))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("# ")))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("## ")))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("### ")))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("#### ")))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("##### ")))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("###### ")))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        // It should at maximum add 6
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("###### ")))
    }

    /**
     * Tests that the header button adds the syntax for headers correctly when the current line is
     * empty NOT empty. This should add "# ", and further clicks should only add a "#" (without a space), up to
     * 6 times
     */
    @Test
    fun header_addsSyntaxCorrectlyWhenLineIsNotEmpty() {
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .perform(typeText("Hello"))
                // The cursor should be at the end, and should stay at the end at all times
                .check(matches(cursorPosition(start = 5)))

        onView(withId(R.id.markdownHeader))
                .check(matches(isDisplayed()))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("# Hello")))
                .check(matches(cursorPosition(start = 7)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("## Hello")))
                .check(matches(cursorPosition(start = 8)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("### Hello")))
                .check(matches(cursorPosition(start = 9)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("#### Hello")))
                .check(matches(cursorPosition(start = 10)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("##### Hello")))
                .check(matches(cursorPosition(start = 11)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("###### Hello")))
                .check(matches(cursorPosition(start = 12)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        // It should at maximum add 6
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("###### Hello")))
                .check(matches(cursorPosition(start = 12)))
    }

    /**
     * Tests that the header button adds the syntax for headers correctly when the current line is
     * empty NOT empty, and a space is already at the start. This should only add "#", without adding a space
     */
    @Test
    fun header_addsSyntaxCorrectlyWhenLineIsNotEmptyWithSpaceAlreadyAtStart() {
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .perform(typeText(" Hello"))
                // The cursor should be at the end, and should stay at the end at all times
                .check(matches(cursorPosition(start = 6)))

        onView(withId(R.id.markdownHeader))
                .check(matches(isDisplayed()))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("# Hello")))
                .check(matches(cursorPosition(start = 7)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("## Hello")))
                .check(matches(cursorPosition(start = 8)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("### Hello")))
                .check(matches(cursorPosition(start = 9)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("#### Hello")))
                .check(matches(cursorPosition(start = 10)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("##### Hello")))
                .check(matches(cursorPosition(start = 11)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("###### Hello")))
                .check(matches(cursorPosition(start = 12)))

        onView(withId(R.id.markdownHeader))
                .perform(click())
        // It should at maximum add 6
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("###### Hello")))
                .check(matches(cursorPosition(start = 12)))
    }


    /**
     * Tests that when the bold button is clicked without any selection, and the cursor not set
     * (by the EditText not having focus) that the cursor goes to the middle of the syntax so
     * that the user can start typing in the bold section
     */
    @Test
    fun bold_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
        onView(withId(R.id.markdownBold))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("****")))
                .check(matches(cursorPosition(start = 2)))
    }

    /**
     * Tests that when the bold button is clicked without any selection, but with the text input having focus,
     * that the cursor goes to the middle of the syntax so that the user can start typing in the bold section
     */
    @Test
    fun bold_noSelectionPutsCursorInTheMiddleWithCursorSet() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                .perform(click())

        onView(withId(R.id.markdownBold))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("****")))
                .check(matches(cursorPosition(start = 2)))
                // If we use "typeText" here it performs a click and the cursor will move
                .perform(typeTextIntoFocusedView("Hello"))
                .check(matches(editTextEqualTo("**Hello**")))
    }

    /**
     * Tests that when the bold button is clicked with a selection on the text that the selection
     * is wrapped with bold syntax, and that the cursor is moved to the end of the syntax so the user
     * can continue typing the other text
     */
    @Test
    fun bold_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                // Select "Hello there"
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownBold))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("**Hello there** general Kenobi")))
                .check(matches(cursorPosition(start = 15)))
    }


    /**
     * Tests that when the italic button is clicked without any selection, and the cursor not set
     * (by the EditText not having focus) that the cursor goes to the middle of the syntax so
     * that the user can start typing in the italic section
     */
    @Test
    fun italic_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
        onView(withId(R.id.markdownItalic))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("**")))
                .check(matches(cursorPosition(start = 1)))
    }

    /**
     * Tests that when the italic button is clicked without any selection, but with the text input having focus,
     * that the cursor goes to the middle of the syntax so that the user can start typing in the italic section
     */
    @Test
    fun italic_noSelectionPutsCursorInTheMiddleWithCursorSet() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                .perform(click())

        onView(withId(R.id.markdownItalic))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("**")))
                .check(matches(cursorPosition(start = 1)))
                // If we use "typeText" here it performs a click and the cursor will move
                .perform(typeTextIntoFocusedView("Hello"))
                .check(matches(editTextEqualTo("*Hello*")))
    }

    /**
     * Tests that when the italic button is clicked with a selection on the text that the selection
     * is wrapped with italic syntax, and that the cursor is moved to the end of the syntax so the user
     * can continue typing the other text
     */
    @Test
    fun italic_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                // Select "Hello there"
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownItalic))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("*Hello there* general Kenobi")))
                .check(matches(cursorPosition(start = 13)))
    }


    /**
     * Tests that when the strikethrough button is clicked without any selection, and the cursor not set
     * (by the EditText not having focus) that the cursor goes to the middle of the syntax so
     * that the user can start typing in the strikethrough section
     */
    @Test
    fun strikethrough_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
        onView(withId(R.id.markdownStrikethrough))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("~~~~")))
                .check(matches(cursorPosition(start = 2)))
    }

    /**
     * Tests that when the strikethrough button is clicked without any selection, but with the text input having focus,
     * that the cursor goes to the middle of the syntax so that the user can start typing in the strikethrough section
     */
    @Test
    fun strikethrough_noSelectionPutsCursorInTheMiddleWithCursorSet() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                .perform(click())

        onView(withId(R.id.markdownStrikethrough))
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("~~~~")))
                .check(matches(cursorPosition(start = 2)))
                // If we use "typeText" here it performs a click and the cursor will move
                .perform(typeTextIntoFocusedView("Hello"))
                .check(matches(editTextEqualTo("~~Hello~~")))
    }

    /**
     * Tests that when the strikethrough button is clicked with a selection on the text that the selection
     * is wrapped with strikethrough syntax, and that the cursor is moved to the end of the syntax so the user
     * can continue typing the other text
     */
    @Test
    fun strikethrough_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                // Select "Hello there"
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownStrikethrough))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("~~Hello there~~ general Kenobi")))
                .check(matches(cursorPosition(start = 15)))
    }


    /**
     * Tests that a dialog is opened when the link button is pressed
     */
    @Test
    fun link_linkButtonOpensDialog() {
        onView(withId(R.id.markdownLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(matches(isDisplayed()))
    }

    /**
     * Tests that a dialog is opened when the link button is pressed
     */
    @Test
    fun link_linkDialogCancelButtonClosesDialog() {
        onView(withId(R.id.markdownLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(matches(isDisplayed()))
        onView(withId(R.id.btnCancelLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(doesNotExist())
    }

    /**
     * Tests that the add link button in the link dialog is disabled by default, since no text has been
     * added
     */
    @Test
    fun link_linkDialogAddButtonIsDisabledWithoutFields() {
        onView(withId(R.id.markdownLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(matches(isDisplayed()))

        onView(withId(R.id.btnAddLink))
                .check(matches(not(isEnabled())))
    }

    /**
     * Tests that the add link button in the link dialog is disabled when only the text input has text
     */
    @Test
    fun link_linkDialogAddButtonIsDisabledWithOnlyTextFieldSet() {
        onView(withId(R.id.markdownLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(matches(isDisplayed()))

        onView(withId(R.id.textText))
                .perform(typeText("Check out this link"))
        onView(withId(R.id.btnAddLink))
                .check(matches(not(isEnabled())))
    }

    /**
     * Tests that the add link button in the link dialog is enabled when both input fields have text
     */
    @Test
    fun link_linkDialogAddButtonIsEnabledWithBothFieldsSet() {
        onView(withId(R.id.markdownLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(matches(isDisplayed()))

        onView(withId(R.id.textText))
                .perform(typeText("Check out this link"))
        onView(withId(R.id.linkText))
                .perform(typeText("https://example.com"))
        onView(withId(R.id.btnAddLink))
                .check(matches(isEnabled()))
    }

    /**
     * Tests that adding a link from the link dialog correctly adds the link to the markdown input
     */
    @Test
    fun link_linkDialogAddsMarkdownCorrectly() {
        onView(withId(R.id.markdownLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(matches(isDisplayed()))

        onView(withId(R.id.textText))
                .perform(typeText("Check out this link"))
        onView(withId(R.id.linkText))
                .perform(typeText("https://example.com"))
        onView(withId(R.id.btnAddLink))
                .check(matches(isEnabled()))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("[Check out this link](https://example.com)")))
    }


    /**
     * Tests that the quote button adds the syntax for quotes at the start of the line when no text
     * is in the input field
     */
    @Test
    fun quote_quoteSyntaxIsAddedToTheStartOfTheLineWhenNoText() {
        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(">")))
    }

    /**
     * Tests that the quote button adds the syntax for quotes at the start of the line when no text
     * is in the input field and that the cursor is at the same spot
     */
    @Test
    fun quote_quoteSyntaxIsAddedToTheStartOfTheLineWithText() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))
                .check(matches(cursorPosition(11)))

        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(">Hello there")))
                .check(matches(cursorPosition(12)))
    }

    /**
     * Tests that the quote button adds the syntax at the start of the correct line when the input
     * field has multiple lines
     */
    @Test
    fun quote_quoteSyntaxIsAddedToTheStartOfTheCorrectLineInMultiline() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))
                .check(matches(cursorPosition(11)))
                .perform(typeText("\nGeneral Kenobi"))
                .check(matches(cursorPosition(26)))

        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                // The quote is added to the second line
                .check(matches(editTextEqualTo("Hello there\n>General Kenobi")))
                .check(matches(cursorPosition(27)))
    }

    /**
     * Tests that the quote button adds the syntax at the start of the correct line when the input
     * field has multiple lines and the input has multiple quotes on the different lines
     */
    @Test
    fun quote_quoteSyntaxIsAddedToTheStartOfTheCorrectLineInMultilineWithMultipleQoutes() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))
                .check(matches(cursorPosition(11)))
                .perform(typeText("\nGeneral Kenobi"))
                .check(matches(cursorPosition(26)))

        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                // The quote is added to the second line
                .check(matches(editTextEqualTo("Hello there\n>General Kenobi")))
                .check(matches(cursorPosition(27)))
                // Set cursor to an arbitrary position on the first line
                .perform(setCursorPosition(5))

        // Quote should now be added to the start
        onView(withId(R.id.markdownQuote))
                .perform(click())
        onView(withId(R.id.replyText))
                // The quote is added to the second line
                .check(matches(editTextEqualTo(">Hello there\n>General Kenobi")))
                .perform(setCursorPosition(6))
    }

    /**
     * Tests that the quote button adds the syntax at the start of the correct line and that it is removed
     * when later pressed again
     */
    @Test
    fun quote_quoteSyntaxIsRemovedWhenPressedTwice() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))
                .check(matches(cursorPosition(11)))

        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(">Hello there")))
                .check(matches(cursorPosition(12)))

        // Quote should now be removed
        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                // The quote is added to the second line
                .check(matches(editTextEqualTo("Hello there")))
                .check(matches(cursorPosition(11)))
    }


    /**
     * Tests that the quote button adds the syntax at the start of the correct line and that it is removed
     * when later pressed again
     */
    @Test
    fun quote_quoteSyntaxIsRemovedWhenPressedTwiceWithMultipleLines() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there\nGeneral Kenobi"))
                .check(matches(cursorPosition(26)))

        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("Hello there\n>General Kenobi")))
                .check(matches(cursorPosition(27)))

        // Quote should now be removed
        onView(withId(R.id.markdownQuote))
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("Hello there\nGeneral Kenobi")))
                .check(matches(cursorPosition(26)))
    }


    /**
     * Tests that when the spoiler button is clicked without any selection, and the cursor not set
     * (by the EditText not having focus) that the cursor goes to the middle of the syntax so
     * that the user can start typing in the spoiler section
     */
    @Test
    fun spoiler_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
        onView(withId(R.id.markdownSpoiler))
                .perform(scrollTo())
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(">!!<")))
                .check(matches(cursorPosition(start = 2)))
    }

    /**
     * Tests that when the spoiler button is clicked without any selection, but with the text input having focus,
     * that the cursor goes to the middle of the syntax so that the user can start typing in the spoiler section
     */
    @Test
    fun spoiler_noSelectionPutsCursorInTheMiddleWithCursorSet() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                .perform(click())

        onView(withId(R.id.markdownSpoiler))
                .perform(scrollTo())
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(">!!<")))
                .check(matches(cursorPosition(start = 2)))
                // If we use "typeText" here it performs a click and the cursor will move
                .perform(typeTextIntoFocusedView("Hello"))
                .check(matches(editTextEqualTo(">!Hello!<")))
    }

    /**
     * Tests that when the spoiler button is clicked with a selection on the text that the selection
     * is wrapped with spoiler syntax, and that the cursor is moved to the end of the syntax so the user
     * can continue typing the other text
     */
    @Test
    fun spoiler_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                // Select "Hello there"
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownSpoiler))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(">!Hello there!< general Kenobi")))
                .check(matches(cursorPosition(start = 15)))
    }


    /**
     * Tests that when the superscript button is clicked without any selection, and the cursor not set
     * (by the EditText not having focus) that the cursor goes to the middle of the syntax so
     * that the user can start typing in the superscript section
     */
    @Test
    fun superscript_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
        onView(withId(R.id.markdownSuperscript))
                .perform(scrollTo())
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("^()")))
                .check(matches(cursorPosition(start = 2)))
    }

    /**
     * Tests that when the superscript button is clicked without any selection, but with the text input having focus,
     * that the cursor goes to the middle of the syntax so that the user can start typing in the superscript section
     */
    @Test
    fun superscript_noSelectionPutsCursorInTheMiddleWithCursorSet() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                .perform(click())

        onView(withId(R.id.markdownSuperscript))
                .perform(scrollTo())
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("^()")))
                .check(matches(cursorPosition(start = 2)))
                // If we use "typeText" here it performs a click and the cursor will move
                .perform(typeTextIntoFocusedView("Hello"))
                .check(matches(editTextEqualTo("^(Hello)")))
    }

    /**
     * Tests that when the superscript button is clicked with a selection on the text that the selection
     * is wrapped with superscript syntax, and that the cursor is moved to the end of the syntax so the user
     * can continue typing the other text
     */
    @Test
    fun superscript_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownSuperscript))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("^(Hello there) general Kenobi")))
                .check(matches(cursorPosition(start = 14)))
    }

    /**
     * Tests that when the superscript button is clicked with a selection on the text that the selection
     * is wrapped with superscript syntax, and that the cursor is moved to the end of the syntax so the user
     * can continue typing the other text
     */
    @Test
    fun superscript_nestedSuperscriptWithSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownSuperscript))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("^(Hello there) general Kenobi")))
                .check(matches(cursorPosition(start = 14)))

        onView(withId(R.id.replyText))
                // Select "there"
                .perform(setCursorPosition(start = 8, end = 13))

        onView(withId(R.id.markdownSuperscript))
                .perform(click())

        onView(withId(R.id.replyText))
                // Select "there"
                .check(matches(editTextEqualTo("^(Hello ^(there)) general Kenobi")))
                .check(matches(cursorPosition(start = 16)))

        // Select "general" as well (there used to be a bug where the selection got put beyond the text
        // length, causing a crash, which would happen when selecting "general" in this string)
        onView(withId(R.id.replyText))
                // Select "general"
                .perform(setCursorPosition(start = 18, end = 25))

        onView(withId(R.id.markdownSuperscript))
                .perform(click())

        onView(withId(R.id.replyText))
                // Select "there"
                .check(matches(editTextEqualTo("^(Hello ^(there)) ^(general) Kenobi")))
                .check(matches(cursorPosition(start = 28)))
    }


    /**
     * Simple test to ensure the unordered list adds the syntax correctly
     */
    @Test
    fun unorderedList_addsSyntaxCorrectly() {
        onView(withId(R.id.markdownBulletList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("* ")))
                .perform(typeTextIntoFocusedView("Hello there General"))

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(textToCheck = "* Hello there General")))
    }

    /**
     * Tests that a list is automatically continued when enter is pressed on a line that starts with
     * list syntax
     */
    @Test
    fun unorderedList_listContinuesWhenEnterIsPressed() {
        onView(withId(R.id.markdownBulletList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("* ")))
                .perform(typeTextIntoFocusedView("Hello there\n"))
                .check(matches(editTextEqualTo(textToCheck =
                "* Hello there\n" +
                        // Should automatically continue the list
                        "* "
                )))
    }

    /**
     * Tests that a list is automatically continued when enter is pressed on a line that starts with
     * list syntax, and then removed if enter is pressed again
     */
    @Test
    fun unorderedList_listContinuationEndsWhenEnterIsPressedTwice() {
        onView(withId(R.id.markdownBulletList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("* ")))
                .perform(typeTextIntoFocusedView("Hello there"))
                // Once should continue
                .perform(typeTextIntoFocusedView("\n"))
                .check(matches(editTextEqualTo(textToCheck =
                "* Hello there\n" +
                        // Should automatically continue the list
                        "* "
                )))
                // And when enter is pressed again it should now remove the syntax again
                .perform(typeTextIntoFocusedView("\n"))
                .check(matches(editTextEqualTo("* Hello there\n")))
    }

    /**
     * Tests that a list is automatically continued when enter is pressed on a line that starts with
     * list syntax, and if the syntax is manually removed it does nothing (it does not add the syntax back)
     */
    @Test
    fun unorderedList_listContinuationWhenBackIsPressedWorks() {
        onView(withId(R.id.markdownBulletList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("* ")))
                .perform(typeTextIntoFocusedView("Hello there"))
                // Once should continue
                .perform(typeTextIntoFocusedView("\n"))
                .check(matches(editTextEqualTo(textToCheck =
                "* Hello there\n" +
                        // Should automatically continue the list
                        "* "
                )))
                // Removes the space
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                // Removes the syntax
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                .check(matches(editTextEqualTo("* Hello there\n")))
    }


    /**
     * Simple test to ensure the unordered list adds the syntax correctly
     */
    @Test
    fun orderedList_addsSyntaxCorrectly() {
        // Click the input field it to give focus
        onView(withId(R.id.markdownNumberedList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("1. ")))
                .perform(typeTextIntoFocusedView("Hello there\nGeneral"))

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo(textToCheck =
                "1. Hello there\n" +
                        // Might look weird but which number it is in the raw markdown doesn't matter
                        // and is always "1." for simplicities sake
                        "1. General"
                )))
    }

    /**
     * Tests that a list is automatically continued when enter is pressed on a line that starts with
     * list syntax
     */
    @Test
    fun orderedList_listContinuesWhenEnterIsPressed() {
        onView(withId(R.id.markdownNumberedList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("1. ")))
                .perform(typeTextIntoFocusedView("Hello there"))
                .perform(typeTextIntoFocusedView("\n"))
                .check(matches(editTextEqualTo(textToCheck =
                "1. Hello there\n" +
                        // Should automatically continue the list
                        "1. "
                )))
    }

    /**
     * Tests that a list is automatically continued when enter is pressed on a line that starts with
     * list syntax
     */
    @Test
    fun orderedList_listContinuationEndsWhenEnterIsPressedTwice() {
        onView(withId(R.id.markdownNumberedList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("1. ")))
                .perform(typeTextIntoFocusedView("Hello there"))
                // Once should continue
                .perform(typeTextIntoFocusedView("\n"))
                .check(matches(editTextEqualTo(textToCheck =
                "1. Hello there\n" +
                        // Should automatically continue the list
                        "1. "
                )))
                // And when enter is pressed again it should now remove the syntax again
                .perform(typeTextIntoFocusedView("\n"))
                .check(matches(editTextEqualTo("1. Hello there\n")))
    }

    /**
     * Tests that a list is automatically continued when enter is pressed on a line that starts with
     * list syntax, and if the syntax is manually removed it does nothing (it does not add the syntax back)
     */
    @Test
    fun orderedList_listContinuationWhenBackIsPressedWorks() {
        onView(withId(R.id.markdownNumberedList))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("1. ")))
                .perform(typeTextIntoFocusedView("Hello there"))
                // Once should continue
                .perform(typeTextIntoFocusedView("\n"))
                .check(matches(editTextEqualTo(textToCheck =
                "1. Hello there\n" +
                        // Should automatically continue the list
                        "1. "
                )))
                // Removes the space
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                // Removes the "."
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                // Removes the "1"
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                .check(matches(editTextEqualTo("1. Hello there\n")))
    }


    /**
     * Tests that when the inline code button is clicked without any selection, and the cursor not set
     * (by the EditText not having focus) that the cursor goes to the middle of the syntax so
     * that the user can start typing in the italic section
     */
    @Test
    fun inlineCode_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
        onView(withId(R.id.markdownInlineCode))
                .perform(scrollTo())
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("``")))
                .check(matches(cursorPosition(start = 1)))
    }

    /**
     * Tests that when the inline code button is clicked without any selection, but with the text input having focus,
     * that the cursor goes to the middle of the syntax so that the user can start typing in the inline code section
     */
    @Test
    fun inlineCode_noSelectionPutsCursorInTheMiddleWithCursorSet() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                .perform(click())

        onView(withId(R.id.markdownInlineCode))
                .perform(scrollTo())
                .perform(click())
        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("``")))
                .check(matches(cursorPosition(start = 1)))
                // If we use "typeText" here it performs a click and the cursor will move
                .perform(typeTextIntoFocusedView("Hello"))
                .check(matches(editTextEqualTo("`Hello`")))
    }

    /**
     * Tests that when the inline code button is clicked with a selection on the text that the selection
     * is wrapped with inline code syntax, and that the cursor is moved to the end of the syntax so the user
     * can continue typing the other text
     */
    @Test
    fun inlineCode_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownInlineCode))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("`Hello there` general Kenobi")))
                .check(matches(cursorPosition(start = 13)))
    }


    /**
     * Basic test for the code block button to ensure that the syntax is added correctly to an empty input field
     */
    @Test
    fun codeBlock_addsSyntaxCorrectlyIntoEmptyInputField() {
        onView(withId(R.id.markdownCodeBlock))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                // When nothing is in the input field the button should only add the 4 spaces and no lines
                .check(matches(editTextEqualTo("    ")))
    }

    /**
     * Basic test for the code block button to ensure that the syntax is added correctly to an input
     * field with some text previously typed
     */
    @Test
    fun codeBlock_addsSyntaxCorrectlyToInputFieldWithText() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))

        onView(withId(R.id.markdownCodeBlock))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                // With something in the input field it should add 2 newlines after it
                // When nothing is in the input field the button should only add the 4 spaces and no lines
                .check(matches(editTextEqualTo("Hello there\n\n    ")))
    }

    /**
     * Tests that the code block continues when enter is pressed on inside a code block
     */
    @Test
    fun codeBlock_continuesSyntaxWhenEnterIsPressed() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))

        onView(withId(R.id.markdownCodeBlock))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                // With something in the input field it should add 2 newlines after it
                // When nothing is in the input field the button should only add the 4 spaces and no lines
                .check(matches(editTextEqualTo("Hello there\n\n    ")))
                // Type some code and then press enter, should automatically continue the code block
                .perform(typeTextIntoFocusedView("if(true)\n"))
                .check(matches(editTextEqualTo("Hello there\n\n    if(true)\n    ")))
    }

    /**
     * Tests that the code block syntax does not continue when enter is pressed twice
     */
    @Test
    fun codeBlock_doesNotContinuesSyntaxWhenEnterIsPressedTwice() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))

        onView(withId(R.id.markdownCodeBlock))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                // With something in the input field it should add 2 newlines after it
                // When nothing is in the input field the button should only add the 4 spaces and no lines
                .check(matches(editTextEqualTo("Hello there\n\n    ")))
                // Type some code and then press enter, should automatically continue the code block
                .perform(typeTextIntoFocusedView("if(true)\n"))
                .check(matches(editTextEqualTo("Hello there\n\n    if(true)\n    ")))
                // Type enter again
                .perform(typeTextIntoFocusedView("\n"))
                // Should now remove the syntax, but not the newline
                .check(matches(editTextEqualTo("Hello there\n\n    if(true)\n")))
    }

    /**
     * Tests that the code block syntax does not continue when enter is pressed twice
     */
    @Test
    fun codeBlock_doesNotAddSyntaxWhenManuallyRemoved() {
        onView(withId(R.id.replyText))
                .perform(typeText("Hello there"))

        onView(withId(R.id.markdownCodeBlock))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                // With something in the input field it should add 2 newlines after it
                // When nothing is in the input field the button should only add the 4 spaces and no lines
                .check(matches(editTextEqualTo("Hello there\n\n    ")))
                // Type some code and then press enter, should automatically continue the code block
                .perform(typeTextIntoFocusedView("if(true)\n"))
                .check(matches(editTextEqualTo("Hello there\n\n    if(true)\n    ")))
                // Remove the spaces manually, this should not add the syntax back
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                .perform(pressKey(KeyEvent.KEYCODE_DEL))
                .check(matches(editTextEqualTo("Hello there\n\n    if(true)\n")))
    }
}