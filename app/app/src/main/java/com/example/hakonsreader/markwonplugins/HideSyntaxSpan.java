package com.example.hakonsreader.markwonplugins;


import android.text.TextPaint;
import android.text.style.CharacterStyle;

/**
 * Class to "remove" parts of a text. The text isn't actually removed, but the text is hidden
 *
 * <p>Taken from the code originally from {@link RedditSpoilerPlugin}</p>
 */
class HideSyntaxSpan extends CharacterStyle {
    // TODO find out how to actually remove the text so it doesnt take up more space than necessary

    @Override
    public void updateDrawState(TextPaint tp) {
        // set transparent color
        tp.setColor(0);
    }
}