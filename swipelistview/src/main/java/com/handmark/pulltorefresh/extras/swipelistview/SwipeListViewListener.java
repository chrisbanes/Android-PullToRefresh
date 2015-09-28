/*
 * Copyright (C) 2013 47 Degrees, LLC
 * http://47deg.com
 * hello@47deg.com
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

/**
 * Listener to get callback notifications for the SwipeListView
 */
public interface SwipeListViewListener {

    /**
     * Called when open animation finishes
     * @param position of the view in the list
     * @param toRight Open to right
     */
    void onOpened(int position, boolean toRight);

    /**
     * Called when close animation finishes
     * @param position of the view in the list
     * @param fromRight Close from right
     */
    void onClosed(int position, boolean fromRight);

    /**
     * Called when the list changed
     */
    void onListChanged();

    /**
     * Called when user is moving an item
     * @param position of the view in the list
     * @param x Current position X
     */
    void onMove(int position, float x);

    /**
     * Start open item
     * @param position of the view in the list
     * @param action current action
     * @param right to right
     */
    void onStartOpen(int position, int action, boolean right);

    /**
     * Start close item
     * @param position of the view in the list
     * @param right
     */
    void onStartClose(int position, boolean right);

    /**
     * Called when user clicks on the front view
     * @param position of the view in the list
     */
    void onClickFrontView(int position);

    /**
     * Called when user clicks on the back view
     * @param position of the view in the list
     */
    void onClickBackView(int position);

    /**
     * Called when user dismisses items
     * @param reverseSortedPositions Items dismissed
     */
    void onDismiss(int[] reverseSortedPositions);

    /**
     * Used when user want to change swipe list mode on some rows. Return SWIPE_MODE_DEFAULT
     * if you don't want to change swipe list mode
     * @param position position that you want to change
     * @return type
     */
    int onChangeSwipeMode(int position);

    /**
     * Called when user choice item
     * @param position of the view in the list that choice
     * @param selected if item is selected or not
     */
    void onChoiceChanged(int position, boolean selected);

    /**
     * User start choice items
     */
    void onChoiceStarted();

    /**
     * User end choice items
     */
    void onChoiceEnded();

    /**
     * User is in first item of list
     */
    void onFirstListItem();

    /**
     * User is in last item of list
     */
    void onLastListItem();

}
