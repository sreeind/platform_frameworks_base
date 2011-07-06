/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.recent;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.util.Log;
import android.util.Slog;
import android.view.View;

/* package */ class Choreographer implements Animator.AnimatorListener {
    // should group this into a multi-property animation
    private static final int OPEN_DURATION = 136;
    private static final int CLOSE_DURATION = 250;
    private static final String TAG = RecentsPanelView.TAG;
    private static final boolean DEBUG = RecentsPanelView.DEBUG;

    boolean mVisible;
    int mPanelHeight;
    View mRootView;
    View mScrimView;
    View mContentView;
    AnimatorSet mContentAnim;
    Animator.AnimatorListener mListener;

    // the panel will start to appear this many px from the end
    final int HYPERSPACE_OFFRAMP = 200;

    public Choreographer(View root, View scrim, View content, Animator.AnimatorListener listener) {
        mRootView = root;
        mScrimView = scrim;
        mContentView = content;
        mListener = listener;
    }

    void createAnimation(boolean appearing) {
        float start, end;

        if (RecentsPanelView.DEBUG) Log.e(TAG, "createAnimation()", new Exception());

        // 0: on-screen
        // height: off-screen
        float y = mContentView.getTranslationY();
        if (appearing) {
            // we want to go from near-the-top to the top, unless we're half-open in the right
            // general vicinity
            start = (y < HYPERSPACE_OFFRAMP) ? y : HYPERSPACE_OFFRAMP;
            end = 0;
        } else {
            start = y;
            end = y + HYPERSPACE_OFFRAMP;
        }

        Animator posAnim = ObjectAnimator.ofFloat(mContentView, "translationY",
                start, end);
        posAnim.setInterpolator(appearing
                ? new android.view.animation.DecelerateInterpolator(2.5f)
                : new android.view.animation.AccelerateInterpolator(2.5f));

        Animator glowAnim = ObjectAnimator.ofFloat(mContentView, "alpha",
                mContentView.getAlpha(), appearing ? 1.0f : 0.0f);
        glowAnim.setInterpolator(appearing
                ? new android.view.animation.AccelerateInterpolator(1.0f)
                : new android.view.animation.DecelerateInterpolator(1.0f));

        Animator bgAnim = ObjectAnimator.ofInt(mScrimView.getBackground(),
                "alpha", appearing ? 0 : 255, appearing ? 255 : 0);

        mContentAnim = new AnimatorSet();
        mContentAnim
                .play(bgAnim)
                .with(glowAnim)
                .with(posAnim);
        mContentAnim.setDuration(appearing ? OPEN_DURATION : CLOSE_DURATION);
        mContentAnim.addListener(this);
        if (mListener != null) {
            mContentAnim.addListener(mListener);
        }
    }

    void startAnimation(boolean appearing) {
        if (DEBUG) Slog.d(TAG, "startAnimation(appearing=" + appearing + ")");

        createAnimation(appearing);

        mContentView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mContentAnim.start();

        mVisible = appearing;
    }

    void jumpTo(boolean appearing) {
        mContentView.setTranslationY(appearing ? 0 : mPanelHeight);
    }

    public void setPanelHeight(int h) {
        if (DEBUG) Slog.d(TAG, "panelHeight=" + h);
        mPanelHeight = h;
    }

    public void onAnimationCancel(Animator animation) {
        if (DEBUG) Slog.d(TAG, "onAnimationCancel");
        // force this to zero so we close the window
        mVisible = false;
    }

    public void onAnimationEnd(Animator animation) {
        if (DEBUG) Slog.d(TAG, "onAnimationEnd");
        if (!mVisible) {
            mRootView.setVisibility(View.GONE);
        }
        mContentView.setLayerType(View.LAYER_TYPE_NONE, null);
        mContentAnim = null;
    }

    public void onAnimationRepeat(Animator animation) {
    }

    public void onAnimationStart(Animator animation) {
    }
}