package com.handmark.pulltorefresh.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.ExpandableListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.SimpleExpandableListAdapter;

import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshExpandableListView;

public class PullToRefreshExpandableListActivity extends ExpandableListActivity {
    private static final String KEY = "key";
    private LinkedList<String> mListItems, mGroupItems;
    private PullToRefreshExpandableListView mPullRefreshListView;
    private SimpleExpandableListAdapter mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pull_to_refresh_expandable_list);

        mPullRefreshListView = (PullToRefreshExpandableListView) findViewById(R.id.pull_refresh_expandable_list);

        // Set a listener to be invoked when the list should be refreshed.
        mPullRefreshListView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Do work to refresh the list here.
                new GetDataTask().execute();
            }
        });

        mListItems = new LinkedList<String>();
        mListItems.addAll(Arrays.asList(mChildStrings));
        mGroupItems = new LinkedList<String>();
        mGroupItems.addAll(Arrays.asList(mGroupStrings));

        List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
        for (String group : mGroupStrings) {
            Map<String, String> groupMap1 = new HashMap<String, String>();
            groupData.add(groupMap1);
            groupMap1.put(KEY, group);

            List<Map<String, String>> childList = new ArrayList<Map<String, String>>();
            for (String string : mChildStrings) {
                Map<String, String> childMap = new HashMap<String, String>();
                childList.add(childMap);
                childMap.put(KEY, string);
            }
            childData.add(childList);
        }

        mAdapter = new SimpleExpandableListAdapter(this, groupData,
                android.R.layout.simple_expandable_list_item_1, new String[] { KEY },
                new int[] { android.R.id.text1 }, childData,
                android.R.layout.simple_expandable_list_item_2, new String[] { KEY },
                new int[] { android.R.id.text1 });
        setListAdapter(mAdapter);
    }

    private class GetDataTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... params) {
            // Simulates a background job.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return mChildStrings;
        }

        @Override
        protected void onPostExecute(String[] result) {
            mListItems.addFirst("Added after refresh...");
            mAdapter.notifyDataSetChanged();

            // Call onRefreshComplete when the list has been refreshed.
            mPullRefreshListView.onRefreshComplete();

            super.onPostExecute(result);
        }
    }

    private final String[] mChildStrings = { "Child One", "Child Two", "Child Three", "Child Four",
            "Child Five", "Child Six" };

    private final String[] mGroupStrings = { "Group One", "Group Two", "Group Three" };
}
