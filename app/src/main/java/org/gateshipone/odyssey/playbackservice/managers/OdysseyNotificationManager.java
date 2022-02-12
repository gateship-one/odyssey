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

package org.gateshipone.odyssey.playbackservice.managers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.OdysseyMainActivity;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.playbackservice.PlaybackService;

/*
 * This class manages all the notifications from the main playback service.
 */
public class OdysseyNotificationManager {
    // Context needed for various Notification building
    private final Context mContext;

    // PendingIntent ids
    private static final int NOTIFICATION_INTENT_PREVIOUS = 0;
    private static final int NOTIFICATION_INTENT_PLAYPAUSE = 1;
    private static final int NOTIFICATION_INTENT_NEXT = 2;
    private static final int NOTIFICATION_INTENT_QUIT = 3;
    private static final int NOTIFICATION_INTENT_OPENGUI = 4;

    private static final int PENDING_INTENT_UPDATE_CURRENT_FLAG =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

    // Just a constant
    private static final int NOTIFICATION_ID = 42;

    private static final String NOTIFICATION_CHANNEL_ID = "Playback";

    // Notification objects
    private final android.app.NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;

    // Notification itself
    private Notification mNotification;

    //Preferences for notifications
    private boolean mHideMediaOnLockscreen;

    // Save last track and last image
    private Bitmap mLastBitmap = null;
    private TrackModel mLastTrack = null;

    private PlaybackService.PLAYSTATE mLastState;

    private MediaSessionCompat.Token mLastToken;

    private boolean mHideArtwork;

