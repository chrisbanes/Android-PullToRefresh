package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;

public abstract class PullToRefreshAdapterViewBase<T extends AbsListView> extends PullToRefreshBase<T> implements
		OnScrollListener {

	private int lastSavedFirstVisibleItem = -1;
	private OnScrollListener onScrollListener;
	private OnLastItemVisibleListener onLastItemVisibleListener;
	private View emptyView;
	private FrameLayout refreshableViewHolder;

	public PullToRefreshAdapterViewBase(Context context) {
		super(context);
		refreshableView.setOnScrollListener(this);
	}

	public PullToRefreshAdapterViewBase(Context context, int mode) {
		super(context, mode);
		refreshableView.setOnScrollListener(this);
	}

	public PullToRefreshAdapterViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		refreshableView.setOnScrollListener(this);
	}

	abstract public ContextMenuInfo getContextMenuInfo();

	public final void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
			final int totalItemCount) {

		if (null != onLastItemVisibleListener) {
			// detect if last item is visible
			if (visibleItemCount > 0 && (firstVisibleItem + visibleItemCount == totalItemCount)) {
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

	/**
	 * Sets the Empty View to be used by the Adapter View.
	 * 
	 * We need it handle it ourselves so that we can Pull-to-Refresh when the
	 * Empty View is shown.
	 * 
	 * Please note, you do <strong>not</strong> usually need to call this method
	 * yourself. Calling setEmptyView on the AdapterView will automatically call
	 * this method and set everything up. This includes when the Android
	 * Framework automatically sets the Empty View based on it's ID.
	 * 
	 * @param newEmptyView
	 *            - Empty View to be used
	 */
	public final void setEmptyView(View newEmptyView) {
		// If we already have an Empty View, remove it
		if (null != emptyView) {
			refreshableViewHolder.removeView(emptyView);
		}

		if (null != newEmptyView) {
			ViewParent newEmptyViewParent = newEmptyView.getParent();
			if (null != newEmptyViewParent && newEmptyViewParent instanceof ViewGroup) {
				((ViewGroup) newEmptyViewParent).removeView(newEmptyView);
			}

			this.refreshableViewHolder.addView(newEmptyView, ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.FILL_PARENT);
		}

		if (refreshableView instanceof EmptyViewMethodAccessor) {
			((EmptyViewMethodAccessor) refreshableView).setEmptyViewInternal(newEmptyView);
		} else {
			this.refreshableView.setEmptyView(newEmptyView);
		}
	}

	public final void setOnLastItemVisibleListener(OnLastItemVisibleListener listener) {
		onLastItemVisibleListener = listener;
	}

	public final void setOnScrollListener(OnScrollListener listener) {
		onScrollListener = listener;
	}

	protected void addRefreshableView(Context context, T refreshableView) {
		refreshableViewHolder = new FrameLayout(context);
		refreshableViewHolder.addView(refreshableView, ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT);
		addView(refreshableViewHolder, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1.0f));
	};

	protected boolean isReadyForPullDown() {
		return isFirstItemVisible();
	}

	protected boolean isReadyForPullUp() {
		return isLastItemVisible();
	}

	private boolean isFirstItemVisible() {
		if (this.refreshableView.getCount() == 0) {
			return true;
		} else if (refreshableView.getFirstVisiblePosition() == 0) {
			
			final View firstVisibleChild = refreshableView.getChildAt(0);
			
			if (firstVisibleChild != null) {
				return firstVisibleChild.getTop() >= refreshableView.getTop();
			}
		}
		
		return false;
	}

	private boolean isLastItemVisible() {
		final int count = this.refreshableView.getCount();
		final int lastVisiblePosition = refreshableView.getLastVisiblePosition();

		if (count == 0) {
			return true;
		} else if (lastVisiblePosition == count - 1) {

			final int childIndex = lastVisiblePosition - refreshableView.getFirstVisiblePosition();
			final View lastVisibleChild = refreshableView.getChildAt(childIndex);

			if (lastVisibleChild != null) {
				return lastVisibleChild.getBottom() <= refreshableView.getBottom();
			}
		}
		
		return false;
	}
}
