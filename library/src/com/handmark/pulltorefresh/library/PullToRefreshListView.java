package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.ListView;

public class PullToRefreshListView extends PullToRefreshBase<ListView> {

	private class PullToRefreshInternalListView extends ListView {

		PullToRefreshInternalListView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public void setAdapter(ListAdapter adapter) {
			super.setAdapter(adapter);
			resetHeader();
		}
	}

	public PullToRefreshListView(Context context) {
		super(context);
	}

	public PullToRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected ListView createAdapterView(Context context, AttributeSet attrs) {
		ListView lv = new PullToRefreshInternalListView(context, attrs);

		// Set it to this so it can be used in ListActivity/ListFragment
		lv.setId(android.R.id.list);
		return lv;
	}

}
