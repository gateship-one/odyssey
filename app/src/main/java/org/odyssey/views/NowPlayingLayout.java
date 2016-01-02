package org.odyssey.views;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.odyssey.R;

public class NowPlayingLayout extends RelativeLayout {

    private final ViewDragHelper mDragHelper;

    /**
     * Upper view part which is dragged up & down
     */
    private View mHeaderView;

    /**
     * Main view of draggable part
     */
    private View mMainView;

    private ImageButton mTopPlayPauseButton;
    private ImageButton mTopPlaylistButton;
    private ImageButton mTopMenuButton;

    private LinearLayout mDraggedUpButtons;
    private LinearLayout mDraggedDownButtons;

    /**
     * Absolute pixel position of upper layout bound
     */
    private int mTopPosition;

    /**
     * relative dragposition
     */
    private float mDragOffset;

    /**
     * Height of non-draggable part.
     * (Layout height - draggable part)
     */
    private int mDragRange;

    private ImageView mCoverImage;

    private CurrentPlaylistView mPlaylistView;

    public NowPlayingLayout(Context context) {
        this(context, null, 0);
    }

    public NowPlayingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NowPlayingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, 1f, new BottomDragCallbackHelper());
    }

    public void maximize() {
        smoothSlideTo(0f);
    }

    public void minimize() {
        smoothSlideTo(1f);
    }

    boolean smoothSlideTo(float slideOffset) {
        final int topBound = getPaddingTop();
        int y = (int) (topBound + slideOffset * mDragRange);

        if (mDragHelper.smoothSlideViewTo(mHeaderView, mHeaderView.getLeft(), y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    private class BottomDragCallbackHelper extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mHeaderView;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            mTopPosition = top;

            mDragOffset = (float) top / mDragRange;

            requestLayout();

            // Set inverse alpha values for smooth layout transition.
            // Visibility still needs to be set otherwise parts of the buttons
            // are not clickable.
            mDraggedDownButtons.setAlpha(mDragOffset);
            mDraggedUpButtons.setAlpha(1.0f-mDragOffset);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top = getPaddingTop();
            if (yvel > 0 || (yvel == 0 && mDragOffset > 0.5f)) {
                top += mDragRange;
            }
            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
            invalidate();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return mDragRange;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            int bottomBound = getHeight() - mHeaderView.getHeight() - mHeaderView.getPaddingBottom();

            final int newTop = Math.min(Math.max(top, topBound), bottomBound);
            
            return newTop;
        }

        @Override
        public void onViewDragStateChanged(int state) {

            super.onViewDragStateChanged(state);

            if(state == ViewDragHelper.STATE_IDLE) {
                if (mDragOffset == 0.0f) {
                    // top
                    mDraggedDownButtons.setVisibility(INVISIBLE);
                    mDraggedUpButtons.setVisibility(VISIBLE);
                } else {
                    // bottom
                    mDraggedDownButtons.setVisibility(VISIBLE);
                    mDraggedUpButtons.setVisibility(INVISIBLE);
                }
            } else {
                /* Show both layouts to enable a smooth transition via
                alpha values of the layouts.
                 */
                mDraggedDownButtons.setVisibility(VISIBLE);
                mDraggedUpButtons.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDragHelper.processTouchEvent(ev);

        final float x = ev.getX();
        final float y = ev.getY();

        boolean isHeaderViewUnder = mDragHelper.isViewUnder(mHeaderView, (int) x, (int) y);

        return isHeaderViewUnder && isViewHit(mHeaderView, (int) x, (int) y) || isViewHit(mMainView, (int) x, (int) y);
    }


    private boolean isViewHit(View view, int x, int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHeaderView = findViewById(R.id.now_playing_headerLayout);
        mMainView = findViewById(R.id.now_playing_bodyLayout);

        mTopPlayPauseButton = (ImageButton) findViewById(R.id.now_playing_topPlayPauseButton);
        mTopPlaylistButton = (ImageButton) findViewById(R.id.now_playing_topPlaylistButton);
        mTopMenuButton = (ImageButton) findViewById(R.id.now_playing_topMenuButton);

        mCoverImage = (ImageView)findViewById(R.id.now_playing_cover);
        mPlaylistView = (CurrentPlaylistView)findViewById(R.id.now_playing_playlist);

        mDraggedUpButtons = (LinearLayout)findViewById(R.id.now_playing_layout_dragged_up);
        mDraggedDownButtons = (LinearLayout)findViewById(R.id.now_playing_layout_dragged_down);

        // set dragging part default to bottom
        mDragOffset = 1.0f;
        mDraggedUpButtons.setVisibility(INVISIBLE);
        mDraggedDownButtons.setVisibility(VISIBLE);
        mDraggedUpButtons.setAlpha(0.0f);

        // Add listeners to playlist button
        // FIXME: Clean up this code a bit. And a nice transition?
        mTopPlaylistButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if ( mPlaylistView.getVisibility() == View.INVISIBLE) {
                    mPlaylistView.setVisibility(View.VISIBLE);
                } else {
                    mPlaylistView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mDragRange = (getHeight() - mHeaderView.getHeight());

        int newTop = mTopPosition;

        // fix height at top or bottom if state idle
        if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
            newTop = (int) (mDragRange * mDragOffset);
        }

        mHeaderView.layout(
                0,
                newTop,
                r,
                newTop + mHeaderView.getMeasuredHeight());

        mMainView.layout(
                0,
                newTop + mHeaderView.getMeasuredHeight(),
                r,
                newTop  + b);
    }
}
