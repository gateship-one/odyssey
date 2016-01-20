package org.odyssey.playbackservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import org.odyssey.OdysseyMainActivity;
import org.odyssey.R;
import org.odyssey.models.TrackModel;
import org.odyssey.utils.CoverBitmapGenerator;

/*
 * This class manages all the notifications from the main playback service.
 */
public class OdysseyNotificationManager {
    // Context needed for various Notification building
    Context mContext;

    // PendingIntent ids
    private static final int NOTIFICATION_INTENT_PREVIOUS = 0;
    private static final int NOTIFICATION_INTENT_PLAYPAUSE = 1;
    private static final int NOTIFICATION_INTENT_NEXT = 2;
    private static final int NOTIFICATION_INTENT_QUIT = 3;
    private static final int NOTIFICATION_INTENT_OPENGUI = 4;

    // Just a constant
    private static final int NOTIFICATION_ID = 42;

    // Notification objects
    android.app.NotificationManager mNotificationManager;
    NotificationCompat.Builder mNotificationBuilder;

    // Notification itself
    Notification mNotification;

    // Asynchronous image fetcher
    private CoverBitmapGenerator mNotificationCoverGenerator;

    private TrackModel mLastTrack = null;

    public OdysseyNotificationManager(Context context) {
        mContext = context;

        mNotificationBuilder = new NotificationCompat.Builder(mContext).setSmallIcon(R.drawable.odyssey_notification).setContentTitle("Odyssey").setContentText("");
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationCoverGenerator = new CoverBitmapGenerator(mContext, new NotificationCoverListener());

    }

    /*
     * Creates a android system notification with two different remoteViews. One
     * for the normal layout and one for the big one. Sets the different
     * attributes of the remoteViews and starts a thread for Cover generation.
     */
    void updateNotification(TrackModel track, PlaybackService.PLAYSTATE state) {
        if (track != null && state != PlaybackService.PLAYSTATE.STOPPED) {

            RemoteViews remoteViewBig = new RemoteViews(mContext.getPackageName(), R.layout.notification_big);
            RemoteViews remoteViewSmall = new RemoteViews(mContext.getPackageName(), R.layout.notification_small);
            remoteViewBig.setTextViewText(R.id.notification_big_track, track.getTrackName());
            remoteViewBig.setTextViewText(R.id.notification_big_artist, track.getTrackArtistName());
            remoteViewBig.setTextViewText(R.id.notification_big_album, track.getTrackAlbumName());

            remoteViewSmall.setTextViewText(R.id.notification_small_track, track.getTrackName());
            remoteViewSmall.setTextViewText(R.id.notification_small_artist, track.getTrackArtistName());
            remoteViewSmall.setTextViewText(R.id.notification_small_album, track.getTrackAlbumName());

            // Set pendingintents
            // Previous song action
            Intent prevIntent = new Intent(PlaybackService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViewBig.setOnClickPendingIntent(R.id.notification_big_previous, prevPendingIntent);

            // Pause/Play action
            if (state == PlaybackService.PLAYSTATE.PLAYING) {
                Intent pauseIntent = new Intent(PlaybackService.ACTION_PAUSE);
                PendingIntent pausePendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViewBig.setOnClickPendingIntent(R.id.notification_big_play, pausePendingIntent);
                remoteViewSmall.setOnClickPendingIntent(R.id.notification_small_play, pausePendingIntent);
                // Set right drawable
                remoteViewBig.setImageViewResource(R.id.notification_big_play, R.drawable.ic_pause_24dp);
                remoteViewSmall.setImageViewResource(R.id.notification_small_play, R.drawable.ic_pause_24dp);

            } else {
                Intent playIntent = new Intent(PlaybackService.ACTION_PLAY);
                PendingIntent playPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_PLAYPAUSE, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViewBig.setOnClickPendingIntent(R.id.notification_big_play, playPendingIntent);
                remoteViewSmall.setOnClickPendingIntent(R.id.notification_small_play, playPendingIntent);
                // Set right drawable
                remoteViewBig.setImageViewResource(R.id.notification_big_play, R.drawable.ic_play_arrow_24dp);
                remoteViewSmall.setImageViewResource(R.id.notification_small_play, R.drawable.ic_play_arrow_24dp);
            }

            // Next song action
            Intent nextIntent = new Intent(PlaybackService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViewBig.setOnClickPendingIntent(R.id.notification_big_next, nextPendingIntent);
            remoteViewSmall.setOnClickPendingIntent(R.id.notification_small_next, nextPendingIntent);

            // Quit action
            Intent quitIntent = new Intent(PlaybackService.ACTION_QUIT);
            PendingIntent quitPendingIntent = PendingIntent.getBroadcast(mContext, NOTIFICATION_INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViewBig.setOnClickPendingIntent(R.id.notification_big_close, quitPendingIntent);

            // Cover but only if changed
            if ( mLastTrack == null || !track.getTrackAlbumName().equals(mLastTrack.getTrackAlbumName())) {
                remoteViewBig.setImageViewResource(R.id.notification_big_image, R.drawable.cover_placeholder_96dp);
                remoteViewSmall.setImageViewResource(R.id.notification_small_image, R.drawable.cover_placeholder_96dp);

                mNotificationCoverGenerator.getImage(track);
                mLastTrack = track;
            }

            // Open application intent
            Intent resultIntent = new Intent(mContext, OdysseyMainActivity.class);
            resultIntent.putExtra("Fragment", "currentsong");
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_HISTORY);

            // Swipe away intent
            mNotificationBuilder.setDeleteIntent(quitPendingIntent);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, NOTIFICATION_INTENT_OPENGUI, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setContentIntent(resultPendingIntent);

            mNotification = mNotificationBuilder.build();
            mNotification.bigContentView = remoteViewBig;
            mNotification.contentView = remoteViewSmall;

            // Check if run from service

            if ( mContext instanceof Service ) {
                if ( state == PlaybackService.PLAYSTATE.PLAYING) {
                    ((Service)mContext).startForeground(NOTIFICATION_ID, mNotification);
                } else {
                    ((Service)mContext).stopForeground(false);
                }
            }

            mNotificationManager.notify(NOTIFICATION_ID, mNotification);

        } else {
            clearNotification();
        }
    }

    /* Removes the Foreground notification */
    public void clearNotification() {
        if (mNotification != null) {
            if ( mContext instanceof Service ) {
                ((Service)mContext).stopForeground(true);
                mNotificationBuilder.setOngoing(false);
                mNotificationManager.cancel(NOTIFICATION_ID);
            }
            mNotification = null;
            mLastTrack = null;
        }
    }

    /*
     * Receives the generated album picture from a separate thread for the
     * notification controls. Sets it and notifies the system that the
     * notification has changed
     */
    private class NotificationCoverListener implements CoverBitmapGenerator.CoverBitmapListener {

        @Override
        public void receiveBitmap(BitmapDrawable bm) {
            // Check if notification exists and set picture
            if (mNotification != null && mNotification.bigContentView != null && bm != null) {
                // Set the image in the remoteView
                mNotification.bigContentView.setImageViewBitmap(R.id.notification_big_image, bm.getBitmap());
                // Notify android about the change
                mNotificationManager.notify(NOTIFICATION_ID, mNotification);
            }
            if (mNotification != null && mNotification.contentView != null && bm != null) {
                // Set the image in the remoteView
                mNotification.contentView.setImageViewBitmap(R.id.notification_small_image, bm.getBitmap());
                // Notify android about the change
                mNotificationManager.notify(NOTIFICATION_ID, mNotification);
            }
        }

    }
}
