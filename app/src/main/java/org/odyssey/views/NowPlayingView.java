package org.odyssey.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.odyssey.R;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.NowPlayingInformation;
import org.odyssey.playbackservice.PlaybackService;
import org.odyssey.playbackservice.PlaybackServiceConnection;
import org.odyssey.playbackservice.managers.PlaybackStatusHelper;
import org.odyssey.utils.CoverBitmapGenerator;

import java.util.Timer;
import java.util.TimerTask;

public class NowPlayingView extends RelativeLayout implements SeekBar.OnSeekBarChangeListener, PopupMenu.OnMenuItemClickListener {

    private final ViewDragHelper mDragHelper;

    /**
     * Upper view part which is dragged up & down
     */
    private View mHeaderView;

    /**
     * Main view of draggable part
     */
    private View mMainView;

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
    private ImageView mTopCoverImage;

    private CurrentPlaylistView mPlaylistView;

    private PlaybackServiceConnection mServiceConnection = null;
    private NowPlayingReceiver mNowPlayingReceiver = null;
    private CoverBitmapGenerator mCoverGenerator = null;
    private Timer mRefreshTimer = null;

    // Dragstatus receiver (usually the hosting activity)
    private NowPlayingDragStatusReceiver mDragStatusReceiver = null;

    // buttons
    ImageButton mTopPlayPauseButton;
    ImageButton mTopPlaylistButton;
    ImageButton mTopMenuButton;

    // bottom buttons
    ImageButton mBottomRepeatButton;
    ImageButton mBottomPreviousButton;
    ImageButton mBottomPlayPauseButton;
    ImageButton mBottomNextButton;
    ImageButton mBottomRandomButton;

    // seekbar
    SeekBar mPositionSeekbar;

    // textviews
    TextView mTrackName;
    TextView mTrackArtistName;
    TextView mTrackAlbumName;
    TextView mElapsedTime;
    TextView mDuration;

    private String mLastAlbumName;

