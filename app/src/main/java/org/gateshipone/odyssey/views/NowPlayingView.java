/*
 * Copyright (C) 2018 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.views;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.TooltipCompat;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.dialogs.ChooseBookmarkDialog;
import org.gateshipone.odyssey.dialogs.ChoosePlaylistDialog;
import org.gateshipone.odyssey.dialogs.ErrorDialog;
import org.gateshipone.odyssey.dialogs.TimeDurationDialog;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;
import org.gateshipone.odyssey.playbackservice.PlaybackService;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;
import org.gateshipone.odyssey.playbackservice.managers.PlaybackServiceStatusHelper;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.gateshipone.odyssey.utils.ThemeUtils;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class NowPlayingView extends RelativeLayout implements SeekBar.OnSeekBarChangeListener, PopupMenu.OnMenuItemClickListener, ArtworkManager.onNewAlbumImageListener,
        ArtworkManager.onNewArtistImageListener, SharedPreferences.OnSharedPreferenceChangeListener {

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

    /**
     * Main cover imageview
     */
    private AlbumArtistView mCoverImage;

    /**
     * Small cover image, part of the draggable header
     */
    private ImageView mTopCoverImage;

    /**
     * View that contains the playlist ListVIew
     */
    private CurrentPlaylistView mPlaylistView;

    /**
     * ViewSwitcher used for switching between the main cover image and the playlist
     */
    private ViewSwitcher mViewSwitcher;

    /**
     * Connection to the PBS for requesting information about current song/status.
     */
    private PlaybackServiceConnection mServiceConnection = null;

    /**
     * Receiver for NowPlayingInformation items (that include information about state changes, song
     * changes).
     */
    private NowPlayingReceiver mNowPlayingReceiver = null;

    /**
     * Asynchronous loader for coverimages for TrackItems.
     */
    private CoverBitmapLoader mCoverLoader = null;

    /**
     * Timer that periodically updates the state of the view (seekbar)
     */
    private Timer mRefreshTimer = null;

    /**
     * Observer for information about the state of the draggable part of this view.
     * This is probably the Activity of which this view is part of.
     * (Used for smooth statusbar transition and state resuming)
     */
    private NowPlayingDragStatusReceiver mDragStatusReceiver = null;

    /**
     * Top buttons in the draggable header part.
     */
    private ImageButton mTopPlayPauseButton;
    private ImageButton mTopPlaylistButton;

    /**
     * Buttons in the bottom part of the view
     */
    private ImageButton mBottomRepeatButton;
    private ImageButton mBottomPlayPauseButton;
    private ImageButton mBottomRandomButton;

    /**
     * Seekbar used for seeking and informing the user of the current playback position.
     */
    private SeekBar mPositionSeekbar;

    private LinearLayout mHeaderTextLayout;

    /**
     * Various textviews for track information
     */
    private TextView mTrackTitle;
    private TextView mTrackSubtitle;
    private TextView mElapsedTime;
    private TextView mDuration;

    /**
     * Name of the last played album. This is used for a optimization of cover fetching. If album
     * did not change with a track, there is no need to refetch the cover.
     */
    private TrackModel mLastTrack;

    /**
     * Flag whether artworks should be hidden.
     */
    private boolean mHideArtwork = true;

    /**
     * Flag whether the nowplaying hint should be shown. This should only be true if the app was used the first time.
     */
    private boolean mShowNPVHint = false;

    /**
     * Flag whether the views switches between album cover and artist image
     */
    private boolean mShowArtistImage = false;

    /**
     * The state of the playbackservice.
     */
    private PlaybackService.PLAYSTATE mPlaybackServiceState;

    /**
     * Saves the setting if the english or regional wikipedia is requested
     */
    private boolean mUseEnglishWikipedia;

    public NowPlayingView(Context context) {
        this(context, null, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, 1f, new BottomDragCallbackHelper());
        mPlaybackServiceState = PlaybackService.PLAYSTATE.STOPPED;

        mLastTrack = new TrackModel();

        mServiceConnection = new PlaybackServiceConnection(getContext().getApplicationContext());
        mServiceConnection.setNotifier(new ServiceConnectionListener());
    }

    /**
     * Minimizes the view with an animation.
     */
    public void minimize() {
        smoothSlideTo(1f);
    }

    /**
     * Slides the view to the given position.
     *
     * @param slideOffset 0.0 - 1.0 (0.0 is dragged down, 1.0 is dragged up)
     * @return If the move was successful
     */
    private boolean smoothSlideTo(float slideOffset) {
        final int topBound = getPaddingTop();
        int y = (int) (topBound + slideOffset * mDragRange);

        if (mDragHelper.smoothSlideViewTo(mHeaderView, mHeaderView.getLeft(), y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    /**
     * Called if the user drags the seekbar to a new position or the seekbar is altered from
     * outside. Just do some seeking, if the action is done by the user.
     *
     * @param seekBar  Seekbar of which the progress was changed.
     * @param progress The new position of the seekbar.
     * @param fromUser If the action was initiated by the user.
     */
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

    /**
     * Called if the user starts moving the seekbar. We do not handle this for now.
     *
     * @param seekBar SeekBar that is used for dragging.
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    /**
     * Called if the user ends moving the seekbar.
     *
     * @param seekBar SeekBar that is used for dragging.
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
        updateTrackPosition();
    }

    /**
     * Set the position of the draggable view to the given offset. This is done without an animation.
     * Can be used to resume a certain state of the view (e.g. on resuming an activity)
     *
     * @param offset Offset to position the view to from 0.0 - 1.0 (0.0 dragged up, 1.0 dragged down)
     */
    public void setDragOffset(float offset) {
        if (offset > 1.0f || offset < 0.0f) {
            mDragOffset = 1.0f;
        }
        mDragOffset = offset;

        invalidate();
        requestLayout();

        // Set inverse alpha values for smooth layout transition.
        // Visibility still needs to be set otherwise parts of the buttons
        // are not clickable.
        mDraggedDownButtons.setAlpha(mDragOffset);
        mDraggedUpButtons.setAlpha(1.0f - mDragOffset);

        // Calculate the margin to smoothly resize text field
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mHeaderTextLayout.getLayoutParams();
        layoutParams.setMarginEnd((int) (mTopPlaylistButton.getWidth() * (1.0 - mDragOffset)));
        mHeaderTextLayout.setLayoutParams(layoutParams);

        // Notify the observers about the change
        if (mDragStatusReceiver != null) {
            mDragStatusReceiver.onDragPositionChanged(offset);
        }

        if (mDragOffset == 0.0f) {
            // top
            mDraggedDownButtons.setVisibility(INVISIBLE);
            mDraggedUpButtons.setVisibility(VISIBLE);
            mCoverImage.setVisibility(VISIBLE);
            if (mDragStatusReceiver != null) {
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
            }
        } else {
            // bottom
            mDraggedDownButtons.setVisibility(VISIBLE);
            mDraggedUpButtons.setVisibility(INVISIBLE);
            mCoverImage.setVisibility(INVISIBLE);
            if (mDragStatusReceiver != null) {
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
            }
        }
    }

    /**
     * Menu click listener. This method gets called when the user selects an item of the popup menu (right top corner).
     *
     * @param item MenuItem that was clicked.
     * @return Returns true if the item was handled by this method. False otherwise.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
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
                // open dialog in order to save the current playlist as a playlist in the mediastore
                ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog();
                choosePlaylistDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChoosePlaylistDialog");
                return true;
            case R.id.view_nowplaying_action_createbookmark:
                // open dialog in order to save the current playlist as a bookmark in the odyssey db
                ChooseBookmarkDialog chooseBookmarkDialog = new ChooseBookmarkDialog();
                chooseBookmarkDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChooseBookmarkDialog");
                return true;
            case R.id.view_nowplaying_action_startequalizer:
                // start the audio equalizer
                Activity activity = (Activity) getContext();
                if (activity != null) {
                    Intent startEqualizerIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                    startEqualizerIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getContext().getPackageName());
                    startEqualizerIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);

                    try {
                        startEqualizerIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mServiceConnection.getPBS().getAudioSessionID());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    try {
                        activity.startActivityForResult(startEqualizerIntent, 0);
                    } catch (ActivityNotFoundException e) {
                        ErrorDialog equalizerNotFoundDlg = ErrorDialog.newInstance(R.string.dialog_equalizer_not_found_title, R.string.dialog_equalizer_not_found_message);
                        equalizerNotFoundDlg.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "EqualizerNotFoundDialog");
                    }
                }
                return true;
            case R.id.action_wikipedia_album: {
                openWikipediaPage(true);
                return true;
            }
            case R.id.action_wikipedia_artist: {
                openWikipediaPage(false);
                return true;
            }
            case R.id.view_nowplaying_action_share_track: {
                shareCurrentTrack();
                return true;
            }
            case R.id.view_nowplaying_action_start_sleep_timer: {
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                final long durationMS = sharedPref.getLong(getContext().getString(R.string.pref_last_used_sleep_timer_key), 0);

                final TimeDurationDialog dialog = TimeDurationDialog.newInstance(durationMS);
                dialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "TimeDurationDialog");
                return true;
            }
            case R.id.view_nowplaying_action_cancel_sleep_timer: {
                try {
                    mServiceConnection.getPBS().cancelSleepTimer();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
            }
            default:
                return false;
        }
    }

    private void openWikipediaPage(boolean showAlbum) {
        TrackModel track;
        try {
            track = mServiceConnection.getPBS().getCurrentSong();
        } catch (RemoteException e) {
            return;
        }

        String name;
        if (showAlbum) {
            name = track.getTrackAlbumName();
        } else {
            name = track.getTrackArtistName();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (mUseEnglishWikipedia) {
            intent.setData(Uri.parse("https://en.wikipedia.org/wiki/" + name));
        } else {
            intent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/" + name));
        }
        getContext().startActivity(intent);
    }

    /**
     * Saves the current playlist. This just calls the PBS and asks him to save the playlist.
     *
     * @param playlistName Name of the playlist to save.
     */
    public void savePlaylist(String playlistName) {

        // call pbs and save current playlist to mediastore
        try {
            mServiceConnection.getPBS().savePlaylist(playlistName);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Saves the current state as a bookmark. This just calls the PBS and asks him to save the state.
     *
     * @param bookmarkTitle Name of the bookmark to store.
     */
    public void createBookmark(String bookmarkTitle) {
        // call pbs and create bookmark with the given title for the current state
        try {
            mServiceConnection.getPBS().createBookmark(bookmarkTitle);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void newAlbumImage(AlbumModel album) {
        if (!mHideArtwork && mLastTrack.getTrackAlbumKey().equals(album.getAlbumKey())) {
            mCoverLoader.getAlbumImage(album, mCoverImage.getWidth(), mCoverImage.getHeight());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getContext().getString(R.string.pref_hide_artwork_key))) {
            mHideArtwork = sharedPreferences.getBoolean(key, getContext().getResources().getBoolean(R.bool.pref_hide_artwork_default));

            if (!mHideArtwork) {
                // Start cover loading
                try {
                    mCoverLoader.getImage(mServiceConnection.getPBS().getCurrentSong(), mCoverImage.getWidth(), mCoverImage.getHeight());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                // Hide artwork here
                showPlaceholderImage();
                mCoverImage.clearArtistImage();
            }
            mPlaylistView.hideArtwork(mHideArtwork);
        } else if (key.equals(getContext().getString(R.string.pref_use_english_wikipedia_key))) {
            mUseEnglishWikipedia = sharedPreferences.getBoolean(key, getContext().getResources().getBoolean(R.bool.pref_use_english_wikipedia_default));
        } else if (key.equals(getContext().getString(R.string.pref_show_npv_artist_image_key))) {
            mShowArtistImage = sharedPreferences.getBoolean(key, getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));

            // Show artist image if artwork is requested
            if (mShowArtistImage && !mHideArtwork) {
                mCoverLoader.getArtistImage(mLastTrack, mCoverImage.getWidth(), mCoverImage.getHeight());
            } else {
                // Hide artist image
                mCoverImage.clearArtistImage();
            }
        }
    }

    @Override
    public void newArtistImage(ArtistModel artist) {
        if (mShowArtistImage && !mHideArtwork && mLastTrack.getTrackArtistName().equals(artist.getArtistName())) {
            mCoverLoader.getArtistImage(artist, mCoverImage.getWidth(), mCoverImage.getHeight());
        }
    }

    /**
     * Observer class for changes of the drag status.
     */
    private class BottomDragCallbackHelper extends ViewDragHelper.Callback {

        /**
         * Checks if a given child view should act as part of the drag. This is only true for the header
         * element of this View-class.
         *
         * @param child     Child that was touched by the user
         * @param pointerId Id of the pointer used for touching the view.
         * @return True if the view should be allowed to be used as dragging part, false otheriwse.
         */
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (child == mHeaderView) {
                // start the refresh task if state is playing
                if (mPlaybackServiceState == PlaybackService.PLAYSTATE.PLAYING) {
                    startRefreshTask();
                }
                // report the change of the view
                if (mDragStatusReceiver != null) {
                    // Disable scrolling of the text views
                    mTrackTitle.setSelected(false);
                    mTrackSubtitle.setSelected(false);

                    mDragStatusReceiver.onStartDrag();
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * Called if the position of the draggable view is changed. This rerequests the layout of the view.
         *
         * @param changedView The view that was changed.
         * @param left        Left position of the view (should stay constant in this case)
         * @param top         Top position of the view
         * @param dx          Dimension of the width
         * @param dy          Dimension of the height
         */
        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            // Save the heighest top position of this view.
            mTopPosition = top;

            // Calculate the new drag offset
            mDragOffset = (float) top / mDragRange;

            // Relayout this view
            requestLayout();

            // Set inverse alpha values for smooth layout transition.
            // Visibility still needs to be set otherwise parts of the buttons
            // are not clickable.
            mDraggedDownButtons.setAlpha(mDragOffset);
            mDraggedUpButtons.setAlpha(1.0f - mDragOffset);

            // Calculate the margin to smoothly resize text field
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mHeaderTextLayout.getLayoutParams();
            layoutParams.setMarginEnd((int) (mTopPlaylistButton.getWidth() * (1.0 - mDragOffset)));
            mHeaderTextLayout.setLayoutParams(layoutParams);

            if (mDragStatusReceiver != null) {
                mDragStatusReceiver.onDragPositionChanged(mDragOffset);
            }

        }

        /**
         * Called if the user lifts the finger(release the view) with a velocity
         *
         * @param releasedChild View that was released
         * @param xvel          x position of the view
         * @param yvel          y position of the view
         */
        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            int top = getPaddingTop();
            if (yvel > 0 || (yvel == 0 && mDragOffset > 0.5f)) {
                top += mDragRange;
            }
            // Snap the view to top/bottom position
            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
            invalidate();
        }

        /**
         * Returns the range within a view is allowed to be dragged.
         *
         * @param child Child to get the dragrange for
         * @return Dragging range
         */
        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            return mDragRange;
        }


        /**
         * Clamps (limits) the view during dragging to the top or bottom(plus header height)
         *
         * @param child Child that is being dragged
         * @param top   Top position of the dragged view
         * @param dy    Delta value of the height
         * @return The limited height value (or valid position inside the clamped range).
         */
        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            final int topBound = getPaddingTop();
            int bottomBound = getHeight() - mHeaderView.getHeight() - mHeaderView.getPaddingBottom();

            return Math.min(Math.max(top, topBound), bottomBound);
        }

        /**
         * Called when the drag state changed. Informs observers that it is either dragged up or down.
         * Also sets the visibility of button groups in the header
         *
         * @param state New drag state
         */
        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);

            // Check if the new state is the idle state. If then notify the observer (if one is registered)
            if (state == ViewDragHelper.STATE_IDLE) {
                // Enable scrolling of the text views
                mTrackTitle.setSelected(true);
                mTrackSubtitle.setSelected(true);

                if (mDragOffset == 0.0f) {
                    // Called when dragged up
                    mDraggedDownButtons.setVisibility(INVISIBLE);
                    mDraggedUpButtons.setVisibility(VISIBLE);
                    mCoverImage.setVisibility(VISIBLE);
                    if (mDragStatusReceiver != null) {
                        mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
                    }
                } else {
                    // Called when dragged down
                    mDraggedDownButtons.setVisibility(VISIBLE);
                    mDraggedUpButtons.setVisibility(INVISIBLE);
                    mCoverImage.setVisibility(INVISIBLE);
                    if (mDragStatusReceiver != null) {
                        mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
                    }

                    // stop refresh task
                    stopRefreshTask();
                }
            } else {
                /*
                 * Show both layouts to enable a smooth transition via
                 * alpha values of the layouts.
                 */
                mDraggedDownButtons.setVisibility(VISIBLE);
                mDraggedUpButtons.setVisibility(VISIBLE);
                mCoverImage.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * Informs the dragHelper about a scroll movement.
     */
    @Override
    public void computeScroll() {
        // Continues the movement of the View Drag Helper and sets the invalidation for this View
        // if the animation is not finished and needs continuation
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Handles touch inputs to some views, to make sure, the ViewDragHelper is called.
     *
     * @param ev Touch input event
     * @return True if handled by this view or false otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Call the drag helper
        mDragHelper.processTouchEvent(ev);

        // Get the position of the new touch event
        final float x = ev.getX();
        final float y = ev.getY();

        // Check if the position lies in the bounding box of the header view (which is draggable)
        boolean isHeaderViewUnder = mDragHelper.isViewUnder(mHeaderView, (int) x, (int) y);

        // Check if drag is handled by the helper, or the header or mainview. If not notify the system that input is not yet handled.
        return isHeaderViewUnder && isViewHit(mHeaderView, (int) x, (int) y) || isViewHit(mMainView, (int) x, (int) y);
    }


    /**
     * Checks if an input to coordinates lay within a View
     *
     * @param view View to check with
     * @param x    x value of the input
     * @param y    y value of the input
     * @return True if input coordinates lay within the given view
     */
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

    /**
     * Asks the ViewGroup about the size of all its children and paddings around.
     *
     * @param widthMeasureSpec  The width requirements for this view
     * @param heightMeasureSpec The height requirements for this view
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        // Calculate the margin to smoothly resize text field
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mHeaderTextLayout.getLayoutParams();
        layoutParams.setMarginEnd((int) (mTopPlaylistButton.getMeasuredHeight() * (1.0 - mDragOffset)));
        mHeaderTextLayout.setLayoutParams(layoutParams);
    }

    /**
     * Called after the layout inflater is finished.
     * Sets all global view variables to the ones inflatd.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get both main views (header and bottom part)
        mHeaderView = findViewById(R.id.now_playing_headerLayout);
        mMainView = findViewById(R.id.now_playing_bodyLayout);

        // header buttons
        mTopPlayPauseButton = findViewById(R.id.now_playing_topPlayPauseButton);
        mTopPlaylistButton = findViewById(R.id.now_playing_topPlaylistButton);
        ImageButton topMenuButton = findViewById(R.id.now_playing_topMenuButton);

        // bottom buttons
        mBottomRepeatButton = findViewById(R.id.now_playing_bottomRepeatButton);
        mBottomPlayPauseButton = findViewById(R.id.now_playing_bottomPlayPauseButton);
        mBottomRandomButton = findViewById(R.id.now_playing_bottomRandomButton);
        ImageButton bottomPreviousButton = findViewById(R.id.now_playing_bottomPreviousButton);
        ImageButton bottomNextButton = findViewById(R.id.now_playing_bottomNextButton);

        // Main cover image
        mCoverImage = findViewById(R.id.now_playing_cover);
        // Small header cover image
        mTopCoverImage = findViewById(R.id.now_playing_topCover);

        // View with the ListView of the playlist
        mPlaylistView = findViewById(R.id.now_playing_playlist);

        // view switcher for cover and playlist view
        mViewSwitcher = findViewById(R.id.now_playing_view_switcher);

        // Button container for the buttons shown if dragged up
        mDraggedUpButtons = findViewById(R.id.now_playing_layout_dragged_up);
        // Button container for the buttons shown if dragged down
        mDraggedDownButtons = findViewById(R.id.now_playing_layout_dragged_down);

        // textviews
        mTrackTitle = findViewById(R.id.now_playing_track_title);
        // For marquee scrolling the TextView need selected == true
        mTrackTitle.setSelected(true);
        mTrackSubtitle = findViewById(R.id.now_playing_track_subtitle);
        // For marquee scrolling the TextView need selected == true
        mTrackSubtitle.setSelected(true);

        // Textviews directly under the seekbar
        mElapsedTime = findViewById(R.id.now_playing_elapsedTime);
        mDuration = findViewById(R.id.now_playing_duration);

        mHeaderTextLayout = findViewById(R.id.now_playing_header_textLayout);

        // seekbar (position)
        mPositionSeekbar = findViewById(R.id.now_playing_seekBar);
        mPositionSeekbar.setOnSeekBarChangeListener(this);

        // set dragging part default to bottom
        mDragOffset = 1.0f;
        mDraggedUpButtons.setVisibility(INVISIBLE);
        mDraggedDownButtons.setVisibility(VISIBLE);
        mDraggedUpButtons.setAlpha(0.0f);

        // add listener to top playpause button
        mTopPlayPauseButton.setOnClickListener(arg0 -> {
            try {
                mServiceConnection.getPBS().togglePause();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        // Add listeners to top playlist button
        mTopPlaylistButton.setOnClickListener(v -> {

            if (mViewSwitcher.getCurrentView() != mPlaylistView) {
                setViewSwitcherStatus(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.PLAYLIST_VIEW);
            } else {
                setViewSwitcherStatus(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.COVER_VIEW);
            }

            // report the change of the view
            if (mDragStatusReceiver != null) {
                // set view status
                if (mViewSwitcher.getCurrentView() == mCoverImage) {
                    // cover image is shown
                    mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.COVER_VIEW);
                } else {
                    // playlist view is shown
                    mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.PLAYLIST_VIEW);
                }
            }
        });
        TooltipCompat.setTooltipText(mTopPlaylistButton, getResources().getString(R.string.action_npv_show_playlist));

        // Add listener to top menu button
        topMenuButton.setOnClickListener(this::showAdditionalOptionsMenu);
        TooltipCompat.setTooltipText(topMenuButton, getResources().getString(R.string.action_npv_more_options));

        // Add listener to bottom repeat button
        mBottomRepeatButton.setOnClickListener(arg0 -> {
            try {
                mServiceConnection.getPBS().toggleRepeat();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        // Add listener to bottom previous button
        bottomPreviousButton.setOnClickListener(arg0 -> {
            try {
                mServiceConnection.getPBS().previous();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        // Add listener to bottom playpause button
        mBottomPlayPauseButton.setOnClickListener(arg0 -> {
            try {
                mServiceConnection.getPBS().togglePause();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        // Add listener to bottom next button
        bottomNextButton.setOnClickListener(arg0 -> {
            try {
                mServiceConnection.getPBS().next();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        // Add listener to bottom random button
        mBottomRandomButton.setOnClickListener(arg0 -> {
            try {
                mServiceConnection.getPBS().toggleRandom();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        mCoverLoader = new CoverBitmapLoader(getContext(), new CoverReceiverClass());

        invalidate();
    }

    /**
     * Called to open the popup menu on the top right corner.
     *
     * @param v View to which the popup menu should be attached
     */
    private void showAdditionalOptionsMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        // Inflate the menu from a menu xml file
        popupMenu.inflate(R.menu.popup_menu_nowplaying_view);
        // Set the main NowPlayingView as a listener (directly implements callback)
        popupMenu.setOnMenuItemClickListener(this);
        Menu menu = popupMenu.getMenu();

        // Check if the current view is the cover or the playlist. If it is the playlist hide its actions.
        // If the viewswitcher only has one child the dual pane layout is used
        if (mViewSwitcher.getDisplayedChild() == 0 && (mViewSwitcher.getChildCount() > 1)) {
            menu.setGroupEnabled(R.id.action_group_playlist, false);
            menu.setGroupVisible(R.id.action_group_playlist, false);
        }

        boolean hasActiveSleepTimer = false;
        try {
            hasActiveSleepTimer = mServiceConnection.getPBS().hasActiveSleepTimer();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (hasActiveSleepTimer) {
            menu.findItem(R.id.view_nowplaying_action_start_sleep_timer).setVisible(false);
            menu.findItem(R.id.view_nowplaying_action_cancel_sleep_timer).setVisible(true);
        } else {
            menu.findItem(R.id.view_nowplaying_action_start_sleep_timer).setVisible(true);
            menu.findItem(R.id.view_nowplaying_action_cancel_sleep_timer).setVisible(false);
        }

        // Open the menu itself
        popupMenu.show();
    }


    /**
     * Called when a layout is requested from the graphics system.
     *
     * @param changed If the layout is changed (size, ...)
     * @param l       Left position
     * @param t       Top position
     * @param r       Right position
     * @param b       Bottom position
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Calculate the maximal range that the view is allowed to be dragged
        mDragRange = (getMeasuredHeight() - mHeaderView.getMeasuredHeight());

        // New temporary top position, to fix the view at top or bottom later if state is idle.
        int newTop = mTopPosition;

        // fix height at top or bottom if state idle
        if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
            newTop = (int) (mDragRange * mDragOffset);
        }

        // Request the upper part of the NowPlayingView (header)
        mHeaderView.layout(
                0,
                newTop,
                r,
                newTop + mHeaderView.getMeasuredHeight());

        // Request the lower part of the NowPlayingView (main part)
        mMainView.layout(
                0,
                newTop + mHeaderView.getMeasuredHeight(),
                r,
                newTop + b);
    }

    /**
     * Stop the refresh timer when the view is not visible to the user anymore.
     * Unregister the receiver for NowPlayingInformation intends, not needed anylonger.
     */
    public void onPause() {
        if (mRefreshTimer != null) {
            // Cancel the running refresh timer
            mRefreshTimer.cancel();
            mRefreshTimer.purge();
            // Clean the reference
            mRefreshTimer = null;
        }
        if (mNowPlayingReceiver != null) {
            // Unregister the broadcast receiver
            getContext().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewAlbumImageListener(this);
        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewArtistImageListener(this);

        mServiceConnection.closeConnection();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Resumes refreshing operation because the view is visible to the user again.
     * Also registers to the NowPlayingInformation intends again.
     */
    public void onResume() {
        // Create new Receiver
        if (mNowPlayingReceiver != null) {
            getContext().getApplicationContext().unregisterReceiver(mNowPlayingReceiver);
            mNowPlayingReceiver = null;
        }
        mNowPlayingReceiver = new NowPlayingReceiver();
        getContext().getApplicationContext().registerReceiver(mNowPlayingReceiver, new IntentFilter(PlaybackServiceStatusHelper.MESSAGE_NEWTRACKINFORMATION));
        // get the playbackservice, when the connection is successfully established the timer gets restarted
        mServiceConnection.openConnection();

        // Reenable scrolling views after resuming
        if (mTrackTitle != null) {
            mTrackTitle.setSelected(true);
        }

        if (mTrackSubtitle != null) {
            mTrackSubtitle.setSelected(true);
        }

        invalidate();
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewAlbumImageListener(this);
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewArtistImageListener(this);

        // Register shared preference listener
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        mHideArtwork = sharedPref.getBoolean(getContext().getString(R.string.pref_hide_artwork_key), getContext().getResources().getBoolean(R.bool.pref_hide_artwork_default));
        mShowNPVHint = sharedPref.getBoolean(getContext().getResources().getString(R.string.pref_show_npv_hint), true);

        mPlaylistView.hideArtwork(mHideArtwork);

        sharedPref.registerOnSharedPreferenceChangeListener(this);

        mUseEnglishWikipedia = sharedPref.getBoolean(getContext().getString(R.string.pref_use_english_wikipedia_key), getContext().getResources().getBoolean(R.bool.pref_use_english_wikipedia_default));

        mShowArtistImage = sharedPref.getBoolean(getContext().getString(R.string.pref_show_npv_artist_image_key), getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));
    }

    /**
     * Updates all sub-views with the new pbs states
     *
     * @param info the new pbs states including the current track
     */
    private void updateStatus(NowPlayingInformation info) {

        // If called without a nowplayinginformation, ask the PBS directly for the information.
        // After the establishing of the service connection it can be that a track is playing and we've not yet received the NowPlayingInformation
        if (info == null) {
            try {
                info = mServiceConnection.getPBS().getNowPlayingInformation();
            } catch (RemoteException e) {
                e.printStackTrace();

                // an error occured so create a default instance to clear the view
                info = new NowPlayingInformation();
            }
        }

        // notify playlist has changed
        mPlaylistView.playlistChanged(info);

        // get current track
        TrackModel currentTrack = info.getCurrentTrack();

        // set tracktitle, album, artist and albumcover
        mTrackTitle.setText(currentTrack.getTrackDisplayedName());


        // Check if the album title changed. If true, start the cover generator thread.
        if (!currentTrack.getTrackAlbumKey().equals(mLastTrack.getTrackAlbumKey())) {
            // Show placeholder until image is loaded
            showPlaceholderImage();

            if (!mHideArtwork) {
                // Start the cover loader
                mCoverLoader.getImage(currentTrack, mCoverImage.getWidth(), mCoverImage.getHeight());
            }
        }

        // If artist name changed check for a new image
        if (!currentTrack.getTrackArtistName().equals(mLastTrack.getTrackArtistName())) {
            mCoverImage.clearArtistImage();
            if (!mHideArtwork && mShowArtistImage) {
                // Start the cover loader
                mCoverLoader.getArtistImage(currentTrack, mCoverImage.getWidth(), mCoverImage.getHeight());
            }
        }
        // Save the last track
        mLastTrack = currentTrack;

        // Set the artist of the track
        String trackInformation = "";
        if (!currentTrack.getTrackArtistName().isEmpty() && !currentTrack.getTrackAlbumName().isEmpty()) {
            trackInformation = getResources().getString(R.string.track_title_template, currentTrack.getTrackArtistName(), currentTrack.getTrackAlbumName());
        } else if (!currentTrack.getTrackArtistName().isEmpty()) {
            trackInformation = currentTrack.getTrackArtistName();
        } else if (!currentTrack.getTrackAlbumName().isEmpty()) {
            trackInformation = currentTrack.getTrackAlbumName();
        }
        mTrackSubtitle.setText(trackInformation);

        // Calculate the margin to avoid cut off textviews
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mHeaderTextLayout.getLayoutParams();
        layoutParams.setMarginEnd((int) (mTopPlaylistButton.getWidth() * (1.0 - mDragOffset)));
        mHeaderTextLayout.setLayoutParams(layoutParams);

        // Set the track duration
        mDuration.setText(FormatHelper.formatTracktimeFromMS(getContext(), currentTrack.getTrackDuration()));

        // set up seekbar (set maximum value, track total duration)
        mPositionSeekbar.setMax((int) currentTrack.getTrackDuration());

        // update seekbar and elapsedview
        updateTrackPosition();

        // save the state
        mPlaybackServiceState = info.getPlayState();

        // update play buttons
        switch (mPlaybackServiceState) {
            case PLAYING:
                mTopPlayPauseButton.setImageResource(R.drawable.ic_pause_48dp);
                mBottomPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_fill_48dp);

                // show the hint if necessary
                if (mShowNPVHint) {
                    showHint();
                }

                // start refresh task if view is visible
                if (mDragOffset == 0.0f) {
                    startRefreshTask();
                }

                break;
            case PAUSE:
            case RESUMED:
            case STOPPED:
                mTopPlayPauseButton.setImageResource(R.drawable.ic_play_arrow_48dp);
                mBottomPlayPauseButton.setImageResource(R.drawable.ic_play_circle_fill_48dp);

                // stop refresh task
                stopRefreshTask();

                break;
        }

        // update repeat button
        switch (info.getRepeat()) {
            case REPEAT_OFF:
                mBottomRepeatButton.setImageResource(R.drawable.ic_repeat_24dp);
                mBottomRepeatButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent)));
                break;
            case REPEAT_ALL:
                mBottomRepeatButton.setImageResource(R.drawable.ic_repeat_24dp);
                mBottomRepeatButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), android.R.attr.colorAccent)));
                break;
            case REPEAT_TRACK:
                mBottomRepeatButton.setImageResource(R.drawable.ic_repeat_one_24dp);
                mBottomRepeatButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), android.R.attr.colorAccent)));
                break;
        }

        // update random button
        switch (info.getRandom()) {
            case RANDOM_OFF:
                mBottomRandomButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent)));
                break;
            case RANDOM_ON:
                mBottomRandomButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), android.R.attr.colorAccent)));
                break;
        }
    }

    /**
     * Get the current trackposition from the PBS and update the seekbar and the elapsed view.
     */
    private void updateTrackPosition() {
        // get trackposition
        int trackPosition = 0;
        try {
            trackPosition = mServiceConnection.getPBS().getTrackPosition();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // update the seekbar
        mPositionSeekbar.setProgress(trackPosition);
        // update the elapsed view
        mElapsedTime.setText(FormatHelper.formatTracktimeFromMS(getContext(), trackPosition));
    }

    /**
     * Stop the refresh task if one exists.
     */
    private void stopRefreshTask() {
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer.purge();
            mRefreshTimer = null;
        }
    }

    /**
     * Start a periodically time to update the seekbar. If one is already existing overwrite it.
     */
    private void startRefreshTask() {
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer.purge();
            mRefreshTimer = null;
        }
        mRefreshTimer = new Timer();
        mRefreshTimer.scheduleAtFixedRate(new RefreshTask(), 0, 500);
    }

    /**
     * Can be used to register an observer to this view, that is notified when a change of the dragstatus, offset happens.
     *
     * @param receiver Observer to register, only one observer at a time is possible.
     */
    public void registerDragStatusReceiver(NowPlayingDragStatusReceiver receiver) {
        mDragStatusReceiver = receiver;
        // Initial status notification
        if (mDragStatusReceiver != null) {

            // set drag status
            if (mDragOffset == 0.0f) {
                // top
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
            } else {
                // bottom
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
            }

            // set view status
            if (mViewSwitcher.getCurrentView() == mCoverImage) {
                // cover image is shown
                mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.COVER_VIEW);
            } else {
                // playlist view is shown
                mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.PLAYLIST_VIEW);
            }
        }
    }

    /**
     * Set the viewswitcher of cover/playlist view to the requested state.
     *
     * @param view the view which should be displayed.
     */
    public void setViewSwitcherStatus(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS view) {
        int color = 0;

        switch (view) {
            case COVER_VIEW:
                // change the view only if the requested view is not displayed
                if (mViewSwitcher.getCurrentView() != mCoverImage) {
                    mViewSwitcher.showNext();
                }
                color = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);
                TooltipCompat.setTooltipText(mTopPlaylistButton, getResources().getString(R.string.action_npv_show_playlist));
                break;
            case PLAYLIST_VIEW:
                // change the view only if the requested view is not displayed
                if (mViewSwitcher.getCurrentView() != mPlaylistView) {
                    mViewSwitcher.showNext();
                }
                color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
                TooltipCompat.setTooltipText(mTopPlaylistButton, getResources().getString(R.string.action_npv_show_cover));
                break;
        }

        // tint the button according to the requested view
        mTopPlaylistButton.setImageTintList(ColorStateList.valueOf(color));
    }

    /**
     * Observers if the connection to the PBS is successfully established. If so the status updates
     * can be started.
     */
    private class ServiceConnectionListener implements PlaybackServiceConnection.ConnectionNotifier {

        /**
         * Called when the service connection to the PBS is established.
         */
        @Override
        public void onConnect() {
            // Initial update of the current track information. Null as Track results in the update status
            // method requesting the current track.

            // Register the service connection to the PlaylistView (it needs it to start its listadapter)
            mPlaylistView.registerPBServiceConnection(mServiceConnection);

            // Already running in main UI thread handler here. No need for runOnUIThread
            updateStatus(null);
        }

        /**
         * Called when the service is disconnected.
         */
        @Override
        public void onDisconnect() {
            // Do nothing for now.
            mPlaylistView.unregisterPBSeviceConnection();
        }

    }

    /**
     * Private BroadcastReceiver for handling PBS NowPlayingInformation broadcasts to update the shown
     * information when a new track starts or the PBS status changes because of other reasons (repeat,random state, ...)
     */
    private class NowPlayingReceiver extends BroadcastReceiver {

        /**
         * Called when receiving a NowPlayingInformation parcelable.
         *
         * @param context Context of the received broadcasts
         * @param intent  Intent with the message content
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PlaybackServiceStatusHelper.MESSAGE_NEWTRACKINFORMATION)) {
                // Extract nowplaying info
                final NowPlayingInformation info = intent.getParcelableExtra(PlaybackServiceStatusHelper.INTENT_NOWPLAYINGNAME);

                Activity activity = (Activity) getContext();
                if (activity != null) {
                    // Run the updateStatus method in the UI thread because it touches all the gui elements.
                    activity.runOnUiThread(() -> {
                        // update views
                        updateStatus(info);
                    });
                }
            }
        }
    }

    /**
     * Private class that handles when the CoverGenerator finishes its fetching of cover images.
     */
    private class CoverReceiverClass implements CoverBitmapLoader.CoverBitmapReceiver {

        /**
         * Called when a bitmap is created
         *
         * @param bm Bitmap ready for use in the UI
         */
        @Override
        public void receiveAlbumBitmap(final Bitmap bm) {
            if (bm != null) {
                Activity activity = (Activity) getContext();
                if (activity != null) {
                    // Run on the UI thread of the activity because we are modifying gui elements.
                    activity.runOnUiThread(() -> {
                        // Set the main cover image
                        mCoverImage.setAlbumImage(bm);
                        // Set the small header image
                        mTopCoverImage.setImageBitmap(bm);
                    });
                }
            }
        }

        /**
         * Called when a bitmap is created
         *
         * @param bm Bitmap ready for use in the UI
         */
        @Override
        public void receiveArtistBitmap(final Bitmap bm) {
            if (bm != null) {
                Activity activity = (Activity) getContext();
                if (activity != null) {
                    // Run on the UI thread of the activity because we are modifying gui elements.
                    activity.runOnUiThread(() -> mCoverImage.setArtistImage(bm));
                }
            }
        }
    }


    private void showPlaceholderImage() {
        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_background_primary);

        Drawable drawable = getResources().getDrawable(R.drawable.cover_placeholder, null);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);

        // Show the placeholder image until the cover fetch process finishes
        mCoverImage.clearAlbumImage();

        tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.odyssey_color_text_accent);

        drawable = getResources().getDrawable(R.drawable.cover_placeholder_96dp, null);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);


        // The same for the small header image
        mTopCoverImage.setImageDrawable(drawable);
    }

    /**
     * This method will drag up the NPV a little for 1 second.
     * <p>
     * This should be called only the first time music is played with Odyssey.
     */
    private void showHint() {
        mShowNPVHint = false;

        SharedPreferences.Editor sharedPrefEditor = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        sharedPrefEditor.putBoolean(getContext().getString(R.string.pref_show_npv_hint), false);
        sharedPrefEditor.apply();

        if (mDragOffset == 1.0f) {
            // show hint only if the npv ist not already dragged up
            smoothSlideTo(0.75f);

            Handler handler = new Handler();
            handler.postDelayed(() -> {
                if (mDragOffset > 0.0f) {
                    smoothSlideTo(1.0f);
                }
            }, 3000);
        }
    }

    /**
     * Simple sharing for the current track.
     * <p>
     * This will only work if the track can be found in the mediastore.
     */
    private void shareCurrentTrack() {
        TrackModel track;
        try {
            track = mServiceConnection.getPBS().getCurrentSong();
        } catch (RemoteException e) {
            return;
        }

        if (track == null) {
            return;
        }

        // get mediastore uri
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // build the uri for the current track
        Uri trackUri = Uri.parse(uri.toString() + "/" + track.getTrackId());

        // set up intent for sharing
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, trackUri);
        shareIntent.setType("audio/*");

        // start sharing
        getContext().startActivity(Intent.createChooser(shareIntent, getContext().getString(R.string.share_chooser_title)));
    }

    /**
     * Private class that handles periodically updates of the duration views (seekbar, textviews)
     */
    private class RefreshTask extends TimerTask {

        @Override
        public void run() {
            Activity activity = (Activity) getContext();
            // Run on the UI thread because we are updating gui elements
            if (activity != null) {
                activity.runOnUiThread(NowPlayingView.this::updateTrackPosition);
            }

        }
    }

    /**
     * Public interface used by observers to be notified about a change in drag state or drag position.
     */
    public interface NowPlayingDragStatusReceiver {
        // Possible values for DRAG_STATUS (up,down)
        enum DRAG_STATUS {
            DRAGGED_UP, DRAGGED_DOWN
        }

        // Possible values for the view in the viewswitcher (cover, playlist)
        enum VIEW_SWITCHER_STATUS {
            COVER_VIEW, PLAYLIST_VIEW
        }

        // Called when the whole view is either completely dragged up or down
        void onStatusChanged(DRAG_STATUS status);

        // Called continuously during dragging.
        void onDragPositionChanged(float pos);

        // Called when the view switcher switches between cover and playlist view
        void onSwitchedViews(VIEW_SWITCHER_STATUS view);

        // Called when the user starts the drag
        void onStartDrag();
    }
}
