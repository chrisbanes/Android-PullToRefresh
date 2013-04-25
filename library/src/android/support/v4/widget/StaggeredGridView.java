/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v4.widget;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListAdapter;

/**
 * ListView and GridView just not complex enough? Try StaggeredGridView!
 * 
 * <p>
 * StaggeredGridView presents a multi-column grid with consistent column sizes
 * but varying row sizes between the columns. Each successive item from a
 * {@link android.widget.ListAdapter ListAdapter} will be arranged from top to
 * bottom, left to right. The largest vertical gap is always filled first.
 * </p>
 * 
 * <p>
 * Item views may span multiple columns as specified by their
 * {@link LayoutParams}. The attribute <code>android:layout_span</code> may be
 * used when inflating item views from xml.
 * </p>
 */
public class StaggeredGridView extends ViewGroup
{
    private static final String TAG = "StaggeredGridView";
    private static final boolean DEBUG = true;

    /**
     * There are a few things you should know if you're going to make
     * modifications to StaggeredGridView.
     * 
     * Like ListView, SGV populates from an adapter and recycles views that fall
     * out of the visible boundaries of the grid. A few invariants always hold:
     * 
     * - mFirstPosition is the adapter position of the View returned by
     * getChildAt(0). <br />
     * - Any child index can be translated to an adapter position by adding
     * mFirstPosition. <br />
     * - Any adapter position can be translated to a child index by subtracting
     * mFirstPosition.<br />
     * - Views for items in the range [mFirstPosition, mFirstPosition +
     * getChildCount()) are currently attached to the grid as children. All
     * other adapter positions do not have active views.
     * 
     * This means a few things thanks to the staggered grid's nature. Some views
     * may stay attached long after they have scrolled offscreen if removing and
     * recycling them would result in breaking one of the invariants above.
     * 
     * LayoutRecords are used to track data about a particular item's layout
     * after the associated view has been removed. These let positioning and the
     * choice of column for an item remain consistent even though the rules for
     * filling content up vs. filling down vary.
     * 
     * Whenever layout parameters for a known LayoutRecord change, other
     * LayoutRecords before or after it may need to be invalidated. e.g. if the
     * item's height or the number of columns it spans changes, all bets for
     * other items in the same direction are off since the cached information no
     * longer applies.
     */

    private ListAdapter mAdapter;

    public static final int COLUMN_COUNT_AUTO = -1;

    private int mColCountSetting = 2;
    private int mColCount = 2;
    private int mMinColWidth = 0;
    private int mItemMargin;

    private int[] mItemTops;
    private int[] mItemBottoms;

    private boolean mFastChildLayout;
    private boolean mPopulating;
    private boolean mForcePopulateOnLayout;
    private boolean mInLayout;
    private int mRestoreOffset;

    private final RecycleBin mRecycler = new RecycleBin();

    private final AdapterDataSetObserver mObserver = new AdapterDataSetObserver();

    private boolean mDataChanged;
    private int mOldItemCount;
    private int mItemCount;
    private boolean mHasStableIds;

    private int mFirstPosition;

    private final int mTouchSlop;
    private final int mMaximumVelocity;
    private final int mFlingVelocity;
    private float mLastTouchY;
    private float mTouchRemainderY;
    private int mActivePointerId;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DRAGGING = 1;
    private static final int TOUCH_MODE_FLINGING = 2;

    private int mTouchMode;
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private final ScrollerCompat mScroller;

    private final EdgeEffectCompat mTopEdge;
    private final EdgeEffectCompat mBottomEdge;
    private OnScrollListener mOnScrollListener;

    private static final class LayoutRecord
    {
        public int column;
        public long id = -1;
        public int height;
        public int span;
        private int[] mMargins;

        private final void ensureMargins()
        {
            if (mMargins == null)
            {
                // Don't need to confirm length;
                // all layoutrecords are purged when column count changes.
                mMargins = new int[span * 2];
            }
        }

        public final int getMarginAbove(int col)
        {
            if (mMargins == null)
            {
                return 0;
            }
            return mMargins[col * 2];
        }

        public final int getMarginBelow(int col)
        {
            if (mMargins == null)
            {
                return 0;
            }
            return mMargins[col * 2 + 1];
        }

        public final void setMarginAbove(int col, int margin)
        {
            if (mMargins == null && margin == 0)
            {
                return;
            }
            ensureMargins();
            mMargins[col * 2] = margin;
        }

        public final void setMarginBelow(int col, int margin)
        {
            if (mMargins == null && margin == 0)
            {
                return;
            }
            ensureMargins();
            mMargins[col * 2 + 1] = margin;
        }

        @Override
        public String toString()
        {
            String result = "LayoutRecord{c=" + column + ", id=" + id + " h=" + height + " s="
                    + span;
            if (mMargins != null)
            {
                result += " margins[above, below](";
                for (int i = 0; i < mMargins.length; i += 2)
                {
                    result += "[" + mMargins[i] + ", " + mMargins[i + 1] + "]";
                }
                result += ")";
            }
            return result + "}";
        }
    }

    private final SparseArrayCompat<LayoutRecord> mLayoutRecords = new SparseArrayCompat<LayoutRecord>();

    public StaggeredGridView(Context context)
    {
        this(context, null);
    }

    public StaggeredGridView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public StaggeredGridView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMaximumVelocity = vc.getScaledMaximumFlingVelocity();
        mFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mScroller = ScrollerCompat.from(context);

