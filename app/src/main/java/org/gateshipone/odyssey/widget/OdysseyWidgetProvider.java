/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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
import android.widget.RemoteViews;

import org.gateshipone.odyssey.activities.OdysseyMainActivity;
import org.gateshipone.odyssey.activities.OdysseySplashActivity;
import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.NowPlayingInformation;
import org.gateshipone.odyssey.playbackservice.PlaybackService;
import org.gateshipone.odyssey.playbackservice.managers.PlaybackServiceStatusHelper;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;

public class
OdysseyWidgetProvider extends AppWidgetProvider {
    private RemoteViews mViews;
    private AppWidgetManager mAppWidgetManager;
    private Context mContext;

    private static TrackModel mLastTrack = null;
    private static NowPlayingInformation mLastInfo;
    private static Bitmap mLastCover = null;

    private final static int INTENT_OPENGUI = 0;
    private final static int INTENT_PREVIOUS = 1;
    private final static int INTENT_PLAYPAUSE = 2;
    private final static int INTENT_NEXT = 3;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        mContext = context;

        // Perform this loop procedure for each App Widget that belongs to this
        // provider
        for (int appWidgetId : appWidgetIds) {
            // Tell the AppWidgetManager to perform an update on the current app widget
            mAppWidgetManager = appWidgetManager;
            if (null != mViews) {
                appWidgetManager.updateAppWidget(appWidgetId, mViews);
            } else {
                setWidgetContent(null);
            }
        }
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        mLastTrack = null;
    }

    /**
     * This is the broadcast receiver for NowPlayingInformation objects sent by the PBS
     *
     * @param context Context used for this receiver
     * @param intent  Intent containing the NowPlayingInformation as a payload.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Save the context to use later
        mContext = context;

        // Type checks
        if (intent.getAction().equals(PlaybackServiceStatusHelper.MESSAGE_NEWTRACKINFORMATION)) {

            // Extract the payload from the intent
            NowPlayingInformation info = intent.getParcelableExtra(PlaybackServiceStatusHelper.INTENT_NOWPLAYINGNAME);

            // Check if a payload was sent
            if (null != info) {
                // Refresh the widget with the new information
                setWidgetContent(info);

                // Save the information for later usage (when the asynchronous bitmap loader finishes)
                mLastInfo = info;
            }
        }
    }

    /**
     * Updates the widget by creating a new RemoteViews object and setting all the intents for the
     * buttons and the TextViews correctly.
     *
     * @param info
     */
    private void setWidgetContent(NowPlayingInformation info) {
        boolean nowPlaying = false;
        // Create a new RemoteViews object containing the default widget layout
        mViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_odyssey_big);

        // Check if valid object
        if (info != null) {
            TrackModel item = info.getCurrentTrack();
            if (item != null) {
                mViews.setTextViewText(R.id.widget_big_trackName, item.getTrackName());
                mViews.setTextViewText(R.id.widget_big_ArtistAlbum, item.getTrackArtistName());

                // Check if the tracks album changed
                if (mLastTrack == null || !mLastTrack.getTrackAlbumKey().equals(item.getTrackAlbumKey())) {
                    // If the albumKey changed, then it is necessary to start the image loader
                    mViews.setImageViewResource(R.id.widget_big_cover, R.drawable.odyssey_notification);
                    CoverBitmapLoader coverLoader = new CoverBitmapLoader(mContext, new CoverReceiver());
                    coverLoader.getImage(item);
                    mLastCover = null;
                } else if (mLastTrack.getTrackAlbumKey().equals(item.getTrackAlbumKey()) && mLastCover != null) {
                    // Reuse the image from last calls if the album is the same
                    mViews.setImageViewBitmap(R.id.widget_big_cover, mLastCover);
                }
            }

            // Set the images of the play button dependent on the playback state.
            PlaybackService.PLAYSTATE playState = info.getPlayState();

            if (playState == PlaybackService.PLAYSTATE.PLAYING) {
                // Show pause icon
                nowPlaying = true;
                mViews.setImageViewResource(R.id.widget_big_play, R.drawable.ic_pause_48dp);
            } else {
                // Show play icon
                mViews.setImageViewResource(R.id.widget_big_play, R.drawable.ic_play_arrow_48dp);
            }

            // Save the last track information to check if the album key changed (see above).
            mLastTrack = item;
        }


        // set button actions
        // Main action
        Intent mainIntent = new Intent(mContext, OdysseySplashActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (nowPlaying) {
            // add intent only if playing is active
            mainIntent.putExtra(OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW);
        }
        PendingIntent mainPendingIntent = PendingIntent.getActivity(mContext, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mViews.setOnClickPendingIntent(R.id.widget_big_cover, mainPendingIntent);

        // Play/Pause action
        Intent playPauseIntent = new Intent(mContext, PlaybackService.class);
        playPauseIntent.putExtra("action", PlaybackService.ACTION_TOGGLEPAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(mContext, INTENT_PLAYPAUSE, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mViews.setOnClickPendingIntent(R.id.widget_big_play, playPausePendingIntent);

        // Previous song action
        Intent prevIntent = new Intent(mContext, PlaybackService.class);
        prevIntent.putExtra("action", PlaybackService.ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getService(mContext, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mViews.setOnClickPendingIntent(R.id.widget_big_previous, prevPendingIntent);

        // Next song action
        Intent nextIntent = new Intent(mContext, PlaybackService.class);
        nextIntent.putExtra("action", PlaybackService.ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(mContext, INTENT_NEXT, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        mViews.setOnClickPendingIntent(R.id.widget_big_next, nextPendingIntent);

        // Send the widget to the launcher by transfering the remote view
        mAppWidgetManager = AppWidgetManager.getInstance(mContext);
        mAppWidgetManager.updateAppWidget(new ComponentName(mContext, OdysseyWidgetProvider.class), mViews);
    }

    private class CoverReceiver implements CoverBitmapLoader.CoverBitmapListener {

        public CoverReceiver() {
        }

        /**
         * Sets the global image variable for this track and recall the update method to refresh
         * the views.
         *
         * @param bm Bitmap fetched for the currently running track.
         */
        @Override
        public void receiveBitmap(Bitmap bm) {
            // Check if a valid image was found.
            if (bm != null) {
                // Set the globally used variable
                mLastCover = bm;

                // Call the update method to refresh the view
                setWidgetContent(mLastInfo);
            }
        }
    }
}
