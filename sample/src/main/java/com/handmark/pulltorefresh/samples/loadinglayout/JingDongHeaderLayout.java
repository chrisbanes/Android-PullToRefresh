package com.handmark.pulltorefresh.samples.loadinglayout;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.internal.LoadingLayoutBase;
import com.handmark.pulltorefresh.samples.R;
import com.nineoldandroids.view.ViewHelper;

/**
 * Created by zwenkai on 2015/12/19.
 */
public class JingDongHeaderLayout extends LoadingLayoutBase{

    static final String LOG_TAG = "PullToRefresh-JingDongHeaderLayout";

    private FrameLayout mInnerLayout;

    private final TextView mHeaderText;
    private final TextView mSubHeaderText;

    private CharSequence mPullLabel;
    private CharSequence mRefreshingLabel;
    private CharSequence mReleaseLabel;

    private ImageView mImgGoods;
    private ImageView mImgPerson;
    private AnimationDrawable animP;

    public JingDongHeaderLayout(Context context) {
        this(context, PullToRefreshBase.Mode.PULL_FROM_START);
    }

    public JingDongHeaderLayout(Context context, PullToRefreshBase.Mode mode) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.jingdong_header_loadinglayout, this);

        mInnerLayout = (FrameLayout) findViewById(R.id.fl_inner);
        mHeaderText = (TextView) mInnerLayout.findViewById(R.id.pull_to_refresh_text);
        mSubHeaderText = (TextView) mInnerLayout.findViewById(R.id.pull_to_refresh_sub_text);
        mImgGoods = (ImageView) mInnerLayout.findViewById(R.id.imageView1);
        mImgPerson = (ImageView) mInnerLayout.findViewById(R.id.imageView2);

        LayoutParams lp = (LayoutParams) mInnerLayout.getLayoutParams();
        lp.gravity = mode == PullToRefreshBase.Mode.PULL_FROM_END ? Gravity.TOP : Gravity.BOTTOM;

        // Load in labels
        mPullLabel = context.getString(R.string.pull_to_refresh_pull_label);
        mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
        mReleaseLabel = context.getString(R.string.pull_to_refresh_release_label);

        reset();
    }

    @Override
    public final int getContentSize() {
        return mInnerLayout.getHeight();
    }

    public final void onPull(float scaleOfLayout) {
        float i = scaleOfLayout;
        float j = scaleOfLayout;
        Log.e("onPull", "scaleOfLayout=" + scaleOfLayout);

        if (mImgGoods.getVisibility() != View.VISIBLE) {
            mImgGoods.setVisibility(View.VISIBLE);
        }
        if (i > 1) {
            i = 1;
        }
        //透明度动画
        ObjectAnimator animeAlphaP = ObjectAnimator.ofFloat(mImgPerson, "alpha", -1, 1).setDuration(300);
        animeAlphaP.setCurrentPlayTime((long) (i * 300));
        ObjectAnimator animeAlphaG = ObjectAnimator.ofFloat(mImgGoods, "alpha", -1, 1).setDuration(300);
        animeAlphaG.setCurrentPlayTime((long) (i * 300));

        //缩放动画
        ViewHelper.setPivotX(mImgPerson, 0);
        ViewHelper.setPivotY(mImgPerson, 0);
        ObjectAnimator animePX = ObjectAnimator.ofFloat(mImgPerson, "scaleX", 0, 1).setDuration(300);
        animePX.setCurrentPlayTime((long) (i * 300));
        ObjectAnimator animePY = ObjectAnimator.ofFloat(mImgPerson, "scaleY", 0, 1).setDuration(300);
        animePY.setCurrentPlayTime((long) (i * 300));
        if (j > 0.8) {
            j = 0.8f;
        }

        ObjectAnimator animeGX = ObjectAnimator.ofFloat(mImgGoods, "scaleX", 0, 1).setDuration(300);
        animeGX.setCurrentPlayTime((long) (j * 300 * 1.25));
        ObjectAnimator animeGY = ObjectAnimator.ofFloat(mImgGoods, "scaleY", 0, 1).setDuration(300);
        animeGY.setCurrentPlayTime((long) (j * 300 * 1.25));
    }

    @Override
    public final void pullToRefresh() {
        mSubHeaderText.setText(mPullLabel);
    }

    @Override
    public final void refreshing() {
        mSubHeaderText.setText(mRefreshingLabel);

        if (animP == null) {
            mImgPerson.setImageResource(R.drawable.refreshing_anim);
            animP = (AnimationDrawable) mImgPerson.getDrawable();
        }
        animP.start();
        if (mImgGoods.getVisibility() == View.VISIBLE) {
            mImgGoods.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public final void releaseToRefresh() {
        mSubHeaderText.setText(mReleaseLabel);
    }

    @Override
    public final void reset() {
        if (animP != null) {
            animP.stop();
            animP = null;
        }
        mImgPerson.setImageResource(R.drawable.app_refresh_people_0);
        if (mImgGoods.getVisibility() == View.VISIBLE) {
            mImgGoods.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void setLastUpdatedLabel(CharSequence label) {
        mSubHeaderText.setText(label);
    }

    @Override
    public void setPullLabel(CharSequence pullLabel) {
        mPullLabel = pullLabel;
    }

    @Override
    public void setRefreshingLabel(CharSequence refreshingLabel) {
        mRefreshingLabel = refreshingLabel;
    }

    @Override
    public void setReleaseLabel(CharSequence releaseLabel) {
        mReleaseLabel = releaseLabel;
    }

    @Override
    public void setTextTypeface(Typeface tf) {
        mHeaderText.setTypeface(tf);
    }
}