/*
 * Copyright 2012 Intridea.Inc. All rights reserved.
 */

package com.handmark.pulltorefresh.library;

import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * @author yincan 3:40:37 PM
 */
public class SectionListView extends ListView implements EmptyViewMethodAccessor, OnScrollListener {

    private View                         transparentView;

    private PullToRefreshSectionListView parentListView;

    public SectionListView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        commonInitialisation();
    }

    public SectionListView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        commonInitialisation();
    }

    public SectionListView(final Context context) {
        super(context);
        commonInitialisation();
    }

    public void setPullToRefreshSectionListView(PullToRefreshSectionListView parentListView) {
        this.parentListView = parentListView;
    }

    protected final void commonInitialisation() {
        setOnScrollListener(this);
        setVerticalFadingEdgeEnabled(false);
        setFadingEdgeLength(0);
    }

    @Override
    public void setAdapter(final ListAdapter adapter) {
        if (!(adapter instanceof SectionListAdapter)) {
            throw new IllegalArgumentException("The adapter needds to be of type " + SectionListAdapter.class
                    + " and is " + adapter.getClass());
        }
        super.setAdapter(adapter);
        final ViewParent parent = getParent();
        if (!(parent instanceof FrameLayout)) {
            throw new IllegalStateException("Section List should have FrameLayout as parent!");
        }
        if (transparentView != null) {
            ((FrameLayout) parent).removeView(transparentView);
        }
        transparentView = ((SectionListAdapter) adapter).getTransparentSectionView();
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        ((FrameLayout) parent).addView(transparentView, lp);
        if (adapter.isEmpty()) {
            transparentView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
            final int totalItemCount) {
        
        if (firstVisibleItem > 0) {
            if (transparentView != null) {
                this.transparentView.setVisibility(View.VISIBLE);
            }
            SectionListAdapter adapter = null;
            adapter = (SectionListAdapter) ((HeaderViewListAdapter) getAdapter()).getWrappedAdapter();
            if (adapter != null) {
                adapter.makeSectionInvisibleIfFirstInList(firstVisibleItem);
            }
        } else {
            if (transparentView != null) {
                this.transparentView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        // do nothing
    }

    @Override
    public void setEmptyView(View emptyView) {
        parentListView.setEmptyView(emptyView);
    }

    @Override
    public void setEmptyViewInternal(View emptyView) {
        super.setEmptyView(emptyView);
    }

    public ContextMenuInfo getContextMenuInfo() {
        return super.getContextMenuInfo();
    }
}