    public OdysseyNotificationManager(Context context) {
        mContext = context;
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /*
     * Creates a android system notification with two different remoteViews. One
     * for the normal layout and one for the big one. Sets the different
     * attributes of the remoteViews and starts a thread for Cover generation.
     */
    public synchronized void updateNotification(TrackModel track, PlaybackService.PLAYSTATE state, MediaSessionCompat.Token mediaSessionToken) {
        mLastState = state;
        mLastToken = mediaSessionToken;
        if (track != null) {
            openChannel();
            mNotificationBuilder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID);

            // Open application intent
            Intent mainIntent = new Intent(mContext, OdysseyMainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            mainIntent.putExtra(OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.REQUESTEDVIEW.NOWPLAYING.ordinal());
            PendingIntent contentPendingIntent = PendingIntent.getActivity(mContext, NOTIFICATION_INTENT_OPENGUI, mainIntent, PENDING_INTENT_UPDATE_CURRENT_FLAG);
            mNotificationBuilder.setContentIntent(contentPendingIntent);

            // Set pendingintents
            // Previous song action
            Intent prevIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PREVIOUS, prevIntent, PENDING_INTENT_UPDATE_CURRENT_FLAG);
            NotificationCompat.Action prevAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_previous_48dp, "Previous", prevPendingIntent).build();

            // Pause/Play action
            PendingIntent playPauseIntent;
            int playPauseIcon;
            if (state == PlaybackService.PLAYSTATE.PLAYING) {
                Intent pauseIntent = new Intent(PlaybackService.ACTION_PAUSE);
                playPauseIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, pauseIntent, PENDING_INTENT_UPDATE_CURRENT_FLAG);
                playPauseIcon = R.drawable.ic_pause_48dp;
            } else {
                Intent playIntent = new Intent(PlaybackService.ACTION_PLAY);
                playPauseIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, playIntent, PENDING_INTENT_UPDATE_CURRENT_FLAG);
                playPauseIcon = R.drawable.ic_play_arrow_48dp;
            }
            NotificationCompat.Action playPauseAction = new NotificationCompat.Action.Builder(playPauseIcon, "PlayPause", playPauseIntent).build();

            // Next song action
            Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_NEXT, nextIntent, PENDING_INTENT_UPDATE_CURRENT_FLAG);
            NotificationCompat.Action nextAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_next_48dp, "Next", nextPendingIntent).build();

            // Quit action
            Intent quitIntent = new Intent(PlaybackService.ACTION_QUIT);
            PendingIntent quitPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_QUIT, quitIntent, PENDING_INTENT_UPDATE_CURRENT_FLAG);
            mNotificationBuilder.setDeleteIntent(quitPendingIntent);

            mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            mNotificationBuilder.setSmallIcon(R.drawable.odyssey_notification);
            mNotificationBuilder.addAction(prevAction);
            mNotificationBuilder.addAction(playPauseAction);
            mNotificationBuilder.addAction(nextAction);
            androidx.media.app.NotificationCompat.MediaStyle notificationStyle = new androidx.media.app.NotificationCompat.MediaStyle();
            notificationStyle.setShowActionsInCompactView(1, 2);
            notificationStyle.setMediaSession(mediaSessionToken);
            mNotificationBuilder.setStyle(notificationStyle);
            mNotificationBuilder.setContentTitle(mContext.getResources().getString(R.string.notification_sensitive_content_replacement));

            // Remove unnecessary time info
            mNotificationBuilder.setShowWhen(false);

            //Build the public notification
            Notification notificationPublic = mNotificationBuilder.build();

            mNotificationBuilder.setContentTitle(track.getTrackDisplayedName());
            mNotificationBuilder.setContentText(track.getTrackArtistName());

            // Cover but only if changed
            if (mLastTrack == null || track.getTrackAlbumId() != mLastTrack.getTrackAlbumId()) {
                mLastTrack = track;
                mLastBitmap = null;
            }

            // Only set image if an saved one is available
            if (mLastBitmap != null && !mHideArtwork) {
                mNotificationBuilder.setLargeIcon(mLastBitmap);
            }

            // Build the private notification
            if (mHideMediaOnLockscreen) {
                mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            }

            mNotification = mNotificationBuilder.build();
            mNotification.publicVersion = notificationPublic;

            // Check if run from service and check if playing or pause.
            // Pause notification should be dismissible.
            if (mContext instanceof Service) {
                if (state == PlaybackService.PLAYSTATE.PLAYING) {
                    ((Service) mContext).startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    ((Service) mContext).stopForeground(false);
                }
            }

            // Send the notification away
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }

    }

    /* Removes the Foreground notification */
    public void clearNotification() {
        if (mNotification != null) {
            if (mContext instanceof Service) {
                ((Service) mContext).stopForeground(true);
                mNotificationBuilder.setOngoing(false);
                mNotificationManager.cancel(NOTIFICATION_ID);
            }
            mNotification = null;
            mLastTrack = null;
            mLastBitmap = null;
            mNotificationBuilder = null;
        }
    }

    /*
     * Receives the generated album picture from the main status helper for the
     * notification controls. Sets it and notifies the system that the
     * notification has changed
     */
    public synchronized void setNotificationImage(Bitmap bm) {
        // Check if notification exists and set picture
        mLastBitmap = bm;

        // Completely redo the notification otherwise the image is not shown sometimes
        if (mNotification != null && bm != null) {
            updateNotification(mLastTrack, mLastState, mLastToken);
        }
    }

    public void hideArtwork(boolean enable) {
        mHideArtwork = enable;
    }

    /*
     * Set the visibility (PRIVATE, PUBLIC) of the notification to allow hiding sensitive content.
     * Updates the notification immediately.
     * @param enable True to set visibility to PRIVATE, false for PUBLIC
     *
     */
    public void hideMediaOnLockscreen(boolean enable) {
        mHideMediaOnLockscreen = enable;
        if (mNotification != null) {
            mNotification.visibility = mHideMediaOnLockscreen ? Notification.VISIBILITY_PRIVATE : Notification.VISIBILITY_PUBLIC;

            if (mNotificationManager != null) {
                mNotificationManager.notify(NOTIFICATION_ID, mNotification);
            }
        }
    }

    /**
     * Creates the {@link NotificationChannel} for devices running Android O or higher
     */
    private void openChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, mContext.getResources().getString(R.string.notification_channel_playback), android.app.NotificationManager.IMPORTANCE_LOW);
            // Disable lights & vibration
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setVibrationPattern(null);

            // Allow lockscreen playback control
            channel.setLockscreenVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC);

            // Register the channel
            mNotificationManager.createNotificationChannel(channel);
        }
    }
}
