package com.handmark.pulltorefresh.library;

import android.annotation.TargetApi;
import android.view.View;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;

@TargetApi(9)
final class OverscrollHelper {

	static void overScrollBy(PullToRefreshBase<?> view, int deltaY, int scrollY, boolean isTouchEvent) {

		// Check that OverScroll is enabled
		if (view.isPullToRefreshOverScrollEnabled()) {
			final Mode mode = view.getMode();

			// Check that we're not disabled, and the event isn't from touch
			if (mode != Mode.DISABLED && !isTouchEvent) {
				final int newY = (deltaY + scrollY);

				if (newY != 0) {
					// Check the mode supports the overscroll direction, and
					// then move scroll
					if ((mode.canPullDown() && newY < 0) || (mode.canPullUp() && newY > 0)) {
						view.setHeaderScroll(view.getScrollY() + newY);
					}
				} else {
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
