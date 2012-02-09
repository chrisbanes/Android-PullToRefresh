# Pull To Refresh AdapterViews for Android

![Screenshot](https://github.com/chrisbanes/Android-PullToRefresh/raw/master/header_graphic.png)

This project aims to provide a reusable Pull to Refresh widget for Android. It is originally (loosely) based on Johan Nilsson's [Library](https://github.com/johannilsson/android-pulltorefresh) (mainly for graphics, strings and animations), and has been vastly improved since then.

## Features

 * Supports both Pulling Down from the top, and Pulling Up from the bottom
 * Animated Scrolling for all devices (Tested on 1.6+)
 * Works for all AbsListView. I've implemented both ListView and GridView.
 * Works with ExpandableListView (thanks to Stefano Dacchille)
 * Integrated End of List Listener ( setOnLastItemVisibleListener() )
 * Maven Support (thanks to Stefano Dacchille)
 * No longer shows the Tap to Refresh view when the AdapterView can not fill itself.

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


### Pull Up to Refresh

By default this library is set to Pull Down to Refresh, but if you instead to Pull Up to Refresh you can do so via XML:

``` xml
<com.handmark.pulltorefresh.library.PullToRefreshListView
    xmlns:ptr="http://schemas.android.com/apk/res/YOUR_APP_PACKAGE_NAME"
    android:id="@+id/pull_refresh_list"
    android:layout_height="fill_parent"
    android:layout_width="fill_parent"
    ptr:mode="pullUpFromBottom" />
```

You can even set the View to enable both Pulling Up and Pulling Down using the new 'both' setting. For example:

``` xml
<com.handmark.pulltorefresh.library.PullToRefreshListView
    xmlns:ptr="http://schemas.android.com/apk/res/YOUR_APP_PACKAGE_NAME"
    android:id="@+id/pull_refresh_list"
    android:layout_height="fill_parent"
    android:layout_width="fill_parent"
    ptr:mode="both" />
```

## Pull Requests

I will gladly accept pull requests for fixes and feature enhancements but please do them in the dev branch. The master branch is for the latest stable code,  dev is where I try things out before releasing them as stable. Any pull requests that are against master from now on will be closed asking for you to do another pull against dev.

## Changelog

### v1.2

* New (better) way of handling Touch Events
* Add new way for PullToRefreshListView to work. Allows the user to scroll while it's refreshing.
* Fix Pull from Bottom happening when the view wasn't completely at the bottom
* Allow outside code to update the Widget to be refreshing (see `setRefreshing()`)
* Fix Padding being doubled when added via XML
* New Translations

### v1.2.1

* Fix bug where disabling scrolling has no effect when manual refreshing (thanks Maxim Galkin)

### v1.2.2

* String Updates (thanks to Steve Lhomme)
* Add Missing Constructor (thanks to mcxiaoke)

## Acknowledgments

* [Stefano Dacchille](https://github.com/stefanodacchille)
* [Steve Lhomme](https://github.com/robUx4)
* [Maxim Galkin](https://github.com/mgalkin)


## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
