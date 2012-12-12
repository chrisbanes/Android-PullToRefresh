package com.handmark.pulltorefresh.library;

import java.util.HashSet;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.handmark.pulltorefresh.library.internal.LoadingLayout;

public class LoadingLayoutProxy implements ILoadingLayout {

	private final PullToRefreshBase<?> mPullToRefreshView;
	private final HashSet<LoadingLayout> mLoadingLayouts;

	LoadingLayoutProxy(PullToRefreshBase<?> pullToRefreshView) {
		mPullToRefreshView = pullToRefreshView;
		mLoadingLayouts = new HashSet<LoadingLayout>();
	}

	void addLayout(LoadingLayout layout) {
		if (null != layout) {
			mLoadingLayouts.add(layout);
		}
	}

	@Override
	public void setLastUpdatedLabel(CharSequence label) {
		for (LoadingLayout layout : mLoadingLayouts) {
			layout.setLastUpdatedLabel(label);
		}

		mPullToRefreshView.refreshLoadingViewsSize();
	}

	@Override
	public void setLoadingDrawable(Drawable drawable) {
		for (LoadingLayout layout : mLoadingLayouts) {
			layout.setLoadingDrawable(drawable);
		}

		mPullToRefreshView.refreshLoadingViewsSize();
	}

	@Override
	public void setRefreshingLabel(CharSequence refreshingLabel) {
		for (LoadingLayout layout : mLoadingLayouts) {
			layout.setRefreshingLabel(refreshingLabel);
		}
	}

	@Override
	public void setPullLabel(CharSequence label) {
		for (LoadingLayout layout : mLoadingLayouts) {
			layout.setPullLabel(label);
		}
	}

	@Override
	public void setReleaseLabel(CharSequence label) {
		for (LoadingLayout layout : mLoadingLayouts) {
			layout.setRefreshingLabel(label);
		}
	}

	public void setTextTypeface(Typeface tf) {
		for (LoadingLayout layout : mLoadingLayouts) {
			layout.setTextTypeface(tf);
		}

		mPullToRefreshView.refreshLoadingViewsSize();
	}
}
