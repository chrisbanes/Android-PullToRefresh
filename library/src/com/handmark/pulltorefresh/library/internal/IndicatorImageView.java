package com.handmark.pulltorefresh.library.internal;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class IndicatorImageView extends ImageView implements AnimationListener {

	static final int ANIMATION_DURATION = 300;

	private Animation mFadeInAnim, mFadeOutAnim;

	public IndicatorImageView(Context context) {
		super(context);

		mFadeInAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
		mFadeInAnim.setAnimationListener(this);
		mFadeInAnim.setDuration(ANIMATION_DURATION);

		mFadeOutAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
		mFadeOutAnim.setAnimationListener(this);
		mFadeOutAnim.setDuration(ANIMATION_DURATION);
	}

	public boolean isVisible() {
		Animation currentAnim = getAnimation();
		if (null != currentAnim) {
			if (currentAnim == mFadeInAnim) {
				return true;
			} else if (currentAnim == mFadeOutAnim) {
				return false;
			}
		}

		return getVisibility() == View.VISIBLE;
	}

	public void hide() {
		startAnimation(mFadeOutAnim);
	}

	public void show() {
		startAnimation(mFadeInAnim);
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if (animation == mFadeOutAnim) {
			setVisibility(View.GONE);
		} else if (animation == mFadeInAnim) {
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
