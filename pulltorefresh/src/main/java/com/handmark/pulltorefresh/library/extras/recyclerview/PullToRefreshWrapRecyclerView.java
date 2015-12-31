/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.handmark.pulltorefresh.library.extras.recyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.LoadingLayoutBase;
import com.handmark.pulltorefresh.library.LoadingLayoutProxy;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.R;

import java.lang.reflect.Constructor;

/**
 * 版权所有：XXX有限公司
 *
 * PullToRefreshWrapRecyclerView
 *
 * @author zhou.wenkai  zwenkai@foxmail.com ,Created on 2015-9-23 09:07:33
 * Major Function：对PullToRefresh的扩展,增加支持RecyclerView
 *
 * 注:如果您修改了本类请填写以下内容作为记录，如非本人操作劳烦通知，谢谢！！！
 * @author mender，Modified Date Modify Content:
 */
public class PullToRefreshWrapRecyclerView extends PullToRefreshBase<WrapRecyclerView> {

    private LoadingLayoutBase mHeaderLoadingView;
    private LoadingLayoutBase mFooterLoadingView;

    private FrameLayout mLvHeaderLoadingFrame;
    private FrameLayout mLvFooterLoadingFrame;

    private boolean mRecyclerViewExtrasEnabled;

    public PullToRefreshWrapRecyclerView(Context context) {
        super(context);
    }

    public PullToRefreshWrapRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullToRefreshWrapRecyclerView(Context context, Mode mode) {
        super(context, mode);
    }

    public PullToRefreshWrapRecyclerView(Context context, Mode mode, AnimationStyle style) {
        super(context, mode, style);
    }

    @Override
    public final Orientation getPullToRefreshScrollDirection() {
        return Orientation.VERTICAL;
    }

    @Override
    protected void onPullToRefresh() {
        super.onPullToRefresh();

        WrapAdapter adapter = mRefreshableView.getAdapter();
        if (!mRecyclerViewExtrasEnabled || !getShowViewWhileRefreshing() || null == adapter) {
            return;
        }

        final int selection;

        switch (getCurrentMode()) {
            case MANUAL_REFRESH_ONLY:
            case PULL_FROM_END:
                selection = adapter.getItemCount() - 1;
                break;
            case PULL_FROM_START:
            default:
                selection = 0;
                break;
        }

        // Make sure the ListView is scrolled to show the loading
        // header/footer
        mRefreshableView.scrollToPosition(selection);
    }

    @Override
    protected void onRefreshing(final boolean doScroll) {
        /**
         * If we're not showing the Refreshing view, or the list is empty, the
         * the header/footer views won't show so we use the normal method.
         */
        WrapAdapter adapter = mRefreshableView.getAdapter();
        if (!mRecyclerViewExtrasEnabled || !getShowViewWhileRefreshing() || null == adapter) {
            super.onRefreshing(doScroll);
            return;
        }

        super.onRefreshing(false);

        final LoadingLayoutBase origLoadingView, listViewLoadingView, oppositeRecyclerViewLoadingView;
        final int selection, scrollToY;

        switch (getCurrentMode()) {
            case MANUAL_REFRESH_ONLY:
            case PULL_FROM_END:
                origLoadingView = getFooterLayout();
                listViewLoadingView = mFooterLoadingView;
                oppositeRecyclerViewLoadingView = mHeaderLoadingView;
                selection = adapter.getItemCount() - 1;
                scrollToY = getScrollY() - getFooterSize();
                break;
            case PULL_FROM_START:
            default:
                origLoadingView = getHeaderLayout();
                listViewLoadingView = mHeaderLoadingView;
                oppositeRecyclerViewLoadingView = mFooterLoadingView;
                selection = 0;
                scrollToY = getScrollY() + getHeaderSize();
                break;
        }

        // Hide our original Loading View
        origLoadingView.reset();
        origLoadingView.hideAllViews();

        // Make sure the opposite end is hidden too
        oppositeRecyclerViewLoadingView.setVisibility(View.GONE);

        // Show the ListView Loading View and set it to refresh.
        listViewLoadingView.setVisibility(View.VISIBLE);
        listViewLoadingView.refreshing();

        if (doScroll) {
            // We need to disable the automatic visibility changes for now
            disableLoadingLayoutVisibilityChanges();

            // We scroll slightly so that the ListView's header/footer is at the
            // same Y position as our normal header/footer
            setHeaderScroll(scrollToY);

            // Make sure the ListView is scrolled to show the loading
            // header/footer
            mRefreshableView.scrollToPosition(selection);
            // Smooth scroll as normal
            smoothScrollTo(0);
        }
    }

