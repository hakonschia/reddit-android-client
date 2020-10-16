package com.example.hakonsreader.misc;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;

/**
 * Custom double tap listener for {@link com.github.chrisbanes.photoview.PhotoView}. Note that this
 * will invalidate any potential click listeners on the PhotoView itself.
 *
 * <p>Set with: {@link com.github.chrisbanes.photoview.PhotoView#setOnDoubleTapListener(GestureDetector.OnDoubleTapListener)}</p>
 *
 * <p>When a double tap occurs the image either zooms in (if not zoomed in) or zooms out (if
 * zoomed in at)</p>
 */
public class PhotoViewDoubleTapListener implements GestureDetector.OnDoubleTapListener {

    private final PhotoViewAttacher attacher;
    private float scaleFactor = 3f;

    /**
     * The default zoom factor is 3. To set a different see
     * {@link #PhotoViewDoubleTapListener(PhotoViewAttacher, float)}
     *
     * @param attacher The attacher for the {@link com.github.chrisbanes.photoview.PhotoView}, retrieved with
     *                 {@link PhotoView#getAttacher()}
     */
    public PhotoViewDoubleTapListener(PhotoViewAttacher attacher) {
        this.attacher = attacher;
    }

    /**
     * @param attacher The attacher for the {@link com.github.chrisbanes.photoview.PhotoView}, retrieved with
     *                 {@link PhotoView#getAttacher()}
     * @param scaleFactor The factor to scale the image on double taps. Must be within the range of
     *                    {@link PhotoViewAttacher#getMinimumScale()} and {@link PhotoViewAttacher#getMaximumScale()}.
     *                    Default is 3
     */
    public PhotoViewDoubleTapListener(PhotoViewAttacher attacher, float scaleFactor) {
        this.attacher = attacher;

        this.scaleFactor = scaleFactor;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        // If we are zoomed in at all on double taps then go back to original size
        // otherwise zoom in. Default for PhotoView has two zoom states, this removes the
        // intermediate step (because I don't like that)
        // Comparing floats like this is probably bad? Doesn't seem to matter though
        if (attacher.getScale() > 1f) {
            attacher.setScale(1f, true);
        } else {
            attacher.setScale(scaleFactor, motionEvent.getX(), motionEvent.getY(), true);
        }

        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        // Not implemented
        return false;
    }
    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        // Not implemented
        return false;
    }

}
