package com.handmark.pulltorefresh.library.extras.recyclerview;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 版权所有：XXX有限公司
 *
 * WrapRecyclerView
 *
 * @author zhou.wenkai ,Created on 2015-11-24 10:48:29
 * Major Function：A RecyclerView that allows for headers and footers as well.
 *
 * 注:如果您修改了本类请填写以下内容作为记录，如非本人操作劳烦通知，谢谢！！！
 * @author mender，Modified Date Modify Content:
 */
public class WrapRecyclerView extends RecyclerView {

    private WrapAdapter mWrapAdapter;
    private boolean shouldAdjustSpanSize;

    // Defines available view type integers for headers and footers.
    static final int BASE_HEADER_VIEW_TYPE = -1 << 10;
    static final int BASE_FOOTER_VIEW_TYPE = -1 << 11;

    /**
     * A class that represents a fixed view in a list, for example a header at the top
     * or a footer at the bottom.
     */
    public class FixedViewInfo {
        /** The view to add to the list */
        public View view;
        /** The data backing the view. This is returned from {RecyclerView.Adapter#getItemViewType(int)}. */
        public int viewType;
    }

    private ArrayList<FixedViewInfo> mHeaderViewInfos = new ArrayList<>();
    private ArrayList<FixedViewInfo> mFooterViewInfos = new ArrayList<>();

    public WrapRecyclerView(Context context) {
        super(context);
    }

    public WrapRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WrapRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if(adapter instanceof WrapAdapter) {
            mWrapAdapter = (WrapAdapter) adapter;
            super.setAdapter(adapter);
        } else {
            mWrapAdapter = new WrapAdapter(mHeaderViewInfos, mFooterViewInfos, adapter);
            super.setAdapter(mWrapAdapter);
        }

        if(shouldAdjustSpanSize) {
            mWrapAdapter.adjustSpanSize(this);
        }
    }

    /**
     * Retrieves the previously set wrap adapter or null if no adapter is set.
     *
     * @return The previously set adapter
     */
    @Override
    public WrapAdapter getAdapter() {
        return mWrapAdapter;
    }

    /**
     * Gets the real adapter
     *
     * @return T:
     * @version 1.0
     */
    public Adapter getWrappedAdapter() {
        if(mWrapAdapter == null) {
            throw new IllegalStateException("You must set a adapter before!");
        }
        return mWrapAdapter.getWrappedAdapter();
    }

    /**
     * Adds a header view
     *
     * @param view
     * @version 1.0
     */
    public void addHeaderView(View view) {
        if (null == view) {
            throw new IllegalArgumentException("the view to add must not be null!");
        }

        final FixedViewInfo info = new FixedViewInfo();
        info.view = view;
        info.viewType = BASE_HEADER_VIEW_TYPE + mHeaderViewInfos.size();
        mHeaderViewInfos.add(info);

        if(null != mWrapAdapter) {
            mWrapAdapter.updateHeaderViewInfos(mFooterViewInfos);
        }
    }

    /**
     * Adds a footer view
     *
     * @param view
     * @version 1.0
     */
    public void addFooterView(View view) {
        if (null == view) {
            throw new IllegalArgumentException("the view to add must not be null!");
        }

        final FixedViewInfo info = new FixedViewInfo();
        info.view = view;
        info.viewType = BASE_FOOTER_VIEW_TYPE + mFooterViewInfos.size();
        mFooterViewInfos.add(info);

        if(null != mWrapAdapter) {
            mWrapAdapter.updateFooterViewInfos(mFooterViewInfos);
        }
    }

    public void removeHeaderView(View view) {
        for (WrapRecyclerView.FixedViewInfo fixedViewInfo : mHeaderViewInfos) {
            if(fixedViewInfo.view == view) {
                mHeaderViewInfos.remove(view);
            }
        }
        if(null != mWrapAdapter) {
            mWrapAdapter.updateHeaderViewInfos(mFooterViewInfos);
        }
    }

    public void removeFooterView(View view) {
        for (WrapRecyclerView.FixedViewInfo fixedViewInfo : mFooterViewInfos) {
            if(fixedViewInfo.view == view) {
                mFooterViewInfos.remove(view);
            }
        }
        if(null != mWrapAdapter) {
            mWrapAdapter.updateFooterViewInfos(mFooterViewInfos);
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout instanceof GridLayoutManager || layout instanceof StaggeredGridLayoutManager) {
            this.shouldAdjustSpanSize = true;
        }
    }

    /**
     * gets the headers view
     *
     * @return List:
     * @version 1.0
     */
    public List<View> getHeadersView() {
        if(mWrapAdapter == null) {
            throw new IllegalStateException("You must set a adapter before!");
        }
        return mWrapAdapter.getHeadersView();
    }

    /**
     * gets the footers view
     *
     * @return List:
     * @version 1.0
     */
    public List<View> getFootersView() {
        if(mWrapAdapter == null) {
            throw new IllegalStateException("You must set a adapter before!");
        }
        return mWrapAdapter.getFootersView();
    }

    /**
     * Setting the visibility of the header views
     *
     * @param shouldShow
     * @version 1.0
     */
    public void setFooterVisibility(boolean shouldShow) {
        if(mWrapAdapter == null) {
            throw new IllegalStateException("You must set a adapter before!");
        }
        mWrapAdapter.setFooterVisibility(shouldShow);
    }

    /**
     * Setting the visibility of the footer views
     *
     * @param shouldShow
     * @version 1.0
     */
    public void setHeaderVisibility(boolean shouldShow) {
        if(mWrapAdapter == null) {
            throw new IllegalStateException("You must set a adapter before!");
        }
        mWrapAdapter.setHeaderVisibility(shouldShow);
    }

    /**
     * get the count of headers
     *
     * @return number of headers
     * @version 1.0
     */
    public int getHeadersCount() {
        if(mWrapAdapter == null) {
            throw new IllegalStateException("You must set a adapter before!");
        }
        return mWrapAdapter.getHeadersCount();
    }

    /**
     * get the count of footers
     *
     * @return the number of footers
     * @version 1.0
     */
    public int getFootersCount() {
        if(mWrapAdapter == null) {
            throw new IllegalStateException("You must set a adapter before!");
        }
        return mWrapAdapter.getFootersCount();
    }

}