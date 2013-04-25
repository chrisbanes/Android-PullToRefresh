/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.handmark.pulltorefresh.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.widget.StaggeredGridView;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.View;

public class PullToRefreshStaggeredGridView extends PullToRefreshBase<StaggeredGridView> {

	private final static String TAG = PullToRefreshStaggeredGridView.class.getSimpleName();

	private static final OnRefreshListener<StaggeredGridView> defaultOnRefreshListener = new OnRefreshListener<StaggeredGridView>() {

		@Override
		public void onRefresh(PullToRefreshBase<StaggeredGridView> refreshView) {
//			refreshView.getRefreshableView().reload();
		}

	};

//	private final WebChromeClient defaultWebChromeClient = new WebChromeClient() {
//
//		@Override
//		public void onProgressChanged(WebView view, int newProgress) {
//			if (newProgress == 100) {
//				onRefreshComplete();
//			}
//		}
//
//	};

	public PullToRefreshStaggeredGridView(Context context) {
		super(context);

		/**
		 * Added so that by default, Pull-to-Refresh refreshes the page
		 */
		setOnRefreshListener(defaultOnRefreshListener);
//		mRefreshableView.setWebChromeClient(defaultWebChromeClient);
	}

	public PullToRefreshStaggeredGridView(Context context, AttributeSet attrs) {
		super(context, attrs);

		/**
		 * Added so that by default, Pull-to-Refresh refreshes the page
		 */
		setOnRefreshListener(defaultOnRefreshListener);
//		mRefreshableView.setWebChromeClient(defaultWebChromeClient);
	}

	public PullToRefreshStaggeredGridView(Context context, Mode mode) {
		super(context, mode);

		/**
		 * Added so that by default, Pull-to-Refresh refreshes the page
		 */
		setOnRefreshListener(defaultOnRefreshListener);
//		mRefreshableView.setWebChromeClient(defaultWebChromeClient);
	}

	public PullToRefreshStaggeredGridView(Context context, Mode mode, AnimationStyle style) {
		super(context, mode, style);

		/**
		 * Added so that by default, Pull-to-Refresh refreshes the page
		 */
		setOnRefreshListener(defaultOnRefreshListener);
//		mRefreshableView.setWebChromeClient(defaultWebChromeClient);
	}

	@Override
	public final Orientation getPullToRefreshScrollDirection() {
		return Orientation.VERTICAL;
	}

	@Override
	protected StaggeredGridView createRefreshableView(Context context, AttributeSet attrs) {
		StaggeredGridView gridView;

		if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
			gridView = new InternalStaggeredGridViewSDK9(context, attrs);
		} else {
			gridView = new StaggeredGridView(context, attrs);
		}

		gridView.setId(R.id.gridview);
		return gridView;
	}

	@Override
	protected boolean isReadyForPullStart() {
//		Log.e(TAG, "[isReadyForPullStart] child count:"+mRefreshableView.getChildCount());
		boolean result = false;
		View v = mRefreshableView.getChildAt(0);
		if(mRefreshableView.getFirstPosition()==0){
			if(v!=null){
				// getTop() and getBottom() are relative to the ListView, 
				// so if getTop() is negative, it is not fully visible
				boolean isTopFullyVisible = v.getTop() >= 0;

				result = isTopFullyVisible;
			}
		}
		return result;
	}

	@Override
	protected boolean isReadyForPullEnd() {
		boolean result = false;
		int last = mRefreshableView.getChildCount() - 1;
		View v = mRefreshableView.getChildAt(last);

		int firstVisiblePosition = mRefreshableView.getFirstPosition();
		int visibleItemCount = mRefreshableView.getChildCount();
		int itemCount	= mRefreshableView.getItemCount();
		if(firstVisiblePosition+visibleItemCount>=itemCount){
			if(v!=null){
				boolean isLastFullyVisible = v.getBottom() <= mRefreshableView.getHeight();

				result = isLastFullyVisible;
			}
		}
		return result;
	}

	@Override
	protected void onPtrRestoreInstanceState(Bundle savedInstanceState) {
		super.onPtrRestoreInstanceState(savedInstanceState);
//		mRefreshableView.restoreState(savedInstanceState);
	}

	@Override
	protected void onPtrSaveInstanceState(Bundle saveState) {
		super.onPtrSaveInstanceState(saveState);
//		mRefreshableView.saveState(saveState);
	}
	
	@TargetApi(9)
	final class InternalStaggeredGridViewSDK9 extends StaggeredGridView {

		// WebView doesn't always scroll back to it's edge so we add some
		// fuzziness
		static final int OVERSCROLL_FUZZY_THRESHOLD = 2;

		// WebView seems quite reluctant to overscroll so we use the scale
		// factor to scale it's value
		static final float OVERSCROLL_SCALE_FACTOR = 1.5f;

		public InternalStaggeredGridViewSDK9(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		
		
		@Override
		protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
				int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

			final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
					scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

			// Does all of the hard work...
//			OverscrollHelper.overScrollBy(PullToRefreshStaggeredGridView.this, deltaX, scrollX, deltaY, scrollY,
//					getScrollRange(), OVERSCROLL_FUZZY_THRESHOLD, OVERSCROLL_SCALE_FACTOR, isTouchEvent);


			// Does all of the hard work...
			OverscrollHelper.overScrollBy(PullToRefreshStaggeredGridView.this, deltaX, scrollX, deltaY, getScrollRange(), isTouchEvent);

			
			return returnValue;
		}
		
		/**
		 * Taken from the AOSP ScrollView source
		 */
		private int getScrollRange() {
			int scrollRange = 0;
			if (getChildCount() > 0) {
				View child = getChildAt(0);
				scrollRange = Math.max(0, child.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
			}
			return scrollRange;
		}

	}
}
