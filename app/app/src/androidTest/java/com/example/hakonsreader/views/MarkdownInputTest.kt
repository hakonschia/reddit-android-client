package com.example.hakonsreader.views

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.*
import com.example.hakonsreader.activities.MockActivity
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MarkdownInputTest {
    private lateinit var markdownInput: MarkdownInput

    @Before
    fun setup() {
        MockActivity.layout = R.layout.x_mock_markdown_input
        val activity = ActivityScenario.launch<MockActivity>(Intent(InstrumentationRegistry.getInstrumentation().targetContext, MockActivity::class.java))
        activity.onActivity {
            markdownInput = it.findViewById(R.id.markdownMock)
        }
    }


    /**
     * Tests that the header button adds the syntax for headers correctly when the current line is
     * empty. This should add "# ", and further clicks should only add a "#" (without a space), up to
     * 6 times
     */
    @Test
    fun syntaxButtonsHeader_addsSyntaxCorrectlyWhenLineIsEmpty() {
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
    fun syntaxButtonsHeader_addsSyntaxCorrectlyWhenLineIsNotEmpty() {
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
    fun syntaxButtonsHeader_addsSyntaxCorrectlyWhenLineIsNotEmptyWithSpaceAlreadyAtStart() {
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
    fun syntaxButtonsBold_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsBold_noSelectionPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsBold_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
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
    fun syntaxButtonsItalic_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsItalic_noSelectionPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsItalic_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
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
    fun syntaxButtonsStrikethrough_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsStrikethrough_noSelectionPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsStrikethrough_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
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
    fun syntaxButtonsLink_linkButtonOpensDialog() {
        onView(withId(R.id.markdownLink))
                .perform(click())
        onView(withId(R.id.addLinkHeader))
                .check(matches(isDisplayed()))
    }

    /**
     * Tests that a dialog is opened when the link button is pressed
     */
    @Test
    fun syntaxButtonsLink_linkDialogCancelButtonClosesDialog() {
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
    fun syntaxButtonsLink_linkDialogAddButtonIsDisabledWithoutFields() {
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
    fun syntaxButtonsLink_linkDialogAddButtonIsDisabledWithOnlyTextFieldSet() {
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
    fun syntaxButtonsLink_linkDialogAddButtonIsEnabledWithBothFieldsSet() {
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
    fun syntaxButtonsLink_linkDialogAddsMarkdownCorrectly() {
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
    fun syntaxButtonsQuote_quoteSyntaxIsAddedToTheStartOfTheLineWhenNoText() {
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
    fun syntaxButtonsQuote_quoteSyntaxIsAddedToTheStartOfTheLineWithText() {
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
    fun syntaxButtonsQuote_quoteSyntaxIsAddedToTheStartOfTheCorrectLineInMultiline() {
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
    fun syntaxButtonsQuote_quoteSyntaxIsAddedToTheStartOfTheCorrectLineInMultilineWithMultipleQoutes() {
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
    fun syntaxButtonsQuote_quoteSyntaxIsRemovedWhenPressedTwice() {
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
    fun syntaxButtonsQuote_quoteSyntaxIsRemovedWhenPressedTwiceWithMultipleLines() {
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
    fun syntaxButtonsSpoiler_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsSpoiler_noSelectionPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsSpoiler_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
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
    fun syntaxButtonsSuperscript_noSelectionAndNoFocusPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsSuperscript_noSelectionPutsCursorInTheMiddleWithCursorSet() {
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
    fun syntaxButtonsSuperscript_withSelectionPutsSyntaxAroundSelectionAndMovesCursorToEnd() {
        // Click the input field it to give focus
        onView(withId(R.id.replyText))
                // Select "Hello there"
                .perform(typeText("Hello there general Kenobi"), setCursorPosition(start = 0, end = 11))

        onView(withId(R.id.markdownSuperscript))
                .perform(scrollTo())
                .perform(click())

        onView(withId(R.id.replyText))
                .check(matches(editTextEqualTo("^(Hello there) general Kenobi")))
                .check(matches(cursorPosition(start = 14)))
    }
}