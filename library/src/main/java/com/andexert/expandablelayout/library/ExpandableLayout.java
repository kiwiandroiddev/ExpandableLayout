/***********************************************************************************
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2014 Robin Chutaux
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ***********************************************************************************/
package com.andexert.expandablelayout.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class ExpandableLayout extends RelativeLayout {

    public static final int DEFAULT_DURATION_INT_RESOURCE_ID = android.R.integer.config_shortAnimTime;

    public interface OnExpansionProgressListener {
        /**
         * @param layoutExpansion The value of the layout's expansion progress (0.0 - 1.0)
         *                        after it has been run through the interpolation function where
         *                        0.0 = fully collapsed and 1.0 = fully expanded.
         */
        void onExpansionProgress(float layoutExpansion);
    }

    private boolean isAnimationRunning = false;
    private boolean isOpened = false;
    private Integer duration;
    private FrameLayout contentLayout;
    private FrameLayout headerLayout;
    private Animation animation;
    private Interpolator interpolator;

    private OnExpansionProgressListener expansionProgressListener;

    public ExpandableLayout(Context context) {
        super(context);
    }

    public ExpandableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ExpandableLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(final Context context, AttributeSet attrs) {
        if (isInEditMode())
            return;

        final View rootView = View.inflate(context, R.layout.view_expandable, this);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout);

        initDuration(typedArray);
        initInterpolator(typedArray);
        initHeaderLayout(rootView, typedArray);
        initContentLayout(rootView, typedArray);

        typedArray.recycle();
    }

    private void initContentLayout(View rootView, TypedArray typedArray) {
        final int contentID = typedArray.getResourceId(R.styleable.ExpandableLayout_el_contentLayout, -1);
        if (contentID == -1) {
            throw new IllegalArgumentException("ContentLayout cannot be null!");
        }

        final View contentView = View.inflate(getContext(), contentID, null);
        contentLayout = (FrameLayout) rootView.findViewById(R.id.view_expandable_contentLayout);
        contentLayout.addView(contentView);
        contentLayout.setVisibility(GONE);
    }

    private void initHeaderLayout(View rootView, TypedArray typedArray) {
        final int headerID = typedArray.getResourceId(R.styleable.ExpandableLayout_el_headerLayout, -1);
        if (headerID == -1) {
            throw new IllegalArgumentException("HeaderLayout cannot be null!");
        }

        final View headerView = View.inflate(getContext(), headerID, null);
        headerLayout = (FrameLayout) rootView.findViewById(R.id.view_expandable_headerlayout);
        headerLayout.addView(headerView);
        headerLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contentLayout.getVisibility() == VISIBLE) {
                    collapse(contentLayout);
                } else {
                    expand(contentLayout);
                }
            }
        });
    }

    private void initDuration(TypedArray typedArray) {
        duration = typedArray.getInt(R.styleable.ExpandableLayout_el_duration,
                getContext().getResources().getInteger(DEFAULT_DURATION_INT_RESOURCE_ID));
    }

    private void initInterpolator(TypedArray typedArray) {
        final int interpolatorID = typedArray.getResourceId(R.styleable.ExpandableLayout_el_interpolator, -1);
        if (interpolatorID != -1) {
            interpolator = AnimationUtils.loadInterpolator(getContext(), interpolatorID);
        }
    }

    private void expand(final View layout) {
        if (isAnimationRunning)
            return;

        layout.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int targetHeight = layout.getMeasuredHeight();
        layout.getLayoutParams().height = 0;
        layout.setVisibility(VISIBLE);

        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (expansionProgressListener != null) {
                    expansionProgressListener.onExpansionProgress(interpolatedTime);
                }

                if (interpolatedTime == 1)
                    isOpened = true;
                layout.getLayoutParams().height = (interpolatedTime == 1) ?
                        LayoutParams.WRAP_CONTENT :
                        (int) (targetHeight * interpolatedTime);
                layout.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setInterpolator(interpolator);
        animation.setDuration(duration);
        layout.startAnimation(animation);

        isAnimationRunning = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isAnimationRunning = false;
            }
        }, duration);
    }

    private void collapse(final View layout) {
        if (isAnimationRunning)
            return;

        final int initialHeight = layout.getMeasuredHeight();
        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (expansionProgressListener != null) {
                    expansionProgressListener.onExpansionProgress(1.0f - interpolatedTime);
                }

                if (interpolatedTime == 1) {
                    layout.setVisibility(View.GONE);
                    isOpened = false;
                } else {
                    layout.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    layout.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        animation.setInterpolator(interpolator);
        animation.setDuration(duration);
        layout.startAnimation(animation);

        isAnimationRunning = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isAnimationRunning = false;
            }
        }, duration);
    }

    public Boolean isOpened() {
        return isOpened;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getDuration() {
        return duration;
    }

    public FrameLayout getHeaderLayout() {
        return headerLayout;
    }

    public FrameLayout getContentLayout() {
        return contentLayout;
    }

    public void setExpansionProgressListener(OnExpansionProgressListener expansionProgressListener) {
        this.expansionProgressListener = expansionProgressListener;
    }

    public OnExpansionProgressListener getExpansionProgressListener() {
        return expansionProgressListener;
    }

    public void show() {
        expand(contentLayout);
    }

    public void hide() {
        collapse(contentLayout);
    }

    @Override
    public void setLayoutAnimationListener(Animation.AnimationListener animationListener) {
        animation.setAnimationListener(animationListener);
    }
}
