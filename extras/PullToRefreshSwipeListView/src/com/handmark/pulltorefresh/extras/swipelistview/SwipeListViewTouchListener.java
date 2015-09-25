/*
 * Copyright (C) 2013 47 Degrees, LLC
 * http://47deg.com
 * hello@47deg.com
 *
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.handmark.pulltorefresh.extras.swipelistview;

import static com.nineoldandroids.view.ViewHelper.setAlpha;
import static com.nineoldandroids.view.ViewHelper.setTranslationX;
import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

/**
 * Touch listener impl for the SwipeListView
 */
public class SwipeListViewTouchListener implements View.OnTouchListener {

    private static final int DISPLACE_CHOICE = 80;

    private int swipeMode = SwipeListView.SWIPE_MODE_BOTH;
    private boolean swipeOpenOnLongPress = true;
    private boolean swipeClosesAllItemsWhenListMoves = true;

    private int swipeFrontView = 0;
    private int swipeBackView = 0;

    private Rect rect = new Rect();

    // Cached ViewConfiguration and system-wide constant values
    private int slop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private long configShortAnimationTime;
    private long animationTime;

    private float leftOffset = 0;
    private float rightOffset = 0;

    private int swipeDrawableChecked = 0;
    private int swipeDrawableUnchecked = 0;

    // Fixed properties
    private SwipeListView swipeListView;
    private int viewWidth = 1; // 1 and not 0 to prevent dividing by zero

    private List<PendingDismissData> pendingDismisses = new ArrayList<PendingDismissData>();
    private int dismissAnimationRefCount = 0;

    private float downX;
    private boolean swiping;
    private boolean swipingRight;
    private VelocityTracker velocityTracker;
    private int downPosition;
    private View parentView;
    private View frontView;
    private View backView;
    private boolean paused;

    private int swipeCurrentAction = SwipeListView.SWIPE_ACTION_NONE;

    private int swipeActionLeft = SwipeListView.SWIPE_ACTION_REVEAL;
    private int swipeActionRight = SwipeListView.SWIPE_ACTION_REVEAL;

    private List<Boolean> opened = new ArrayList<Boolean>();
    private List<Boolean> openedRight = new ArrayList<Boolean>();
    private boolean listViewMoving;
    private List<Boolean> checked = new ArrayList<Boolean>();
    private int oldSwipeActionRight;
    private int oldSwipeActionLeft;

    /**
     * Constructor
     *
     * @param swipeListView  SwipeListView
     * @param swipeFrontView front view Identifier
     * @param swipeBackView  back view Identifier
     */
    public SwipeListViewTouchListener(SwipeListView swipeListView, int swipeFrontView, int swipeBackView) {
        this.swipeFrontView = swipeFrontView;
        this.swipeBackView = swipeBackView;
        ViewConfiguration vc = ViewConfiguration.get(swipeListView.getContext());
        slop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        configShortAnimationTime = swipeListView.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        animationTime = configShortAnimationTime;
        this.swipeListView = swipeListView;
    }

    /**
     * Sets current item's parent view
     *
     * @param parentView Parent view
     */
    private void setParentView(View parentView) {
        this.parentView = parentView;
    }

