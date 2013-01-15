package com.handmark.pulltorefresh.library.internal;

import com.handmark.pulltorefresh.library.LogManager;

public class Utils {

	static final String LOG_TAG = "PullToRefresh";

	public static void warnDeprecation(String depreacted, String replacement) {
		LogManager.getLogger().w(LOG_TAG, "You're using the deprecated " + depreacted + " attr, please switch over to " + replacement);
	}

}
