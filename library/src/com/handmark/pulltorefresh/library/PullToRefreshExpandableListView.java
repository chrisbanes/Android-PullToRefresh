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
package com.handmark.pulltorefresh.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ExpandableListView;

import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;

public class PullToRefreshExpandableListView extends PullToRefreshAdapterViewBase<ExpandableListView> {

	class InternalExpandableListView extends ExpandableListView implements EmptyViewMethodAccessor {

		public InternalExpandableListView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		public void setEmptyView(View emptyView) {
			PullToRefreshExpandableListView.this.setEmptyView(emptyView);
		}

		@Override
		public void setEmptyViewInternal(View emptyView) {
			super.setEmptyView(emptyView);
		}

		public ContextMenuInfo getContextMenuInfo() {
			return super.getContextMenuInfo();
		}
	}

	public PullToRefreshExpandableListView(Context context) {
		super(context);
	}

	public PullToRefreshExpandableListView(Context context, Mode mode) {
		super(context, mode);
	}

	public PullToRefreshExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected final ExpandableListView createRefreshableView(Context context, AttributeSet attrs) {
		ExpandableListView lv = new InternalExpandableListView(context, attrs);

		// Set it to this so it can be used in ListActivity/ListFragment
		lv.setId(android.R.id.list);
		return lv;
	}

	@Override
	public ContextMenuInfo getContextMenuInfo() {
		return ((InternalExpandableListView) getRefreshableView()).getContextMenuInfo();
	}
}
