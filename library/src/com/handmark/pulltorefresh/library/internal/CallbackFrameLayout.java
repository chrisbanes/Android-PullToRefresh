package com.handmark.pulltorefresh.library.internal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class CallbackFrameLayout extends FrameLayout {

	private OnSizeChangedListener mSizeChangedListener;

	public CallbackFrameLayout(Context context) {
		super(context);
	}

	public CallbackFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public final void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		if (null != mSizeChangedListener) {
			mSizeChangedListener.onSizeChanged(this, w, h);
		}
	}

	public void setOnSizeChangedListener(OnSizeChangedListener listener) {
		mSizeChangedListener = listener;
	}

	public static interface OnSizeChangedListener {
		void onSizeChanged(View view, int newWidth, int newHeight);
	}

}
