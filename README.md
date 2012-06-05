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
 * Indicators to show the user when a Pull-to-Refresh is available
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

There are also a number of Customisation options for the View. Please see the [Customisation](https://github.com/chrisbanes/Android-PullToRefresh/wiki/Customisation) page for more information.

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
        // Call onRefreshComplete when the list has been refreshed.
        pullToRefreshView.onRefreshComplete();
        super.onPostExecute(result);
    }
}
```


### Pull Up to Refresh

By default this library is set to Pull Down to Refresh, but if you instead to Pull Up to Refresh you can do so via XML. You can even set the View to enable both Pulling Up and Pulling Down using the 'both' setting. Please see the [Customisation](https://github.com/chrisbanes/Android-PullToRefresh/wiki/Customisation) page for more information.

## Pull Requests

I will gladly accept pull requests for fixes and feature enhancements but please do them in the dev branch. The master branch is for the latest stable code,  dev is where I try things out before releasing them as stable. Any pull requests that are against master from now on will be closed asking for you to do another pull against dev.

## Changelog
Please see the new [Changelog](https://github.com/chrisbanes/Android-PullToRefresh/wiki/Changelog) page to see what's recently changed.

## Acknowledgments

* [Stefano Dacchille](https://github.com/stefanodacchille)
* [Steve Lhomme](https://github.com/robUx4)
* [Maxim Galkin](https://github.com/mgalkin)


## License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
