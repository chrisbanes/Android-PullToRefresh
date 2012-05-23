package com.handmark.pulltorefresh.library.internal;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.R;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class IndicatorImageView extends ImageView implements AnimationListener {

	private Animation mInAnim, mOutAnim;

	public IndicatorImageView(Context context, PullToRefreshBase.Mode mode) {
		super(context);

		int inAnimResId, outAnimResId;
		
		switch (mode) {
			case PULL_UP_TO_REFRESH:
				inAnimResId = R.anim.slide_in_from_bottom;
				outAnimResId = R.anim.slide_out_to_bottom;
				break;
			default:
			case PULL_DOWN_TO_REFRESH:
				inAnimResId = R.anim.slide_in_from_top;
				outAnimResId = R.anim.slide_out_to_top;
				break;
		}
		
		mInAnim = AnimationUtils.loadAnimation(context, inAnimResId);
		mInAnim.setAnimationListener(this);
		
		mOutAnim = AnimationUtils.loadAnimation(context, outAnimResId);
		mOutAnim.setAnimationListener(this);
	}

	public boolean isVisible() {
		Animation currentAnim = getAnimation();
		if (null != currentAnim) {
			if (currentAnim == mInAnim) {
				return true;
			} else if (currentAnim == mOutAnim) {
				return false;
			}
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
