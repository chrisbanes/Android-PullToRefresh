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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.R;

public class FlipLoadingLayout extends LoadingLayout {

	static final int FLIP_ANIMATION_DURATION = 150;

	private final Animation mRotateAnimation, mResetRotateAnimation;

	public FlipLoadingLayout(Context context, Mode mode, TypedArray attrs) {
		super(context, mode, attrs);

		mRotateAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
		mRotateAnimation.setFillAfter(true);

		mResetRotateAnimation = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		mResetRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
		mResetRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
		mResetRotateAnimation.setFillAfter(true);
	}

	@Override
	protected void onLoadingDrawableSet(Drawable imageDrawable) {
		// NO-OP
	}

	@Override
	protected void onPullYImpl(float scaleOfHeight) {
		// NO-OP
	}

	@Override
	protected void pullToRefreshImpl() {
		mHeaderImage.clearAnimation();
		mHeaderImage.startAnimation(mResetRotateAnimation);
	}

	@Override
	protected void refreshingImpl() {
		mHeaderImage.clearAnimation();
		mHeaderImage.setVisibility(View.INVISIBLE);
		mHeaderProgress.setVisibility(View.VISIBLE);
	}

	@Override
	protected void releaseToRefreshImpl() {
		mHeaderImage.clearAnimation();
		mHeaderImage.startAnimation(mRotateAnimation);
	}

	@Override
	protected void resetImpl() {
		mHeaderImage.setVisibility(View.VISIBLE);
		mHeaderProgress.setVisibility(View.GONE);
	}

	@Override
	protected int getDefaultTopDrawableResId() {
		return R.drawable.default_ptr_flip_top;
	}

	@Override
	protected int getDefaultBottomDrawableResId() {
		return R.drawable.default_ptr_flip_bottom;
	}

}
