
package com.handmark.pulltorefresh.sample;

import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshSectionListView;
import com.handmark.pulltorefresh.library.SectionListAdapter;
import com.handmark.pulltorefresh.library.SectionListItem;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PullToRefreshSectionListActivity extends ListActivity {

    static final int                     MENU_MANUAL_REFRESH = 0;
    static final int                     MENU_DISABLE_SCROLL = 1;

    private List<SectionListItem>        items;
    private PullToRefreshSectionListView mPullRefreshListView;
    private StandardArrayAdapter         mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pull_to_refresh_section_list);

        mPullRefreshListView = (PullToRefreshSectionListView) findViewById(R.id.pull_refresh_list);

        // Set a listener to be invoked when the list should be refreshed.
        mPullRefreshListView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Do work to refresh the list here.
                new GetDataTask().execute();
            }
        });

        com.handmark.pulltorefresh.library.SectionListView actualListView = (com.handmark.pulltorefresh.library.SectionListView) mPullRefreshListView
                .getRefreshableView();

        actualListView.addHeaderView(getLayoutInflater().inflate(R.layout.budget_item_txn_list_header_view, null));

        actualListView.addFooterView(getLayoutInflater().inflate(R.layout.budget_item_txn_list_header_view, null));

        View emptyLayout = getLayoutInflater().inflate(R.layout.empty_layout, null);
        mPullRefreshListView.setEmptyView(emptyLayout);

        items = new ArrayList<SectionListItem>();

        mAdapter = new StandardArrayAdapter();
        sectionListAdapter = new SectionListAdapter(getLayoutInflater(), mAdapter);
        // You can also just use setListAdapter(mAdapter)
        actualListView.setAdapter(sectionListAdapter);

        new GetDataTask().execute();
    }

    private SectionListAdapter sectionListAdapter = null;

    private class StandardArrayAdapter extends BaseAdapter {

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = getLayoutInflater().inflate(R.layout.list_row_view, null);
            ((TextView) view.findViewById(R.id.row_number)).setText("position = " + position);
            ((TextView) view.findViewById(R.id.row_message)).setText(items.get(position).item.toString());
            return view;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    private class GetDataTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... params) {
            // Simulates a background job.
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
            }
            return mStrings;
        }

        @Override
        protected void onPostExecute(String[] result) {

            items.add(new SectionListItem("After refresh", "section"));
            mAdapter.notifyDataSetChanged();

            // Call onRefreshComplete when the list has been refreshed.
            mPullRefreshListView.onRefreshComplete();

            super.onPostExecute(result);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_MANUAL_REFRESH, 0, "Manual Refresh");
        menu.add(0, MENU_DISABLE_SCROLL, 1,
                mPullRefreshListView.isDisableScrollingWhileRefreshing() ? "Enable Scrolling while Refreshing"
                        : "Disable Scrolling while Refreshing");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem disableItem = menu.findItem(MENU_DISABLE_SCROLL);
        disableItem
                .setTitle(mPullRefreshListView.isDisableScrollingWhileRefreshing() ? "Enable Scrolling while Refreshing"
                        : "Disable Scrolling while Refreshing");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case MENU_MANUAL_REFRESH:
                new GetDataTask().execute();
                mPullRefreshListView.setRefreshing(false);
                break;
            case MENU_DISABLE_SCROLL:
                mPullRefreshListView.setDisableScrollingWhileRefreshing(!mPullRefreshListView
                        .isDisableScrollingWhileRefreshing());
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private String[] mStrings = {
            "Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam", "Abondance", "Ackawi", "Acorn", "Adelost",
            "Affidelice au Chablis", "Afuega'l Pitu", "Airag", "Airedale", "Aisy Cendre", "Allgauer Emmentaler",
            "Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam", "Abondance", "Ackawi", "Acorn", "Adelost",
            "Affidelice au Chablis", "Afuega'l Pitu", "Airag", "Airedale", "Aisy Cendre", "Allgauer Emmentaler"
                              };
}
