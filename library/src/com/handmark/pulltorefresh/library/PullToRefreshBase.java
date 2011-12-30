package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;

public abstract class PullToRefreshBase<T extends AbsListView> extends LinearLayout implements OnTouchListener,
        OnScrollListener {

	private final class SmoothScrollRunnable implements Runnable {

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

	static final int PULL_TO_REFRESH = 0;
	static final int RELEASE_TO_REFRESH = PULL_TO_REFRESH + 1;
	static final int REFRESHING = RELEASE_TO_REFRESH + 1;
	static final int EVENT_COUNT = 3;

	public static final int MODE_PULL_DOWN_TO_REFRESH = 0x1;
	public static final int MODE_PULL_UP_TO_REFRESH = 0x2;
	public static final int MODE_BOTH = 0x3;

	// ===========================================================
	// Fields
	// ===========================================================

	private int state = PULL_TO_REFRESH;
	private int mode = MODE_PULL_DOWN_TO_REFRESH;
	private int currentMode;

	private T adapterView;
	private boolean isPullToRefreshEnabled = true;

	private LoadingLayout headerLayout;
	private LoadingLayout footerLayout;
	private int headerHeight;

	private final Handler handler = new Handler();

	private OnTouchListener onTouchListener;
	private OnRefreshListener onRefreshListener;
	private OnScrollListener onScrollListener;

	private OnLastItemVisibleListener onLastItemVisibleListener;
	private int lastSavedFirstVisibleItem = -1;

	private SmoothScrollRunnable currentSmoothScrollRunnable;

	private float startY = -1;
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
	 * Get the Wrapped AdapterView. Anything returned here has already been
	 * added to the content view.
	 * 
	 * @return The AdapterView which is currently wrapped
	 */
	public final T getAdapterView() {
		return adapterView;
	}

	/**
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public final boolean isPullToRefreshEnabled() {
		return isPullToRefreshEnabled;
	}

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public final void onRefreshComplete() {
		resetHeader();
	}

	public final void setOnLastItemVisibleListener(OnLastItemVisibleListener listener) {
		onLastItemVisibleListener = listener;
	}

	public final void setOnRefreshListener(OnRefreshListener listener) {
		onRefreshListener = listener;
	}

	/**
	 * A mutator to enable/disable Pull-to-Refresh for the current AdapterView
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

	public final void setOnScrollListener(OnScrollListener listener) {
		onScrollListener = listener;
	}

	@Override
	public final void setOnTouchListener(OnTouchListener listener) {
		onTouchListener = listener;
	}

	public final void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
	        final int totalItemCount) {

		if (null != onLastItemVisibleListener) {
			// detect if last item is visible
			if (visibleItemCount > 0 && visibleItemCount < totalItemCount
			        && (firstVisibleItem + visibleItemCount == totalItemCount)) {
				// only process first event
				if (firstVisibleItem != lastSavedFirstVisibleItem) {
					lastSavedFirstVisibleItem = firstVisibleItem;
					onLastItemVisibleListener.onLastItemVisible();
				}
			}
		}

		if (null != onScrollListener) {
			onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
		}
	}

	public final void onScrollStateChanged(final AbsListView view, final int scrollState) {
		if (null != onScrollListener) {
			onScrollListener.onScrollStateChanged(view, scrollState);
		}
	}

	@Override
	public final boolean onTouch(View view, MotionEvent ev) {
		if (isPullToRefreshEnabled) {
			// Returning true here stops the ListView being scrollable while we
			// refresh
			if (state == REFRESHING) {
				return true;
			} else {
				return onAdapterViewTouch(view, ev);
			}
		}
		return false;
	}

	/**
	 * This is implemented by derived classes to return the created AdapterView.
	 * If you need to use a custom AdapterView (such as a custom ListView),
	 * override this method and return an instance of your custom class.
	 * 
	 * Be sure to set the ID of the view in this method, especially if you're
	 * using a ListActivity or ListFragment.
	 * 
	 * @param context
	 * @param attrs
	 *            AttributeSet from wrapped class. Means that anything you
	 *            include in the XML layout declaration will be routed to the
	 *            AdapterView
	 * @return New instance of the AdapterView
	 */
	protected abstract T createAdapterView(Context context, AttributeSet attrs);

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

		// AdapterView
		// By passing the attrs, we can add ListView/GridView params via XML
		adapterView = this.createAdapterView(context, attrs);
		adapterView.setOnTouchListener(this);
		adapterView.setOnScrollListener(this);
		addView(adapterView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1.0f));

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
			adapterView.setBackgroundResource(a.getResourceId(R.styleable.PullToRefresh_adapterViewBackground,
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

	private boolean onAdapterViewTouch(View view, MotionEvent event) {

		switch (event.getAction()) {
			case MotionEvent.ACTION_MOVE:
				updateEventStates(event);

				if (isPullingToRefresh() && startY == -1) {
					startY = event.getY();

					// Need to set current Mode if we're using both
					if (mode == MODE_BOTH) {
						if (isUserDraggingDownwards()) {
							currentMode = MODE_PULL_DOWN_TO_REFRESH;
						} else if (isUserDraggingUpwards()) {
							currentMode = MODE_PULL_UP_TO_REFRESH;
						}
					}
					return false;
				}

				if (startY != -1 && !adapterView.isPressed()) {
					pullEvent(event, startY);
					return true;
				}
				break;
			case MotionEvent.ACTION_UP:
				initializeYsHistory();
				startY = -1;

				if (state == RELEASE_TO_REFRESH) {
					setRefreshing();
					if (onRefreshListener != null) {
						onRefreshListener.onRefresh();
					}
				} else {
					smoothScrollTo(0);
				}
				break;
		}

		if (null != onTouchListener) {
			return onTouchListener.onTouch(view, event);
		}
		return false;
	}

	private void pullEvent(MotionEvent event, float firstY) {
		float averageY = average(lastYs);

		final int height;
		switch (currentMode) {
			case MODE_PULL_UP_TO_REFRESH:
				height = (int) Math.max(firstY - averageY, 0);
				setHeaderScroll(height);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				height = (int) Math.max(averageY - firstY, 0);
				setHeaderScroll(-height);
				break;
		}

		if (state == PULL_TO_REFRESH && headerHeight < height) {
			state = RELEASE_TO_REFRESH;
			if (null != headerLayout) {
				headerLayout.releaseToRefresh();
			}
			if (null != footerLayout) {
				footerLayout.releaseToRefresh();
			}
		} else if (state == RELEASE_TO_REFRESH && headerHeight >= height) {
			state = PULL_TO_REFRESH;
			if (null != headerLayout) {
				headerLayout.pullToRefresh();
			}
			if (null != footerLayout) {
				footerLayout.pullToRefresh();
			}
		}
	}

	private void setHeaderScroll(int y) {
		scrollTo(0, y);
	}

	private void setRefreshing() {
		state = REFRESHING;
		if (null != headerLayout) {
			headerLayout.refreshing();
		}
		if (null != footerLayout) {
			footerLayout.refreshing();
		}

		switch (currentMode) {
			case MODE_PULL_DOWN_TO_REFRESH:
				smoothScrollTo(-headerHeight);
				break;
			case MODE_PULL_UP_TO_REFRESH:
				smoothScrollTo(headerHeight);
				break;
		}
	}

	private float average(float[] ysArray) {
		float avg = 0;
		for (int i = 0; i < EVENT_COUNT; i++) {
			avg += ysArray[i];
		}
		return avg / EVENT_COUNT;
	}

	private void initializeYsHistory() {
		for (int i = 0; i < EVENT_COUNT; i++) {
			lastYs[i] = 0;
		}
	}

	private void updateEventStates(MotionEvent event) {
		for (int i = 0; i < EVENT_COUNT - 1; i++) {
			lastYs[i] = lastYs[i + 1];
		}

		float y = event.getY();
		lastYs[EVENT_COUNT - 1] = y;
	}

	private boolean isPullingToRefresh() {
		if (isPullToRefreshEnabled && state != REFRESHING) {
			switch (mode) {
				case MODE_PULL_DOWN_TO_REFRESH:
					return isFirstItemVisible() && isUserDraggingDownwards();
				case MODE_PULL_UP_TO_REFRESH:
					return isLastItemVisible() && isUserDraggingUpwards();
				case MODE_BOTH:
					return (isFirstItemVisible() && isUserDraggingDownwards())
					        || (isLastItemVisible() && isUserDraggingUpwards());
			}
		}
		return false;
	}

	private boolean isFirstItemVisible() {
		if (this.adapterView.getCount() == 0) {
			return true;
		} else if (adapterView.getFirstVisiblePosition() == 0) {
			return adapterView.getChildAt(0).getTop() >= adapterView.getTop();
		} else {
			return false;
		}
	}

	private boolean isLastItemVisible() {
		final int count = this.adapterView.getCount();
		if (count == 0) {
			return true;
		} else if (adapterView.getLastVisiblePosition() == count - 1) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isUserDraggingDownwards() {
		return this.isUserDraggingDownwards(0, EVENT_COUNT - 1);
	}

	private boolean isUserDraggingDownwards(int from, int to) {
		return lastYs[from] != 0 && lastYs[to] != 0 && Math.abs(lastYs[from] - lastYs[to]) > 10
		        && lastYs[from] < lastYs[to];
	}

	private boolean isUserDraggingUpwards() {
		return this.isUserDraggingUpwards(0, EVENT_COUNT - 1);
	}

	private boolean isUserDraggingUpwards(int from, int to) {
		return lastYs[from] != 0 && lastYs[to] != 0 && Math.abs(lastYs[to] - lastYs[from]) > 10
		        && lastYs[to] < lastYs[from];
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
