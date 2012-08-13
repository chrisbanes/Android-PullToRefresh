package com.handmark.pulltorefresh.library;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;

final class OverscrollHelper {

	static void overScrollBy(PullToRefreshBase<?> view, int deltaY, int scrollY, boolean isTouchEvent) {
		final Mode mode = view.getCurrentMode();

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
