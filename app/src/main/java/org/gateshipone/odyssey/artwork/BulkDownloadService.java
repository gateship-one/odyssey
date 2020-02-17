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

package org.gateshipone.odyssey.artwork;

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

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artwork.network.ArtworkRequestModel;
import org.gateshipone.odyssey.artwork.network.ImageResponse;
import org.gateshipone.odyssey.artwork.network.InsertImageTask;
import org.gateshipone.odyssey.artwork.network.artprovider.ArtProvider;
import org.gateshipone.odyssey.artwork.storage.ArtworkDatabaseManager;
import org.gateshipone.odyssey.artwork.storage.ImageNotFoundException;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.NetworkUtils;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BulkDownloadService extends Service implements InsertImageTask.ImageSavedCallback, ArtProvider.ArtFetchError {
    private static final String TAG = BulkDownloadService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 84;

    private static final String NOTIFICATION_CHANNEL_ID = "BulkDownloader";

    public static final String ACTION_CANCEL_BULKDOWNLOAD = "org.gateshipone.odyssey.bulkdownload.cancel";

    public static final String ACTION_START_BULKDOWNLOAD = "org.gateshipone.odyssey.bulkdownload.start";

    public static final String BUNDLE_KEY_ARTIST_PROVIDER = "org.gateshipone.odyssey.artist_provider";

    public static final String BUNDLE_KEY_ALBUM_PROVIDER = "org.gateshipone.odyssey.album_provider";

    public static final String BUNDLE_KEY_WIFI_ONLY = "org.gateshipone.odyssey.wifi_only";

    public static final String BUNDLE_KEY_USE_LOCAL_IMAGES = "org.gateshipone.odyssey.use_local_images";

    private NotificationManager mNotificationManager;

    private NotificationCompat.Builder mBuilder;

    private int mSumArtworkRequests;

    private ActionReceiver mBroadcastReceiver;

    private PowerManager.WakeLock mWakelock;

    private ConnectionStateReceiver mConnectionStateChangeReceiver;

    private boolean mWifiOnly;

    private boolean mUseLocalImages;

    final private LinkedList<ArtworkRequestModel> mArtworkRequestQueue = new LinkedList<>();

    private ArtworkManager mArtworkManager;

    private ArtworkDatabaseManager mDatabaseManager;

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
        if (intent != null && ACTION_START_BULKDOWNLOAD.equals(intent.getAction())) {
            Log.v(TAG, "Starting bulk download in service with thread id: " + Thread.currentThread().getId());

            // reset counter
            mSumArtworkRequests = 0;

            String artistProvider = getString(R.string.pref_artwork_provider_artist_default);
            String albumProvider = getString(R.string.pref_artwork_provider_album_default);
            mWifiOnly = true;

            // read setting from extras
            Bundle extras = intent.getExtras();
            if (extras != null) {
                artistProvider = extras.getString(BUNDLE_KEY_ARTIST_PROVIDER, getString(R.string.pref_artwork_provider_artist_default));
                albumProvider = extras.getString(BUNDLE_KEY_ALBUM_PROVIDER, getString(R.string.pref_artwork_provider_album_default));
                mWifiOnly = intent.getBooleanExtra(BUNDLE_KEY_WIFI_ONLY, true);
                mUseLocalImages = intent.getBooleanExtra(BUNDLE_KEY_USE_LOCAL_IMAGES, false);
            }

            if (artistProvider.equals(getString(R.string.pref_artwork_provider_none_key)) && albumProvider.equals(getString(R.string.pref_artwork_provider_none_key))) {
                return START_NOT_STICKY;
            }

            if (!NetworkUtils.isDownloadAllowed(this, mWifiOnly)) {
                return START_NOT_STICKY;
            }

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "odyssey:wakelock:bulkdownloader");

            // FIXME do some timeout checking. e.g. 5 minutes no new image then cancel the process
            mWakelock.acquire();

            mArtworkManager = ArtworkManager.getInstance(getApplicationContext());
            mArtworkManager.initialize(artistProvider, albumProvider, mWifiOnly, mUseLocalImages);

            mDatabaseManager = ArtworkDatabaseManager.getInstance(getApplicationContext());

            runAsForeground();

            createArtworkRequestQueue(!albumProvider.equals(getApplicationContext().getString((R.string.pref_artwork_provider_none_key))),
                    !artistProvider.equals(getApplicationContext().getString((R.string.pref_artwork_provider_none_key))));
        }
        return START_STICKY;
    }

    @Override
    public void onImageSaved(final ArtworkRequestModel artworkRequestModel, final Context applicationContext) {
        mArtworkManager.onImageSaved(artworkRequestModel, applicationContext);

        performNextRequest();
    }

    @Override
    public void fetchJSONException(final ArtworkRequestModel model, final Context context, final JSONException exception) {
        Log.e(TAG, "JSONException fetching: " + model.getLoggingString());
        ImageResponse imageResponse = new ImageResponse();
        imageResponse.model = model;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertImageTask(context, this).execute(imageResponse);
    }

    @Override
    public void fetchVolleyError(final ArtworkRequestModel model, final Context context, final VolleyError error) {
        Log.e(TAG, "VolleyError for request: " + model.getLoggingString());

        if (error != null) {
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse != null && networkResponse.statusCode == 503) {
                finishedLoading();
                return;
            }
        }

        ImageResponse imageResponse = new ImageResponse();
        imageResponse.model = model;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertImageTask(context, this).execute(imageResponse);
    }

    public void fetchError(final ArtworkRequestModel model, final Context context) {
        Log.e(TAG, "JSONException fetching: " + model.getLoggingString());
        ImageResponse imageResponse = new ImageResponse();
        imageResponse.model = model;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertImageTask(context, this).execute(imageResponse);
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
                .setContentTitle(getString(R.string.downloader_notification_initialize))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.downloader_notification_remaining_images) + ' ' + 0))
                .setProgress(0, 0, false)
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

    private void createArtworkRequestQueue(final boolean fetchAlbums, final boolean fetchArtists) {
        mArtworkRequestQueue.clear();

        if (fetchAlbums) {
            List<AlbumModel> albums = MusicLibraryHelper.getAllAlbums(getApplicationContext());

            for (AlbumModel album : albums) {
                mArtworkRequestQueue.add(new ArtworkRequestModel(album));
            }
        }

        if (fetchArtists) {
            List<ArtistModel> artists = MusicLibraryHelper.getAllArtists(false, getApplicationContext());

            for (ArtistModel artist : artists) {
                mArtworkRequestQueue.add(new ArtworkRequestModel(artist));
            }
        }

        startBulkDownload();
    }

    private void startBulkDownload() {
        Log.v(TAG, "Bulkloading started with: " + mArtworkRequestQueue.size());

        mSumArtworkRequests = mArtworkRequestQueue.size();

        mBuilder.setContentTitle(getString(R.string.downloader_notification_remaining_images));

        if (mArtworkRequestQueue.isEmpty()) {
            finishedLoading();
        } else {
            performNextRequest();
        }
    }

    private void performNextRequest() {
        ArtworkRequestModel requestModel;
        while (true) {
            synchronized (mArtworkRequestQueue) {
                updateNotification(mArtworkRequestQueue.size());

                requestModel = mArtworkRequestQueue.pollFirst();
            }

            if (requestModel != null) {
                if (checkRequest(requestModel)) {
                    createRequest(requestModel);
                    return;
                }
            } else {
                finishedLoading();
                return;
            }
        }
    }

    private boolean checkRequest(@NonNull final ArtworkRequestModel requestModel) {
        switch (requestModel.getType()) {
            case ALBUM: {
                AlbumModel album = (AlbumModel) requestModel.getGenericModel();
                if (mUseLocalImages || album.getAlbumArtURL() == null || album.getAlbumArtURL().isEmpty()) {
                    try {
                        mDatabaseManager.getAlbumImage(getApplicationContext(), album);
                    } catch (ImageNotFoundException e) {
                        return true;
                    }
                }
            }
            break;
            case ARTIST: {
                try {
                    mDatabaseManager.getArtistImage(getApplicationContext(), (ArtistModel) requestModel.getGenericModel());
                } catch (ImageNotFoundException e) {
                    return true;
                }
            }
            break;
        }
        return false;
    }

    private void createRequest(final ArtworkRequestModel requestModel) {
        switch (requestModel.getType()) {
            case ALBUM:
                mArtworkManager.fetchImage((AlbumModel) requestModel.getGenericModel(), getApplicationContext(), this, this);
                break;
            case ARTIST:
                mArtworkManager.fetchImage((ArtistModel) requestModel.getGenericModel(), getApplicationContext(), this, this);
                break;
        }

    }

    private void finishedLoading() {
        mArtworkRequestQueue.clear();

        ArtworkManager.getInstance(getApplicationContext()).cancelAllRequests(getApplicationContext());

        mNotificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
        if (mWakelock.isHeld()) {
            mWakelock.release();
        }
    }

    private void updateNotification(final int pendingRequests) {
        Log.v(TAG, "Remaining requests: " + pendingRequests);

        int finishedRequests = mSumArtworkRequests - pendingRequests;

        if (finishedRequests % 10 == 0) {
            mBuilder.setProgress(mSumArtworkRequests, finishedRequests, false);
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.downloader_notification_remaining_images) + ' ' + finishedRequests + '/' + mSumArtworkRequests));
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

            // Allow lockscreen control
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // Register the channel
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "Broadcast requested");
            if (ACTION_CANCEL_BULKDOWNLOAD.equals(intent.getAction())) {
                Log.e(TAG, "Cancel requested");
                finishedLoading();
            }
        }
    }

    private class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NetworkUtils.isDownloadAllowed(context, mWifiOnly)) {
                // Cancel all downloads
                Log.v(TAG, "Cancel all downloads because of connection change");
                finishedLoading();
            }

        }
    }
}

