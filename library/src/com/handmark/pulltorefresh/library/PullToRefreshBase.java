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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.handmark.pulltorefresh.library.internal.LoadingLayout;
import com.handmark.pulltorefresh.library.internal.SDK16;

public abstract class PullToRefreshBase<T extends View> extends LinearLayout implements IPullToRefresh<T> {

	// ===========================================================
	// Constants
	// ===========================================================

	static final boolean DEBUG = false;

	static final String LOG_TAG = "PullToRefresh";

	static final float FRICTION = 2.0f;

	public static final int SMOOTH_SCROLL_DURATION_MS = 200;
	public static final int SMOOTH_SCROLL_LONG_DURATION_MS = 325;

	static final int PULL_TO_REFRESH = 0x0;
	static final int RELEASE_TO_REFRESH = 0x1;
	static final int REFRESHING = 0x2;
	static final int MANUAL_REFRESHING = 0x3;

	static final Mode DEFAULT_MODE = Mode.PULL_DOWN_TO_REFRESH;

	static final String STATE_STATE = "ptr_state";
	static final String STATE_MODE = "ptr_mode";
	static final String STATE_CURRENT_MODE = "ptr_current_mode";
	static final String STATE_DISABLE_SCROLLING_REFRESHING = "ptr_disable_scrolling";
	static final String STATE_SHOW_REFRESHING_VIEW = "ptr_show_refreshing_view";
	static final String STATE_SUPER = "ptr_super";

	// ===========================================================
	// Fields
	// ===========================================================

	private int mTouchSlop;
	private float mLastMotionX;
	private float mLastMotionY;
	private float mInitialMotionY;

	private boolean mIsBeingDragged = false;
	private boolean isPullStart = true;
	private int mState = PULL_TO_REFRESH;
	private Mode mMode = DEFAULT_MODE;

	private Mode mCurrentMode;
	T mRefreshableView;
	private FrameLayout mRefreshableViewWrapper;

	private boolean mShowViewWhileRefreshing = true;
	private boolean mDisableScrollingWhileRefreshing = true;
	private boolean mFilterTouchEvents = true;
	private boolean mOverScrollEnabled = true;

	private Interpolator mScrollAnimationInterpolator;

	private LoadingLayout mHeaderLayout;
	private LoadingLayout mFooterLayout;

	private int mHeaderHeight;

	private OnRefreshListener<T> mOnRefreshListener;
	private OnRefreshListener2<T> mOnRefreshListener2;

	private SmoothScrollRunnable mCurrentSmoothScrollRunnable;

	// ===========================================================
	// Constructors
	// ===========================================================

	public PullToRefreshBase(Context context) {
		super(context);
		init(context, null);
	}

	public PullToRefreshBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public PullToRefreshBase(Context context, Mode mode) {
		super(context);
		mMode = mode;
		init(context, null);
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		if (DEBUG) {
			Log.d(LOG_TAG, "addView: " + child.getClass().getSimpleName());
		}

		final T refreshableView = getRefreshableView();

		if (refreshableView instanceof ViewGroup) {
			((ViewGroup) refreshableView).addView(child, index, params);
		} else {
			throw new UnsupportedOperationException("Refreshable View is not a ViewGroup so can't addView");
		}
	}

	@Override
	public final Mode getCurrentMode() {
		return mCurrentMode;
	}

	@Override
	public final boolean getFilterTouchEvents() {
		return mFilterTouchEvents;
	}

	@Override
	public final Mode getMode() {
		return mMode;
	}

	@Override
	public final T getRefreshableView() {
		return mRefreshableView;
	}

	@Override
	public final boolean getShowViewWhileRefreshing() {
		return mShowViewWhileRefreshing;
	}

	@Override
	public final boolean hasPullFromTop() {
		return mCurrentMode == Mode.PULL_DOWN_TO_REFRESH;
	}

	@Override
	public final boolean isDisableScrollingWhileRefreshing() {
		return mDisableScrollingWhileRefreshing;
	}

	@Override
	public final boolean isPullToRefreshEnabled() {
		return mMode != Mode.DISABLED;
	}

