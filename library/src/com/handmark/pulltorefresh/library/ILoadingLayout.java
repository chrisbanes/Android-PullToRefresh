package com.handmark.pulltorefresh.library;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

public interface ILoadingLayout {

	/**
	 * Set the Last Updated Text. This displayed under the main label when
	 * Pulling
	 *
	 * @param label - Label to set
	 */
	public void setLastUpdatedLabel(CharSequence label);

	/**
	 * Set the drawable used in the loading layout. This is the same as calling
	 * <code>setLoadingDrawable(drawable, Mode.BOTH)</code>
	 *
	 * @param drawable - Drawable to display
	 */
	public void setLoadingDrawable(Drawable drawable);

	/**
	 * Set Text to show when the Widget is being Pulled
	 * <code>setPullLabel(releaseLabel, Mode.BOTH)</code>
	 *
	 * @param pullLabel - CharSequence to display
	 */
	public void setPullLabel(CharSequence pullLabel);

	/**
	 * Set Text to show when the Widget is refreshing
	 * <code>setRefreshingLabel(releaseLabel, Mode.BOTH)</code>
	 *
	 * @param refreshingLabel - CharSequence to display
	 */
	public void setRefreshingLabel(CharSequence refreshingLabel);

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released. This is the same as calling
	 * <code>setReleaseLabel(releaseLabel, Mode.BOTH)</code>
	 *
	 * @param releaseLabel - CharSequence to display
	 */
	public void setReleaseLabel(CharSequence releaseLabel);

	/**
	 * Sets the typeface and style in which the text should be
	 * displayed. Please see
	 * {@link android.widget.TextView#setTypeface(Typeface)
	 * TextView#setTypeface(Typeface)}.
	 */
	public void setTextTypeface(Typeface tf);

	/**
	 * Sets the background for the LoadingLayout. Please see
	 * {@link android.view.View#setBackground(android.graphics.drawable.Drawable)}
	 *
	 * @param drawable - Drawable to use as background
	 */
	public void setBackground(Drawable drawable);

}
