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
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.R;

public class IndicatorLayout extends FrameLayout implements AnimationListener {

	private Animation mInAnim, mOutAnim;
	private ImageView mArrowImageView;

	public IndicatorLayout(Context context, PullToRefreshBase.Mode mode) {
		super(context);

		mArrowImageView = new ImageView(context);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		addView(mArrowImageView, lp);

		int inAnimResId, outAnimResId;
		switch (mode) {
			case PULL_UP_TO_REFRESH:
				inAnimResId = R.anim.slide_in_from_bottom;
				outAnimResId = R.anim.slide_out_to_bottom;
				setBackgroundResource(R.drawable.indicator_bg_bottom);
				mArrowImageView.setImageResource(R.drawable.arrow_up);
				break;
			default:
			case PULL_DOWN_TO_REFRESH:
				inAnimResId = R.anim.slide_in_from_top;
				outAnimResId = R.anim.slide_out_to_top;
				setBackgroundResource(R.drawable.indicator_bg_top);
				mArrowImageView.setImageResource(R.drawable.arrow_down);
				break;
		}

		mInAnim = AnimationUtils.loadAnimation(context, inAnimResId);
		mInAnim.setAnimationListener(this);

		mOutAnim = AnimationUtils.loadAnimation(context, outAnimResId);
		mOutAnim.setAnimationListener(this);
	}

	public final boolean isVisible() {
		Animation currentAnim = getAnimation();
		if (null != currentAnim) {
			return mInAnim == currentAnim;
		}

		return getVisibility() == View.VISIBLE;
	}

	public void hide() {
		startAnimation(mOutAnim);
	}

	public void show() {
		startAnimation(mInAnim);
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if (animation == mOutAnim) {
			setVisibility(View.GONE);
		} else if (animation == mInAnim) {
			setVisibility(View.VISIBLE);
		}

		clearAnimation();
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// NO-OP
	}

	@Override
	public void onAnimationStart(Animation animation) {
		setVisibility(View.VISIBLE);
	}

}
