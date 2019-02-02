/*
 * Copyright (C) 2019 Team Gateship-One
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

package org.gateshipone.odyssey.artworkdatabase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.utils.NetworkUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BulkDownloadService extends Service {
    private static final String TAG = BulkDownloadService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 84;

    private static final String NOTIFICATION_CHANNEL_ID = "BulkDownloader";

    public static final String ACTION_CANCEL_BULKDOWNLOAD = "org.gateshipone.odyssey.bulkdownload.cancel";

    public static final String ACTION_START_BULKDOWNLOAD = "org.gateshipone.odyssey.bulkdownload.start";

    public static final String BUNDLE_KEY_ARTIST_PROVIDER = "org.gateshipone.odyssey.artist_provider";

    public static final String BUNDLE_KEY_ALBUM_PROVIDER = "org.gateshipone.odyssey.album_provider";

    public static final String BUNDLE_KEY_WIFI_ONLY = "org.gateshipone.odyssey.wifi_only";

    private NotificationManager mNotificationManager;

    private NotificationCompat.Builder mBuilder;

    private int mRemainingArtists;

    private int mRemainingAlbums;

    private int mSumImageDownloads;

    private ActionReceiver mBroadcastReceiver;

    private PowerManager.WakeLock mWakelock;

    private ConnectionStateReceiver mConnectionStateChangeReceiver;

    private boolean mWifiOnly;

    /**
     * Called when the service is created because it is requested by an activity
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        mConnectionStateChangeReceiver = new ConnectionStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionStateChangeReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        unregisterReceiver(mConnectionStateChangeReceiver);
        Log.v(TAG, "Calling super.onDestroy()");
        super.onDestroy();
        Log.v(TAG, "Called super.onDestroy()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(ACTION_START_BULKDOWNLOAD)) {
            Log.v(TAG, "Starting bulk download in service with thread id: " + Thread.currentThread().getId());

            // reset counter
            mRemainingArtists = 0;
            mRemainingAlbums = 0;
            mSumImageDownloads = 0;

            String artistProvider = getString(R.string.pref_artwork_provider_artist_default);
            String albumProvider = getString(R.string.pref_artwork_provider_album_default);
            mWifiOnly = true;

            // read setting from extras
            Bundle extras = intent.getExtras();
            if (extras != null) {
                artistProvider = extras.getString(BUNDLE_KEY_ARTIST_PROVIDER, getString(R.string.pref_artwork_provider_artist_default));
                albumProvider = extras.getString(BUNDLE_KEY_ALBUM_PROVIDER, getString(R.string.pref_artwork_provider_album_default));
                mWifiOnly = intent.getBooleanExtra(BUNDLE_KEY_WIFI_ONLY, true);
            }

            if (artistProvider.equals(getString(R.string.pref_artwork_provider_none_key)) && albumProvider.equals(getString(R.string.pref_artwork_provider_none_key))) {
                return START_NOT_STICKY;
            }

            if (!NetworkUtils.isDownloadAllowed(this, mWifiOnly)) {
                return START_NOT_STICKY;
            }

//            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "odyssey:wakelock:bulkdownloader");
//
//            // FIXME do some timeout checking. e.g. 5 minutes no new image then cancel the process
//            mWakelock.acquire();

//            ArtworkManager artworkManager = ArtworkManager.getInstance(getApplicationContext());
//            artworkManager.initialize(artistProvider, albumProvider, mWifiOnly);
//            artworkManager.bulkLoadImages(this, getApplicationContext());
        }
        return START_STICKY;
    }

    private void runAsForeground() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new ActionReceiver();

            // Create a filter to only handle certain actions
            IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(ACTION_CANCEL_BULKDOWNLOAD);

            registerReceiver(mBroadcastReceiver, intentFilter);
        }

        mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.downloader_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)))
                .setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false)
                .setSmallIcon(R.drawable.odyssey_notification);

        openChannel();

        mBuilder.setOngoing(true);

        // Cancel action
        Intent nextIntent = new Intent(BulkDownloadService.ACTION_CANCEL_BULKDOWNLOAD);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        androidx.core.app.NotificationCompat.Action cancelAction = new androidx.core.app.NotificationCompat.Action.Builder(R.drawable.ic_close_24dp, getString(R.string.dialog_action_cancel), nextPendingIntent).build();

        mBuilder.addAction(cancelAction);

        Notification notification = mBuilder.build();
        startForeground(NOTIFICATION_ID, notification);
        mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

//    @Override
//    public void startAlbumLoading(int albumCount) {
//        Log.v(TAG, "Albumloading started with: " + albumCount + " albums");
//        mSumImageDownloads += albumCount;
//        mRemainingAlbums = albumCount;
//        runAsForeground();
//    }
//
//    @Override
//    public void startArtistLoading(int artistCount) {
//        Log.v(TAG, "Artistloading started with: " + artistCount + " artists");
//        mSumImageDownloads += artistCount;
//        mRemainingArtists = artistCount;
//        runAsForeground();
//    }
//
//    @Override
//    public void albumsRemaining(int remainingAlbums) {
//        Log.v(TAG, "AlbumsRemaining: " + remainingAlbums + " artists");
//        mRemainingAlbums = remainingAlbums;
//        updateNotification();
//    }
//
//    @Override
//    public void artistsRemaining(int remainingArtists) {
//        Log.v(TAG, "ArtistsRemaining: " + remainingArtists + " artists");
//        mRemainingArtists = remainingArtists;
//        updateNotification();
//    }
//
//    @Override
//    public void finishedLoading() {
//        mNotificationManager.cancel(NOTIFICATION_ID);
//        stopForeground(true);
//        stopSelf();
//        if (mWakelock.isHeld()) {
//            mWakelock.release();
//        }
//    }

    private void updateNotification() {
        if ((mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) % 10 == 0) {
            mBuilder.setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false);
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)));
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    /**
     * Opens a notification channel and disables the LED and vibration
     */
    private void openChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, this.getResources().getString(R.string.notification_channel_downloader), android.app.NotificationManager.IMPORTANCE_LOW);
            // Disable lights & vibration
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setVibrationPattern(null);

            // Register the channel
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "Broadcast requested");
            if (intent.getAction().equals(ACTION_CANCEL_BULKDOWNLOAD)) {
                Log.e(TAG, "Cancel requested");
//                ArtworkManager.getInstance(getApplicationContext()).cancelAllRequests(getApplicationContext());
                mNotificationManager.cancel(NOTIFICATION_ID);
                stopForeground(true);
                stopSelf();
            }
        }
    }

    private class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NetworkUtils.isDownloadAllowed(context, mWifiOnly)) {
                // Cancel all downloads
                Log.v(TAG, "Cancel all downloads because of connection change");
//                LimitingRequestQueue.getInstance(BulkDownloadService.this).cancelAll(request -> true);
            }

        }
    }
}

