package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import com.handmark.pulltorefresh.library.internal.LoadingLayout;

public abstract class PullToRefreshBase<T extends View> extends LinearLayout {

	final class SmoothScrollRunnable implements Runnable {

		static final int ANIMATION_DURATION_MS = 190;
		static final int ANIMATION_FPS = 1000 / 60;

		private final Interpolator mInterpolator;
		private final int mScrollToY;
		private final int mScrollFromY;
		private final Handler mHandler;

		private boolean mContinueRunning = true;
		private long mStartTime = -1;
		private int mCurrentY = -1;

		public SmoothScrollRunnable(Handler handler, int fromY, int toY) {
			mHandler = handler;
			mScrollFromY = fromY;
			mScrollToY = toY;
			mInterpolator = new AccelerateDecelerateInterpolator();
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
				long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / ANIMATION_DURATION_MS;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

				final int deltaY = Math.round((mScrollFromY - mScrollToY)
						* mInterpolator.getInterpolation(normalizedTime / 1000f));
				mCurrentY = mScrollFromY - deltaY;
				setHeaderScroll(mCurrentY);
			}

			// If we're not at the target Y, keep going...
			if (mContinueRunning && mScrollToY != mCurrentY) {
				mHandler.postDelayed(this, ANIMATION_FPS);
			}
		}

