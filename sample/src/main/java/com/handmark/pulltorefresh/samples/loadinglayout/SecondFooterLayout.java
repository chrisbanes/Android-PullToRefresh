package com.handmark.pulltorefresh.samples.loadinglayout;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.handmark.pulltorefresh.samples.R;

/**
 * Created by zwenkai on 2016/4/10.
 */
public class SecondFooterLayout extends FrameLayout {

    private FrameLayout mInnerLayout;
   private final TextView mHasDataText;
    private final TextView mNoDataText;

    public SecondFooterLayout(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.listview_footer_second_loadinglayout, this);

        mInnerLayout = (FrameLayout) findViewById(R.id.fl_inner);
        mHasDataText = (TextView) mInnerLayout.findViewById(R.id.second_footer_has_data);
        mNoDataText = (TextView) mInnerLayout.findViewById(R.id.second_footer_no_data);
    }

    public void setNoData() {
        mHasDataText.setVisibility(GONE);
        mNoDataText.setVisibility(VISIBLE);
    }
}