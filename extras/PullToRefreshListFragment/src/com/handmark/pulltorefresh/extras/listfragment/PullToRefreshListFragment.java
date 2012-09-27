package com.handmark.pulltorefresh.extras.listfragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class PullToRefreshListFragment extends ListFragment {

	private PullToRefreshListView mPullToRefreshListView;

	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View layout = super.onCreateView(inflater, container, savedInstanceState);

		ListView lv = (ListView) layout.findViewById(android.R.id.list);
		ViewGroup parent = (ViewGroup) lv.getParent();

		// Iterate through parent's children until we find the ListView
		for (int i = 0, z = parent.getChildCount(); i < z; i++) {
			View child = parent.getChildAt(i);

			if (child == lv) {
				// Remove the ListView first
				parent.removeViewAt(i);

				// Now create ListView, and add it in it's place...
				mPullToRefreshListView = onCreatePullToRefreshListView(inflater, savedInstanceState);
				parent.addView(mPullToRefreshListView, i, lv.getLayoutParams());
				break;
			}
		}

		return layout;
	}

	protected PullToRefreshListView onCreatePullToRefreshListView(LayoutInflater inflater, Bundle savedInstanceState) {
		return new PullToRefreshListView(getActivity());
	}

	protected final PullToRefreshListView getPullToRefreshListView() {
		return mPullToRefreshListView;
	}
}