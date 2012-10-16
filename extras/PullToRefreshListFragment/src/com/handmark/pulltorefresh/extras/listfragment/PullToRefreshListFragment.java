package com.handmark.pulltorefresh.extras.listfragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

/**
 * A sample implementation of how to the PullToRefreshListView with
 * ListFragment. This implementation simply replaces the ListView that
 * ListFragment creates with a new PullToRefreshListView. This means that
 * ListFragment still works 100% (e.g. <code>setListShown(...)</code>).
 * 
 * The new PullToRefreshListView is created in the method
 * <code>onCreatePullToRefreshListView()</code>. If you wish to customise the
 * PullToRefreshListView then override this method and return your customised
 * instance.
 * 
 * @author Chris Banes
 * 
 */
public class PullToRefreshListFragment extends ListFragment {

	private PullToRefreshListView mPullToRefreshListView;

	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View layout = super.onCreateView(inflater, container, savedInstanceState);

		ListView lv = (ListView) layout.findViewById(android.R.id.list);
		ViewGroup parent = (ViewGroup) lv.getParent();

        //Remove ListView and add PullToRefreshListView in its place
        int lvIndex = parent.indexOfChild(lv);
        parent.removeViewAt(lvIndex);
        mPullToRefreshListView = onCreatePullToRefreshListView(inflater, savedInstanceState);
        parent.addView(mPullToRefreshListView, lvIndex, lv.getLayoutParams());

		return layout;
	}

	/**
	 * @return The {@link PullToRefreshListView} attached to this ListFragment.
	 */
	public final PullToRefreshListView getPullToRefreshListView() {
		return mPullToRefreshListView;
	}

	/**
	 * Returns the {@link PullToRefreshListView} which will replace the ListView
	 * created from ListFragment. You should override this method if you wish to
	 * customise the {@link PullToRefreshListView} from the default.
	 * 
	 * @param inflater
	 *            - LayoutInflater which can be used to inflate from XML.
	 * @param savedInstanceState
	 *            - Bundle passed through from
	 *            {@link ListFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)
	 *            onCreateView(...)}
	 * @return The {@link PullToRefreshListView} which will replace the
	 *         ListView.
	 */
	protected PullToRefreshListView onCreatePullToRefreshListView(LayoutInflater inflater, Bundle savedInstanceState) {
		return new PullToRefreshListView(getActivity());
	}

}