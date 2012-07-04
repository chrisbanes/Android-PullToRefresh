package com.handmark.pulltorefresh.samples;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class LauncherActivity extends ListActivity {

	public static final String[] options = { "ListView", "ExpandableListView", "GridView", "WebView" };

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
		}

		startActivity(intent);
	}

}
