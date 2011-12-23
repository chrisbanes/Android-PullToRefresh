package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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

	// ===========================================================
	// Fields
	// ===========================================================

	private int state = PULL_TO_REFRESH;
	private int mode = MODE_PULL_DOWN_TO_REFRESH;

	private T adapterView;
	private boolean isPullToRefreshEnabled = true;

	private ProgressBar headerProgress;
	private TextView headerText;
	private ImageView headerImage;
	private Animation rotateToPointUpAnimation, rotateToPointDownAnimation;
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

	private String releaseLabel;
	private String pullLabel;
	private String refreshingLabel;

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
		this.releaseLabel = releaseLabel;
	}

	public final void setPullLabel(String pullLabel) {
		this.pullLabel = pullLabel;
	}

	public final void setRefreshingLabel(String refreshingLabel) {
		this.refreshingLabel = refreshingLabel;
	}

	public final void setHeaderProgress(ProgressBar headerProgress) {
		this.headerProgress = headerProgress;
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
		headerImage.setVisibility(View.VISIBLE);
		headerProgress.setVisibility(View.GONE);
		headerText.setText(R.string.pull_to_refresh_pull_label);

		smoothScrollTo(0);
	}

	private void init(Context context, AttributeSet attrs) {

		// Styleables from XML
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);
		mode = a.getInteger(R.styleable.PullToRefresh_mode, MODE_PULL_DOWN_TO_REFRESH);

		setOrientation(LinearLayout.VERTICAL);

		// AdapterView
		// By passing the attrs, we can add ListView/GridView params via XML
		adapterView = this.createAdapterView(context, attrs);
		adapterView.setOnTouchListener(this);
		adapterView.setOnScrollListener(this);
		addView(adapterView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1.0f));

		// Header
		ViewGroup header = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header, this,
		        false);
		headerText = (TextView) header.findViewById(R.id.pull_to_refresh_text);
		pullLabel = context.getString(R.string.pull_to_refresh_pull_label);
		refreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
		releaseLabel = context.getString(R.string.pull_to_refresh_release_label);
		headerImage = (ImageView) header.findViewById(R.id.pull_to_refresh_image);
		headerProgress = (ProgressBar) header.findViewById(R.id.pull_to_refresh_progress);

		int index;
		if (mode == MODE_PULL_UP_TO_REFRESH) {
			headerImage.setImageResource(R.drawable.pulltorefresh_up_arrow);
			index = 1;
		} else {
			headerImage.setImageResource(R.drawable.pulltorefresh_down_arrow);
			index = 0;
		}

		addView(header, index, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
		        ViewGroup.LayoutParams.WRAP_CONTENT));
		measureView(header);
		headerHeight = header.getMeasuredHeight();

		// Styleables from XML
		if (a.hasValue(R.styleable.PullToRefresh_headerTextColor)) {
			headerText.setTextColor(a.getColor(R.styleable.PullToRefresh_headerTextColor, Color.BLACK));
		}
		if (a.hasValue(R.styleable.PullToRefresh_headerBackground)) {
			this.setBackgroundResource(a.getResourceId(R.styleable.PullToRefresh_headerBackground, Color.WHITE));
		}
		if (a.hasValue(R.styleable.PullToRefresh_adapterViewBackground)) {
			adapterView.setBackgroundResource(a.getResourceId(R.styleable.PullToRefresh_adapterViewBackground,
			        Color.WHITE));
		}
		a.recycle();

		// Animations
		final Interpolator interpolator = new LinearInterpolator();
		rotateToPointUpAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f,
		        Animation.RELATIVE_TO_SELF, 0.5f);
		rotateToPointUpAnimation.setInterpolator(interpolator);
		rotateToPointUpAnimation.setDuration(125);
		rotateToPointUpAnimation.setFillAfter(true);

		rotateToPointDownAnimation = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f,
		        Animation.RELATIVE_TO_SELF, 0.5f);
		rotateToPointDownAnimation.setInterpolator(interpolator);
		rotateToPointDownAnimation.setDuration(125);
		rotateToPointDownAnimation.setFillAfter(true);

		// Hide Header View
		switch (mode) {
			case MODE_PULL_UP_TO_REFRESH:
				setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), -headerHeight);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				setPadding(getPaddingLeft(), -headerHeight, getPaddingRight(), getPaddingBottom());
				break;
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
					if (startY == -1) {
						startY = event.getY();
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

		int height;
		switch (mode) {
			case MODE_PULL_UP_TO_REFRESH:
				height = (int) Math.max(firstY - averageY, 0);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				height = (int) Math.max(averageY - firstY, 0);
				break;
		}

		setHeaderScroll(height);

		if (state == PULL_TO_REFRESH && headerHeight < height) {
			state = RELEASE_TO_REFRESH;
			headerText.setText(releaseLabel);
			headerImage.clearAnimation();

			switch (mode) {
				case MODE_PULL_UP_TO_REFRESH:
					headerImage.startAnimation(rotateToPointDownAnimation);
					break;
				case MODE_PULL_DOWN_TO_REFRESH:
				default:
					headerImage.startAnimation(rotateToPointUpAnimation);
					break;
			}

		} else if (state == RELEASE_TO_REFRESH && headerHeight >= height) {
			state = PULL_TO_REFRESH;
			headerText.setText(pullLabel);
			headerImage.clearAnimation();

			switch (mode) {
				case MODE_PULL_UP_TO_REFRESH:
					headerImage.startAnimation(rotateToPointUpAnimation);
					break;
				case MODE_PULL_DOWN_TO_REFRESH:
				default:
					headerImage.startAnimation(rotateToPointDownAnimation);
					break;
			}
		}
	}

	private void setHeaderScroll(int y) {
		switch (mode) {
			case MODE_PULL_UP_TO_REFRESH:
				scrollTo(0, y);
				break;
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				scrollTo(0, -y);
				break;
		}
	}

	private int getHeaderScroll() {
		switch (mode) {
			case MODE_PULL_UP_TO_REFRESH:
				return getScrollY();
			case MODE_PULL_DOWN_TO_REFRESH:
			default:
				return -getScrollY();
		}
	}

	private void setRefreshing() {
		state = REFRESHING;
		headerText.setText(refreshingLabel);
		headerImage.clearAnimation();
		headerImage.setVisibility(View.INVISIBLE);
		headerProgress.setVisibility(View.VISIBLE);
		smoothScrollTo(headerHeight);
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
					return isUserDraggingDownwards() && isFirstItemVisible();
				case MODE_PULL_UP_TO_REFRESH:
					return isUserDraggingUpwards() && isLastItemVisible();
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

		this.currentSmoothScrollRunnable = new SmoothScrollRunnable(handler, getHeaderScroll(), y);
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
