package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;
import android.widget.ListAdapter;

public class PullToRefreshGridView extends PullToRefreshBase<GridView> {

	private class PullToRefreshInternalGridView extends GridView {

		PullToRefreshInternalGridView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public void setAdapter(ListAdapter adapter) {
			super.setAdapter(adapter);
			resetHeader();
		}
	}

	public PullToRefreshGridView(Context context) {
		super(context);
	}

	public PullToRefreshGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected GridView createAdapterView(Context context, AttributeSet attrs) {
		GridView gv = new PullToRefreshInternalGridView(context, attrs);

		// Use Generated ID (from res/values/ids.xml)
		gv.setId(R.id.gridview);
		return gv;
	}

}
