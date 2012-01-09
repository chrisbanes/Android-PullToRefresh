package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.handmark.pulltorefresh.library.internal.LoadingLayout;

public abstract class PullToRefreshBase<T extends View> extends LinearLayout implements OnScrollListener {

	final class SmoothScrollRunnable implements Runnable {

		static final int ANIMATION_DURATION_MS = 190;
		static final int ANIMATION_FPS = 1000 / 60;

		private final Interpolator interpolator;
		private final int scrollToY;
		private final int scrollFromY;
		private final Handler handler;

		private boolean continueRunning = true;
		private long startTime = -1;
		private int currentY = -1;

		public SmoothScrollRunnable(Handler handler, int fromY, int toY) {
			this.handler = handler;
			this.scrollFromY = fromY;
			this.scrollToY = toY;
			this.interpolator = new AccelerateDecelerateInterpolator();
		}

		@Override
		public void run() {

			/**
			 * Only set startTime if this is the first time we're starting, else
			 * actually calculate the Y delta
			 */
			if (startTime == -1) {
				startTime = System.currentTimeMillis();
			} else {

				/**
				 * We do do all calculations in long to reduce software float
				 * calculations. We use 1000 as it gives us good accuracy and
				 * small rounding errors
				 */
				long normalizedTime = (1000 * (System.currentTimeMillis() - startTime)) / ANIMATION_DURATION_MS;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

				final int deltaY = Math.round((scrollFromY - scrollToY)
				        * interpolator.getInterpolation(normalizedTime / 1000f));
				this.currentY = scrollFromY - deltaY;
				setHeaderScroll(currentY);
			}

			// If we're not at the target Y, keep going...
			if (continueRunning && scrollToY != currentY) {
				handler.postDelayed(this, ANIMATION_FPS);
			}
		}

		public void stop() {
			this.continueRunning = false;
			this.handler.removeCallbacks(this);
		}
	};

	// ===========================================================
	// Constants
	// ===========================================================

	static final int PULL_TO_REFRESH = 0x0;
	static final int RELEASE_TO_REFRESH = 0x1;
	static final int REFRESHING = 0x2;

	static final int EVENT_COUNT = 3;
	static final int LAST_EVENT_INDEX = EVENT_COUNT - 1;

	static final float FRICTION_LEVEL = 1.5f;

	public static final int MODE_PULL_DOWN_TO_REFRESH = 0x1;
	public static final int MODE_PULL_UP_TO_REFRESH = 0x2;
	public static final int MODE_BOTH = 0x3;

	// ===========================================================
	// Fields
	// ===========================================================

	private int state = PULL_TO_REFRESH;
	private int mode = MODE_PULL_DOWN_TO_REFRESH;
	private int currentMode;
	private boolean disableScrollingWhileRefreshing = true;

	FrameLayout refreshableViewHolder;
	T refreshableView;
	private boolean isPullToRefreshEnabled = true;

	private LoadingLayout headerLayout;
	private LoadingLayout footerLayout;
	private int headerHeight;

	private final Handler handler = new Handler();

	private OnRefreshListener onRefreshListener;

	private SmoothScrollRunnable currentSmoothScrollRunnable;

	private int startY = -1;
	private final float[] lastYs = new float[EVENT_COUNT];

	// ===========================================================
	// Constructors
	// ===========================================================

	public PullToRefreshBase(Context context) {
		this(context, null);
	}

	public PullToRefreshBase(Context context, int mode) {
		this(context);
		this.mode = mode;
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
		return refreshableView;
	}

	/**
	 * Get the Wrapped Refreshable View. Anything returned here has already been
	 * added to the content view.
	 * 
	 * @return The View which is currently wrapped
	 */
	public final T getRefreshableView() {
		return refreshableView;
	}

