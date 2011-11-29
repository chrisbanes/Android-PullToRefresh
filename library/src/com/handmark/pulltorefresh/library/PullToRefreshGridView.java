package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

public class PullToRefreshGridView extends PullToRefreshBase<GridView> {

	public PullToRefreshGridView(Context context) {
		super(context);
	}

	public PullToRefreshGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected final GridView createAdapterView(Context context, AttributeSet attrs) {
		GridView gv = new GridView(context, attrs);

		// Use Generated ID (from res/values/ids.xml)
		gv.setId(R.id.gridview);
		return gv;
	}

}