    @Override
    protected void onReset() {
        /**
         * If the extras are not enabled, just call up to super and return.
         */
        if (!mRecyclerViewExtrasEnabled) {
            super.onReset();
            return;
        }

        final LoadingLayoutBase originalLoadingLayout, recyclerViewLoadingLayout;
        final int scrollToHeight, selection;
        final boolean scrollLvToEdge;

        switch (getCurrentMode()) {
            case MANUAL_REFRESH_ONLY:
            case PULL_FROM_END:
                originalLoadingLayout = getFooterLayout();
                recyclerViewLoadingLayout = mFooterLoadingView;
                selection = mRefreshableView.getAdapter().getItemCount() - 1;
                scrollToHeight = getFooterSize();
                scrollLvToEdge = Math.abs(getLastVisiblePosition() - selection) <= 1;
                break;
            case PULL_FROM_START:
            default:
                originalLoadingLayout = getHeaderLayout();
                recyclerViewLoadingLayout = mHeaderLoadingView;
                scrollToHeight = -getHeaderSize();
                selection = 0;
                scrollLvToEdge = Math.abs(getFirstVisiblePosition() - selection) <= 1;
                break;
        }

        // If the RecyclerView header loading layout is showing, then we need to
        // flip so that the original one is showing instead
        if (recyclerViewLoadingLayout.getVisibility() == View.VISIBLE) {

            // Set our Original View to Visible
            originalLoadingLayout.showInvisibleViews();

            // Hide the RecyclerView Header/Footer
            recyclerViewLoadingLayout.setVisibility(View.GONE);

            /**
             * Scroll so the View is at the same Y as the ListView
             * header/footer, but only scroll if: we've pulled to refresh, it's
             * positioned correctly
             */
            if (scrollLvToEdge && getState() != State.MANUAL_REFRESHING) {
                mRefreshableView.scrollToPosition(selection);
                setHeaderScroll(scrollToHeight);
            }
        }

        // Finally, call up to super
        super.onReset();
    }

    @Override
    protected LoadingLayoutProxy createLoadingLayoutProxy(final boolean includeStart, final boolean includeEnd) {
        LoadingLayoutProxy proxy = super.createLoadingLayoutProxy(includeStart, includeEnd);

        if (mRecyclerViewExtrasEnabled) {
            final Mode mode = getMode();

            if (includeStart && mode.showHeaderLoadingLayout()) {
                proxy.addLayout(mHeaderLoadingView);
            }
            if (includeEnd && mode.showFooterLoadingLayout()) {
                proxy.addLayout(mFooterLoadingView);
            }
        }

        return proxy;
    }

    @Override
    protected WrapRecyclerView createRefreshableView(Context context,AttributeSet attrs) {
        WrapRecyclerView recyclerView = new InternalWrapRecyclerView(context, attrs);
        return recyclerView;
    }