	/**
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public final boolean isPullToRefreshEnabled() {
		return isPullToRefreshEnabled;
	}

	public void setDisableScrollingWhileRefreshing(boolean disableScrollingWhileRefreshing) {
		this.disableScrollingWhileRefreshing = disableScrollingWhileRefreshing;
	}

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public final void onRefreshComplete() {
		resetHeader();
	}

	public final void setOnRefreshListener(OnRefreshListener listener) {
		onRefreshListener = listener;
	}

	/**
	 * A mutator to enable/disable Pull-to-Refresh for the current View
	 * 
	 * @param enable
	 *            Whether Pull-To-Refresh should be used
	 */
	public final void setPullToRefreshEnabled(boolean enabled) {
		this.isPullToRefreshEnabled = enabled;
	}

	public final void setReleaseLabel(String releaseLabel) {
		if (null != headerLayout) {
			headerLayout.setReleaseLabel(releaseLabel);
		}
		if (null != footerLayout) {
			footerLayout.setReleaseLabel(releaseLabel);
		}
	}

	public final void setPullLabel(String pullLabel) {
		if (null != headerLayout) {
			headerLayout.setPullLabel(pullLabel);
		}
		if (null != footerLayout) {
			footerLayout.setPullLabel(pullLabel);
		}
	}

	public final void setRefreshingLabel(String refreshingLabel) {
		if (null != headerLayout) {
			headerLayout.setRefreshingLabel(refreshingLabel);
		}
		if (null != footerLayout) {
			footerLayout.setRefreshingLabel(refreshingLabel);
		}
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public final boolean onTouchEvent(MotionEvent event) {

		if (isPullToRefreshEnabled) {

			if (state == REFRESHING && disableScrollingWhileRefreshing) {
				return true;
			}

			switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					if (startY != -1) {
						updateEventStates(event);
						pullEvent(event, startY);
						return true;
					} else if (checkEventForInitialPull(event)) {
						return true;
					}
					break;
				case MotionEvent.ACTION_UP:
					if (startY != -1) {
						initializeYsHistory();
						startY = -1;

						if (state == RELEASE_TO_REFRESH && null != onRefreshListener) {
							setRefreshing();
							onRefreshListener.onRefresh();
						} else {
							smoothScrollTo(0);
						}
						return true;
					}
					break;
				case MotionEvent.ACTION_DOWN:
					// We need to return true here so that we can later catch
					// ACTION_MOVE
					return true;
			}
		}

		return super.onTouchEvent(event);
	}

	@Override
	public final boolean onInterceptTouchEvent(MotionEvent event) {

		if (isPullToRefreshEnabled) {

			if (state == REFRESHING && disableScrollingWhileRefreshing) {
				return true;
			}

			switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					if (checkEventForInitialPull(event)) {
						return true;
					}
					break;
			}
		}

		return false;
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
	 * @param attrs
	 *            AttributeSet from wrapped class. Means that anything you
	 *            include in the XML layout declaration will be routed to the
	 *            created View
	 * @return New instance of the Refreshable View
	 */
	protected abstract T createRefreshableView(Context context, AttributeSet attrs);

	/**
	 * Implemented by derived class to return whether the View is in a state
	 * where the user can Pull to Refresh by scrolling down.
	 * 
	 * @return true if the View is currently the correct state (for example, top
	 *         of a ListView)
	 */
	protected abstract boolean isReadyForPullDown();

	/**
	 * Implemented by derived class to return whether the View is in a state
	 * where the user can Pull to Refresh by scrolling up.
	 * 
	 * @return true if the View is currently in the correct state (for example,
	 *         bottom of a ListView)
	 */
	protected abstract boolean isReadyForPullUp();

	// ===========================================================
	// Methods
	// ===========================================================

	protected final void resetHeader() {
		state = PULL_TO_REFRESH;
		initializeYsHistory();
		startY = -1;

		if (null != headerLayout) {
			headerLayout.reset();
		}
		if (null != footerLayout) {
			footerLayout.reset();
		}

		smoothScrollTo(0);
	}