        mTopEdge = new EdgeEffectCompat(context);
        mBottomEdge = new EdgeEffectCompat(context);
        setWillNotDraw(false);
        setClipToPadding(false);
    }

    /**
     * Set a fixed number of columns for this grid. Space will be divided evenly
     * among all columns, respecting the item margin between columns. The
     * default is 2. (If it were 1, perhaps you should be using a
     * {@link android.widget.ListView ListView}.)
     * 
     * @param colCount
     *            Number of columns to display.
     * @see #setMinColumnWidth(int)
     */
    public void setColumnCount(int colCount)
    {
        if (colCount < 1 && colCount != COLUMN_COUNT_AUTO)
        {
            throw new IllegalArgumentException("Column count must be at least 1 - received "
                    + colCount);
        }
        final boolean needsPopulate = colCount != mColCount;
        mColCount = mColCountSetting = colCount;
        if (needsPopulate)
        {
            populate();
        }
    }

    public int getColumnCount()
    {
        return mColCount;
    }

    /**
     * Set a minimum column width for
     * 
     * @param minColWidth
     */
    public void setMinColumnWidth(int minColWidth)
    {
        mMinColWidth = minColWidth;
        setColumnCount(COLUMN_COUNT_AUTO);
    }

    /**
     * Set the margin between items in pixels. This margin is applied both
     * vertically and horizontally.
     * 
     * @param marginPixels
     *            Spacing between items in pixels
     */
    public void setItemMargin(int marginPixels)
    {
        final boolean needsPopulate = marginPixels != mItemMargin;
        mItemMargin = marginPixels;
        if (needsPopulate)
        {
            populate();
        }
    }

    /**
     * Return the first adapter position with a view currently attached as a
     * child view of this grid.
     * 
     * @return the adapter position represented by the view at getChildAt(0).
     */
    public int getFirstPosition()
    {
        return mFirstPosition;
    }
    
    /**
     * @return the total number of items in the grid displayed or not
     */
    public int getItemCount()
    {
        return mItemCount;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                mVelocityTracker.clear();
                mScroller.abortAnimation();
                mLastTouchY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mTouchRemainderY = 0;
                if (mTouchMode == TOUCH_MODE_FLINGING)
                {
                    // Catch!
                    // Stopped a fling. It is a scroll.
                    setTouchMode(TOUCH_MODE_DRAGGING);
                    
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
            {
                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0)
                {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id "
                            + mActivePointerId
                            + " - did StaggeredGridView receive an inconsistent " + "event stream?");
                    return false;
                }
                final float y = MotionEventCompat.getY(ev, index);
                final float dy = y - mLastTouchY + mTouchRemainderY;
                final int deltaY = (int) dy;
                mTouchRemainderY = dy - deltaY;

                if (Math.abs(dy) > mTouchSlop)
                {
                    setTouchMode(TOUCH_MODE_DRAGGING);
                    return true;
                }
            }
        }

        return false;
    }
    
    public void setTouchMode(int newState){
//    	mTouchMode = newState;
    	switch (newState) {
		case TOUCH_MODE_DRAGGING:
			reportScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
			break;
		case TOUCH_MODE_FLINGING:
			reportScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
			break;
		case TOUCH_MODE_IDLE:
			reportScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
			break;
		}
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                mVelocityTracker.clear();
                mScroller.abortAnimation();
                mLastTouchY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mTouchRemainderY = 0;
                break;

            case MotionEvent.ACTION_MOVE:
            {
                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0)
                {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id "
                            + mActivePointerId
                            + " - did StaggeredGridView receive an inconsistent " + "event stream?");
                    return false;
                }
                final float y = MotionEventCompat.getY(ev, index);
                final float dy = y - mLastTouchY + mTouchRemainderY;
                final int deltaY = (int) dy;
                mTouchRemainderY = dy - deltaY;

                if (Math.abs(dy) > mTouchSlop)
                {
                    setTouchMode(TOUCH_MODE_DRAGGING);
                }

                if (mTouchMode == TOUCH_MODE_DRAGGING)
                {
                    mLastTouchY = y;

                    if (!trackMotionScroll(deltaY, true))
                    {
                        // Break fling velocity if we impacted an edge.
                        mVelocityTracker.clear();
                    }
                }
            }
                break;

            case MotionEvent.ACTION_CANCEL:
                setTouchMode(TOUCH_MODE_IDLE);
                break;

            case MotionEvent.ACTION_UP:
            {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final float velocity = VelocityTrackerCompat.getYVelocity(mVelocityTracker,
                        mActivePointerId);
                if (Math.abs(velocity) > mFlingVelocity)
                { // TODO
                    setTouchMode(TOUCH_MODE_FLINGING);
                    mScroller.fling(0, 0, 0, (int) velocity, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    mLastTouchY = 0;
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                else
                {
                    setTouchMode(TOUCH_MODE_IDLE);
                }

            }
                break;
        }
        return true;
    }

    /**
     * 
     * @param deltaY
     *            Pixels that content should move by
     * @return true if the movement completed, false if it was stopped
     *         prematurely.
     */
    private boolean trackMotionScroll(int deltaY, boolean allowOverScroll)
    {
        final boolean contentFits = contentFits();
        final int allowOverhang = Math.abs(deltaY);

        final int overScrolledBy;
        final int movedBy;
        if (!contentFits)
        {
            final int overhang;
            final boolean up;
            mPopulating = true;
            if (deltaY > 0)
            {
                overhang = fillUp(mFirstPosition - 1, allowOverhang);
                up = true;
            }
            else
            {
                overhang = fillDown(mFirstPosition + getChildCount(), allowOverhang) + mItemMargin;
                up = false;
            }
            movedBy = Math.min(overhang, allowOverhang);
            offsetChildren(up ? movedBy : -movedBy);
            recycleOffscreenViews();
            mPopulating = false;
            overScrolledBy = allowOverhang - overhang;
        }
        else
        {
            overScrolledBy = allowOverhang;
            movedBy = 0;
        }

        if (allowOverScroll)
        {
            final int overScrollMode = ViewCompat.getOverScrollMode(this);

            if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS
                    || (overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS && !contentFits))
            {

                if (overScrolledBy > 0)
                {
                    EdgeEffectCompat edge = deltaY > 0 ? mTopEdge : mBottomEdge;
                    edge.onPull((float) Math.abs(deltaY) / getHeight());
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
        }
        
        invokeOnItemScrollListener();
        
        return deltaY == 0 || movedBy != 0;
    }

    private final boolean contentFits()
    {
        if (mFirstPosition != 0 || getChildCount() != mItemCount)
        {
            return false;
        }

        int topmost = Integer.MAX_VALUE;
        int bottommost = Integer.MIN_VALUE;
        for (int i = 0; i < mColCount; i++)
        {
            if (mItemTops[i] < topmost)
            {
                topmost = mItemTops[i];
            }
            if (mItemBottoms[i] > bottommost)
            {
                bottommost = mItemBottoms[i];
            }
        }

        return topmost >= getPaddingTop() && bottommost <= getHeight() - getPaddingBottom();
    }

    private void recycleAllViews()
    {
        for (int i = 0; i < getChildCount(); i++)
        {
            mRecycler.addScrap(getChildAt(i));
        }

        if (mInLayout)
        {
            removeAllViewsInLayout();
        }
        else
        {
            removeAllViews();
        }
    }

    /**
     * Important: this method will leave offscreen views attached if they are
     * required to maintain the invariant that child view with index i is always
     * the view corresponding to position mFirstPosition + i.
     */
    private void recycleOffscreenViews()
    {
        final int height = getHeight();
        final int clearAbove = -mItemMargin;
        final int clearBelow = height + mItemMargin;
        for (int i = getChildCount() - 1; i >= 0; i--)
        {
            final View child = getChildAt(i);
            if (child.getTop() <= clearBelow)
            {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            if (mInLayout)
            {
                removeViewsInLayout(i, 1);
            }
            else
            {
                removeViewAt(i);
            }

            mRecycler.addScrap(child);
        }

        while (getChildCount() > 0)
        {
            final View child = getChildAt(0);
            if (child.getBottom() >= clearAbove)
            {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            if (mInLayout)
            {
                removeViewsInLayout(0, 1);
            }
            else
            {
                removeViewAt(0);
            }

            mRecycler.addScrap(child);
            mFirstPosition++;
        }

        final int childCount = getChildCount();
        if (childCount > 0)
        {
            // Repair the top and bottom column boundaries from the views we
            // still have
            Arrays.fill(mItemTops, Integer.MAX_VALUE);
            Arrays.fill(mItemBottoms, Integer.MIN_VALUE);

            for (int i = 0; i < childCount; i++)
            {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int top = child.getTop() - mItemMargin;
                final int bottom = child.getBottom();
                final LayoutRecord rec = mLayoutRecords.get(mFirstPosition + i);

                final int colEnd = lp.column + Math.min(mColCount, lp.span);
                for (int col = lp.column; col < colEnd; col++)
                {
                    final int colTop = top - rec.getMarginAbove(col - lp.column);
                    final int colBottom = bottom + rec.getMarginBelow(col - lp.column);
                    if (colTop < mItemTops[col])
                    {
                        mItemTops[col] = colTop;
                    }
                    if (colBottom > mItemBottoms[col])
                    {
                        mItemBottoms[col] = colBottom;
                    }
                }
            }

            for (int col = 0; col < mColCount; col++)
            {
                if (mItemTops[col] == Integer.MAX_VALUE)
                {
                    // If one was untouched, both were.
                    mItemTops[col] = 0;
                    mItemBottoms[col] = 0;
                }
            }
        }
    }

    @Override
    public void computeScroll()
    {
        if (mScroller.computeScrollOffset())
        {
            final int y = mScroller.getCurrY();
            final int dy = (int) (y - mLastTouchY);
            mLastTouchY = y;
            final boolean stopped = !trackMotionScroll(dy, false);

            if (!stopped && !mScroller.isFinished())
            {
            	invokeOnItemScrollListener();
                ViewCompat.postInvalidateOnAnimation(this);
            }
            else
            {
                if (stopped)
                {
                    final int overScrollMode = ViewCompat.getOverScrollMode(this);
                    if (overScrollMode != ViewCompat.OVER_SCROLL_NEVER)
                    {
                        final EdgeEffectCompat edge;
                        if (dy > 0)
                        {
                            edge = mTopEdge;
                        }
                        else
                        {
                            edge = mBottomEdge;
                        }
                        edge.onAbsorb(Math.abs((int) mScroller.getCurrVelocity()));
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                    mScroller.abortAnimation();
                }
                setTouchMode(TOUCH_MODE_IDLE);
            }
        }
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);

        if (mTopEdge != null)
        {
            boolean needsInvalidate = false;
            if (!mTopEdge.isFinished())
            {
                mTopEdge.draw(canvas);
                needsInvalidate = true;
            }
            if (!mBottomEdge.isFinished())
            {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                canvas.translate(-width, getHeight());
                canvas.rotate(180, width, 0);
                mBottomEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
                needsInvalidate = true;
            }

            if (needsInvalidate)
            {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    public void beginFastChildLayout()
    {
        mFastChildLayout = true;
    }

    public void endFastChildLayout()
    {
        mFastChildLayout = false;
        populate();
    }

    @Override
    public void requestLayout()
    {
        if (!mPopulating && !mFastChildLayout)
        {
            super.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY)
        {
            Log.e(TAG, "onMeasure: must have an exact width or match_parent! "
                    + "Using fallback spec of EXACTLY " + widthSize);
            widthMode = MeasureSpec.EXACTLY;
        }
        if (heightMode != MeasureSpec.EXACTLY)
        {
            Log.e(TAG, "onMeasure: must have an exact height or match_parent! "
                    + "Using fallback spec of EXACTLY " + heightSize);
            heightMode = MeasureSpec.EXACTLY;
        }

        setMeasuredDimension(widthSize, heightSize);

        if (mColCountSetting == COLUMN_COUNT_AUTO)
        {
            final int colCount = widthSize / mMinColWidth;
            if (colCount != mColCount)
            {
                mColCount = colCount;
                mForcePopulateOnLayout = true;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        mInLayout = true;
        populate();
        mInLayout = false;
        mForcePopulateOnLayout = false;

        final int width = r - l;
        final int height = b - t;
        mTopEdge.setSize(width, height);
        mBottomEdge.setSize(width, height);
    }

    private void populate()
    {
        if (getWidth() == 0 || getHeight() == 0)
        {
            return;
        }

        if (mColCount == COLUMN_COUNT_AUTO)
        {
            final int colCount = getWidth() / mMinColWidth;
            if (colCount != mColCount)
            {
                mColCount = colCount;
            }
        }

        final int colCount = mColCount;
        if (mItemTops == null || mItemTops.length != colCount)
        {
            mItemTops = new int[colCount];
            mItemBottoms = new int[colCount];
            final int top = getPaddingTop();
            final int offset = top + Math.min(mRestoreOffset, 0);
            Arrays.fill(mItemTops, offset);
            Arrays.fill(mItemBottoms, offset);
            mLayoutRecords.clear();
            if (mInLayout)
            {
                removeAllViewsInLayout();
            }
            else
            {
                removeAllViews();
            }
            mRestoreOffset = 0;
        }

        mPopulating = true;
        layoutChildren(mDataChanged);
        fillDown(mFirstPosition + getChildCount(), 0);
        fillUp(mFirstPosition - 1, 0);
        mPopulating = false;
        mDataChanged = false;
    }

    private void dumpItemPositions()
    {
        final int childCount = getChildCount();
        Log.d(TAG, "dumpItemPositions:");
        Log.d(TAG, " => Tops:");
        for (int i = 0; i < mColCount; i++)
        {
            Log.d(TAG, "  => " + mItemTops[i]);
            boolean found = false;
            for (int j = 0; j < childCount; j++)
            {
                final View child = getChildAt(j);
                if (mItemTops[i] == child.getTop() - mItemMargin)
                {
                    found = true;
                }
            }
            if (!found)
            {
                Log.d(TAG, "!!! No top item found for column " + i + " value " + mItemTops[i]);
            }
        }
        Log.d(TAG, " => Bottoms:");
        for (int i = 0; i < mColCount; i++)
        {
            Log.d(TAG, "  => " + mItemBottoms[i]);
            boolean found = false;
            for (int j = 0; j < childCount; j++)
            {
                final View child = getChildAt(j);
                if (mItemBottoms[i] == child.getBottom())
                {
                    found = true;
                }
            }
            if (!found)
            {
                Log.d(TAG, "!!! No bottom item found for column " + i + " value " + mItemBottoms[i]);
            }
        }
    }

    final void offsetChildren(int offset)
    {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            final View child = getChildAt(i);
            child.layout(child.getLeft(), child.getTop() + offset, child.getRight(),
                    child.getBottom() + offset);
        }

        final int colCount = mColCount;
        for (int i = 0; i < colCount; i++)
        {
            mItemTops[i] += offset;
            mItemBottoms[i] += offset;
        }
    }

    /**
     * Measure and layout all currently visible children.
     * 
     * @param queryAdapter
     *            true to requery the adapter for view data
     */
    final void layoutChildren(boolean queryAdapter)
    {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int itemMargin = mItemMargin;
        final int colWidth = (getWidth() - paddingLeft - paddingRight - itemMargin
                * (mColCount - 1))
                / mColCount;
        int rebuildLayoutRecordsBefore = -1;
        int rebuildLayoutRecordsAfter = -1;

        Arrays.fill(mItemBottoms, Integer.MIN_VALUE);

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final int col = lp.column;
            final int position = mFirstPosition + i;
            final boolean needsLayout = queryAdapter || child.isLayoutRequested();

            if (queryAdapter)
            {
                View newView = obtainView(position, child);
                if (newView != child)
                {
                    removeViewAt(i);
                    addView(newView, i);
                    child = newView;
                }
                lp = (LayoutParams) child.getLayoutParams(); // Might have
                                                             // changed
            }

            final int span = Math.min(mColCount, lp.span);
            final int widthSize = colWidth * span + itemMargin * (span - 1);

            if (needsLayout)
            {
                final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

                final int heightSpec;
                if (lp.height == LayoutParams.WRAP_CONTENT)
                {
                    heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                }
                else
                {
                    heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                }

                child.measure(widthSpec, heightSpec);
            }

            int childTop = mItemBottoms[col] > Integer.MIN_VALUE ? mItemBottoms[col] + mItemMargin
                    : child.getTop();
            if (span > 1)
            {
                int lowest = childTop;
                for (int j = col + 1; j < col + span; j++)
                {
                    final int bottom = mItemBottoms[j] + mItemMargin;
                    if (bottom > lowest)
                    {
                        lowest = bottom;
                    }
                }
                childTop = lowest;
            }
            final int childHeight = child.getMeasuredHeight();
            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft + col * (colWidth + itemMargin);
            final int childRight = childLeft + child.getMeasuredWidth();
            child.layout(childLeft, childTop, childRight, childBottom);

            for (int j = col; j < col + span; j++)
            {
                mItemBottoms[j] = childBottom;
            }

            final LayoutRecord rec = mLayoutRecords.get(position);
            if (rec != null && rec.height != childHeight)
            {
                // Invalidate our layout records for everything before this.
                rec.height = childHeight;
                rebuildLayoutRecordsBefore = position;
            }

            if (rec != null && rec.span != span)
            {
                // Invalidate our layout records for everything after this.
                rec.span = span;
                rebuildLayoutRecordsAfter = position;
            }
        }

        // Update mItemBottoms for any empty columns
        for (int i = 0; i < mColCount; i++)
        {
            if (mItemBottoms[i] == Integer.MIN_VALUE)
            {
                mItemBottoms[i] = mItemTops[i];
            }
        }

        if (rebuildLayoutRecordsBefore >= 0 || rebuildLayoutRecordsAfter >= 0)
        {
            if (rebuildLayoutRecordsBefore >= 0)
            {
                invalidateLayoutRecordsBeforePosition(rebuildLayoutRecordsBefore);
            }
            if (rebuildLayoutRecordsAfter >= 0)
            {
                invalidateLayoutRecordsAfterPosition(rebuildLayoutRecordsAfter);
            }
            for (int i = 0; i < childCount; i++)
            {
                final int position = mFirstPosition + i;
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                LayoutRecord rec = mLayoutRecords.get(position);
                if (rec == null)
                {
                    rec = new LayoutRecord();
                    mLayoutRecords.put(position, rec);
                }
                rec.column = lp.column;
                rec.height = child.getHeight();
                rec.id = lp.id;
                rec.span = Math.min(mColCount, lp.span);
            }
        }
    }

    final void invalidateLayoutRecordsBeforePosition(int position)
    {
        int endAt = 0;
        while (endAt < mLayoutRecords.size() && mLayoutRecords.keyAt(endAt) < position)
        {
            endAt++;
        }
        mLayoutRecords.removeAtRange(0, endAt);
    }

    final void invalidateLayoutRecordsAfterPosition(int position)
    {
        int beginAt = mLayoutRecords.size() - 1;
        while (beginAt >= 0 && mLayoutRecords.keyAt(beginAt) > position)
        {
            beginAt--;
        }
        beginAt++;
        mLayoutRecords.removeAtRange(beginAt + 1, mLayoutRecords.size() - beginAt);
    }

    /**
     * Should be called with mPopulating set to true
     * 
     * @param fromPosition
     *            Position to start filling from
     * @param overhang
     *            the number of extra pixels to fill beyond the current top edge
     * @return the max overhang beyond the beginning of the view of any added
     *         items at the top
     */
    final int fillUp(int fromPosition, int overhang)
    {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int itemMargin = mItemMargin;
        final int colWidth = (getWidth() - paddingLeft - paddingRight - itemMargin
                * (mColCount - 1))
                / mColCount;
        final int gridTop = getPaddingTop();
        final int fillTo = gridTop - overhang;
        int nextCol = getNextColumnUp();
        int position = fromPosition;

        while (nextCol >= 0 && mItemTops[nextCol] > fillTo && position >= 0)
        {
            final View child = obtainView(position, null);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (child.getParent() != this)
            {
                if (mInLayout)
                {
                    addViewInLayout(child, 0, lp);
                }
                else
                {
                    addView(child, 0);
                }
            }

            final int span = Math.min(mColCount, lp.span);
            final int widthSize = colWidth * span + itemMargin * (span - 1);
            final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

            LayoutRecord rec;
            if (span > 1)
            {
                rec = getNextRecordUp(position, span);
                nextCol = rec.column;
            }
            else
            {
                rec = mLayoutRecords.get(position);
            }

            boolean invalidateBefore = false;
            if (rec == null)
            {
                rec = new LayoutRecord();
                mLayoutRecords.put(position, rec);
                rec.column = nextCol;
                rec.span = span;
            }
            else if (span != rec.span)
            {
                rec.span = span;
                rec.column = nextCol;
                invalidateBefore = true;
            }
            else
            {
                nextCol = rec.column;
            }

            if (mHasStableIds)
            {
                final long id = mAdapter.getItemId(position);
                rec.id = id;
                lp.id = id;
            }

            lp.column = nextCol;

            final int heightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT)
            {
                heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            else
            {
                heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }
            child.measure(widthSpec, heightSpec);

            final int childHeight = child.getMeasuredHeight();
            if (invalidateBefore || (childHeight != rec.height && rec.height > 0))
            {
                invalidateLayoutRecordsBeforePosition(position);
            }
            rec.height = childHeight;

            final int startFrom;
            if (span > 1)
            {
                int highest = mItemTops[nextCol];
                for (int i = nextCol + 1; i < nextCol + span; i++)
                {
                    final int top = mItemTops[i];
                    if (top < highest)
                    {
                        highest = top;
                    }
                }
                startFrom = highest;
            }
            else
            {
                startFrom = mItemTops[nextCol];
            }
            final int childBottom = startFrom;
            final int childTop = childBottom - childHeight;
            final int childLeft = paddingLeft + nextCol * (colWidth + itemMargin);
            final int childRight = childLeft + child.getMeasuredWidth();
            child.layout(childLeft, childTop, childRight, childBottom);

            for (int i = nextCol; i < nextCol + span; i++)
            {
                mItemTops[i] = childTop - rec.getMarginAbove(i - nextCol) - itemMargin;
            }

            nextCol = getNextColumnUp();
            mFirstPosition = position--;
        }

        int highestView = getHeight();
        for (int i = 0; i < mColCount; i++)
        {
            if (mItemTops[i] < highestView)
            {
                highestView = mItemTops[i];
            }
        }
        return gridTop - highestView;
    }

    /**
     * Should be called with mPopulating set to true
     * 
     * @param fromPosition
     *            Position to start filling from
     * @param overhang
     *            the number of extra pixels to fill beyond the current bottom
     *            edge
     * @return the max overhang beyond the end of the view of any added items at
     *         the bottom
     */
    final int fillDown(int fromPosition, int overhang)
    {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int itemMargin = mItemMargin;
        final int colWidth = (getWidth() - paddingLeft - paddingRight - itemMargin
                * (mColCount - 1))
                / mColCount;
        final int gridBottom = getHeight() - getPaddingBottom();
        final int fillTo = gridBottom + overhang;
        int nextCol = getNextColumnDown();
        int position = fromPosition;

        while (nextCol >= 0 && mItemBottoms[nextCol] < fillTo && position < mItemCount)
        {
            final View child = obtainView(position, null);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (child.getParent() != this)
            {
                if (mInLayout)
                {
                    addViewInLayout(child, -1, lp);
                }
                else
                {
                    addView(child);
                }
            }

            final int span = Math.min(mColCount, lp.span);
            final int widthSize = colWidth * span + itemMargin * (span - 1);
            final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

            LayoutRecord rec;
            if (span > 1)
            {
                rec = getNextRecordDown(position, span);
                nextCol = rec.column;
            }
            else
            {
                rec = mLayoutRecords.get(position);
            }

            boolean invalidateAfter = false;
            if (rec == null)
            {
                rec = new LayoutRecord();
                mLayoutRecords.put(position, rec);
                rec.column = nextCol;
                rec.span = span;
            }
            else if (span != rec.span)
            {
                rec.span = span;
                rec.column = nextCol;
                invalidateAfter = true;
            }
            else
            {
                nextCol = rec.column;
            }

            if (mHasStableIds)
            {
                final long id = mAdapter.getItemId(position);
                rec.id = id;
                lp.id = id;
            }

            lp.column = nextCol;

            final int heightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT)
            {
                heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
            else
            {
                heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }
            child.measure(widthSpec, heightSpec);

            final int childHeight = child.getMeasuredHeight();
            if (invalidateAfter || (childHeight != rec.height && rec.height > 0))
            {
                invalidateLayoutRecordsAfterPosition(position);
            }
            rec.height = childHeight;

            final int startFrom;
            if (span > 1)
            {
                int lowest = mItemBottoms[nextCol];
                for (int i = nextCol + 1; i < nextCol + span; i++)
                {
                    final int bottom = mItemBottoms[i];
                    if (bottom > lowest)
                    {
                        lowest = bottom;
                    }
                }
                startFrom = lowest;
            }
            else
            {
                startFrom = mItemBottoms[nextCol];
            }
            final int childTop = startFrom + itemMargin;
            final int childBottom = childTop + childHeight;
            final int childLeft = paddingLeft + nextCol * (colWidth + itemMargin);
            final int childRight = childLeft + child.getMeasuredWidth();
            child.layout(childLeft, childTop, childRight, childBottom);

            for (int i = nextCol; i < nextCol + span; i++)
            {
                mItemBottoms[i] = childBottom + rec.getMarginBelow(i - nextCol);
            }

            nextCol = getNextColumnDown();
            position++;
        }

        int lowestView = 0;
        for (int i = 0; i < mColCount; i++)
        {
            if (mItemBottoms[i] > lowestView)
            {
                lowestView = mItemBottoms[i];
            }
        }
        return lowestView - gridBottom;
    }

    /**
     * @return column that the next view filling upwards should occupy. This is
     *         the bottom-most position available for a single-column item.
     */
    final int getNextColumnUp()
    {
        int result = -1;
        int bottomMost = Integer.MIN_VALUE;

        final int colCount = mColCount;
        for (int i = colCount - 1; i >= 0; i--)
        {
            final int top = mItemTops[i];
            if (top > bottomMost)
            {
                bottomMost = top;
                result = i;
            }
        }
        return result;
    }

    /**
     * Return a LayoutRecord for the given position
     * 
     * @param position
     * @param span
     * @return
     */
    final LayoutRecord getNextRecordUp(int position, int span)
    {
        LayoutRecord rec = mLayoutRecords.get(position);
        if (rec == null)
        {
            rec = new LayoutRecord();
            rec.span = span;
            mLayoutRecords.put(position, rec);
        }
        else if (rec.span != span)
        {
            throw new IllegalStateException("Invalid LayoutRecord! Record had span=" + rec.span
                    + " but caller requested span=" + span + " for position=" + position);
        }
        int targetCol = -1;
        int bottomMost = Integer.MIN_VALUE;

        final int colCount = mColCount;
        for (int i = colCount - span; i >= 0; i--)
        {
            int top = Integer.MAX_VALUE;
            for (int j = i; j < i + span; j++)
            {
                final int singleTop = mItemTops[j];
                if (singleTop < top)
                {
                    top = singleTop;
                }
            }
            if (top > bottomMost)
            {
                bottomMost = top;
                targetCol = i;
            }
        }

        rec.column = targetCol;

        for (int i = 0; i < span; i++)
        {
            rec.setMarginBelow(i, mItemTops[i + targetCol] - bottomMost);
        }

        return rec;
    }

    /**
     * @return column that the next view filling downwards should occupy. This
     *         is the top-most position available.
     */
    final int getNextColumnDown()
    {
        int result = -1;
        int topMost = Integer.MAX_VALUE;

        final int colCount = mColCount;
        for (int i = 0; i < colCount; i++)
        {
            final int bottom = mItemBottoms[i];
            if (bottom < topMost)
            {
                topMost = bottom;
                result = i;
            }
        }
        return result;
    }

    final LayoutRecord getNextRecordDown(int position, int span)
    {
        LayoutRecord rec = mLayoutRecords.get(position);
        if (rec == null)
        {
            rec = new LayoutRecord();
            rec.span = span;
            mLayoutRecords.put(position, rec);
        }
        else if (rec.span != span)
        {
            throw new IllegalStateException("Invalid LayoutRecord! Record had span=" + rec.span
                    + " but caller requested span=" + span + " for position=" + position);
        }
        int targetCol = -1;
        int topMost = Integer.MAX_VALUE;

        final int colCount = mColCount;
        for (int i = 0; i <= colCount - span; i++)
        {
            int bottom = Integer.MIN_VALUE;
            for (int j = i; j < i + span; j++)
            {
                final int singleBottom = mItemBottoms[j];
                if (singleBottom > bottom)
                {
                    bottom = singleBottom;
                }
            }
            if (bottom < topMost)
            {
                topMost = bottom;
                targetCol = i;
            }
        }

        rec.column = targetCol;

        for (int i = 0; i < span; i++)
        {
            rec.setMarginAbove(i, topMost - mItemBottoms[i + targetCol]);
        }

        return rec;
    }

    /**
     * Obtain a populated view from the adapter. If optScrap is non-null and is
     * not reused it will be placed in the recycle bin.
     * 
     * @param position
     *            position to get view for
     * @param optScrap
     *            Optional scrap view; will be reused if possible
     * @return A new view, a recycled view from mRecycler, or optScrap
     */
    final View obtainView(int position, View optScrap)
    {
        View view = mRecycler.getTransientStateView(position);
        if (view != null)
        {
            return view;
        }

        // Reuse optScrap if it's of the right type (and not null)
        final int optType = optScrap != null ? ((LayoutParams) optScrap.getLayoutParams()).viewType
                : -1;
        final int positionViewType = mAdapter.getItemViewType(position);
        final View scrap = optType == positionViewType ? optScrap : mRecycler
                .getScrapView(positionViewType);

        view = mAdapter.getView(position, scrap, this);

        if (view != scrap && scrap != null)
        {
            // The adapter didn't use it; put it back.
            mRecycler.addScrap(scrap);
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();

        if (view.getParent() != this)
        {
            if (lp == null)
            {
                lp = generateDefaultLayoutParams();
            }
            else if (!checkLayoutParams(lp))
            {
                lp = generateLayoutParams(lp);
            }
        }

        final LayoutParams sglp = (LayoutParams) lp;
        sglp.position = position;
        sglp.viewType = positionViewType;

        return view;
    }

    public ListAdapter getAdapter()
    {
        return mAdapter;
    }

    public void setAdapter(ListAdapter adapter)
    {
        if (mAdapter != null)
        {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        // TODO: If the new adapter says that there are stable IDs, remove
        // certain layout records
        // and onscreen views if they have changed instead of removing all of
        // the state here.
        clearAllState();
        mAdapter = adapter;
        mDataChanged = true;
        mOldItemCount = mItemCount = adapter != null ? adapter.getCount() : 0;
        if (adapter != null)
        {
            adapter.registerDataSetObserver(mObserver);
            mRecycler.setViewTypeCount(adapter.getViewTypeCount());
            mHasStableIds = adapter.hasStableIds();
        }
        else
        {
            mHasStableIds = false;
        }
        populate();
    }

    /**
     * Clear all state because the grid will be used for a completely different
     * set of data.
     */
    private void clearAllState()
    {
        // Clear all layout records and views
        mLayoutRecords.clear();
        removeAllViews();

        // Reset to the top of the grid
        resetStateForGridTop();

        // Clear recycler because there could be different view types now
        mRecycler.clear();
    }

    /**
     * Reset all internal state to be at the top of the grid.
     */
    private void resetStateForGridTop()
    {
        // Reset mItemTops and mItemBottoms
        final int colCount = mColCount;
        if (mItemTops == null || mItemTops.length != colCount)
        {
            mItemTops = new int[colCount];
            mItemBottoms = new int[colCount];
        }
        final int top = getPaddingTop();
        Arrays.fill(mItemTops, top);
        Arrays.fill(mItemBottoms, top);

        // Reset the first visible position in the grid to be item 0
        mFirstPosition = 0;
        mRestoreOffset = 0;
    }

    /**
     * Scroll the list so the first visible position in the grid is the first
     * item in the adapter.
     */
    public void setSelectionToTop()
    {
        // Clear out the views (but don't clear out the layout records or
        // recycler because the data
        // has not changed)
        removeAllViews();

        // Reset to top of grid
        resetStateForGridTop();

        // Start populating again
        populate();
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams()
    {
        return new LayoutParams(LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp)
    {
        return new LayoutParams(lp);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams lp)
    {
        return lp instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs)
    {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);
        final int position = mFirstPosition;
        ss.position = position;
        if (position >= 0 && mAdapter != null && position < mAdapter.getCount())
        {
            ss.firstId = mAdapter.getItemId(position);
        }
        if (getChildCount() > 0)
        {
            ss.topOffset = getChildAt(0).getTop() - mItemMargin - getPaddingTop();
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mDataChanged = true;
        mFirstPosition = ss.position;
        mRestoreOffset = ss.topOffset;
        requestLayout();
    }

    public static class LayoutParams extends ViewGroup.LayoutParams
    {
        private static final int[] LAYOUT_ATTRS = new int[] { android.R.attr.layout_span };

        private static final int SPAN_INDEX = 0;

        /**
         * The number of columns this item should span
         */
        public int span = 1;

        /**
         * Item position this view represents
         */
        int position;

        /**
         * Type of this view as reported by the adapter
         */
        int viewType;

        /**
         * The column this view is occupying
         */
        int column;

        /**
         * The stable ID of the item this view displays
         */
        long id = -1;

        public LayoutParams(int height)
        {
            super(MATCH_PARENT, height);

            if (this.height == MATCH_PARENT)
            {
                Log.w(TAG, "Constructing LayoutParams with height FILL_PARENT - "
                        + "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs)
        {
            super(c, attrs);

            if (this.width != MATCH_PARENT)
            {
                Log.w(TAG, "Inflation setting LayoutParams width to " + this.width
                        + " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT)
            {
                Log.w(TAG, "Inflation setting LayoutParams height to MATCH_PARENT - "
                        + "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }

            TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            span = a.getInteger(SPAN_INDEX, 1);
            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other)
        {
            super(other);

            if (this.width != MATCH_PARENT)
            {
                Log.w(TAG, "Constructing LayoutParams with width " + this.width
                        + " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT)
            {
                Log.w(TAG, "Constructing LayoutParams with height MATCH_PARENT - "
                        + "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }
    }

    private class RecycleBin
    {
        private ArrayList<View>[] mScrapViews;
        private int mViewTypeCount;
        private int mMaxScrap;

        private SparseArray<View> mTransientStateViews;

        public void setViewTypeCount(int viewTypeCount)
        {
            if (viewTypeCount < 1)
            {
                throw new IllegalArgumentException("Must have at least one view type ("
                        + viewTypeCount + " types reported)");
            }
            if (viewTypeCount == mViewTypeCount)
            {
                return;
            }

            ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++)
            {
                scrapViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mScrapViews = scrapViews;
        }

        public void clear()
        {
            final int typeCount = mViewTypeCount;
            for (int i = 0; i < typeCount; i++)
            {
                mScrapViews[i].clear();
            }
            if (mTransientStateViews != null)
            {
                mTransientStateViews.clear();
            }
        }

        public void clearTransientViews()
        {
            if (mTransientStateViews != null)
            {
                mTransientStateViews.clear();
            }
        }

        public void addScrap(View v)
        {
            final LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (ViewCompat.hasTransientState(v))
            {
                if (mTransientStateViews == null)
                {
                    mTransientStateViews = new SparseArray<View>();
                }
                mTransientStateViews.put(lp.position, v);
                return;
            }

            final int childCount = getChildCount();
            if (childCount > mMaxScrap)
            {
                mMaxScrap = childCount;
            }

            ArrayList<View> scrap = mScrapViews[lp.viewType];
            if (scrap.size() < mMaxScrap)
            {
                scrap.add(v);
            }
        }

        public View getTransientStateView(int position)
        {
            if (mTransientStateViews == null)
            {
                return null;
            }

            final View result = mTransientStateViews.get(position);
            if (result != null)
            {
                mTransientStateViews.remove(position);
            }
            return result;
        }

        public View getScrapView(int type)
        {
            ArrayList<View> scrap = mScrapViews[type];
            if (scrap.isEmpty())
            {
                return null;
            }

            final int index = scrap.size() - 1;
            final View result = scrap.get(index);
            scrap.remove(index);
            return result;
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver
    {
        @Override
        public void onChanged()
        {
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();

            // TODO: Consider matching these back up if we have stable IDs.
            mRecycler.clearTransientViews();

            if (!mHasStableIds)
            {
                // Clear all layout records and recycle the views
                mLayoutRecords.clear();
                recycleAllViews();

                // Reset item bottoms to be equal to item tops
                final int colCount = mColCount;
                for (int i = 0; i < colCount; i++)
                {
                    mItemBottoms[i] = mItemTops[i];
                }
            }

            // TODO: consider repopulating in a deferred runnable instead
            // (so that successive changes may still be batched)
            requestLayout();
        }

        @Override
        public void onInvalidated()
        {
        }
    }

    static class SavedState extends BaseSavedState
    {
        long firstId = -1;
        int position;
        int topOffset;

        SavedState(Parcelable superState)
        {
            super(superState);
        }

        private SavedState(Parcel in)
        {
            super(in);
            firstId = in.readLong();
            position = in.readInt();
            topOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeLong(firstId);
            out.writeInt(position);
            out.writeInt(topOffset);
        }

        @Override
        public String toString()
        {
            return "StaggereGridView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this)) + " firstId=" + firstId
                    + " position=" + position + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }

	public void setOnScrollListener(OnScrollListener scrollListener) {
		mOnScrollListener = scrollListener;
		invokeOnItemScrollListener();
	}
	
	void reportScrollStateChange(int newState) {
		if (newState != mTouchMode) {
			mTouchMode = newState;
			if (mOnScrollListener != null) {
				mOnScrollListener.onScrollStateChanged(this, newState);
			}
		}
	}
	
	void invokeOnItemScrollListener() {

		if (mOnScrollListener != null) {
			mOnScrollListener.onScroll(this, mFirstPosition, getChildCount(), mItemCount);
		}

	}
	
	 /**
     * Interface definition for a callback to be invoked when the list or grid
     * has been scrolled.
     */
    public interface OnScrollListener {

        /**
         * The view is not scrolling. Note navigating the list using the trackball counts as
         * being in the idle state since these transitions are not animated.
         */
        public static int SCROLL_STATE_IDLE = 0;

        /**
         * The user is scrolling using touch, and their finger is still on the screen
         */
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;

        /**
         * The user had previously been scrolling using touch and had performed a fling. The
         * animation is now coasting to a stop
         */
        public static int SCROLL_STATE_FLING = 2;

        /**
         * Callback method to be invoked while the list view or grid view is being scrolled. If the
         * view is being scrolled, this method will be called before the next frame of the scroll is
         * rendered. In particular, it will be called before any calls to
         * {@link Adapter#getView(int, View, ViewGroup)}.
         *
         * @param view The view whose scroll state is being reported
         *
         * @param scrollState The current scroll state. One of {@link #SCROLL_STATE_IDLE},
         * {@link #SCROLL_STATE_TOUCH_SCROLL} or {@link #SCROLL_STATE_IDLE}.
         */
        public void onScrollStateChanged(ViewGroup view, int scrollState);

        /**
         * Callback method to be invoked when the list or grid has been scrolled. This will be
         * called after the scroll has completed
         * @param view The view whose scroll state is being reported
         * @param firstVisibleItem the index of the first visible cell (ignore if
         *        visibleItemCount == 0)
         * @param visibleItemCount the number of visible cells
         * @param totalItemCount the number of items in the list adaptor
         */
        public void onScroll(ViewGroup view, int firstVisibleItem, int visibleItemCount, int totalItemCount);
    }



}