    @Override
    protected void handleStyledAttributes(TypedArray a) {
        super.handleStyledAttributes(a);

        mRecyclerViewExtrasEnabled = a.getBoolean(R.styleable.PullToRefresh_ptrRecyclerViewExtrasEnabled, true);

        if (mRecyclerViewExtrasEnabled) {
            final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);

            // Create Loading Views ready for use later
            mLvHeaderLoadingFrame = new FrameLayout(getContext());
            mHeaderLoadingView = createLoadingLayout(getContext(), Mode.PULL_FROM_START, a);
            mHeaderLoadingView.setVisibility(View.GONE);
            mLvHeaderLoadingFrame.addView(mHeaderLoadingView, lp);
            mRefreshableView.addHeaderView(mLvHeaderLoadingFrame);

//            final FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(getMeasuredWidth(),
//                    FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);
            mLvFooterLoadingFrame = new FrameLayout(getContext());
            mFooterLoadingView = createLoadingLayout(getContext(), Mode.PULL_FROM_END, a);
            mFooterLoadingView.setVisibility(View.GONE);
            mLvFooterLoadingFrame.addView(mFooterLoadingView, lp);

            /**
             * If the value for Scrolling While Refreshing hasn't been
             * explicitly set via XML, enable Scrolling While Refreshing.
             */
            if (!a.hasValue(R.styleable.PullToRefresh_ptrScrollingWhileRefreshingEnabled)) {
                setScrollingWhileRefreshingEnabled(true);
            }
        }
    }

    @Override
    public void setHeaderLayout(LoadingLayoutBase headerLayout) {
        super.setHeaderLayout(headerLayout);

        try {
            Constructor c = headerLayout.getClass().getDeclaredConstructor(new Class[]{Context.class});
            LoadingLayoutBase mHeaderLayout = (LoadingLayoutBase)c.newInstance(new Object[]{getContext()});
            if(null != mHeaderLayout) {
                mRefreshableView.removeHeaderView(mLvHeaderLoadingFrame);
                final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);

                mLvHeaderLoadingFrame = new FrameLayout(getContext());
                mHeaderLoadingView = mHeaderLayout;
                mHeaderLoadingView.setVisibility(View.GONE);
                mLvHeaderLoadingFrame.addView(mHeaderLoadingView, lp);
                mRefreshableView.addHeaderView(mLvHeaderLoadingFrame);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setFooterLayout(LoadingLayoutBase footerLayout) {
        super.setFooterLayout(footerLayout);

        try {
            Constructor c = footerLayout.getClass().getDeclaredConstructor(new Class[]{Context.class});
            LoadingLayoutBase mFooterLayout = (LoadingLayoutBase)c.newInstance(new Object[]{getContext()});
            if(null != mFooterLayout) {
                mRefreshableView.removeFooterView(mLvFooterLoadingFrame);
                final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);

                mLvFooterLoadingFrame = new FrameLayout(getContext());
                mFooterLoadingView = mFooterLayout;
                mFooterLoadingView.setVisibility(View.GONE);
                mLvFooterLoadingFrame.addView(mFooterLoadingView, lp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected boolean isReadyForPullStart() {
        return isFirstItemVisible();
    }

    @Override
    protected boolean isReadyForPullEnd() {
        return isLastItemVisible();
    }

    /**
     * @Description: 判断第一个条目是否完全可见
     *
     * @return boolean:
     * @version 1.0
     * @date 2015-9-23
     * @Author zhou.wenkai
     */
    private boolean isFirstItemVisible() {
        final Adapter<?> adapter = getRefreshableView().getAdapter();

        // 如果未设置Adapter或者Adapter没有数据可以下拉刷新
        if (null == adapter || adapter.getItemCount() == 0) {
            if (DEBUG) {
                Log.d(LOG_TAG, "isFirstItemVisible. Empty View.");
            }
            return true;

        } else {
            // 第一个条目完全展示,可以刷新
            if (getFirstVisiblePosition() == 0) {
                return mRefreshableView.getChildAt(0).getTop() >= mRefreshableView
                        .getTop();
            }
        }

        return false;
    }

    /**
     * @Description: 获取第一个可见子View的位置下标
     *
     * @return int: 位置
     * @version 1.0
     * @date 2015-9-23
     * @Author zhou.wenkai
     */
    private int getFirstVisiblePosition() {
        View firstVisibleChild = mRefreshableView.getChildAt(0);
        return firstVisibleChild != null ? mRefreshableView
                .getChildAdapterPosition(firstVisibleChild) : -1;
    }

    /**
     * @Description: 判断最后一个条目是否完全可见
     *
     * @return boolean:
     * @version 1.0
     * @date 2015-9-23
     * @Author zhou.wenkai
     */
    private boolean isLastItemVisible() {
        final Adapter<?> adapter = getRefreshableView().getAdapter();

        // 如果未设置Adapter或者Adapter没有数据可以上拉刷新
        if (null == adapter || adapter.getItemCount() == 0) {
            if (DEBUG) {
                Log.d(LOG_TAG, "isLastItemVisible. Empty View.");
            }
            return true;

        } else {
            // 最后一个条目View完全展示,可以刷新
            int lastVisiblePosition = getLastVisiblePosition();
            if(lastVisiblePosition >= mRefreshableView.getAdapter().getItemCount()-1) {
                return mRefreshableView.getChildAt(
                        mRefreshableView.getChildCount() - 1).getBottom() <= mRefreshableView
                        .getBottom();
            }
        }

        return false;
    }

    /**
     * @Description: 获取最后一个可见子View的位置下标
     *
     * @return int: 位置
     * @version 1.0
     * @date 2015-9-23
     * @Author zhou.wenkai
     */
    private int getLastVisiblePosition() {
        View lastVisibleChild = mRefreshableView.getChildAt(mRefreshableView
                .getChildCount() - 1);
        return lastVisibleChild != null ? mRefreshableView
                .getChildAdapterPosition(lastVisibleChild) : -1;
    }

    protected class InternalWrapRecyclerView extends WrapRecyclerView {

        private boolean mAddedLvFooter = false;

        public InternalWrapRecyclerView(Context context) {
            super(context);
        }

        public InternalWrapRecyclerView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public InternalWrapRecyclerView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public void setAdapter(Adapter adapter) {
            // Add the Footer View at the last possible moment
            if (null != mLvFooterLoadingFrame && !mAddedLvFooter) {
                addFooterView(mLvFooterLoadingFrame);
                mAddedLvFooter = true;
            }

            super.setAdapter(adapter);
        }
    }

}