	private void init(Context context, AttributeSet attrs) {

		setOrientation(LinearLayout.VERTICAL);

		// Styleables from XML
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);
		mode = a.getInteger(R.styleable.PullToRefresh_mode, MODE_PULL_DOWN_TO_REFRESH);

		// Refreshable View
		// By passing the attrs, we can add ListView/GridView params via XML
		refreshableView = this.createRefreshableView(context, attrs);

		refreshableViewHolder = new FrameLayout(context);
		refreshableViewHolder.addView(refreshableView, ViewGroup.LayoutParams.FILL_PARENT,
		        ViewGroup.LayoutParams.FILL_PARENT);
		addView(refreshableViewHolder, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1.0f));

		// Loading View Strings
		String pullLabel = context.getString(R.string.pull_to_refresh_pull_label);
		String refreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
		String releaseLabel = context.getString(R.string.pull_to_refresh_release_label);

		// Add Loading Views
		if (mode == MODE_PULL_DOWN_TO_REFRESH || mode == MODE_BOTH) {
			headerLayout = new LoadingLayout(context, MODE_PULL_DOWN_TO_REFRESH, releaseLabel, pullLabel,
			        refreshingLabel);
			addView(headerLayout, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
			        ViewGroup.LayoutParams.WRAP_CONTENT));
			measureView(headerLayout);
			headerHeight = headerLayout.getMeasuredHeight();
		}
		if (mode == MODE_PULL_UP_TO_REFRESH || mode == MODE_BOTH) {
			footerLayout = new LoadingLayout(context, MODE_PULL_UP_TO_REFRESH, releaseLabel, pullLabel, refreshingLabel);
			addView(footerLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
			        ViewGroup.LayoutParams.WRAP_CONTENT));
			measureView(footerLayout);
			headerHeight = footerLayout.getMeasuredHeight();
		}

		// Styleables from XML
		if (a.hasValue(R.styleable.PullToRefresh_headerTextColor)) {
			final int color = a.getColor(R.styleable.PullToRefresh_headerTextColor, Color.BLACK);
			if (null != headerLayout) {
				headerLayout.setTextColor(color);
			}
			if (null != footerLayout) {
				footerLayout.setTextColor(color);
			}
		}
		if (a.hasValue(R.styleable.PullToRefresh_headerBackground)) {
			this.setBackgroundResource(a.getResourceId(R.styleable.PullToRefresh_headerBackground, Color.WHITE));
		}
		if (a.hasValue(R.styleable.PullToRefresh_adapterViewBackground)) {
			refreshableView.setBackgroundResource(a.getResourceId(R.styleable.PullToRefresh_adapterViewBackground,
			        Color.WHITE));
		}
		a.recycle();

		// Hide Loading Views
		switch (mode) {
			case MODE_BOTH:
				setPadding(getPaddingLeft(), -headerHeight, getPaddingRight(), -headerHeight);
				break;
			case MODE_PULL_UP_TO_REFRESH:
				setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), -headerHeight);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				setPadding(getPaddingLeft(), -headerHeight, getPaddingRight(), getPaddingBottom());
				break;
		}

		// If we're not using MODE_BOTH, then just set currentMode to current
		// mode
		if (mode != MODE_BOTH) {
			currentMode = mode;
		}
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	private void pullEvent(MotionEvent event, float firstY) {
		final float scrollY = lastYs[LAST_EVENT_INDEX];

		final float height;
		switch (currentMode) {
			case MODE_PULL_UP_TO_REFRESH:
				height = Math.max(firstY - scrollY, 0);
				setHeaderScroll(Math.round(height / FRICTION_LEVEL));
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				height = Math.max(scrollY - firstY, 0);
				setHeaderScroll(Math.round(-height / FRICTION_LEVEL));
				break;
		}

		if (state == PULL_TO_REFRESH && headerHeight < height) {
			state = RELEASE_TO_REFRESH;

			switch (currentMode) {
				case MODE_PULL_UP_TO_REFRESH:
					footerLayout.releaseToRefresh();
					break;
				case MODE_PULL_DOWN_TO_REFRESH:
					headerLayout.releaseToRefresh();
					break;
			}

		} else if (state == RELEASE_TO_REFRESH && headerHeight >= height) {
			state = PULL_TO_REFRESH;

			switch (currentMode) {
				case MODE_PULL_UP_TO_REFRESH:
					footerLayout.pullToRefresh();
					break;
				case MODE_PULL_DOWN_TO_REFRESH:
					headerLayout.pullToRefresh();
					break;
			}
		}
	}

	private boolean checkEventForInitialPull(MotionEvent event) {
		if (startY == -1 && isReadyForPull()) {
			updateEventStates(event);

			// Need to set current Mode if we're using both
			if (mode == MODE_BOTH) {
				if (isUserDraggingDownwards()) {
					currentMode = MODE_PULL_DOWN_TO_REFRESH;
				} else if (isUserDraggingUpwards()) {
					currentMode = MODE_PULL_UP_TO_REFRESH;
				}
			}

			if (isPullingToRefresh()) {
				startY = (int) event.getY();
				return true;
			}
		}
		return false;
	}

	private void setHeaderScroll(int y) {
		scrollTo(0, y);
	}

	private void setRefreshing() {
		state = REFRESHING;

		switch (currentMode) {
			case MODE_PULL_DOWN_TO_REFRESH:
				smoothScrollTo(-headerHeight);
				headerLayout.refreshing();
				break;
			case MODE_PULL_UP_TO_REFRESH:
				smoothScrollTo(headerHeight);
				footerLayout.refreshing();
				break;
		}
	}

	private void initializeYsHistory() {
		for (int i = 0; i < EVENT_COUNT; i++) {
			lastYs[i] = 0.0f;
		}
	}

	private void updateEventStates(MotionEvent event) {
		for (int i = 0, z = event.getHistorySize(); i < z; i++) {
			this.updateEventStates(event.getHistoricalY(i));
		}

		this.updateEventStates(event.getY());
	}

	private void updateEventStates(float y) {
		System.arraycopy(lastYs, 1, lastYs, 0, EVENT_COUNT - 1);
		lastYs[LAST_EVENT_INDEX] = y;
	}

	private boolean isReadyForPull() {
		switch (mode) {
			case MODE_PULL_DOWN_TO_REFRESH:
				return isReadyForPullDown();
			case MODE_PULL_UP_TO_REFRESH:
				return isReadyForPullUp();
			case MODE_BOTH:
				return isReadyForPullUp() || isReadyForPullDown();
		}
		return false;
	}

	private boolean isPullingToRefresh() {
		if (state != REFRESHING) {
			switch (currentMode) {
				case MODE_PULL_DOWN_TO_REFRESH:
					return isReadyForPullDown() && isUserDraggingDownwards();
				case MODE_PULL_UP_TO_REFRESH:
					return isReadyForPullUp() && isUserDraggingUpwards();
			}
		}
		return false;
	}

	private boolean isUserDraggingDownwards() {
		return lastYs[0] != 0 && lastYs[LAST_EVENT_INDEX] != 0 && lastYs[0] < lastYs[LAST_EVENT_INDEX];
	}

	private boolean isUserDraggingUpwards() {
		return lastYs[0] != 0 && lastYs[LAST_EVENT_INDEX] != 0 && lastYs[0] > lastYs[LAST_EVENT_INDEX];
	}

	private void smoothScrollTo(int y) {
		if (null != currentSmoothScrollRunnable) {
			currentSmoothScrollRunnable.stop();
		}

		this.currentSmoothScrollRunnable = new SmoothScrollRunnable(handler, getScrollY(), y);
		handler.post(currentSmoothScrollRunnable);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public static interface OnRefreshListener {

		public void onRefresh();

	}

	public static interface OnLastItemVisibleListener {

		public void onLastItemVisible();

	}

}
