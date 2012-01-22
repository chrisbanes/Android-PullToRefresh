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

	static final float FRICTION = 2.0f;

	static final int PULL_TO_REFRESH = 0x0;
	static final int RELEASE_TO_REFRESH = 0x1;
	static final int REFRESHING = 0x2;

	public static final int MODE_PULL_DOWN_TO_REFRESH = 0x1;
	public static final int MODE_PULL_UP_TO_REFRESH = 0x2;
	public static final int MODE_BOTH = 0x3;

	// ===========================================================
	// Fields
	// ===========================================================

	private int touchSlop;

	private float initialMotionY;
	private float lastMotionX;
	private float lastMotionY;
	private boolean isBeingDragged = false;

	private int state = PULL_TO_REFRESH;
	private int mode = MODE_PULL_DOWN_TO_REFRESH;
	private int currentMode;
	private boolean disableScrollingWhileRefreshing = true;

	T refreshableView;
	private boolean isPullToRefreshEnabled = true;

	private LoadingLayout headerLayout;
	private LoadingLayout footerLayout;
	private int headerHeight;

	private final Handler handler = new Handler();

	private OnRefreshListener onRefreshListener;

	private SmoothScrollRunnable currentSmoothScrollRunnable;

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

	/**
	 * By default the Widget disabled scrolling on the Refreshable View while
	 * refreshing. This method can change this behaviour.
	 * 
	 * @param disableScrollingWhileRefreshing
	 *            - true if you want to disable scrolling while refreshing
	 */
	public final void setDisableScrollingWhileRefreshing(boolean disableScrollingWhileRefreshing) {
		this.disableScrollingWhileRefreshing = disableScrollingWhileRefreshing;
	}

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public final void onRefreshComplete() {
		if (state != PULL_TO_REFRESH) {
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
		onRefreshListener = listener;
	}

	/**
	 * A mutator to enable/disable Pull-to-Refresh for the current View
	 * 
	 * @param enable
	 *            Whether Pull-To-Refresh should be used
	 */
	public final void setPullToRefreshEnabled(boolean enable) {
		this.isPullToRefreshEnabled = enable;
	}

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public final void setReleaseLabel(String releaseLabel) {
		if (null != headerLayout) {
			headerLayout.setReleaseLabel(releaseLabel);
		}
		if (null != footerLayout) {
			footerLayout.setReleaseLabel(releaseLabel);
		}
	}

	/**
	 * Set Text to show when the Widget is being Pulled
	 * 
	 * @param pullLabel
	 *            - String to display
	 */
	public final void setPullLabel(String pullLabel) {
		if (null != headerLayout) {
			headerLayout.setPullLabel(pullLabel);
		}
		if (null != footerLayout) {
			footerLayout.setPullLabel(pullLabel);
		}
	}

	/**
	 * Set Text to show when the Widget is refreshing
	 * 
	 * @param refreshingLabel
	 *            - String to display
	 */
	public final void setRefreshingLabel(String refreshingLabel) {
		if (null != headerLayout) {
			headerLayout.setRefreshingLabel(refreshingLabel);
		}
		if (null != footerLayout) {
			footerLayout.setRefreshingLabel(refreshingLabel);
		}
	}

	public final void setRefreshing() {
		this.setRefreshing(true);
	}

	/**
	 * Sets the Widget to be in the refresh state. The UI will be updated to
	 * show the 'Refreshing' view.
	 * 
	 * @param doScroll
	 *            - true if you want to force a scroll to the Refreshing view.
	 */
	public final void setRefreshing(boolean doScroll) {
		if (state == REFRESHING)
			return;

		state = REFRESHING;

		switch (currentMode) {
			case MODE_PULL_DOWN_TO_REFRESH:
				if (doScroll)
					smoothScrollTo(-headerHeight);
				headerLayout.refreshing();
				break;
			case MODE_PULL_UP_TO_REFRESH:
				if (doScroll)
					smoothScrollTo(headerHeight);
				footerLayout.refreshing();
				break;
		}
	}

	public final boolean hasPullFromTop() {
		return currentMode != MODE_PULL_UP_TO_REFRESH;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public final boolean onTouchEvent(MotionEvent event) {
		if (!isPullToRefreshEnabled) {
			return false;
		}

		if (state == REFRESHING && disableScrollingWhileRefreshing) {
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}

		switch (event.getAction()) {

			case MotionEvent.ACTION_MOVE: {
				if (isBeingDragged) {
					lastMotionY = event.getY();
					this.pullEvent();
				}
				break;
			}

			case MotionEvent.ACTION_DOWN: {
				if (isReadyForPull()) {
					lastMotionY = initialMotionY = event.getY();
				}
				break;
			}

			case MotionEvent.ACTION_CANCEL: {
				if (isBeingDragged) {
					isBeingDragged = false;
					if (state == RELEASE_TO_REFRESH && null != onRefreshListener) {
						setRefreshing(true);
						onRefreshListener.onRefresh();
					} else {
						smoothScrollTo(0);
					}
				}
				break;
			}

			case MotionEvent.ACTION_UP: {
				if (isBeingDragged) {
					isBeingDragged = false;
					if (state == RELEASE_TO_REFRESH && null != onRefreshListener) {
						setRefreshing(true);
						onRefreshListener.onRefresh();
					} else {
						smoothScrollTo(0);
					}
				}
				break;
			}
		}

		return true;
	}

	@Override
	public final boolean onInterceptTouchEvent(MotionEvent event) {

		if (!isPullToRefreshEnabled) {
			return false;
		}

		if (state == REFRESHING) {
			return disableScrollingWhileRefreshing;
		}

		final int action = event.getAction();

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			isBeingDragged = false;
			return false;
		}

		if (action != MotionEvent.ACTION_DOWN && isBeingDragged) {
			return true;
		}

		switch (action) {
			case MotionEvent.ACTION_MOVE: {
				if (isReadyForPull()) {

					final float y = event.getY();
					final float dy = y - lastMotionY;
					final float yDiff = Math.abs(dy);
					final float xDiff = Math.abs(event.getX() - lastMotionX);

					if (yDiff > touchSlop && yDiff > xDiff) {
						if ((mode == MODE_PULL_DOWN_TO_REFRESH || mode == MODE_BOTH) && dy >= 0.0001f
								&& isReadyForPullDown()) {
							lastMotionY = y;
							isBeingDragged = true;
							if (mode == MODE_BOTH) {
								currentMode = MODE_PULL_DOWN_TO_REFRESH;
							}
						} else if ((mode == MODE_PULL_UP_TO_REFRESH || mode == MODE_BOTH) && dy <= 0.0001f
								&& isReadyForPullUp()) {
							lastMotionY = y;
							isBeingDragged = true;
							if (mode == MODE_BOTH) {
								currentMode = MODE_PULL_UP_TO_REFRESH;
							}
						}
					}
				}
				break;
			}
			case MotionEvent.ACTION_DOWN: {
				if (isReadyForPull()) {
					lastMotionY = initialMotionY = event.getY();
					lastMotionX = event.getX();
					isBeingDragged = false;
				}
				break;
			}
		}

		return isBeingDragged;
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
		isBeingDragged = false;

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

		touchSlop = ViewConfiguration.getTouchSlop();

		// Styleables from XML
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);
		mode = a.getInteger(R.styleable.PullToRefresh_mode, MODE_PULL_DOWN_TO_REFRESH);

		// Refreshable View
		// By passing the attrs, we can add ListView/GridView params via XML
		refreshableView = this.createRefreshableView(context, attrs);
		this.addRefreshableView(context, refreshableView);

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

	private void pullEvent() {

		final int height;
		switch (currentMode) {
			case MODE_PULL_UP_TO_REFRESH:
				height = Math.round(Math.max(initialMotionY - lastMotionY, 0) / FRICTION);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				height = Math.round(Math.min(initialMotionY - lastMotionY, 0) / FRICTION);
				break;
		}

		setHeaderScroll(height);

		if (state == PULL_TO_REFRESH && headerHeight < Math.abs(height)) {
			state = RELEASE_TO_REFRESH;

			switch (currentMode) {
				case MODE_PULL_UP_TO_REFRESH:
					footerLayout.releaseToRefresh();
					break;
				case MODE_PULL_DOWN_TO_REFRESH:
					headerLayout.releaseToRefresh();
					break;
			}

		} else if (state == RELEASE_TO_REFRESH && headerHeight >= Math.abs(height)) {
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

	private void setHeaderScroll(int y) {
		scrollTo(0, y);
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

	@Override
	public void setLongClickable(boolean longClickable) {
		getRefreshableView().setLongClickable(longClickable);
	}
}
