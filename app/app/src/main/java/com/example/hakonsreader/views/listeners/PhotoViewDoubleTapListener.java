package com.example.hakonsreader.views.listeners;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.r0adkll.slidr.model.SlidrInterface;

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

    // At what point does it make sense to make a builder for this class, so many constructors


    private final PhotoViewAttacher attacher;
    private float scaleFactor = 3f;
    private final SlidrInterface slidrInterface;

    /**
     * The default zoom factor is 3. To set a different see
     * {@link #PhotoViewDoubleTapListener(PhotoViewAttacher, float)}
     *
     * @param attacher The attacher for the {@link com.github.chrisbanes.photoview.PhotoView}, retrieved with
     *                 {@link PhotoView#getAttacher()}
     */
    public PhotoViewDoubleTapListener(PhotoViewAttacher attacher) {
        this.attacher = attacher;
        this.slidrInterface = null;
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
        this.slidrInterface = null;
    }

    /**
     * @param attacher The attacher for the {@link com.github.chrisbanes.photoview.PhotoView}, retrieved with
     *                 {@link PhotoView#getAttacher()}
     * @param slidrInterface A {@link SlidrInterface} that will be locked when the image is zoomed in and
     *                       unlocked when the image is zoomed out again
     */
    public PhotoViewDoubleTapListener(PhotoViewAttacher attacher, SlidrInterface slidrInterface) {
        this.attacher = attacher;
        this.slidrInterface = slidrInterface;
    }

    /**
     * @param attacher The attacher for the {@link com.github.chrisbanes.photoview.PhotoView}, retrieved with
     *                 {@link PhotoView#getAttacher()}
     * @param scaleFactor The factor to scale the image on double taps. Must be within the range of
     *                    {@link PhotoViewAttacher#getMinimumScale()} and {@link PhotoViewAttacher#getMaximumScale()}.
     *                    Default is 3
     * @param slidrInterface A {@link SlidrInterface} that will be locked when the image is zoomed in and
     *                       unlocked when the image is zoomed out again
     */
    public PhotoViewDoubleTapListener(PhotoViewAttacher attacher, float scaleFactor, SlidrInterface slidrInterface) {
        this.attacher = attacher;
        this.scaleFactor = scaleFactor;
        this.slidrInterface = slidrInterface;
    }


    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        // If we are zoomed in at all on double taps then go back to original size,
        // otherwise zoom in. Default for PhotoView has two zoom states, this removes the
        // intermediate step (because I don't like that)
        if (Float.compare(attacher.getScale(), 1f) > 0) {
            attacher.setScale(1f, true);

            if (slidrInterface != null) {
                slidrInterface.unlock();
            }
        } else {
            // Zooming in. if slidrInterface is set lock it to avoid scrolling around on the image
            // swipes away the activity
            attacher.setScale(scaleFactor, motionEvent.getX(), motionEvent.getY(), true);

            if (slidrInterface != null) {
                slidrInterface.lock();
            }
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