    public NowPlayingView(Context context) {
        this(context, null, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs, int defStyle) {
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            try {
                mServiceConnection.getPBS().seekTo(progress);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.view_nowplaying_action_shuffleplaylist:
                try {
                    mServiceConnection.getPBS().shufflePlaylist();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.view_nowplaying_action_clearplaylist:
                try {
                    mServiceConnection.getPBS().clearPlaylist();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            case R.id.view_nowplaying_action_saveplaylist:
                SavePlaylistDialog dlg = new SavePlaylistDialog();

                dlg.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "SavePlaylistDialog");
                return true;
            default:
                return false;
        }
    }

    public void savePlaylist(String playlistName) {

        // call pbs and save current playlist to mediastore
        try {
            mServiceConnection.getPBS().savePlaylist(playlistName);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
                    if ( mDragStatusReceiver != null ) {
                        mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
                    }
                } else {
                    // bottom
                    mDraggedDownButtons.setVisibility(VISIBLE);
                    mDraggedUpButtons.setVisibility(INVISIBLE);
                    if ( mDragStatusReceiver != null ) {
                        mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
                    }
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

        // top buttons
        mTopPlayPauseButton = (ImageButton) findViewById(R.id.now_playing_topPlayPauseButton);
        mTopPlaylistButton = (ImageButton) findViewById(R.id.now_playing_topPlaylistButton);
        mTopMenuButton = (ImageButton) findViewById(R.id.now_playing_topMenuButton);

        // bottom buttons
        mBottomRepeatButton = (ImageButton) findViewById(R.id.now_playing_bottomRepeatButton);
        mBottomPreviousButton = (ImageButton) findViewById(R.id.now_playing_bottomPreviousButton);
        mBottomPlayPauseButton = (ImageButton) findViewById(R.id.now_playing_bottomPlayPauseButton);
        mBottomNextButton = (ImageButton) findViewById(R.id.now_playing_bottomNextButton);
        mBottomRandomButton = (ImageButton) findViewById(R.id.now_playing_bottomRandomButton);

        mCoverImage = (ImageView) findViewById(R.id.now_playing_cover);
        mTopCoverImage = (ImageView) findViewById(R.id.now_playing_topCover);
        mPlaylistView = (CurrentPlaylistView) findViewById(R.id.now_playing_playlist);

        mDraggedUpButtons = (LinearLayout) findViewById(R.id.now_playing_layout_dragged_up);
        mDraggedDownButtons = (LinearLayout) findViewById(R.id.now_playing_layout_dragged_down);

        // textviews
        mTrackName = (TextView) findViewById(R.id.now_playing_trackName);
        mTrackAlbumName = (TextView) findViewById(R.id.now_playing_trackAlbum);
        mTrackArtistName = (TextView) findViewById(R.id.now_playing_trackArtist);

        mElapsedTime = (TextView) findViewById(R.id.now_playing_elapsedTime);
        mDuration = (TextView) findViewById(R.id.now_playing_duration);

        // seekbar
        mPositionSeekbar = (SeekBar) findViewById(R.id.now_playing_seekBar);
        mPositionSeekbar.setOnSeekBarChangeListener(this);

        // set dragging part default to bottom
        mDragOffset = 1.0f;
        mDraggedUpButtons.setVisibility(INVISIBLE);
        mDraggedDownButtons.setVisibility(VISIBLE);
        mDraggedUpButtons.setAlpha(0.0f);

        // add listener to top playpause button
        mTopPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().togglePause();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // Add listeners to top playlist button
        // FIXME: Clean up this code a bit. And a nice transition?
        mTopPlaylistButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlaylistView.getVisibility() == View.INVISIBLE) {
                    mPlaylistView.setVisibility(View.VISIBLE);
                    TypedValue typedValue = new TypedValue();
                    getContext().getTheme().resolveAttribute(R.attr.odyssey_color_accent, typedValue, true);
                    mTopPlaylistButton.setImageTintList(ColorStateList.valueOf(typedValue.data));
                } else {
                    mPlaylistView.setVisibility(View.INVISIBLE);
                    mTopPlaylistButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorTextLight)));
                }
            }
        });

        // Add listener to top menu button
        mTopMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdditionalOptionsMenu(v);
            }
        });

        // Add listener to bottom repeat button
        mBottomRepeatButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    int repeat = (mServiceConnection.getPBS().getRepeat() == PlaybackService.REPEATSTATE.REPEAT_ALL.ordinal()) ? PlaybackService.REPEATSTATE.REPEAT_OFF.ordinal() : PlaybackService.REPEATSTATE.REPEAT_ALL.ordinal();

                    mServiceConnection.getPBS().setRepeat(repeat);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // Add listener to bottom previous button
        mBottomPreviousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().previous();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // Add listener to bottom playpause button
        mBottomPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().togglePause();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // Add listener to bottom next button
        mBottomNextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    mServiceConnection.getPBS().next();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        // Add listener to bottom random button
        mBottomRandomButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    int random = (mServiceConnection.getPBS().getRandom() == PlaybackService.RANDOMSTATE.RANDOM_ON.ordinal()) ? PlaybackService.RANDOMSTATE.RANDOM_OFF.ordinal() : PlaybackService.RANDOMSTATE.RANDOM_ON.ordinal();

                    mServiceConnection.getPBS().setRandom(random);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        mCoverGenerator = new CoverBitmapGenerator(getContext(), new CoverReceiverClass());
    }

    private void showAdditionalOptionsMenu(View v) {
        PopupMenu menu = new PopupMenu(getContext(), v);

        menu.inflate(R.menu.popup_menu_nowplaying_view);
        menu.setOnMenuItemClickListener(this);
        menu.show();
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

    public void onPause() {
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer.purge();
            mRefreshTimer = null;
        }
        if (mNowPlayingReceiver != null) {
            getContext().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
    }

    public void onResume() {
        if (mNowPlayingReceiver != null) {
            getContext().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        mNowPlayingReceiver = new NowPlayingReceiver();
        getContext().getApplicationContext().registerReceiver(mNowPlayingReceiver, new IntentFilter(PlaybackStatusHelper.MESSAGE_NEWTRACKINFORMATION));
        // get the playbackservice
        mServiceConnection = new PlaybackServiceConnection(getContext().getApplicationContext());
        mServiceConnection.setNotifier(new ServiceConnectionListener());
        mServiceConnection.openConnection();
    }

    private void updateStatus(TrackModel newTrack) {

        // get current track
        TrackModel currentTrack = newTrack;
        if ( newTrack == null ) {
            try {
                currentTrack = mServiceConnection.getPBS().getCurrentSong();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (currentTrack == null) {
            currentTrack = new TrackModel();
        }
        // set tracktitle, album, artist and albumcover
        mTrackName.setText(currentTrack.getTrackName());

        mTrackAlbumName.setText(currentTrack.getTrackAlbumName());
        if (!currentTrack.getTrackAlbumName().equals(mLastAlbumName)) {
            mCoverImage.setImageResource(R.drawable.cover_placeholder);
            mTopCoverImage.setImageResource(R.drawable.cover_placeholder_96dp);
            mCoverGenerator.getImage(currentTrack);
        }
        mLastAlbumName = currentTrack.getTrackAlbumName();
        mTrackArtistName.setText(currentTrack.getTrackArtistName());

        // calculate duration in minutes and seconds
        String seconds = String.valueOf((currentTrack.getTrackDuration() % 60000) / 1000);

        String minutes = String.valueOf(currentTrack.getTrackDuration() / 60000);

        if (seconds.length() == 1) {
            mDuration.setText(minutes + ":0" + seconds);
        } else {
            mDuration.setText(minutes + ":" + seconds);
        }

        // set up seekbar
        mPositionSeekbar.setMax((int) currentTrack.getTrackDuration());

        updateSeekBar();

        updateDurationView();

        try {
            final boolean isRandom = mServiceConnection.getPBS().getRandom() == 1;
            final boolean songPlaying = mServiceConnection.getPBS().getPlaying() == 1;
            final boolean isRepeat = mServiceConnection.getPBS().getRepeat() == 1;

            Activity activity = (Activity) getContext();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update imagebuttons
                        if (songPlaying) {
                            mTopPlayPauseButton.setImageResource(R.drawable.ic_pause_24dp);
                            mBottomPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_fill_24dp);
                        } else {
                            mTopPlayPauseButton.setImageResource(R.drawable.ic_play_arrow_24dp);
                            mBottomPlayPauseButton.setImageResource(R.drawable.ic_play_circle_fill_24dp);
                        }
                        if (isRepeat) {
                            TypedValue typedValue = new TypedValue();
                            getContext().getTheme().resolveAttribute(R.attr.odyssey_color_accent,typedValue,true);
                            mBottomRepeatButton.setImageTintList(ColorStateList.valueOf(typedValue.data));
                        } else {
                            mBottomRepeatButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorTextLight)));
                        }
                        if (isRandom) {
                            TypedValue typedValue = new TypedValue();
                            getContext().getTheme().resolveAttribute(R.attr.odyssey_color_accent,typedValue,true);
                            mBottomRandomButton.setImageTintList(ColorStateList.valueOf(typedValue.data));
                        } else {
                            mBottomRandomButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorTextLight)));
                        }

                    }
                });
            }

        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateSeekBar() {
        try {
            mPositionSeekbar.setProgress(mServiceConnection.getPBS().getTrackPosition());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateDurationView() {
        // calculate duration in minutes and seconds
        String seconds = "";
        String minutes = "";
        try {
            if (mServiceConnection != null) {
                seconds = String.valueOf((mServiceConnection.getPBS().getTrackPosition() % 60000) / 1000);
                minutes = String.valueOf(mServiceConnection.getPBS().getTrackPosition() / 60000);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (seconds.length() == 1) {
            mElapsedTime.setText(minutes + ":0" + seconds);
        } else {
            mElapsedTime.setText(minutes + ":" + seconds);
        }
    }

    public void registerDragStatusReceiver(NowPlayingDragStatusReceiver receiver) {
        mDragStatusReceiver = receiver;
        if (mDragOffset == 0.0f) {
            // top
            if ( mDragStatusReceiver != null ) {
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
            }
        } else {
            // bottom
            if ( mDragStatusReceiver != null ) {
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
            }
        }

    }

    private class ServiceConnectionListener implements PlaybackServiceConnection.ConnectionNotifier {

        @Override
        public void onConnect() {
            updateStatus(null);
            if (mRefreshTimer != null) {
                mRefreshTimer.cancel();
                mRefreshTimer.purge();
                mRefreshTimer = null;
            }
            mRefreshTimer = new Timer();
            mRefreshTimer.scheduleAtFixedRate(new RefreshTask(), 0, 500);
            mPlaylistView.registerPBServiceConnection(mServiceConnection);
        }

        @Override
        public void onDisconnect() {
            // TODO Auto-generated method stub

        }

    }

    private class NowPlayingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackStatusHelper.MESSAGE_NEWTRACKINFORMATION)) {
                // Extract nowplaying info
                final NowPlayingInformation info = intent.getParcelableExtra(PlaybackStatusHelper.INTENT_NOWPLAYINGNAME);

                Activity activity = (Activity) getContext();
                if (activity != null) {

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // notify playlist has changed
                            mPlaylistView.playlistChanged(info);
                            // update views
                            updateStatus(info.getCurrentTrack());
                        }
                    });
                }
            }
        }
    }

    private class CoverReceiverClass implements CoverBitmapGenerator.CoverBitmapListener {

        @Override
        public void receiveBitmap(final BitmapDrawable bm) {
            if (bm != null) {
                Activity activity = (Activity) getContext();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            mCoverImage.setImageDrawable(bm);
                            mTopCoverImage.setImageDrawable(bm);
                        }
                    });
                }
            }
        }
    }

    private class RefreshTask extends TimerTask {

        @Override
        public void run() {
            Activity activity = (Activity) getContext();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDurationView();
                        updateSeekBar();
                    }
                });
            }

        }
    }

    public interface NowPlayingDragStatusReceiver {
        enum DRAG_STATUS {DRAGGED_UP,DRAGGED_DOWN}
        void onStatusChanged(DRAG_STATUS status);
    }

    private class SavePlaylistDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final EditText editTextPlaylistName = new EditText(getActivity());
            editTextPlaylistName.setText(R.string.default_playlist_title);
            builder.setView(editTextPlaylistName);

            builder.setMessage(R.string.dialog_save_playlist).setPositiveButton(R.string.dialog_action_save, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // accept playlist name
                    String playlistName = editTextPlaylistName.getText().toString();
                    savePlaylist(playlistName);
                }
            }).setNegativeButton(R.string.dialog_action_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog dont create playlist
                }
            });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }
}
