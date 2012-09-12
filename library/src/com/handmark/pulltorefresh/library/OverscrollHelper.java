package com.handmark.pulltorefresh.library;

import android.annotation.TargetApi;
import android.util.Log;
import android.view.View;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;

@TargetApi(9)
final class OverscrollHelper {

	static final String LOG_TAG = "OverscrollHelper";

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function.
	 * 
	 * This should only be used on AdapterView's such as ListView as it just
	 * calls through to overScrollBy() with the scrollRange = 0. AdapterView's
	 * do not have a scroll range (i.e. getScrollY() doesn't work).
	 * 
	 * @param view
	 *            - PullToRefreshView that is calling this.
	 * @param deltaY
	 *            - Change in Y in pixels, passed through from from overScrollBy
	 *            call
	 * @param scrollY
	 *            - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param isTouchEvent
	 *            - true if this scroll operation is the result of a touch
	 *            event, passed through from from overScrollBy call
	 */
	static void overScrollBy(PullToRefreshBase<?> view, int deltaY, int scrollY, boolean isTouchEvent) {
		overScrollBy(view, deltaY, scrollY, 0, isTouchEvent);
	}

	/**
	 * Helper method for Overscrolling that encapsulates all of the necessary
	 * function.
	 * 
	 * @param view
	 *            - PullToRefreshView that is calling this.
	 * @param deltaY
	 *            - Change in Y in pixels, passed through from from overScrollBy
	 *            call
	 * @param scrollY
	 *            - Current Y scroll value in pixels before applying deltaY,
	 *            passed through from from overScrollBy call
	 * @param scrollRange
	 *            - Scroll Range of the View, specifically needed for ScrollView
	 * @param isTouchEvent
	 *            - true if this scroll operation is the result of a touch
	 *            event, passed through from from overScrollBy call
	 */
	static void overScrollBy(PullToRefreshBase<?> view, int deltaY, int scrollY, int scrollRange, boolean isTouchEvent) {

		// Check that OverScroll is enabled
		if (view.isPullToRefreshOverScrollEnabled()) {
			final Mode mode = view.getMode();

			// Check that we're not disabled, and the event isn't from touch
			if (mode != Mode.DISABLED && !isTouchEvent) {
				final int newY = (deltaY + scrollY);

				if (PullToRefreshBase.DEBUG) {
					Log.d(LOG_TAG, "OverScroll. DeltaY: " + deltaY + ", ScrollY: " + scrollY + ", NewY: " + newY
							+ ", ScrollRange: " + scrollRange);
				}

				if (newY < 0) {
					// Check the mode supports the overscroll direction, and
					// then move scroll
					if (mode.canPullDown()) {
						view.setHeaderScroll(view.getScrollY() + newY);
					}
				} else if (newY > scrollRange) {
					// Check the mode supports the overscroll direction, and
					// then move scroll
					if (mode.canPullUp()) {
						view.setHeaderScroll(view.getScrollY() + newY - scrollRange);
					}
				} else if (newY == 0 || newY == scrollRange) {
					// Means we've stopped overscrolling, so scroll back to 0
					view.smoothScrollTo(0, PullToRefreshBase.SMOOTH_SCROLL_LONG_DURATION_MS);
				}
			}
		}
	}

	static boolean isAndroidOverScrollEnabled(View view) {
		return view.getOverScrollMode() != View.OVER_SCROLL_NEVER;
	}
}
