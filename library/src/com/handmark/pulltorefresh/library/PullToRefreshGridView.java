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
import android.widget.GridView;

import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;

public class PullToRefreshGridView extends PullToRefreshAdapterViewBase<GridView> {

	class InternalGridView extends GridView implements EmptyViewMethodAccessor {

		public InternalGridView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		public void setEmptyView(View emptyView) {
			PullToRefreshGridView.this.setEmptyView(emptyView);
		}

		@Override
		public void setEmptyViewInternal(View emptyView) {
			super.setEmptyView(emptyView);
		}

		@Override
		public ContextMenuInfo getContextMenuInfo() {
			return super.getContextMenuInfo();
		}
	}

	public PullToRefreshGridView(Context context) {
		super(context);
	}

	public PullToRefreshGridView(Context context, Mode mode) {
		super(context, mode);
	}

	public PullToRefreshGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected final GridView createRefreshableView(Context context, AttributeSet attrs) {
		GridView gv = new InternalGridView(context, attrs);

		// Use Generated ID (from res/values/ids.xml)
		gv.setId(R.id.gridview);
		return gv;
	}

	@Override
	public ContextMenuInfo getContextMenuInfo() {
		return ((InternalGridView) getRefreshableView()).getContextMenuInfo();
	}
}
