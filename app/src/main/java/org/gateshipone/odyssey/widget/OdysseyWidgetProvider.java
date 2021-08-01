/*
 * Copyright (C) 2020 Team Gateship-One
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

package org.gateshipone.odyssey.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.OdysseyMainActivity;
import org.gateshipone.odyssey.activities.OdysseySplashActivity;
import org.gateshipone.odyssey.artwork.ArtworkManager;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;
import org.gateshipone.odyssey.playbackservice.PlaybackService;
import org.gateshipone.odyssey.playbackservice.managers.PlaybackServiceStatusHelper;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;


public class
OdysseyWidgetProvider extends AppWidgetProvider {
    private static final String TAG = OdysseyWidgetProvider.class.getSimpleName();

    @NonNull
    private static NowPlayingInformation mLastInfo = new NowPlayingInformation();
    private static Bitmap mLastCover = null;

    private static boolean mHideArtwork;

    private final static int INTENT_OPENGUI = 0;
    private final static int INTENT_PREVIOUS = 1;
    private final static int INTENT_PLAYPAUSE = 2;
    private final static int INTENT_NEXT = 3;

    @Override
    public synchronized void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        setWidgetContent(mLastInfo, context);
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    /**
     * This is the broadcast receiver for NowPlayingInformation objects sent by the PBS
     *
     * @param context Context used for this receiver
     * @param intent  Intent containing the NowPlayingInformation as a payload.
     */
    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final String action = intent.getAction();

        if (action == null) {
            return;
        }
        // Type checks
        switch (action) {
            case PlaybackServiceStatusHelper.MESSAGE_NEWTRACKINFORMATION: {
                // Extract the payload from the intent
                NowPlayingInformation info = intent.getParcelableExtra(PlaybackServiceStatusHelper.INTENT_NOWPLAYINGNAME);

                // Check if a payload was sent
                if (null != info) {
                    PlaybackService.PLAYSTATE state = info.getPlayState();
                    if (state == PlaybackService.PLAYSTATE.STOPPED || state == PlaybackService.PLAYSTATE.RESUMED) {
                        mLastInfo = new NowPlayingInformation();
                        mLastCover = null;

                        setWidgetContent(mLastInfo, context);

                    } else if (state == PlaybackService.PLAYSTATE.PLAYING || state == PlaybackService.PLAYSTATE.PAUSE) {
                        // Refresh the widget with the new information
                        setWidgetContent(info, context);

                        // Save the information for later usage (when the asynchronous bitmap loader finishes)
                        mLastInfo = info;
                    }
                }
            }
            break;
            case PlaybackServiceStatusHelper.MESSAGE_HIDE_ARTWORK_CHANGED: {
                mHideArtwork = intent.getBooleanExtra(PlaybackServiceStatusHelper.MESSAGE_EXTRA_HIDE_ARTWORK_CHANGED_VALUE, context.getResources().getBoolean(R.bool.pref_hide_artwork_default));
                NowPlayingInformation info = mLastInfo;
                // Forces an update
                mLastInfo = new NowPlayingInformation();
                setWidgetContent(info, context);
                mLastInfo = info;
            }
            break;
            case ArtworkManager.ACTION_NEW_ARTWORK_READY: {
                // Check if the new artwork matches the currently playing track. If so reload the artwork because it is now available.
                long albumId = intent.getLongExtra(ArtworkManager.INTENT_EXTRA_KEY_ALBUM_ID, -1);
                if (!mHideArtwork && mLastInfo.getCurrentTrack().getTrackAlbumId() == albumId) {
                    CoverBitmapLoader coverLoader = new CoverBitmapLoader(context, new CoverReceiver(context));
                    coverLoader.getImage(mLastInfo.getCurrentTrack(), -1, -1);
                    mLastCover = null;
                }
            }
            break;
        }
    }

    /**
     * Updates the widget by creating a new RemoteViews object and setting all the intents for the
     * buttons and the TextViews correctly.
     *
     * @param info    Object holding the information about the current played song.
     * @param context The application context.
     */
    private synchronized void setWidgetContent(@NonNull NowPlayingInformation info, Context context) {
        boolean nowPlaying = false;
        // Create a new RemoteViews object containing the default widget layout
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_odyssey);

        TrackModel item = info.getCurrentTrack();
        views.setTextViewText(R.id.widget_track_title, item.getTrackDisplayedName());
        views.setTextViewText(R.id.widget_artist_album, item.getTrackArtistName());

        switch (info.getPlayState()) {
            case PLAYING:
            case PAUSE: {
                if (!mHideArtwork) {
                    // Check if the tracks album changed
                    if (mLastInfo.getCurrentTrack().getTrackAlbumId() != item.getTrackAlbumId()) {
                        // Album changed, it is necessary to start the image loader
                        views.setImageViewResource(R.id.widget_covert_artwork, R.drawable.odyssey_notification);

                        mLastCover = null;

                        CoverBitmapLoader coverLoader = new CoverBitmapLoader(context, new CoverReceiver(context));
                        coverLoader.getImage(item, -1, -1);
                    } else if (mLastCover != null) {
                        // Reuse the image from last calls because the album is the same
                        views.setImageViewBitmap(R.id.widget_covert_artwork, mLastCover);
                    }
                } else {
                    // Hide artwork requested
                    mLastCover = null;
                    views.setImageViewResource(R.id.widget_covert_artwork, R.drawable.odyssey_notification);
                }
            }
            break;
            case RESUMED:
            case STOPPED:
                mLastCover = null;
                views.setImageViewResource(R.id.widget_covert_artwork, R.drawable.odyssey_notification);
                break;
        }

        // Set the images of the play button dependent on the playback state.
        PlaybackService.PLAYSTATE playState = info.getPlayState();

        if (playState == PlaybackService.PLAYSTATE.PLAYING) {
            // Show pause icon
            nowPlaying = true;
            views.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_pause_48dp);
        } else {
            // Show play icon
            views.setImageViewResource(R.id.widget_play_btn, R.drawable.ic_play_arrow_48dp);
        }

        // set button actions
        // Main action
        Intent mainIntent = new Intent(context, OdysseySplashActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (nowPlaying) {
            // add intent only if playing is active
            mainIntent.putExtra(OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.REQUESTEDVIEW.NOWPLAYING.ordinal());
        }
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_covert_artwork, mainPendingIntent);

        // Play/Pause action
        Intent playPauseIntent = new Intent(context, PlaybackService.class);
        playPauseIntent.putExtra("action", PlaybackService.ACTION_TOGGLEPAUSE);
        PendingIntent playPausePendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playPausePendingIntent = PendingIntent.getForegroundService(context, INTENT_PLAYPAUSE, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            playPausePendingIntent = PendingIntent.getService(context, INTENT_PLAYPAUSE, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        views.setOnClickPendingIntent(R.id.widget_play_btn, playPausePendingIntent);

        // Previous song action
        Intent prevIntent = new Intent(context, PlaybackService.class);
        prevIntent.putExtra("action", PlaybackService.ACTION_PREVIOUS);
        PendingIntent prevPendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prevPendingIntent = PendingIntent.getForegroundService(context, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            prevPendingIntent = PendingIntent.getService(context, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        views.setOnClickPendingIntent(R.id.widget_prev_btn, prevPendingIntent);

        // Next song action
        Intent nextIntent = new Intent(context, PlaybackService.class);
        nextIntent.putExtra("action", PlaybackService.ACTION_NEXT);
        PendingIntent nextPendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nextPendingIntent = PendingIntent.getForegroundService(context, INTENT_NEXT, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            nextPendingIntent = PendingIntent.getService(context, INTENT_NEXT, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        views.setOnClickPendingIntent(R.id.widget_next_btn, nextPendingIntent);

        // Send the widget to the launcher by transferring the remote view
        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, OdysseyWidgetProvider.class), views);
    }

    private class CoverReceiver implements CoverBitmapLoader.CoverBitmapReceiver {
        private Context mContext;

        CoverReceiver(Context context) {
            mContext = context;
        }

        /**
         * Sets the global image variable for this track and recall the update method to refresh
         * the views.
         *
         * @param bm Bitmap fetched for the currently running track.
         */
        @Override
        public void receiveAlbumBitmap(Bitmap bm) {
            // Check if a valid image was found.
            if (bm != null) {
                // Set the globally used variable
                mLastCover = bm;

                // Call the update method to refresh the view
                setWidgetContent(mLastInfo, mContext);
            }
        }

        @Override
        public void receiveArtistBitmap(Bitmap bm) {

        }
    }
}