	@Override
	public final boolean isPullToRefreshOverScrollEnabled() {
		if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
			return mOverScrollEnabled && OverscrollHelper.isAndroidOverScrollEnabled(mRefreshableView);
		}
		return false;
	}

	@Override
	public final boolean isRefreshing() {
		return mState == REFRESHING || mState == MANUAL_REFRESHING;
	}

	@Override
	public final boolean onInterceptTouchEvent(MotionEvent event) {

		if (!isPullToRefreshEnabled()) {
			return false;
		}

		final int action = event.getAction();

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			mIsBeingDragged = false;
			return false;
		}

		if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
			return true;
		}

		switch (action) {
			case MotionEvent.ACTION_MOVE: {
				// If we're refreshing, and the flag is set. Eat all MOVE events
				if (mDisableScrollingWhileRefreshing && isRefreshing()) {
					return true;
				}

				if (isReadyForPull()) {
					final float y = event.getY();
					final float dy = y - mLastMotionY;
					final float yDiff = Math.abs(dy);
					final float xDiff = Math.abs(event.getX() - mLastMotionX);

					if (yDiff > mTouchSlop && (!mFilterTouchEvents || yDiff > xDiff)) {
						if (mMode.canPullDown() && dy >= 1f && isReadyForPullDown()) {
							mLastMotionY = y;
							mIsBeingDragged = true;
							if (mMode == Mode.BOTH) {
								mCurrentMode = Mode.PULL_DOWN_TO_REFRESH;
							}
						} else if (mMode.canPullUp() && dy <= -1f && isReadyForPullUp()) {
							mLastMotionY = y;
							mIsBeingDragged = true;
							if (mMode == Mode.BOTH) {
								mCurrentMode = Mode.PULL_UP_TO_REFRESH;
							}
						}
					}
				}
				break;
			}
			case MotionEvent.ACTION_DOWN: {
				if (isReadyForPull()) {
					mLastMotionY = mInitialMotionY = event.getY();
					mLastMotionX = event.getX();
					mIsBeingDragged = false;
				}
				break;
			}
		}

		return mIsBeingDragged;
	}

	@Override
	public final void onRefreshComplete() {
		if (mState != PULL_TO_REFRESH) {
			resetHeader();
		}
	}

	@Override
	public final boolean onTouchEvent(MotionEvent event) {

		if (!isPullToRefreshEnabled()) {
			return false;
		}

		// If we're refreshing, and the flag is set. Eat the event
		if (mDisableScrollingWhileRefreshing && isRefreshing()) {
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE: {
				if (isPullStart) {
					isPullStart = false;
					onPullToRefresh();
				}
				if (mIsBeingDragged) {
					mLastMotionY = event.getY();
					pullEvent();
					return true;
				}
				break;
			}

			case MotionEvent.ACTION_DOWN: {
				if (isReadyForPull()) {
					mLastMotionY = mInitialMotionY = event.getY();
					return true;
				}
				break;
			}

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				isPullStart = true;
				if (mIsBeingDragged) {
					mIsBeingDragged = false;

					if (mState == RELEASE_TO_REFRESH) {
						if (null != mOnRefreshListener) {
							setRefreshingInternal(true);
							mOnRefreshListener.onRefresh(this);
							return true;

						} else if (null != mOnRefreshListener2) {
							setRefreshingInternal(true);
							if (mCurrentMode == Mode.PULL_DOWN_TO_REFRESH) {
								mOnRefreshListener2.onPullDownToRefresh(this);
							} else if (mCurrentMode == Mode.PULL_UP_TO_REFRESH) {
								mOnRefreshListener2.onPullUpToRefresh(this);
							}
							return true;
						} else {
							// If we don't have a listener, just reset
							resetHeader();
							return true;
						}
					}

					smoothScrollTo(0);
					return true;
				}
				break;
			}
		}

		return false;
	}

	@Override
	public final void setDisableScrollingWhileRefreshing(boolean disableScrollingWhileRefreshing) {
		mDisableScrollingWhileRefreshing = disableScrollingWhileRefreshing;
	}

	@Override
	public final void setFilterTouchEvents(boolean filterEvents) {
		mFilterTouchEvents = filterEvents;
	}

	@Override
	public void setLastUpdatedLabel(CharSequence label) {
		if (null != mHeaderLayout) {
			mHeaderLayout.setSubHeaderText(label);
		}
		if (null != mFooterLayout) {
			mFooterLayout.setSubHeaderText(label);
		}

		// Refresh Height as it may have changed
		refreshLoadingViewsHeight();
	}

	@Override
	public void setLoadingDrawable(Drawable drawable) {
		setLoadingDrawable(drawable, Mode.BOTH);
	}

	@Override
	public void setLoadingDrawable(Drawable drawable, Mode mode) {
		if (null != mHeaderLayout && mode.canPullDown()) {
			mHeaderLayout.setLoadingDrawable(drawable);
		}
		if (null != mFooterLayout && mode.canPullUp()) {
			mFooterLayout.setLoadingDrawable(drawable);
		}

		// The Loading Height may have changed, so refresh
		refreshLoadingViewsHeight();
	}

	@Override
	public void setLongClickable(boolean longClickable) {
		getRefreshableView().setLongClickable(longClickable);
	}

	@Override
	public final void setMode(Mode mode) {
		if (mode != mMode) {
			if (DEBUG) {
				Log.d(LOG_TAG, "Setting mode to: " + mode);
			}
			mMode = mode;
			updateUIForMode();
		}
	}

	@Override
	public final void setOnRefreshListener(OnRefreshListener<T> listener) {
		mOnRefreshListener = listener;
	}

	@Override
	public final void setOnRefreshListener(OnRefreshListener2<T> listener) {
		mOnRefreshListener2 = listener;
	}

	@Override
	public void setPullLabel(String pullLabel) {
		setPullLabel(pullLabel, Mode.BOTH);
	}

	@Override
	public void setPullLabel(String pullLabel, Mode mode) {
		if (null != mHeaderLayout && mode.canPullDown()) {
			mHeaderLayout.setPullLabel(pullLabel);
		}
		if (null != mFooterLayout && mode.canPullUp()) {
			mFooterLayout.setPullLabel(pullLabel);
		}
	}

	@Override
	public final void setPullToRefreshEnabled(boolean enable) {
		setMode(enable ? DEFAULT_MODE : Mode.DISABLED);
	}

	@Override
	public final void setPullToRefreshOverScrollEnabled(boolean enabled) {
		mOverScrollEnabled = enabled;
	}

	@Override
	public final void setRefreshing() {
		setRefreshing(true);
	}

	@Override
	public final void setRefreshing(boolean doScroll) {
		if (!isRefreshing()) {
			setRefreshingInternal(doScroll);
			mState = MANUAL_REFRESHING;
		}
	}

	@Override
	public void setRefreshingLabel(String refreshingLabel) {
		setRefreshingLabel(refreshingLabel, Mode.BOTH);
	}

	@Override
	public void setRefreshingLabel(String refreshingLabel, Mode mode) {
		if (null != mHeaderLayout && mode.canPullDown()) {
			mHeaderLayout.setRefreshingLabel(refreshingLabel);
		}
		if (null != mFooterLayout && mode.canPullUp()) {
			mFooterLayout.setRefreshingLabel(refreshingLabel);
		}
	}

	@Override
	public void setReleaseLabel(String releaseLabel) {
		setReleaseLabel(releaseLabel, Mode.BOTH);
	}

	@Override
	public void setReleaseLabel(String releaseLabel, Mode mode) {
		if (null != mHeaderLayout && mode.canPullDown()) {
			mHeaderLayout.setReleaseLabel(releaseLabel);
		}
		if (null != mFooterLayout && mode.canPullUp()) {
			mFooterLayout.setReleaseLabel(releaseLabel);
		}
	}

	public void setScrollAnimationInterpolator(Interpolator interpolator) {
		mScrollAnimationInterpolator = interpolator;
	}

	@Override
	public final void setShowViewWhileRefreshing(boolean showView) {
		mShowViewWhileRefreshing = showView;
	}

	/**
	 * Used internally for adding view. Need because we override addView to
	 * pass-through to the Refreshable View
	 */
	protected final void addViewInternal(View child, int index, ViewGroup.LayoutParams params) {
		super.addView(child, index, params);
	}

	/**
	 * Used internally for adding view. Need because we override addView to
	 * pass-through to the Refreshable View
	 */
	protected final void addViewInternal(View child, ViewGroup.LayoutParams params) {
		super.addView(child, -1, params);
	}

	protected LoadingLayout createLoadingLayout(Context context, Mode mode, TypedArray attrs) {
		return new LoadingLayout(context, mode, attrs);
	}

	/**
	 * This is implemented by derived classes to return the created View. If you
	 * need to use a custom View (such as a custom ListView), override this
	 * method and return an instance of your custom class.
	 * 
	 * Be sure to set the ID of the view in this method, especially if you're
	 * using a ListActivity or ListFragment.
	 * 
	 * @param context
	 *            Context to create view with
	 * @param attrs
	 *            AttributeSet from wrapped class. Means that anything you
	 *            include in the XML layout declaration will be routed to the
	 *            created View
	 * @return New instance of the Refreshable View
	 */
	protected abstract T createRefreshableView(Context context, AttributeSet attrs);

	protected final LoadingLayout getFooterLayout() {
		return mFooterLayout;
	}

	protected final int getHeaderHeight() {
		return mHeaderHeight;
	}

	protected final LoadingLayout getHeaderLayout() {
		return mHeaderLayout;
	}

	protected FrameLayout getRefreshableViewWrapper() {
		return mRefreshableViewWrapper;
	}

	protected final int getState() {
		return mState;
	}

	/**
	 * Allows Derivative classes to handle the XML Attrs without creating a
	 * TypedArray themsevles
	 * 
	 * @param a
	 *            - TypedArray of PullToRefresh Attributes
	 */
	protected void handleStyledAttributes(TypedArray a) {
	}

	/**
	 * Implemented by derived class to return whether the View is in a mState
	 * where the user can Pull to Refresh by scrolling down.
	 * 
	 * @return true if the View is currently the correct mState (for example,
	 *         top of a ListView)
	 */
	protected abstract boolean isReadyForPullDown();

	/**
	 * Implemented by derived class to return whether the View is in a mState
	 * where the user can Pull to Refresh by scrolling up.
	 * 
	 * @return true if the View is currently in the correct mState (for example,
	 *         bottom of a ListView)
	 */
	protected abstract boolean isReadyForPullUp();

	/**
	 * Called when the UI needs to be updated to the 'Pull to Refresh' state
	 */
	protected void onPullToRefresh() {
		switch (mCurrentMode) {
			case PULL_UP_TO_REFRESH:
				mFooterLayout.pullToRefresh();
				break;
			case PULL_DOWN_TO_REFRESH:
				mHeaderLayout.pullToRefresh();
				break;
		}
	}

	/**
	 * Called when the UI needs to be updated to the 'Release to Refresh' state
	 */
	protected void onReleaseToRefresh() {
		switch (mCurrentMode) {
			case PULL_UP_TO_REFRESH:
				mFooterLayout.releaseToRefresh();
				break;
			case PULL_DOWN_TO_REFRESH:
				mHeaderLayout.releaseToRefresh();
				break;
		}
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;

			mMode = Mode.mapIntToMode(bundle.getInt(STATE_MODE, 0));
			mCurrentMode = Mode.mapIntToMode(bundle.getInt(STATE_CURRENT_MODE, 0));

			mDisableScrollingWhileRefreshing = bundle.getBoolean(STATE_DISABLE_SCROLLING_REFRESHING, true);
			mShowViewWhileRefreshing = bundle.getBoolean(STATE_SHOW_REFRESHING_VIEW, true);

			// Let super Restore Itself
			super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));

			final int viewState = bundle.getInt(STATE_STATE, PULL_TO_REFRESH);
			if (viewState == REFRESHING || viewState == MANUAL_REFRESHING) {
				setRefreshingInternal(true);
				mState = viewState;
			}
			return;
		}

		super.onRestoreInstanceState(state);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putInt(STATE_STATE, mState);
		bundle.putInt(STATE_MODE, mMode.getIntValue());
		bundle.putInt(STATE_CURRENT_MODE, mCurrentMode.getIntValue());
		bundle.putBoolean(STATE_DISABLE_SCROLLING_REFRESHING, mDisableScrollingWhileRefreshing);
		bundle.putBoolean(STATE_SHOW_REFRESHING_VIEW, mShowViewWhileRefreshing);
		bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());
		return bundle;
	}

	protected void resetHeader() {
		mState = PULL_TO_REFRESH;
		mIsBeingDragged = false;

		if (mMode.canPullDown()) {
			mHeaderLayout.reset();
		}
		if (mMode.canPullUp()) {
			mFooterLayout.reset();
		}

		smoothScrollTo(0);
	}

	protected final void setHeaderScroll(int y) {
		scrollTo(0, y);
	}

	protected void setRefreshingInternal(boolean doScroll) {
		mState = REFRESHING;

		if (mMode.canPullDown()) {
			mHeaderLayout.refreshing();
		}
		if (mMode.canPullUp()) {
			mFooterLayout.refreshing();
		}

		if (doScroll) {
			if (mShowViewWhileRefreshing) {
				smoothScrollTo(mCurrentMode == Mode.PULL_DOWN_TO_REFRESH ? -mHeaderHeight : mHeaderHeight);
			} else {
				smoothScrollTo(0);
			}
		}
	}

	/**
	 * Smooth Scroll to Y position using the default duration of
	 * {@value #SMOOTH_SCROLL_DURATION_MS} ms.
	 * 
	 * @param y
	 *            - Y position to scroll to
	 */
	protected final void smoothScrollTo(int y) {
		smoothScrollTo(y, SMOOTH_SCROLL_DURATION_MS);
	}

	/**
	 * Smooth Scroll to Y position using the specific duration
	 * 
	 * @param y
	 *            - Y position to scroll to
	 * @param duration
	 *            - Duration of animation in milliseconds
	 */
	protected final void smoothScrollTo(int y, long duration) {
		if (null != mCurrentSmoothScrollRunnable) {
			mCurrentSmoothScrollRunnable.stop();
		}

		if (getScrollY() != y) {
			if (null == mScrollAnimationInterpolator) {
				// Default interpolator is a Decelerate Interpolator
				mScrollAnimationInterpolator = new DecelerateInterpolator();
			}
			mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(getScrollY(), y, duration);
			post(mCurrentSmoothScrollRunnable);
		}
	}

	/**
	 * Updates the View State when the mode has been set. This does not do any
	 * checking that the mode is different to current state so always updates.
	 */
	protected void updateUIForMode() {
		// Remove Header, and then add Header Loading View again if needed
		if (this == mHeaderLayout.getParent()) {
			removeView(mHeaderLayout);
		}
		if (mMode.canPullDown()) {
			addViewInternal(mHeaderLayout, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
		}

		// Remove Footer, and then add Footer Loading View again if needed
		if (this == mFooterLayout.getParent()) {
			removeView(mFooterLayout);
		}
		if (mMode.canPullUp()) {
			addViewInternal(mFooterLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
		}

		// Hide Loading Views
		refreshLoadingViewsHeight();

		// If we're not using Mode.BOTH, set mCurrentMode to mMode, otherwise
		// set it to pull down
		mCurrentMode = (mMode != Mode.BOTH) ? mMode : Mode.PULL_DOWN_TO_REFRESH;
	}

	private void addRefreshableView(Context context, T refreshableView) {
		mRefreshableViewWrapper = new FrameLayout(context);
		mRefreshableViewWrapper.addView(refreshableView, ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		addViewInternal(mRefreshableViewWrapper, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
	}

	@SuppressWarnings("deprecation")
	private void init(Context context, AttributeSet attrs) {
		setOrientation(LinearLayout.VERTICAL);

		ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();

		// Styleables from XML
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);

		if (a.hasValue(R.styleable.PullToRefresh_ptrMode)) {
			mMode = Mode.mapIntToMode(a.getInteger(R.styleable.PullToRefresh_ptrMode, 0));
		}

		// Refreshable View
		// By passing the attrs, we can add ListView/GridView params via XML
		mRefreshableView = createRefreshableView(context, attrs);
		addRefreshableView(context, mRefreshableView);

		// We need to create now layouts now
		mHeaderLayout = createLoadingLayout(context, Mode.PULL_DOWN_TO_REFRESH, a);
		mFooterLayout = createLoadingLayout(context, Mode.PULL_UP_TO_REFRESH, a);

		// Styleables from XML
		if (a.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
			Drawable background = a.getDrawable(R.styleable.PullToRefresh_ptrHeaderBackground);
			if (null != background) {
				setBackgroundDrawable(background);
			}
		}
		if (a.hasValue(R.styleable.PullToRefresh_ptrAdapterViewBackground)) {
			Drawable background = a.getDrawable(R.styleable.PullToRefresh_ptrAdapterViewBackground);
			if (null != background) {
				mRefreshableView.setBackgroundDrawable(background);
			}
		}
		if (a.hasValue(R.styleable.PullToRefresh_ptrOverScroll)) {
			mOverScrollEnabled = a.getBoolean(R.styleable.PullToRefresh_ptrOverScroll, true);
		}

		// Let the derivative classes have a go at handling attributes, then
		// recycle them...
		handleStyledAttributes(a);
		a.recycle();

		// Finally update the UI for the modes
		updateUIForMode();
	}

	private boolean isReadyForPull() {
		switch (mMode) {
			case PULL_DOWN_TO_REFRESH:
				return isReadyForPullDown();
			case PULL_UP_TO_REFRESH:
				return isReadyForPullUp();
			case BOTH:
				return isReadyForPullUp() || isReadyForPullDown();
		}
		return false;
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	/**
	 * Actions a Pull Event
	 * 
	 * @return true if the Event has been handled, false if there has been no
	 *         change
	 */
	private boolean pullEvent() {

		final int newHeight;
		final int oldHeight = getScrollY();

		switch (mCurrentMode) {
			case PULL_UP_TO_REFRESH:
				newHeight = Math.round(Math.max(mInitialMotionY - mLastMotionY, 0) / FRICTION);
				break;
			case PULL_DOWN_TO_REFRESH:
			default:
				newHeight = Math.round(Math.min(mInitialMotionY - mLastMotionY, 0) / FRICTION);
				break;
		}

		setHeaderScroll(newHeight);

		if (newHeight != 0) {

			float scale = Math.abs(newHeight) / (float) mHeaderHeight;
			switch (mCurrentMode) {
				case PULL_UP_TO_REFRESH:
					mFooterLayout.onPullY(scale);
					break;
				case PULL_DOWN_TO_REFRESH:
					mHeaderLayout.onPullY(scale);
					break;
			}

			if (mState == PULL_TO_REFRESH && mHeaderHeight < Math.abs(newHeight)) {
				mState = RELEASE_TO_REFRESH;
				onReleaseToRefresh();
				return true;

			} else if (mState == RELEASE_TO_REFRESH && mHeaderHeight >= Math.abs(newHeight)) {
				mState = PULL_TO_REFRESH;
				onPullToRefresh();
				return true;
			}
		}

		return oldHeight != newHeight;
	}

	/**
	 * Re-measure the Loading Views height, and adjust internal padding as
	 * necessary
	 */
	private void refreshLoadingViewsHeight() {
		if (mMode.canPullDown()) {
			measureView(mHeaderLayout);
			mHeaderHeight = mHeaderLayout.getMeasuredHeight();
		} else if (mMode.canPullUp()) {
			measureView(mFooterLayout);
			mHeaderHeight = mFooterLayout.getMeasuredHeight();
		} else {
			mHeaderHeight = 0;
		}

		// Hide Loading Views
		switch (mMode) {
			case DISABLED:
				setPadding(0, 0, 0, 0);
			case BOTH:
				setPadding(0, -mHeaderHeight, 0, -mHeaderHeight);
				break;
			case PULL_UP_TO_REFRESH:
				setPadding(0, 0, 0, -mHeaderHeight);
				break;
			case PULL_DOWN_TO_REFRESH:
			default:
				setPadding(0, -mHeaderHeight, 0, 0);
				break;
		}
	}

	public static enum Mode {
		/**
		 * Disable all Pull-to-Refresh gesture handling
		 */
		DISABLED(0x0),

		/**
		 * Only allow the user to Pull Down from the top to refresh, this is the
		 * default.
		 */
		PULL_DOWN_TO_REFRESH(0x1),

		/**
		 * Only allow the user to Pull Up from the bottom to refresh.
		 */
		PULL_UP_TO_REFRESH(0x2),

		/**
		 * Allow the user to both Pull Down from the top, and Pull Up from the
		 * bottom to refresh.
		 */
		BOTH(0x3);

		/**
		 * Maps an int to a specific mode. This is needed when saving state, or
		 * inflating the view from XML where the mode is given through a attr
		 * int.
		 * 
		 * @param modeInt
		 *            - int to map a Mode to
		 * @return Mode that modeInt maps to, or PULL_DOWN_TO_REFRESH by
		 *         default.
		 */
		public static Mode mapIntToMode(int modeInt) {
			switch (modeInt) {
				case 0x0:
					return DISABLED;
				case 0x1:
				default:
					return PULL_DOWN_TO_REFRESH;
				case 0x2:
					return PULL_UP_TO_REFRESH;
				case 0x3:
					return BOTH;
			}
		}

		private int mIntValue;

		// The modeInt values need to match those from attrs.xml
		Mode(int modeInt) {
			mIntValue = modeInt;
		}

		/**
		 * @return true if this mode permits Pulling Down from the top
		 */
		boolean canPullDown() {
			return this == PULL_DOWN_TO_REFRESH || this == BOTH;
		}

		/**
		 * @return true if this mode permits Pulling Up from the bottom
		 */
		boolean canPullUp() {
			return this == PULL_UP_TO_REFRESH || this == BOTH;
		}

		int getIntValue() {
			return mIntValue;
		}

	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * Simple Listener that allows you to be notified when the user has scrolled
	 * to the end of the AdapterView. See (
	 * {@link PullToRefreshAdapterViewBase#setOnLastItemVisibleListener}.
	 * 
	 * @author Chris Banes
	 * 
	 */
	public static interface OnLastItemVisibleListener {

		/**
		 * Called when the user has scrolled to the end of the list
		 */
		public void onLastItemVisible();

	}

	/**
	 * Simple Listener to listen for any callbacks to Refresh.
	 * 
	 * @author Chris Banes
	 */
	public static interface OnRefreshListener<V extends View> {

		/**
		 * onRefresh will be called for both Pull Down from top, and Pull Up
		 * from Bottom
		 */
		public void onRefresh(final PullToRefreshBase<V> refreshView);

	}

	/**
	 * An advanced version of the Listener to listen for callbacks to Refresh.
	 * This listener is different as it allows you to differentiate between Pull
	 * Ups, and Pull Downs.
	 * 
	 * @author Chris Banes
	 */
	public static interface OnRefreshListener2<V extends View> {

		/**
		 * onPullDownToRefresh will be called only when the user has Pulled Down
		 * from the top, and released.
		 */
		public void onPullDownToRefresh(final PullToRefreshBase<V> refreshView);

		/**
		 * onPullUpToRefresh will be called only when the user has Pulled Up
		 * from the bottom, and released.
		 */
		public void onPullUpToRefresh(final PullToRefreshBase<V> refreshView);

	}

	final class SmoothScrollRunnable implements Runnable {

		static final int ANIMATION_DELAY = 10;

		private final Interpolator mInterpolator;
		private final int mScrollToY;
		private final int mScrollFromY;
		private final long mDuration;

		private boolean mContinueRunning = true;
		private long mStartTime = -1;
		private int mCurrentY = -1;

		public SmoothScrollRunnable(int fromY, int toY, long duration) {
			mScrollFromY = fromY;
			mScrollToY = toY;
			mInterpolator = mScrollAnimationInterpolator;
			mDuration = duration;
		}

		@Override
		public void run() {

			/**
			 * Only set mStartTime if this is the first time we're starting,
			 * else actually calculate the Y delta
			 */
			if (mStartTime == -1) {
				mStartTime = System.currentTimeMillis();
			} else {

				/**
				 * We do do all calculations in long to reduce software float
				 * calculations. We use 1000 as it gives us good accuracy and
				 * small rounding errors
				 */
				long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

				final int deltaY = Math.round((mScrollFromY - mScrollToY)
						* mInterpolator.getInterpolation(normalizedTime / 1000f));
				mCurrentY = mScrollFromY - deltaY;
				setHeaderScroll(mCurrentY);
			}

			// If we're not at the target Y, keep going...
			if (mContinueRunning && mScrollToY != mCurrentY) {
				if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
					SDK16.postOnAnimation(PullToRefreshBase.this, this);
				} else {
					postDelayed(this, ANIMATION_DELAY);
				}
			}
		}

		public void stop() {
			mContinueRunning = false;
			removeCallbacks(this);
		}
	}

}
