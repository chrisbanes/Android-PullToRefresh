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
package com.handmark.pulltorefresh.library.internal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.R;

public class LoadingLayout extends LinearLayout {

	static final int DEFAULT_ROTATION_ANIMATION_DURATION = 1200;
	static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

	private final ImageView mHeaderImage;
	private final Matrix mHeaderImageMatrix;
	private boolean mUseIntrinisicAnimation;

	private final TextView mHeaderText;
	private final TextView mSubHeaderText;

	private CharSequence mPullLabel;
	private CharSequence mRefreshingLabel;
	private CharSequence mReleaseLabel;

	private float mRotationPivotX, mRotationPivotY;

	private final Animation mRotateAnimation;

	public LoadingLayout(Context context, final Mode mode, TypedArray attrs) {
		super(context);

		setGravity(Gravity.CENTER_VERTICAL);

		final int tbPadding = getResources().getDimensionPixelSize(R.dimen.header_footer_top_bottom_padding);
		final int lrPadding = getResources().getDimensionPixelSize(R.dimen.header_footer_left_right_padding);
		setPadding(lrPadding, tbPadding, lrPadding, tbPadding);

		LayoutInflater.from(context).inflate(R.layout.pull_to_refresh_header, this);
		mHeaderText = (TextView) findViewById(R.id.pull_to_refresh_text);
		mSubHeaderText = (TextView) findViewById(R.id.pull_to_refresh_sub_text);
		mHeaderImage = (ImageView) findViewById(R.id.pull_to_refresh_image);

		mHeaderImage.setScaleType(ScaleType.MATRIX);
		mHeaderImageMatrix = new Matrix();
		mHeaderImage.setImageMatrix(mHeaderImageMatrix);

		mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mRotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
		mRotateAnimation.setRepeatCount(Animation.INFINITE);
		mRotateAnimation.setRepeatMode(Animation.RESTART);

		switch (mode) {
			case PULL_UP_TO_REFRESH:
				// Load in labels
				mPullLabel = context.getString(R.string.pull_to_refresh_from_bottom_pull_label);
				mRefreshingLabel = context.getString(R.string.pull_to_refresh_from_bottom_refreshing_label);
				mReleaseLabel = context.getString(R.string.pull_to_refresh_from_bottom_release_label);
				break;

			case PULL_DOWN_TO_REFRESH:
			default:
				// Load in labels
				mPullLabel = context.getString(R.string.pull_to_refresh_pull_label);
				mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
				mReleaseLabel = context.getString(R.string.pull_to_refresh_release_label);
				break;
		}

		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderTextColor);
			setTextColor(null != colors ? colors : ColorStateList.valueOf(Color.BLACK));
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderSubTextColor)) {
			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderSubTextColor);
			setSubTextColor(null != colors ? colors : ColorStateList.valueOf(Color.BLACK));
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
			Drawable background = attrs.getDrawable(R.styleable.PullToRefresh_ptrHeaderBackground);
			if (null != background) {
				setBackgroundDrawable(background);
			}
		}

		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.PullToRefresh_ptrHeaderTextAppearance, styleID);
			setTextAppearance(styleID.data);
		}
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance)) {
			TypedValue styleID = new TypedValue();
			attrs.getValue(R.styleable.PullToRefresh_ptrSubHeaderTextAppearance, styleID);
			setSubTextAppearance(styleID.data);
		}

		// Try and get defined drawable from Attrs
		Drawable imageDrawable = null;
		if (attrs.hasValue(R.styleable.PullToRefresh_ptrDrawable)) {
			imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawable);
		}

		// If we don't have a user defined drawable, load the default
		if (null == imageDrawable) {
			imageDrawable = context.getResources().getDrawable(R.drawable.default_ptr_drawable);
		}

		// Set Drawable, and save width/height
		setLoadingDrawable(imageDrawable);

		reset();
	}

	public void reset() {
		mHeaderText.setText(mPullLabel);
		mHeaderImage.setVisibility(View.VISIBLE);

		if (mUseIntrinisicAnimation) {
			((AnimationDrawable) mHeaderImage.getDrawable()).stop();
		} else {
			mHeaderImage.clearAnimation();
		}

		resetImageRotation();

		if (TextUtils.isEmpty(mSubHeaderText.getText())) {
			mSubHeaderText.setVisibility(View.GONE);
		} else {
			mSubHeaderText.setVisibility(View.VISIBLE);
		}
	}

	public void releaseToRefresh() {
		mHeaderText.setText(mReleaseLabel);
	}

	public void setPullLabel(CharSequence pullLabel) {
		mPullLabel = pullLabel;
	}

	public void refreshing() {
		mHeaderText.setText(mRefreshingLabel);

		if (mUseIntrinisicAnimation) {
			((AnimationDrawable) mHeaderImage.getDrawable()).start();
		} else {
			mHeaderImage.startAnimation(mRotateAnimation);
		}

		mSubHeaderText.setVisibility(View.GONE);
	}

	public void setRefreshingLabel(CharSequence refreshingLabel) {
		mRefreshingLabel = refreshingLabel;
	}

	public void setReleaseLabel(CharSequence releaseLabel) {
		mReleaseLabel = releaseLabel;
	}

	public void pullToRefresh() {
		mHeaderText.setText(mPullLabel);
	}

	public void setTextColor(ColorStateList color) {
		mHeaderText.setTextColor(color);
		mSubHeaderText.setTextColor(color);
	}

	public void setSubTextColor(ColorStateList color) {
		mSubHeaderText.setTextColor(color);
	}

	public void setTextColor(int color) {
		setTextColor(ColorStateList.valueOf(color));
	}

	public void setTextAppearance(int value) {
		mHeaderText.setTextAppearance(getContext(), value);
		mSubHeaderText.setTextAppearance(getContext(), value);
	}

	public void setSubTextAppearance(int value) {
		mSubHeaderText.setTextAppearance(getContext(), value);
	}

	public void setLoadingDrawable(Drawable imageDrawable) {
		// Set Drawable, and save width/height
		mHeaderImage.setImageDrawable(imageDrawable);

		if (null != imageDrawable) {
			mRotationPivotX = imageDrawable.getIntrinsicWidth() / 2f;
			mRotationPivotY = imageDrawable.getIntrinsicHeight() / 2f;
		}

		mUseIntrinisicAnimation = (imageDrawable instanceof AnimationDrawable);
	}

	public void setSubTextColor(int color) {
		setSubTextColor(ColorStateList.valueOf(color));
	}

	public void setSubHeaderText(CharSequence label) {
		if (TextUtils.isEmpty(label)) {
			mSubHeaderText.setVisibility(View.GONE);
		} else {
			mSubHeaderText.setText(label);
			mSubHeaderText.setVisibility(View.VISIBLE);
		}
	}

	public void onPullY(float scaleOfHeight) {
		if (!mUseIntrinisicAnimation) {
			mHeaderImageMatrix.setRotate(scaleOfHeight * 90, mRotationPivotX, mRotationPivotY);
			mHeaderImage.setImageMatrix(mHeaderImageMatrix);
		}
	}

	private void resetImageRotation() {
		if (!mUseIntrinisicAnimation) {
			mHeaderImageMatrix.reset();
			mHeaderImage.setImageMatrix(mHeaderImageMatrix);
		}
	}
}
