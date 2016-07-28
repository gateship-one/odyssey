package org.odyssey.playbackservice.managers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.session.MediaSession;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.models.TrackModel;
import org.odyssey.playbackservice.PlaybackService;

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

    // Just a constant
    private static final int NOTIFICATION_ID = 42;

    // Notification objects
    private final android.app.NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;

    // Notification itself
    private Notification mNotification;

    // Save last track and last image
    private Bitmap mLastBitmap = null;
    private TrackModel mLastTrack = null;

    public OdysseyNotificationManager(Context context) {
        mContext = context;

        mNotificationBuilder = new Notification.Builder(mContext);
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /*
     * Creates a android system notification with two different remoteViews. One
     * for the normal layout and one for the big one. Sets the different
     * attributes of the remoteViews and starts a thread for Cover generation.
     */
    public void updateNotification(TrackModel track, PlaybackService.PLAYSTATE state, MediaSession.Token mediaSessionToken) {
        if (track != null) {
            mNotificationBuilder = new Notification.Builder(mContext);

            // Open application intent
            Intent contentIntent = new Intent(mContext, OdysseyMainActivity.class);
            contentIntent.putExtra(OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW);
            contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_HISTORY);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(mContext, NOTIFICATION_INTENT_OPENGUI, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setContentIntent(contentPendingIntent);

            // Set pendingintents
            // Previous song action
            Intent prevIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Action prevAction = new Notification.Action.Builder(R.drawable.ic_skip_previous_48dp, "Previous", prevPendingIntent).build();

            // Pause/Play action
            PendingIntent playPauseIntent;
            int playPauseIcon;
            if (state == PlaybackService.PLAYSTATE.PLAYING) {
                Intent pauseIntent = new Intent(PlaybackService.ACTION_PAUSE);
                playPauseIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_pause_48dp;
            } else {
                Intent playIntent = new Intent(PlaybackService.ACTION_PLAY);
                playPauseIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_play_arrow_48dp;
            }
            Notification.Action playPauseAction = new Notification.Action.Builder(playPauseIcon, "PlayPause", playPauseIntent).build();

            // Next song action
            Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Action nextAction = new Notification.Action.Builder(R.drawable.ic_skip_next_48dp, "Next", nextPendingIntent).build();

            // Quit action
            Intent quitIntent = new Intent(PlaybackService.ACTION_QUIT);
            PendingIntent quitPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setDeleteIntent(quitPendingIntent);

            mNotificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
            mNotificationBuilder.setSmallIcon(R.drawable.odyssey_notification);
            mNotificationBuilder.addAction(prevAction);
            mNotificationBuilder.addAction(playPauseAction);
            mNotificationBuilder.addAction(nextAction);
            Notification.MediaStyle notificationStyle = new Notification.MediaStyle();
            notificationStyle.setShowActionsInCompactView(1, 2);
            notificationStyle.setMediaSession(mediaSessionToken);
            mNotificationBuilder.setStyle(notificationStyle);
            mNotificationBuilder.setContentTitle(track.getTrackName());
            mNotificationBuilder.setContentText(track.getTrackArtistName());

            // Remove unnecessary time info
            mNotificationBuilder.setWhen(0);

            // Cover but only if changed
            if (mLastTrack == null || !track.getTrackAlbumName().equals(mLastTrack.getTrackAlbumName())) {
                mLastTrack = track;
                mLastBitmap = null;
            }

            // Only set image if an saved one is available
            if (mLastBitmap != null) {
                mNotificationBuilder.setLargeIcon(mLastBitmap);
            }

            // Build the notification
            mNotification = mNotificationBuilder.build();

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
        }
    }

    /*
     * Receives the generated album picture from the main status helper for the
     * notification controls. Sets it and notifies the system that the
     * notification has changed
     */
    public void setNotificationImage(BitmapDrawable bm) {
        // Check if notification exists and set picture
        mNotificationBuilder.setLargeIcon(bm.getBitmap());
        mLastBitmap = bm.getBitmap();
        mNotification = mNotificationBuilder.build();
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }
}
