package com.handmark.pulltorefresh.library;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;

public interface IPullToRefresh<T extends View> {

	/**
	 * Get the mode that this view is currently in. This is only really useful
	 * when using <code>Mode.BOTH</code>.
	 * 
	 * @return Mode that the view is currently in
	 */
	public abstract Mode getCurrentMode();

	/**
	 * Returns whether the Touch Events are filtered or not. If true is
	 * returned, then the View will only use touch events where the difference
	 * in the Y-axis is greater than the difference in the X-axis. This means
	 * that the View will not interfere when it is used in a horizontal
	 * scrolling View (such as a ViewPager).
	 * 
	 * @return boolean - true if the View is filtering Touch Events
	 */
	public abstract boolean getFilterTouchEvents();

	/**
	 * Get the mode that this view has been set to. If this returns
	 * <code>Mode.BOTH</code>, you can use <code>getCurrentMode()</code> to
	 * check which mode the view is currently in
	 * 
	 * @return Mode that the view has been set to
	 */
	public abstract Mode getMode();

	/**
	 * Get the Wrapped Refreshable View. Anything returned here has already been
	 * added to the content view.
	 * 
	 * @return The View which is currently wrapped
	 */
	public abstract T getRefreshableView();

	/**
	 * Get whether the 'Refreshing' View should be automatically shown when
	 * refreshing. Returns true by default.
	 * 
	 * @return - true if the Refreshing View will be show
	 */
	public abstract boolean getShowViewWhileRefreshing();

	/**
	 * @deprecated Use the value from <code>getCurrentMode()</code> instead
	 * @return true if the current mode is Mode.PULL_DOWN_TO_REFRESH
	 */
	public abstract boolean hasPullFromTop();

	/**
	 * Returns whether the widget has disabled scrolling on the Refreshable View
	 * while refreshing.
	 * 
	 * @return true if the widget has disabled scrolling while refreshing
	 */
	public abstract boolean isDisableScrollingWhileRefreshing();

	/**
	 * Gets whether Overscroll support is enabled. This is different to
	 * Android's standard Overscroll support (the edge-glow) which is available
	 * from GINGERBREAD onwards
	 * 
	 * @return true - if both PullToRefresh-OverScroll and Android's inbuilt
	 *         OverScroll are enabled
	 */
	public abstract boolean isPullToRefreshOverScrollEnabled();

	/**
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public abstract boolean isPullToRefreshEnabled();

	/**
	 * Returns whether the Widget is currently in the Refreshing mState
	 * 
	 * @return true if the Widget is currently refreshing
	 */
	public abstract boolean isRefreshing();

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public abstract void onRefreshComplete();

	/**
	 * By default the Widget disabled scrolling on the Refreshable View while
	 * refreshing. This method can change this behaviour.
	 * 
	 * @param disableScrollingWhileRefreshing
	 *            - true if you want to disable scrolling while refreshing
	 */
	public abstract void setDisableScrollingWhileRefreshing(boolean disableScrollingWhileRefreshing);

	/**
	 * Set the Touch Events to be filtered or not. If set to true, then the View
	 * will only use touch events where the difference in the Y-axis is greater
	 * than the difference in the X-axis. This means that the View will not
	 * interfere when it is used in a horizontal scrolling View (such as a
	 * ViewPager), but will restrict which types of finger scrolls will trigger
	 * the View.
	 * 
	 * @param filterEvents
	 *            - true if you want to filter Touch Events. Default is true.
	 */
	public abstract void setFilterTouchEvents(boolean filterEvents);

	/**
	 * Set the Last Updated Text. This displayed under the main label when
	 * Pulling
	 * 
	 * @param label
	 *            - Label to set
	 */
	public abstract void setLastUpdatedLabel(CharSequence label);

	/**
	 * Set the drawable used in the loading layout. This is the same as calling
	 * <code>setLoadingDrawable(drawable, Mode.BOTH)</code>
	 * 
	 * @param drawable
	 *            - Drawable to display
	 */
	public abstract void setLoadingDrawable(Drawable drawable);

	/**
	 * Set the drawable used in the loading layout.
	 * 
	 * @param drawable
	 *            - Drawable to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public abstract void setLoadingDrawable(Drawable drawable, Mode mode);

	/**
	 * Set the mode of Pull-to-Refresh that this view will use.
	 * 
	 * @param mode
	 *            - Mode to set the View to
	 */
	public abstract void setMode(Mode mode);

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public abstract void setOnRefreshListener(OnRefreshListener<T> listener);

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public abstract void setOnRefreshListener(OnRefreshListener2<T> listener);

	/**
	 * Sets whether Overscroll support is enabled. This is different to
	 * Android's standard Overscroll support (the edge-glow). This setting only
	 * takes effect when running on device with Android v2.3 or greater.
	 * 
	 * @param enabled
	 *            - true if you want Overscroll enabled
	 */
	public abstract void setPullToRefreshOverScrollEnabled(boolean enabled);

	/**
	 * Set Text to show when the Widget is being Pulled
	 * <code>setPullLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public abstract void setPullLabel(String pullLabel);

	/**
	 * Set Text to show when the Widget is being Pulled
	 * 
	 * @param pullLabel
	 *            - String to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public abstract void setPullLabel(String pullLabel, Mode mode);

	/**
	 * @deprecated This simple calls setMode with an appropriate mode based on
	 *             the passed value.
	 * 
	 * @param enable
	 *            Whether Pull-To-Refresh should be used
	 */
	public abstract void setPullToRefreshEnabled(boolean enable);

	public abstract void setRefreshing();

	/**
	 * Sets the Widget to be in the refresh state. The UI will be updated to
	 * show the 'Refreshing' view.
	 * 
	 * @param doScroll
	 *            - true if you want to force a scroll to the Refreshing view.
	 */
	public abstract void setRefreshing(boolean doScroll);

	/**
	 * Set Text to show when the Widget is refreshing
	 * <code>setRefreshingLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public abstract void setRefreshingLabel(String refreshingLabel);

	/**
	 * Set Text to show when the Widget is refreshing
	 * 
	 * @param refreshingLabel
	 *            - String to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public abstract void setRefreshingLabel(String refreshingLabel, Mode mode);

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released. This is the same as calling
	 * <code>setReleaseLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public abstract void setReleaseLabel(String releaseLabel);

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released
	 * 
	 * @param releaseLabel
	 *            - String to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public abstract void setReleaseLabel(String releaseLabel, Mode mode);

	/**
	 * A mutator to enable/disable whether the 'Refreshing' View should be
	 * automatically shown when refreshing.
	 * 
	 * @param showView
	 */
	public abstract void setShowViewWhileRefreshing(boolean showView);

}