    /**
     * Sets current item's front view
     *
     * @param frontView Front view
     */
    private void setFrontView(View frontView, final int childPosition) {
        this.frontView = frontView;
        frontView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeListView.onClickFrontView(downPosition);
            }
        });

        frontView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (swipeOpenOnLongPress) {
                    if (downPosition >= 0) {
                        openAnimate(childPosition);
                    }
                } else {
                    swapChoiceState(childPosition);
                }
                return false;
            }

        });
}

    /**
     * Set current item's back view
     *
     * @param backView
     */
    private void setBackView(View backView) {
        this.backView = backView;
        backView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeListView.onClickBackView(downPosition);
            }
        });
    }

    /**
     * @return true if the list is in motion
     */
    public boolean isListViewMoving() {
        return listViewMoving;
    }

    /**
     * Sets animation time when the user drops the cell
     *
     * @param animationTime milliseconds
     */
    public void setAnimationTime(long animationTime) {
        if (animationTime > 0) {
            this.animationTime = animationTime;
        } else {
            this.animationTime = configShortAnimationTime;
        }
    }

    /**
     * Sets the right offset
     *
     * @param rightOffset Offset
     */
    public void setRightOffset(float rightOffset) {
        this.rightOffset = rightOffset;
    }

    /**
     * Set the left offset
     *
     * @param leftOffset Offset
     */
    public void setLeftOffset(float leftOffset) {
        this.leftOffset = leftOffset;
    }

    /**
     * Set if all item opened will be close when the user move ListView
     *
     * @param swipeClosesAllItemsWhenListMoves
     */
    public void setSwipeClosesAllItemsWhenListMoves(boolean swipeClosesAllItemsWhenListMoves) {
        this.swipeClosesAllItemsWhenListMoves = swipeClosesAllItemsWhenListMoves;
    }

    /**
     * Set if the user can open an item with long press on cell
     *
     * @param swipeOpenOnLongPress
     */
    public void setSwipeOpenOnLongPress(boolean swipeOpenOnLongPress) {
        this.swipeOpenOnLongPress = swipeOpenOnLongPress;
    }

    /**
     * Sets the swipe mode
     *
     * @param swipeMode
     */
    public void setSwipeMode(int swipeMode) {
        this.swipeMode = swipeMode;
    }

    /**
     * Check is swiping is enabled
     *
     * @return
     */
    protected boolean isSwipeEnabled() {
        return swipeMode != SwipeListView.SWIPE_MODE_NONE;
    }

    /**
     * Return action on left
     *
     * @return Action
     */
    public int getSwipeActionLeft() {
        return swipeActionLeft;
    }

    /**
     * Set action on left
     *
     * @param swipeActionLeft Action
     */
    public void setSwipeActionLeft(int swipeActionLeft) {
        this.swipeActionLeft = swipeActionLeft;
    }

    /**
     * Return action on right
     *
     * @return Action
     */
    public int getSwipeActionRight() {
        return swipeActionRight;
    }

    /**
     * Set action on right
     *
     * @param swipeActionRight Action
     */
    public void setSwipeActionRight(int swipeActionRight) {
        this.swipeActionRight = swipeActionRight;
    }

    /**
     * Set drawable checked (only SWIPE_ACTION_CHOICE)
     *
     * @param swipeDrawableChecked drawable
     */
    protected void setSwipeDrawableChecked(int swipeDrawableChecked) {
        this.swipeDrawableChecked = swipeDrawableChecked;
    }

    /**
     * Set drawable unchecked (only SWIPE_ACTION_CHOICE)
     *
     * @param swipeDrawableUnchecked drawable
     */
    protected void setSwipeDrawableUnchecked(int swipeDrawableUnchecked) {
        this.swipeDrawableUnchecked = swipeDrawableUnchecked;
    }

    /**
     * Adds new items when adapter is modified
     */
    public void resetItems() {
        if (swipeListView.getAdapter() != null) {
            int count = swipeListView.getAdapter().getCount();
            for (int i = opened.size(); i <= count; i++) {
                opened.add(false);
                openedRight.add(false);
                checked.add(false);
            }
        }
    }

    /**
     * Open item
     *
     * @param position Position of list
     */
    protected void openAnimate(int position) {
        final View child = swipeListView.getChildAt(position - swipeListView.getFirstVisiblePosition()).findViewById(swipeFrontView);

        if (child != null) {
            openAnimate(child, position);
        }
    }

    /**
     * Close item
     *
     * @param position Position of list
     */
    protected void closeAnimate(int position) {
        if (swipeListView != null) {
            int firstVisibleChildPosition = swipeListView.getFirstVisiblePosition();
            final View childContainer = swipeListView.getChildAt(position - firstVisibleChildPosition);
            if (childContainer != null) {
                final View child = childContainer.findViewById(swipeFrontView);

                if (child != null) {
                    closeAnimate(child, position);
                }
            }
        }
    }

    /**
     * Swap choice state in item
     *
     * @param position position of list
     */
    private void swapChoiceState(int position) {
        int lastCount = getCountSelected();
        boolean lastChecked = checked.get(position);
        checked.set(position, !lastChecked);
        int count = lastChecked ? lastCount - 1 : lastCount + 1;
        if (lastCount == 0 && count == 1) {
            swipeListView.onChoiceStarted();
            closeOpenedItems();
            setActionsTo(SwipeListView.SWIPE_ACTION_CHOICE);
        }
        if (lastCount == 1 && count == 0) {
            swipeListView.onChoiceEnded();
            returnOldActions();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            swipeListView.setItemChecked(position, !lastChecked);
        }
        swipeListView.onChoiceChanged(position, !lastChecked);
        reloadChoiceStateInView(frontView, position);
    }

    /**
     * Unselected choice state in item
     */
    protected void unselectedChoiceStates() {
        int start = swipeListView.getFirstVisiblePosition();
        int end = swipeListView.getLastVisiblePosition();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i) && i >= start && i <= end) {
                reloadChoiceStateInView(swipeListView.getChildAt(i - start).findViewById(swipeFrontView), i);
            }
            checked.set(i, false);
        }
        swipeListView.onChoiceEnded();
        returnOldActions();
    }

    /**
     * Dismiss an item.
     * @param position is the position of the item to delete.
     * @return 0 if the item is not visible. Otherwise return the height of the cell to dismiss.
     */
    protected int dismiss(int position) {
        opened.remove(position);
        checked.remove(position);
        int start = swipeListView.getFirstVisiblePosition();
        int end = swipeListView.getLastVisiblePosition();
        View view = swipeListView.getChildAt(position - start);
        ++dismissAnimationRefCount;
        if (position >= start && position <= end) {
            performDismiss(view, position, false);
            return view.getHeight();
        } else {
            pendingDismisses.add(new PendingDismissData(position, null));
            return 0;
        }
    }

    /**
     * Draw cell for display if item is selected or not
     *
     * @param frontView view to draw
     * @param position  position in list
     */
    protected void reloadChoiceStateInView(View frontView, int position) {
        if (isChecked(position)) {
            if (swipeDrawableChecked > 0) frontView.setBackgroundResource(swipeDrawableChecked);
        } else {
            if (swipeDrawableUnchecked > 0) frontView.setBackgroundResource(swipeDrawableUnchecked);
        }
    }

    /**
     * Reset the state of front view when the it's recycled by ListView
     *
     * @param frontView view to re-draw
     */
    protected void reloadSwipeStateInView(View frontView, int position) {
        if (!opened.get(position)) {
            setTranslationX(frontView, 0.0f);
        } else {
            if (openedRight.get(position)) {
                setTranslationX(frontView, swipeListView.getWidth());
            } else {
                setTranslationX(frontView, -swipeListView.getWidth());
            }
        }

    }

    /**
     * Get if item is selected
     *
     * @param position position in list
     * @return
     */
    protected boolean isChecked(int position) {
        return position < checked.size() && checked.get(position);
    }

    /**
     * Count selected
     *
     * @return
     */
    protected int getCountSelected() {
        int count = 0;
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i)) {
                count++;
            }
        }
        if(SwipeListView.DEBUG){
            Log.d(SwipeListView.TAG, "selected: " + count);
        }
        return count;
    }

    /**
     * Get positions selected
     *
     * @return
     */
    protected List<Integer> getPositionsSelected() {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i)) {
                list.add(i);
            }
        }
        return list;
    }

    /**
     * Open item
     *
     * @param view     affected view
     * @param position Position of list
     */
    private void openAnimate(View view, int position) {
        if (!opened.get(position)) {
            generateRevealAnimate(view, true, false, position);
        }
    }

    /**
     * Close item
     *
     * @param view     affected view
     * @param position Position of list
     */
    private void closeAnimate(View view, int position) {
        if (opened.get(position)) {
            generateRevealAnimate(view, true, false, position);
        }
    }

    /**
     * Create animation
     *
     * @param view      affected view
     * @param swap      If state should change. If "false" returns to the original position
     * @param swapRight If swap is true, this parameter tells if move is to the right or left
     * @param position  Position of list
     */
    private void generateAnimate(final View view, final boolean swap, final boolean swapRight, final int position) {
        if(SwipeListView.DEBUG){
            Log.d(SwipeListView.TAG, "swap: " + swap + " - swapRight: " + swapRight + " - position: " + position);
        }
        if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_REVEAL) {
            generateRevealAnimate(view, swap, swapRight, position);
        }
        if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_DISMISS) {
            generateDismissAnimate(parentView, swap, swapRight, position);
        }
        if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_CHOICE) {
            generateChoiceAnimate(view, position);
        }
    }

    /**
     * Create choice animation
     *
     * @param view     affected view
     * @param position list position
     */
    private void generateChoiceAnimate(final View view, final int position) {
        animate(view)
                .translationX(0)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        swipeListView.resetScrolling();
                        resetCell();
                    }
                });
    }

    /**
     * Create dismiss animation
     *
     * @param view      affected view
     * @param swap      If will change state. If is "false" returns to the original position
     * @param swapRight If swap is true, this parameter tells if move is to the right or left
     * @param position  Position of list
     */
    private void generateDismissAnimate(final View view, final boolean swap, final boolean swapRight, final int position) {
        int moveTo = 0;
        if (opened.get(position)) {
            if (!swap) {
                moveTo = openedRight.get(position) ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        } else {
            if (swap) {
                moveTo = swapRight ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        }

        int alpha = 1;
        if (swap) {
            ++dismissAnimationRefCount;
            alpha = 0;
        }

        animate(view)
                .translationX(moveTo)
                .alpha(alpha)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (swap) {
                            closeOpenedItems();
                            performDismiss(view, position, true);
                        }
                        resetCell();
                    }
                });

    }

    /**
     * Create reveal animation
     *
     * @param view      affected view
     * @param swap      If will change state. If "false" returns to the original position
     * @param swapRight If swap is true, this parameter tells if movement is toward right or left
     * @param position  list position
     */
    private void generateRevealAnimate(final View view, final boolean swap, final boolean swapRight, final int position) {
        int moveTo = 0;
        if (opened.get(position)) {
            if (!swap) {
                moveTo = openedRight.get(position) ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        } else {
            if (swap) {
                moveTo = swapRight ? (int) (viewWidth - rightOffset) : (int) (-viewWidth + leftOffset);
            }
        }

        animate(view)
                .translationX(moveTo)
                .setDuration(animationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        swipeListView.resetScrolling();
                        if (swap) {
                            boolean aux = !opened.get(position);
                            opened.set(position, aux);
                            if (aux) {
                                swipeListView.onOpened(position, swapRight);
                                openedRight.set(position, swapRight);
                            } else {
                                swipeListView.onClosed(position, openedRight.get(position));
                            }
                        }
                        resetCell();
                    }
                });
    }

    private void resetCell() {
        if (downPosition != ListView.INVALID_POSITION) {
            if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_CHOICE) {
                backView.setVisibility(View.VISIBLE);
            }
            frontView.setClickable(opened.get(downPosition));
            frontView.setLongClickable(opened.get(downPosition));
            frontView = null;
            backView = null;
            downPosition = ListView.INVALID_POSITION;
        }
    }

    /**
     * Set enabled
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        paused = !enabled;
    }

    /**
     * Return ScrollListener for ListView
     *
     * @return OnScrollListener
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {

            private boolean isFirstItem = false;
            private boolean isLastItem = false;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                if (swipeClosesAllItemsWhenListMoves && scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    closeOpenedItems();
                }
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    listViewMoving = true;
                    setEnabled(false);
                }
                if (scrollState != AbsListView.OnScrollListener.SCROLL_STATE_FLING && scrollState != SCROLL_STATE_TOUCH_SCROLL) {
                    listViewMoving = false;
                    downPosition = ListView.INVALID_POSITION;
                    swipeListView.resetScrolling();
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            setEnabled(true);
                        }
                    }, 500);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (isFirstItem) {
                    boolean onSecondItemList = firstVisibleItem == 1;
                    if (onSecondItemList) {
                        isFirstItem = false;
                    }
                } else {
                    boolean onFirstItemList = firstVisibleItem == 0;
                    if (onFirstItemList) {
                        isFirstItem = true;
                        swipeListView.onFirstListItem();
                    }
                }
                if (isLastItem) {
                    boolean onBeforeLastItemList = firstVisibleItem + visibleItemCount == totalItemCount - 1;
                    if (onBeforeLastItemList) {
                        isLastItem = false;
                    }
                } else {
                    boolean onLastItemList = firstVisibleItem + visibleItemCount >= totalItemCount;
                    if (onLastItemList) {
                        isLastItem = true;
                        swipeListView.onLastListItem();
                    }
                }
            }
        };
    }

    /**
     * Close all opened items
     */
    void closeOpenedItems() {
        if (opened != null) {
            int start = swipeListView.getFirstVisiblePosition();
            int end = swipeListView.getLastVisiblePosition();
            for (int i = start; i <= end; i++) {
                if (opened.get(i)) {
                    closeAnimate(swipeListView.getChildAt(i - start).findViewById(swipeFrontView), i);
                }
            }
        }

    }

    /**
     * @see View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!isSwipeEnabled()) {
            return false;
        }

        if (viewWidth < 2) {
            viewWidth = swipeListView.getWidth();
        }

        switch (MotionEventCompat.getActionMasked(motionEvent)) {
            case MotionEvent.ACTION_DOWN: {
                if (paused && downPosition != ListView.INVALID_POSITION) {
                    return false;
                }
                swipeCurrentAction = SwipeListView.SWIPE_ACTION_NONE;

                int childCount = swipeListView.getChildCount();
                int[] listViewCoords = new int[2];
                swipeListView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = swipeListView.getChildAt(i);
                    child.getHitRect(rect);

                    int childPosition = swipeListView.getPositionForView(child);

                    // dont allow swiping if this is on the header or footer or IGNORE_ITEM_VIEW_TYPE or enabled is false on the adapter
                    boolean allowSwipe = swipeListView.getAdapter().isEnabled(childPosition) && swipeListView.getAdapter().getItemViewType(childPosition) >= 0;

                    if (allowSwipe && rect.contains(x, y)) {
                        setParentView(child);
                        setFrontView(child.findViewById(swipeFrontView), childPosition);

                        downX = motionEvent.getRawX();
                        downPosition = childPosition;

                        frontView.setClickable(!opened.get(downPosition));
                        frontView.setLongClickable(!opened.get(downPosition));

                        velocityTracker = VelocityTracker.obtain();
                        velocityTracker.addMovement(motionEvent);
                        if (swipeBackView > 0) {
                            setBackView(child.findViewById(swipeBackView));
                        }
                        break;
                    }
                }
                view.onTouchEvent(motionEvent);
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if (velocityTracker == null || !swiping || downPosition == ListView.INVALID_POSITION) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - downX;
                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(velocityTracker.getXVelocity());
                if (!opened.get(downPosition)) {
                    if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && velocityTracker.getXVelocity() > 0) {
                        velocityX = 0;
                    }
                    if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && velocityTracker.getXVelocity() < 0) {
                        velocityX = 0;
                    }
                }
                float velocityY = Math.abs(velocityTracker.getYVelocity());
                boolean swap = false;
                boolean swapRight = false;
                if (minFlingVelocity <= velocityX && velocityX <= maxFlingVelocity && velocityY * 2 < velocityX) {
                    swapRight = velocityTracker.getXVelocity() > 0;
                    if(SwipeListView.DEBUG){
                        Log.d(SwipeListView.TAG, "swapRight: " + swapRight + " - swipingRight: " + swipingRight);
                    }
                    if (swapRight != swipingRight && swipeActionLeft != swipeActionRight) {
                        swap = false;
                    } else if (opened.get(downPosition) && openedRight.get(downPosition) && swapRight) {
                        swap = false;
                    } else if (opened.get(downPosition) && !openedRight.get(downPosition) && !swapRight) {
                        swap = false;
                    } else {
                        swap = true;
                    }
                } else if (Math.abs(deltaX) > viewWidth / 2) {
                    swap = true;
                    swapRight = deltaX > 0;
                }


                generateAnimate(frontView, swap, swapRight, downPosition);
                if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_CHOICE) {
                    swapChoiceState(downPosition);
                }

                velocityTracker.recycle();
                velocityTracker = null;
                downX = 0;
                // change clickable front view
//                if (swap) {
//                    frontView.setClickable(opened.get(downPosition));
//                    frontView.setLongClickable(opened.get(downPosition));
//                }
                swiping = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (velocityTracker == null || paused || downPosition == ListView.INVALID_POSITION) {
                    break;
                }

                velocityTracker.addMovement(motionEvent);
                velocityTracker.computeCurrentVelocity(1000);
                float velocityX = Math.abs(velocityTracker.getXVelocity());
                float velocityY = Math.abs(velocityTracker.getYVelocity());

                float deltaX = motionEvent.getRawX() - downX;
                float deltaMode = Math.abs(deltaX);

                int swipeMode = this.swipeMode;
                int changeSwipeMode = swipeListView.changeSwipeMode(downPosition);
                if (changeSwipeMode >= 0) {
                    swipeMode = changeSwipeMode;
                }

                if (swipeMode == SwipeListView.SWIPE_MODE_NONE) {
                    deltaMode = 0;
                } else if (swipeMode != SwipeListView.SWIPE_MODE_BOTH) {
                    if (opened.get(downPosition)) {
                        if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && deltaX < 0) {
                            deltaMode = 0;
                        } else if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && deltaX > 0) {
                            deltaMode = 0;
                        }
                    } else {
                        if (swipeMode == SwipeListView.SWIPE_MODE_LEFT && deltaX > 0) {
                            deltaMode = 0;
                        } else if (swipeMode == SwipeListView.SWIPE_MODE_RIGHT && deltaX < 0) {
                            deltaMode = 0;
                        }
                    }
                }
                if (deltaMode > slop && swipeCurrentAction == SwipeListView.SWIPE_ACTION_NONE && velocityY < velocityX) {
                    swiping = true;
                    swipingRight = (deltaX > 0);
                    if(SwipeListView.DEBUG){
                        Log.d(SwipeListView.TAG, "deltaX: " + deltaX + " - swipingRight: " + swipingRight);
                    }
                    if (opened.get(downPosition)) {
                        swipeListView.onStartClose(downPosition, swipingRight);
                        swipeCurrentAction = SwipeListView.SWIPE_ACTION_REVEAL;
                    } else {
                        if (swipingRight && swipeActionRight == SwipeListView.SWIPE_ACTION_DISMISS) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_DISMISS;
                        } else if (!swipingRight && swipeActionLeft == SwipeListView.SWIPE_ACTION_DISMISS) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_DISMISS;
                        } else if (swipingRight && swipeActionRight == SwipeListView.SWIPE_ACTION_CHOICE) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_CHOICE;
                        } else if (!swipingRight && swipeActionLeft == SwipeListView.SWIPE_ACTION_CHOICE) {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_CHOICE;
                        } else {
                            swipeCurrentAction = SwipeListView.SWIPE_ACTION_REVEAL;
                        }
                        swipeListView.onStartOpen(downPosition, swipeCurrentAction, swipingRight);
                    }
                    swipeListView.requestDisallowInterceptTouchEvent(true);
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (MotionEventCompat.getActionIndex(motionEvent) << MotionEventCompat.ACTION_POINTER_INDEX_SHIFT));
                    swipeListView.onTouchEvent(cancelEvent);
                    if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_CHOICE) {
                        backView.setVisibility(View.GONE);
                    }
                }

                if (swiping && downPosition != ListView.INVALID_POSITION) {
                    if (opened.get(downPosition)) {
                        deltaX += openedRight.get(downPosition) ? viewWidth - rightOffset : -viewWidth + leftOffset;
                    }
                    move(deltaX);
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private void setActionsTo(int action) {
        oldSwipeActionRight = swipeActionRight;
        oldSwipeActionLeft = swipeActionLeft;
        swipeActionRight = action;
        swipeActionLeft = action;
    }

    protected void returnOldActions() {
        swipeActionRight = oldSwipeActionRight;
        swipeActionLeft = oldSwipeActionLeft;
    }

    /**
     * Moves the view
     *
     * @param deltaX delta
     */
    public void move(float deltaX) {
        swipeListView.onMove(downPosition, deltaX);
        float posX = ViewHelper.getX(frontView);
        if (opened.get(downPosition)) {
            posX += openedRight.get(downPosition) ? -viewWidth + rightOffset : viewWidth - leftOffset;
        }
        if (posX > 0 && !swipingRight) {
            if(SwipeListView.DEBUG){
                Log.d(SwipeListView.TAG, "change to right");
            }
            swipingRight = !swipingRight;
            swipeCurrentAction = swipeActionRight;
            if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_CHOICE) {
                backView.setVisibility(View.GONE);
            } else {
                backView.setVisibility(View.VISIBLE);
            }
        }
        if (posX < 0 && swipingRight) {
            if(SwipeListView.DEBUG){
                Log.d(SwipeListView.TAG, "change to left");
            }
            swipingRight = !swipingRight;
            swipeCurrentAction = swipeActionLeft;
            if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_CHOICE) {
                backView.setVisibility(View.GONE);
            } else {
                backView.setVisibility(View.VISIBLE);
            }
        }
        if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_DISMISS) {
            setTranslationX(parentView, deltaX);
            setAlpha(parentView, Math.max(0f, Math.min(1f,
                    1f - 2f * Math.abs(deltaX) / viewWidth)));
        } else if (swipeCurrentAction == SwipeListView.SWIPE_ACTION_CHOICE) {
            if ((swipingRight && deltaX > 0 && posX < DISPLACE_CHOICE)
                    || (!swipingRight && deltaX < 0 && posX > -DISPLACE_CHOICE)
                    || (swipingRight && deltaX < DISPLACE_CHOICE)
                    || (!swipingRight && deltaX > -DISPLACE_CHOICE)) {
                setTranslationX(frontView, deltaX);
            }
        } else {
            setTranslationX(frontView, deltaX);
        }
    }

    /**
     * Class that saves pending dismiss data
     */
    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    /**
     * Perform dismiss action
     *
     * @param dismissView     View
     * @param dismissPosition Position of list
     */
    protected void performDismiss(final View dismissView, final int dismissPosition, boolean doPendingDismiss) {
        enableDisableViewGroup((ViewGroup) dismissView, false);
        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = dismissView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(animationTime);

        if (doPendingDismiss) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    --dismissAnimationRefCount;
                    if (dismissAnimationRefCount == 0) {
                        removePendingDismisses(originalHeight);
                    }
                }
            });
        }

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                enableDisableViewGroup((ViewGroup) dismissView, true);
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        pendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }

    /**
     * Remove all pending dismisses.
     */
    protected void resetPendingDismisses() {
        pendingDismisses.clear();
    }

    /**
     * Will call {@link #removePendingDismisses(int)} in animationTime + 100 ms.
     * @param originalHeight will be used to rest the cells height.
     */
    protected void handlerPendingDismisses(final int originalHeight) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removePendingDismisses(originalHeight);
            }
        }, animationTime + 100);
    }

    /**
     * Will delete all pending dismisses.
     * Will call callback onDismiss for all pending dismisses.
     * Will reset all cell height to originalHeight.
     * @param originalHeight is the height of the cell before animation.
     */
    private void removePendingDismisses(int originalHeight) {
        // No active animations, process all pending dismisses.
        // Sort by descending position
        Collections.sort(pendingDismisses);

        int[] dismissPositions = new int[pendingDismisses.size()];
        for (int i = pendingDismisses.size() - 1; i >= 0; i--) {
            dismissPositions[i] = pendingDismisses.get(i).position;
        }
        swipeListView.onDismiss(dismissPositions);

        ViewGroup.LayoutParams lp;
        for (PendingDismissData pendingDismiss : pendingDismisses) {
            // Reset view presentation
            if (pendingDismiss.view != null) {
                setAlpha(pendingDismiss.view, 1f);
                setTranslationX(pendingDismiss.view, 0);
                lp = pendingDismiss.view.getLayoutParams();
                lp.height = originalHeight;
                pendingDismiss.view.setLayoutParams(lp);
            }
        }

        resetPendingDismisses();

    }

    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }

}