		public void stop() {
			mContinueRunning = false;
			mHandler.removeCallbacks(this);
		}
	}

	// ===========================================================
	// Constants
	// ===========================================================

	static final boolean DEBUG = false;
	static final String LOG_TAG = "PullToRefresh";

	static final float FRICTION = 2.0f;

	static final int PULL_TO_REFRESH = 0x0;
	static final int RELEASE_TO_REFRESH = 0x1;
	static final int REFRESHING = 0x2;
	static final int MANUAL_REFRESHING = 0x3;

	public static final int MODE_PULL_DOWN_TO_REFRESH = 0x1;
	public static final int MODE_PULL_UP_TO_REFRESH = 0x2;
	public static final int MODE_BOTH = 0x3;

	// ===========================================================
	// Fields
	// ===========================================================

	private int mTouchSlop;

	private float mInitialMotionY;
	private float mLastMotionX;
	private float mLastMotionY;
	private boolean mIsBeingDragged = false;

	private int mState = PULL_TO_REFRESH;
	private int mMode = MODE_PULL_DOWN_TO_REFRESH;
	private int mCurrentMode;

	private boolean mDisableScrollingWhileRefreshing = true;

	T mRefreshableView;
	private boolean mIsPullToRefreshEnabled = true;

	private LoadingLayout mHeaderLayout;
	private LoadingLayout mFooterLayout;
	private int mHeaderHeight;

	private final Handler mHandler = new Handler();

	private OnRefreshListener mOnRefreshListener;
	private OnRefreshListener2 mOnRefreshListener2;

	private SmoothScrollRunnable mCurrentSmoothScrollRunnable;

	// ===========================================================
	// Constructors
	// ===========================================================

	public PullToRefreshBase(Context context) {
		super(context);
		init(context, null);
	}

	public PullToRefreshBase(Context context, int mode) {
		super(context);
		mMode = mode;
		init(context, null);
	}

	public PullToRefreshBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * Deprecated. Use {@link #getRefreshableView()} from now on.
	 * 
	 * @deprecated
	 * @return The Refreshable View which is currently wrapped
	 */
	public final T getAdapterView() {
		return mRefreshableView;
	}

	/**
	 * Get the Wrapped Refreshable View. Anything returned here has already been
	 * added to the content view.
	 * 
	 * @return The View which is currently wrapped
	 */
	public final T getRefreshableView() {
		return mRefreshableView;
	}

	/**
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public final boolean isPullToRefreshEnabled() {
		return mIsPullToRefreshEnabled;
	}

	/**
	 * Get the mode that this view is currently in. This is only really useful
	 * when using <code>MODE_BOTH</code>.
	 * 
	 * @return int of either <code>MODE_PULL_DOWN_TO_REFRESH</code> or
	 *         <code>MODE_PULL_UP_TO_REFRESH</code>
	 */
	public final int getCurrentMode() {
		return mCurrentMode;
	}

	/**
	 * Get the mode that this view has been set to. If this returns
	 * <code>MODE_BOTH</code>, you can use <code>getCurrentMode()</code> to
	 * check which mode the view is currently in
	 * 
	 * @return int of either <code>MODE_PULL_DOWN_TO_REFRESH</code>,
	 *         <code>MODE_PULL_UP_TO_REFRESH</code> or <code>MODE_BOTH</code>
	 */
	public final int getMode() {
		return mMode;
	}

	/**
	 * Returns whether the widget has disabled scrolling on the Refreshable View
	 * while refreshing.
	 * 
	 * @return true if the widget has disabled scrolling while refreshing
	 */
	public final boolean isDisableScrollingWhileRefreshing() {
		return mDisableScrollingWhileRefreshing;
	}

	/**
	 * Returns whether the Widget is currently in the Refreshing mState
	 * 
	 * @return true if the Widget is currently refreshing
	 */
	public final boolean isRefreshing() {
		return mState == REFRESHING || mState == MANUAL_REFRESHING;
	}

	/**
	 * By default the Widget disabled scrolling on the Refreshable View while
	 * refreshing. This method can change this behaviour.
	 * 
	 * @param disableScrollingWhileRefreshing
	 *            - true if you want to disable scrolling while refreshing
	 */
	public final void setDisableScrollingWhileRefreshing(boolean disableScrollingWhileRefreshing) {
		mDisableScrollingWhileRefreshing = disableScrollingWhileRefreshing;
	}

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public final void onRefreshComplete() {
		if (mState != PULL_TO_REFRESH) {
			resetHeader();
		}
	}

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public final void setOnRefreshListener(OnRefreshListener listener) {
		mOnRefreshListener = listener;
	}

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public final void setOnRefreshListener(OnRefreshListener2 listener) {
		mOnRefreshListener2 = listener;
	}

	/**
	 * A mutator to enable/disable Pull-to-Refresh for the current View
	 * 
	 * @param enable
	 *            Whether Pull-To-Refresh should be used
	 */
	public final void setPullToRefreshEnabled(boolean enable) {
		mIsPullToRefreshEnabled = enable;
	}

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public void setReleaseLabel(String releaseLabel) {
		if (null != mHeaderLayout) {
			mHeaderLayout.setReleaseLabel(releaseLabel);
		}
		if (null != mFooterLayout) {
			mFooterLayout.setReleaseLabel(releaseLabel);
		}
	}

	/**
	 * Set Text to show when the Widget is being Pulled
	 * 
	 * @param pullLabel
	 *            - String to display
	 */
	public void setPullLabel(String pullLabel) {
		if (null != mHeaderLayout) {
			mHeaderLayout.setPullLabel(pullLabel);
		}
		if (null != mFooterLayout) {
			mFooterLayout.setPullLabel(pullLabel);
		}
	}

	/**
	 * Set Text to show when the Widget is refreshing
	 * 
	 * @param refreshingLabel
	 *            - String to display
	 */
	public void setRefreshingLabel(String refreshingLabel) {
		if (null != mHeaderLayout) {
			mHeaderLayout.setRefreshingLabel(refreshingLabel);
		}
		if (null != mFooterLayout) {
			mFooterLayout.setRefreshingLabel(refreshingLabel);
		}
	}

	public final void setRefreshing() {
		setRefreshing(true);
	}

	/**
	 * Sets the Widget to be in the refresh mState. The UI will be updated to
	 * show the 'Refreshing' view.
	 * 
	 * @param doScroll
	 *            - true if you want to force a scroll to the Refreshing view.
	 */
	public final void setRefreshing(boolean doScroll) {
		if (!isRefreshing()) {
			setRefreshingInternal(doScroll);
			mState = MANUAL_REFRESHING;
		}
	}

	public final boolean hasPullFromTop() {
		return mCurrentMode != MODE_PULL_UP_TO_REFRESH;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public final boolean onTouchEvent(MotionEvent event) {
		if (!mIsPullToRefreshEnabled) {
			return false;
		}

		if (isRefreshing() && mDisableScrollingWhileRefreshing) {
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}

		switch (event.getAction()) {

			case MotionEvent.ACTION_MOVE: {
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
				if (mIsBeingDragged) {
					mIsBeingDragged = false;

					if (mState == RELEASE_TO_REFRESH) {

						if (null != mOnRefreshListener) {
							setRefreshingInternal(true);
							mOnRefreshListener.onRefresh();
							return true;

						} else if (null != mOnRefreshListener2) {
							setRefreshingInternal(true);
							if (mCurrentMode == MODE_PULL_DOWN_TO_REFRESH) {
								mOnRefreshListener2.onPullDownToRefresh();
							} else if (mCurrentMode == MODE_PULL_UP_TO_REFRESH) {
								mOnRefreshListener2.onPullUpToRefresh();
							}
							return true;
						}

						return true;
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
	public final boolean onInterceptTouchEvent(MotionEvent event) {

		if (!mIsPullToRefreshEnabled) {
			return false;
		}

		if (isRefreshing() && mDisableScrollingWhileRefreshing) {
			return true;
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
				if (isReadyForPull()) {

					final float y = event.getY();
					final float dy = y - mLastMotionY;
					final float yDiff = Math.abs(dy);
					final float xDiff = Math.abs(event.getX() - mLastMotionX);

					if (yDiff > mTouchSlop && yDiff > xDiff) {
						if ((mMode == MODE_PULL_DOWN_TO_REFRESH || mMode == MODE_BOTH) && dy >= 0.0001f
								&& isReadyForPullDown()) {
							mLastMotionY = y;
							mIsBeingDragged = true;
							if (mMode == MODE_BOTH) {
								mCurrentMode = MODE_PULL_DOWN_TO_REFRESH;
							}
						} else if ((mMode == MODE_PULL_UP_TO_REFRESH || mMode == MODE_BOTH) && dy <= 0.0001f
								&& isReadyForPullUp()) {
							mLastMotionY = y;
							mIsBeingDragged = true;
							if (mMode == MODE_BOTH) {
								mCurrentMode = MODE_PULL_UP_TO_REFRESH;
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

	protected void addRefreshableView(Context context, T refreshableView) {
		addView(refreshableView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1.0f));
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

	protected final LoadingLayout getHeaderLayout() {
		return mHeaderLayout;
	}

	protected final int getHeaderHeight() {
		return mHeaderHeight;
	}
	
	protected final int getState() {
		return mState;
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

	// ===========================================================
	// Methods
	// ===========================================================

	protected void resetHeader() {
		mState = PULL_TO_REFRESH;
		mIsBeingDragged = false;

		if (null != mHeaderLayout) {
			mHeaderLayout.reset();
		}
		if (null != mFooterLayout) {
			mFooterLayout.reset();
		}

		smoothScrollTo(0);
	}

	protected void setRefreshingInternal(boolean doScroll) {
		mState = REFRESHING;

		if (null != mHeaderLayout) {
			mHeaderLayout.refreshing();
		}
		if (null != mFooterLayout) {
			mFooterLayout.refreshing();
		}

		if (doScroll) {
			smoothScrollTo(mCurrentMode == MODE_PULL_DOWN_TO_REFRESH ? -mHeaderHeight : mHeaderHeight);
		}
	}

	protected final void setHeaderScroll(int y) {
		scrollTo(0, y);
	}

	protected final void smoothScrollTo(int y) {
		if (null != mCurrentSmoothScrollRunnable) {
			mCurrentSmoothScrollRunnable.stop();
		}

		if (getScrollY() != y) {
			mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(mHandler, getScrollY(), y);
			mHandler.post(mCurrentSmoothScrollRunnable);
		}
	}

	private void init(Context context, AttributeSet attrs) {

		setOrientation(LinearLayout.VERTICAL);

		mTouchSlop = ViewConfiguration.getTouchSlop();

		// Styleables from XML
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);
		if (a.hasValue(R.styleable.PullToRefresh_ptrMode)) {
			mMode = a.getInteger(R.styleable.PullToRefresh_ptrMode, MODE_PULL_DOWN_TO_REFRESH);
		}

		// Refreshable View
		// By passing the attrs, we can add ListView/GridView params via XML
		mRefreshableView = createRefreshableView(context, attrs);
		addRefreshableView(context, mRefreshableView);

		// Loading View Strings
		String pullLabel = context.getString(R.string.pull_to_refresh_pull_label);
		String refreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
		String releaseLabel = context.getString(R.string.pull_to_refresh_release_label);

		// Add Loading Views
		if (mMode == MODE_PULL_DOWN_TO_REFRESH || mMode == MODE_BOTH) {
			mHeaderLayout = new LoadingLayout(context, MODE_PULL_DOWN_TO_REFRESH, releaseLabel, pullLabel,
					refreshingLabel, a);
			addView(mHeaderLayout, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			measureView(mHeaderLayout);
			mHeaderHeight = mHeaderLayout.getMeasuredHeight();
		}
		if (mMode == MODE_PULL_UP_TO_REFRESH || mMode == MODE_BOTH) {
			mFooterLayout = new LoadingLayout(context, MODE_PULL_UP_TO_REFRESH, releaseLabel, pullLabel,
					refreshingLabel, a);
			addView(mFooterLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			measureView(mFooterLayout);
			mHeaderHeight = mFooterLayout.getMeasuredHeight();
		}

		// Styleables from XML
		if (a.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
			setBackgroundResource(a.getResourceId(R.styleable.PullToRefresh_ptrHeaderBackground, Color.WHITE));
		}
		if (a.hasValue(R.styleable.PullToRefresh_ptrAdapterViewBackground)) {
			mRefreshableView.setBackgroundResource(a.getResourceId(R.styleable.PullToRefresh_ptrAdapterViewBackground,
					Color.WHITE));
		}
		a.recycle();
		a = null;

		// Hide Loading Views
		switch (mMode) {
			case MODE_BOTH:
				setPadding(0, -mHeaderHeight, 0, -mHeaderHeight);
				break;
			case MODE_PULL_UP_TO_REFRESH:
				setPadding(0, 0, 0, -mHeaderHeight);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				setPadding(0, -mHeaderHeight, 0, 0);
				break;
		}

		// If we're not using MODE_BOTH, then just set mCurrentMode to current
		// mMode
		if (mMode != MODE_BOTH) {
			mCurrentMode = mMode;
		}
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
			case MODE_PULL_UP_TO_REFRESH:
				newHeight = Math.round(Math.max(mInitialMotionY - mLastMotionY, 0) / FRICTION);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				newHeight = Math.round(Math.min(mInitialMotionY - mLastMotionY, 0) / FRICTION);
				break;
		}

		setHeaderScroll(newHeight);

		if (newHeight != 0) {
			if (mState == PULL_TO_REFRESH && mHeaderHeight < Math.abs(newHeight)) {
				mState = RELEASE_TO_REFRESH;

				switch (mCurrentMode) {
					case MODE_PULL_UP_TO_REFRESH:
						mFooterLayout.releaseToRefresh();
						break;
					case MODE_PULL_DOWN_TO_REFRESH:
						mHeaderLayout.releaseToRefresh();
						break;
				}

				return true;

			} else if (mState == RELEASE_TO_REFRESH && mHeaderHeight >= Math.abs(newHeight)) {
				mState = PULL_TO_REFRESH;

				switch (mCurrentMode) {
					case MODE_PULL_UP_TO_REFRESH:
						mFooterLayout.pullToRefresh();
						break;
					case MODE_PULL_DOWN_TO_REFRESH:
						mHeaderLayout.pullToRefresh();
						break;
				}

				return true;
			}
		}

		return oldHeight != newHeight;
	}

	private boolean isReadyForPull() {
		switch (mMode) {
			case MODE_PULL_DOWN_TO_REFRESH:
				return isReadyForPullDown();
			case MODE_PULL_UP_TO_REFRESH:
				return isReadyForPullUp();
			case MODE_BOTH:
				return isReadyForPullUp() || isReadyForPullDown();
		}
		return false;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * Simple Listener to listen for any callbacks to Refresh.
	 * 
	 * @author Chris Banes
	 */
	public static interface OnRefreshListener {

		/**
		 * onRefresh will be called for both Pull Down from top, and Pull Up
		 * from Bottom
		 */
		public void onRefresh();

	}

	/**
	 * An advanced version of the Listener to listen for callbacks to Refresh.
	 * This listener is different as it allows you to differentiate between Pull
	 * Ups, and Pull Downs.
	 * 
	 * @author Chris Banes
	 */
	public static interface OnRefreshListener2 {

		/**
		 * onPullDownToRefresh will be called only when the user has Pulled Down
		 * from the top, and released.
		 */
		public void onPullDownToRefresh();

		/**
		 * onPullUpToRefresh will be called only when the user has Pulled Up
		 * from the bottom, and released.
		 */
		public void onPullUpToRefresh();

	}

	public static interface OnLastItemVisibleListener {

		public void onLastItemVisible();

	}

	@Override
	public void setLongClickable(boolean longClickable) {
		getRefreshableView().setLongClickable(longClickable);
	}
}
