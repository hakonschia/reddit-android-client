package com.example.hakonsreader.misc;


import android.content.Context;
import android.content.Intent;
import android.text.Layout;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activities.DispatcherActivity;

/**
 * Set this on a textview and then you can potentially open links locally if applicable
 *
 * Taken from: https://gitlab.com/Commit451/LabCoat/-/blob/0da57c371815902f4ba24fcd7bceaa1e7a8d7bb7/app/src/main/java/com/commit451/gitlab/util/InternalLinkMovementMethod.java
 */
public class InternalLinkMovementMethod extends LinkMovementMethod {
    private static final String TAG = "InternalLinkMovementMethod";

    private final OnLinkClickedListener mOnLinkClickedListener;
    private boolean ignoreNextClick = false;

    public InternalLinkMovementMethod() {
        mOnLinkClickedListener = (linkText, context) -> {
            // Let the dispatch activity handle all links
            Intent intent = new Intent(context, DispatcherActivity.class);
            intent.putExtra(DispatcherActivity.EXTRAS_URL_KEY, linkText);
            context.startActivity(intent);
            return true;
        };
    }

    public InternalLinkMovementMethod(@NonNull OnLinkClickedListener onLinkClickedListener) {
        mOnLinkClickedListener = onLinkClickedListener;
    }

    /**
     * Ignores the next click
     */
    public void ignoreNextClick() {
        ignoreNextClick = true;
    }

    @Override
    public boolean onTouchEvent(TextView widget, android.text.Spannable buffer, android.view.MotionEvent event) {
        int action = event.getAction();

        //http://stackoverflow.com/questions/1697084/handle-textview-link-click-in-my-android-app

        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= widget.getTotalPaddingLeft();
        y -= widget.getTotalPaddingTop();

        x += widget.getScrollX();
        y += widget.getScrollY();

        Layout layout = widget.getLayout();
        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);

        // link is empty sometimes, even though we're still "on" it after having set the background span, so
        // always remove the spans if we have an UP or CANCEL
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            CustomBackgroundColorSpan[] spans = buffer.getSpans(0, buffer.length(), CustomBackgroundColorSpan.class);
            for (CustomBackgroundColorSpan span : spans) {
                buffer.removeSpan(span);
            }
        }

        if (link.length != 0) {
            String url = link[0].getURL();
            int start = buffer.getSpanStart(link[0]);
            int end = buffer.getSpanEnd(link[0]);

            if (action == MotionEvent.ACTION_DOWN) {
                // Set background span to show what is being clicked
                buffer.setSpan(new CustomBackgroundColorSpan(widget.getContext().getColor(R.color.linkColorPressedBackground)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (action == MotionEvent.ACTION_UP) {
                // Even if we ignore the click we have to return true, otherwise the system will
                // handle the click (which probably opens the URL in a browser)
                boolean handled = true;
                if (ignoreNextClick) {
                    ignoreNextClick = false;
                } else {
                    handled = mOnLinkClickedListener.onLinkClicked(url, widget.getContext());
                }

                if (handled) {
                    return true;
                }
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    public interface OnLinkClickedListener {
        boolean onLinkClicked(String url, Context context);
    }

    /**
     * To ensure that we don't remove any potential BackgroundColorSpan on the text that we haven't added
     * ourselves we create our own class to add and remove
     */
    private static class CustomBackgroundColorSpan extends BackgroundColorSpan {

        public CustomBackgroundColorSpan(int color) {
            super(color);
        }
    }
}
