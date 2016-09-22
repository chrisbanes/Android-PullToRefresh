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
package com.handmark.pulltorefresh.samples;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LauncherActivity extends ListActivity { 

	public static final String[] options = { "ListView", "ExpandableListView", "GridView", "WebView", "ScrollView",
			"Horizontal ScrollView", "ViewPager", "ListView Fragment", "WebView Advanced", "ListView in ViewPager",
			"RecyclerView"};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent;

		switch (position) {
			default:
			case 0:
				intent = new Intent(this, PullToRefreshListActivity.class);
				break;
			case 1:
				intent = new Intent(this, PullToRefreshExpandableListActivity.class);
				break;
			case 2:
				intent = new Intent(this, PullToRefreshGridActivity.class);
				break;
			case 3:
				intent = new Intent(this, PullToRefreshWebViewActivity.class);
				break;
			case 4:
				intent = new Intent(this, PullToRefreshScrollViewActivity.class);
				break;
			case 5:
				intent = new Intent(this, PullToRefreshHorizontalScrollViewActivity.class);
				break;
			case 6:
				intent = new Intent(this, PullToRefreshViewPagerActivity.class);
				break;
			case 7:
				intent = new Intent(this, PullToRefreshListFragmentActivity.class);
				break;
			case 8:
				intent = new Intent(this, PullToRefreshWebView2Activity.class);
				break;
			case 9:
				intent = new Intent(this, PullToRefreshListInViewPagerActivity.class);
				break;
			case 10:
				intent = new Intent(this, PullToRefreshRecycleActivity.class);
				break;
		}

		startActivity(intent);
	}

}
