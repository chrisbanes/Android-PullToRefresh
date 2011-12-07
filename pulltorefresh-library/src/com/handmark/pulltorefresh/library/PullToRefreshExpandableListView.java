package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ExpandableListView;

public class PullToRefreshExpandableListView extends PullToRefreshBase<ExpandableListView> {

    public PullToRefreshExpandableListView(Context context) {
        super(context);
    }

    public PullToRefreshExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected final ExpandableListView createAdapterView(Context context, AttributeSet attrs) {
        ExpandableListView lv = new ExpandableListView(context, attrs);

        // Set it to this so it can be used in ListActivity/ListFragment
        lv.setId(android.R.id.list);
        return lv;
    }

}
