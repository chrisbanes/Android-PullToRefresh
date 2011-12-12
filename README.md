# Pull To Refresh AdapterViews for Android

This project aims to provide a reusable Pull to Refresh widget for Android. It is originally based on Johan Nilsson's excellent [Library](https://github.com/johannilsson/android-pulltorefresh), but has been vastly improved since then.

## Features

 * No longer uses a (slow) OnScrollListener
 * No longer shows the Tap to Refresh view when the AdapterView can not fill itself.
 * Animated Scrolling for all devices (Tested on 1.6+)
 * Works for all AdapterViews. I've implemented both ListView and GridView.
 * Works with ExpandableListView (thanks to Stefano Dacchille)
 * Maven Support (thanks to Stefano Dacchille)
 

Repository at <https://github.com/chrisbanes/Android-PullToRefresh>.

## Usage

### Layout

``` xml
<!--
  The PullToRefreshListView replaces a standard ListView widget.
  The ID CAN NOT be @+id/android:list
-->
<com.handmark.pulltorefresh.library.PullToRefreshListView
    android:id="@+id/pull_to_refresh_listview"
    android:layout_height="fill_parent"
    android:layout_width="fill_parent" />
```

It can also be styled using XML, such as in the sample ExpandableListView Sample:

``` xml
<com.handmark.pulltorefresh.library.PullToRefreshExpandableListView
    xmlns:ptr="http://schemas.android.com/apk/res/YOUR_APP_PACKAGE_NAME"
    android:id="@+id/pull_refresh_expandable_list"
    android:layout_height="fill_parent"
    android:layout_width="fill_parent"
    ptr:adapterViewBackground="@android:color/white"
    ptr:headerBackground="@android:color/darker_gray"
    ptr:headerTextColor="@android:color/white" />
```

### Activity

``` java
// Set a listener to be invoked when the list should be refreshed.
PullToRefreshListView pullToRefreshView = (PullToRefreshListView) findViewById(R.id.pull_to_refresh_listview);
pullToRefreshView.setOnRefreshListener(new OnRefreshListener() {
    @Override
    public void onRefresh() {
        // Do work to refresh the list here.
        new GetDataTask().execute();
    }
});

private class GetDataTask extends AsyncTask<Void, Void, String[]> {
    ...
    @Override
    protected void onPostExecute(String[] result) {
        mListItems.addFirst("Added after refresh...");
        // Call onRefreshComplete when the list has been refreshed.
        pullToRefreshView.onRefreshComplete();
        super.onPostExecute(result);
    }
}
```

## Acknowledgments

* [Stefano Dacchille](https://github.com/stefanodacchille) 


## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
