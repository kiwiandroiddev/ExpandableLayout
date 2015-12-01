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
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
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
    public static final String KEY_INSTANCE_STATE = "instanceState";
    public static final String KEY_IS_OPENED = "isOpened";

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
                    collapse();
                } else {
                    expand();
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

    private void expand() {
        if (isAnimationRunning)
            return;

        contentLayout.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int targetHeight = contentLayout.getMeasuredHeight();
        contentLayout.getLayoutParams().height = 0;
        contentLayout.setVisibility(VISIBLE);

        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (expansionProgressListener != null) {
                    expansionProgressListener.onExpansionProgress(interpolatedTime);
                }

                if (interpolatedTime == 1)
                    isOpened = true;
                contentLayout.getLayoutParams().height = (interpolatedTime == 1) ?
                        LayoutParams.WRAP_CONTENT :
                        (int) (targetHeight * interpolatedTime);
                contentLayout.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        if (interpolator != null) {
            animation.setInterpolator(interpolator);
        }
        animation.setDuration(duration);
        contentLayout.startAnimation(animation);

        isAnimationRunning = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isAnimationRunning = false;
            }
        }, duration);
    }

    private void collapse() {
        if (isAnimationRunning)
            return;

        final int initialHeight = contentLayout.getMeasuredHeight();
        animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (expansionProgressListener != null) {
                    expansionProgressListener.onExpansionProgress(1.0f - interpolatedTime);
                }

                if (interpolatedTime == 1) {
                    contentLayout.setVisibility(View.GONE);
                    isOpened = false;
                } else {
                    contentLayout.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    contentLayout.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        if (interpolator != null) {
            animation.setInterpolator(interpolator);
        }
        animation.setDuration(duration);
        contentLayout.startAnimation(animation);

        isAnimationRunning = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isAnimationRunning = false;
            }
        }, duration);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putBoolean(KEY_IS_OPENED, isOpened);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            initOpenState(bundle.getBoolean(KEY_IS_OPENED, this.isOpened));
            state = bundle.getParcelable(KEY_INSTANCE_STATE);
        }
        super.onRestoreInstanceState(state);
    }

    private void initOpenState(boolean opened) {
        if (isAnimationRunning) {
            return;
        }

        if (opened != isOpened) {
            isOpened = opened;
            contentLayout.setVisibility(isOpened ? VISIBLE : GONE);
        }
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
        expand();
    }

    public void hide() {
        collapse();
    }

    @Override
    public void setLayoutAnimationListener(Animation.AnimationListener animationListener) {
        animation.setAnimationListener(animationListener);
    }
}

