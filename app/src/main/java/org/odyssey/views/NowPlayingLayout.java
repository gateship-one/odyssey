package org.odyssey.views;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.odyssey.R;

public class NowPlayingLayout extends RelativeLayout {

    private final ViewDragHelper mDragHelper;

    /**
     * Upper view part which is dragged up & down
     */
    protected View mHeaderView;

    /**
     * Main view of draggable part
     */
    protected View mMainView;

    /**
     * Absolute pixel position of upper layout bound
     */
    private int mTopPosition;

    /**
     * FIXME
     * Offset of drag?
     */
    private float mDragOffset;

    /**
     * Height of non-draggable part.
     * (Layout height - draggable part)
     */
    private int mDragRange;

    private float mInitialMotionY;

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

            mHeaderView.setPivotX(mHeaderView.getWidth());
            mHeaderView.setPivotY(mHeaderView.getHeight());
//            mHeaderView.setScaleX(1 - mDragOffset / 2);
//            mHeaderView.setScaleY(1 - mDragOffset / 2);

//            mDescView.setAlpha(1 - mDragOffset);

            requestLayout();
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
            final int bottomBound = getHeight() - mHeaderView.getHeight() - mHeaderView.getPaddingBottom();

            final int newTop = Math.min(Math.max(top, topBound), bottomBound);
            return newTop;
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

        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();

        boolean isHeaderViewUnder = mDragHelper.isViewUnder(mHeaderView, (int) x, (int) y);
        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                final float dy = y - mInitialMotionY;
                final int slop = mDragHelper.getTouchSlop();
                if (dy * dy < slop * slop && isHeaderViewUnder) {
                    if (mDragOffset == 0) {
                        maximize();
                    } else {
                        minimize();
                    }
                }
                break;
            }
        }

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
        Log.v("DRAGLAYOUT","INFLATE FINISHED");
        mHeaderView = findViewById(R.id.now_playing_headerLayout);
        mMainView = findViewById(R.id.now_playing_bodyLayout);

        mCoverImage = (ImageView)findViewById(R.id.now_playing_cover);
        mPlaylistView = (CurrentPlaylistView)findViewById(R.id.now_playing_playlist);


        // Add listeners to playlist button
        ImageButton playlistBtn = (ImageButton)findViewById(R.id.playlistButton);

        // FIXME: Clean up this code a bit. And a nice transition?
        playlistBtn.setOnClickListener(new OnClickListener() {
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
        mDragRange = getHeight() - mHeaderView.getHeight();

        mHeaderView.layout(
                0,
                mTopPosition,
                r,
                mTopPosition + mHeaderView.getMeasuredHeight());

        mMainView.layout(
                0,
                mTopPosition + mHeaderView.getMeasuredHeight(),
                r,
                mTopPosition  + b);
    }
}
