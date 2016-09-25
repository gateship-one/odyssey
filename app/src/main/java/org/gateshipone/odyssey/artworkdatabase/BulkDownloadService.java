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

package org.gateshipone.odyssey.artworkdatabase;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.network.LimitingRequestQueue;

public class BulkDownloadService extends Service implements ArtworkManager.BulkLoadingProgressCallback {
    private static final String TAG = BulkDownloadService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 84;

    private static final String ACTION_CANCEL = "cancel_download";
    public static final String ACTION_START_BULKDOWNLOAD = "start_download";

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private int mRemainingArtists;
    private int mRemainingAlbums;

    private int mSumImageDownloads;

    private ActionReceiver mBroadcastReceiver;

    private PowerManager.WakeLock mWakelock;

    private ConnectionStateReceiver mConnectionStateChangeReceiver;

    /**
     * Called when the service is created because it is requested by an activity
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        mSumImageDownloads = 0;

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

        if (intent.getAction().equals(ACTION_START_BULKDOWNLOAD)) {
            Log.v(TAG, "Starting bulk download in service with thread id: " + Thread.currentThread().getId());

            ArtworkManager.getInstance(getApplicationContext()).bulkLoadImages(this);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(BulkDownloadService.this);
            ConnectivityManager cm =
                    (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (null == netInfo) {
                return START_NOT_STICKY;
            }
            boolean wifiOnly = sharedPref.getBoolean("pref_download_wifi_only", true);
            boolean isWifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MALP_BulkDownloader");

            // FIXME do some timeout checking. e.g. 5 minutes no new image then cancel the process
            mWakelock.acquire();
        }
        return START_STICKY;

    }

    private void runAsForeground() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new ActionReceiver();

            // Create a filter to only handle certain actions
            IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(ACTION_CANCEL);

            registerReceiver(mBroadcastReceiver, intentFilter);
        }

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.downloader_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getResources().getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)))
                .setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false)
                .setSmallIcon(R.drawable.odyssey_notification);

        mBuilder.setOngoing(true);

        // Cancel action
        Intent nextIntent = new Intent(BulkDownloadService.ACTION_CANCEL);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        android.support.v7.app.NotificationCompat.Action cancelAction = new android.support.v7.app.NotificationCompat.Action.Builder(R.drawable.ic_clear_24dp, getResources().getString(R.string.dialog_action_cancel), nextPendingIntent).build();

        mBuilder.addAction(cancelAction);

        Notification notification = mBuilder.build();
        startForeground(NOTIFICATION_ID, notification);
        mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

    @Override
    public void startAlbumLoading(int albumCount) {
        Log.v(TAG, "Albumloading started with: " + albumCount + " albums");
        mSumImageDownloads += albumCount;
        mRemainingAlbums = albumCount;
        runAsForeground();
    }

    @Override
    public void startArtistLoading(int artistCount) {
        Log.v(TAG, "Artistloading started with: " + artistCount + " artists");
        mSumImageDownloads += artistCount;
        mRemainingArtists = artistCount;
        runAsForeground();
    }

    @Override
    public void albumsRemaining(int remainingAlbums) {
        Log.v(TAG, "AlbumsRemaining: " + remainingAlbums + " artists");
        mRemainingAlbums = remainingAlbums;
        updateNotification();
    }

    @Override
    public void artistsRemaining(int remainingArtists) {
        Log.v(TAG, "ArtistsRemaining: " + remainingArtists + " artists");
        mRemainingArtists = remainingArtists;
        updateNotification();
    }

    @Override
    public void finishedLoading() {
        mNotificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
        mWakelock.release();
    }

    private void updateNotification() {
        if ((mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) % 10 == 0) {
            mBuilder.setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false);
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getResources().getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)));
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    private class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "Broadcast requested");
            if (intent.getAction().equals(ACTION_CANCEL)) {
                Log.e(TAG, "Cancel requested");
                ArtworkManager.getInstance(getApplicationContext()).cancelAllRequests();
                mNotificationManager.cancel(NOTIFICATION_ID);
                stopForeground(true);
                mWakelock.release();
                stopSelf();
            }
        }
    }

    private class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(BulkDownloadService.this);

            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (null == netInfo) {
                return;
            }
            boolean wifiOnly = sharedPref.getBoolean("pref_download_wifi_only", true);
            boolean isWifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

            if (wifiOnly && !isWifi) {
                // Cancel all downloads
                Log.v(TAG, "Cancel all downloads because of connection change");
                LimitingRequestQueue.getInstance(BulkDownloadService.this).cancelAll(new RequestQueue.RequestFilter() {
                    @Override
                    public boolean apply(Request<?> request) {
                        return true;
                    }
                });
            }

        }
    }
}

