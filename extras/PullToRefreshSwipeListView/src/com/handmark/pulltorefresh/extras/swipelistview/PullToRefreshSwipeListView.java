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
package com.handmark.pulltorefresh.extras.swipelistview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
/**
 * 版权所有：XXX有限公司</br>
 *
 * PullToRefreshSwipeListView </br>
 *
 * @author zhou.wenkai<zwenkai@foxmail.com> ,Created on 2015-9-25 11:12:55</br>
 * @description Major Function：对PullToRefresh的扩展,增加支持SwipeListView </br>
 *
 * 注:如果您修改了本类请填写以下内容作为记录，如非本人操作劳烦通知，谢谢！！！</br>
 * @author mender，Modified Date Modify Content:
 */
public class PullToRefreshSwipeListView extends PullToRefreshBase<SwipeListView> {

	public PullToRefreshSwipeListView(Context context) {
		super(context);
	}

	public PullToRefreshSwipeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PullToRefreshSwipeListView(Context context, Mode mode) {
		super(context, mode);
	}

	public PullToRefreshSwipeListView(Context context, Mode mode, AnimationStyle style) {
		super(context, mode, style);
	}

	@Override
	public final Orientation getPullToRefreshScrollDirection() {
		return Orientation.VERTICAL;
	}

	@Override
	protected SwipeListView createRefreshableView(Context context,
			AttributeSet attrs) {
		SwipeListView swipeListView = new SwipeListView(context, attrs);
		return swipeListView;
	}

	@Override
	protected boolean isReadyForPullEnd() {
		return isLastItemVisible();
	}

	@Override
	protected boolean isReadyForPullStart() {
		return isFirstItemVisible();
	}
	
	/**
	 * @Description: 是否第一个条目完全可见
	 * 
	 * @return boolean:
	 * @version 1.0 
	 * @date 2015-9-25
	 * @Author zhou.wenkai
	 */
	private boolean isFirstItemVisible() {
		final Adapter adapter = mRefreshableView.getAdapter();

		if (null == adapter || adapter.isEmpty()) {
			if (DEBUG) {
				Log.d(LOG_TAG, "isFirstItemVisible. Empty View.");
			}
			return true;

		} else {

			if (mRefreshableView.getFirstVisiblePosition() <= 1) {
				final View firstVisibleChild = mRefreshableView.getChildAt(0);
				if (firstVisibleChild != null) {
					return firstVisibleChild.getTop() >= mRefreshableView.getTop();
				}
			}
		}

		return false;
	}
	
	/**
	 * @Description: 是否最后一个条目完全可见
	 * 
	 * @return boolean:
	 * @version 1.0 
	 * @date 2015-9-25
	 * @Author zhou.wenkai
	 */
	private boolean isLastItemVisible() {
		final Adapter adapter = mRefreshableView.getAdapter();

		if (null == adapter || adapter.isEmpty()) {
			if (DEBUG) {
				Log.d(LOG_TAG, "isLastItemVisible. Empty View.");
			}
			return true;
		} else {
			final int lastItemPosition = mRefreshableView.getCount() - 1;
			final int lastVisiblePosition = mRefreshableView.getLastVisiblePosition();

			if (DEBUG) {
				Log.d(LOG_TAG, "isLastItemVisible. Last Item Position: " + lastItemPosition + " Last Visible Pos: "
						+ lastVisiblePosition);
			}

			if (lastVisiblePosition >= lastItemPosition - 1) {
				final int childIndex = lastVisiblePosition - mRefreshableView.getFirstVisiblePosition();
				final View lastVisibleChild = mRefreshableView.getChildAt(childIndex);
				if (lastVisibleChild != null) {
					return lastVisibleChild.getBottom() <= mRefreshableView.getBottom();
				}
			}
		}

		return false;
	}

}
