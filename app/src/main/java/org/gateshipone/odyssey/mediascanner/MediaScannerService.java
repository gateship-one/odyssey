/*
 * Copyright (C) 2017 Team Gateship-One
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

package org.gateshipone.odyssey.mediascanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.utils.FileExplorerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

public class MediaScannerService extends Service {
    private static final String TAG = MediaScannerService.class.getSimpleName();

    public static final String BUNDLE_KEY_DIRECTORY = "org.gateshipone.odyssey.mediascanner.directory";

    public static final String ACTION_START_MEDIASCANNING = "org.gateshipone.odyssey.mediascanner.start";
    public static final String ACTION_CANCEL_MEDIASCANNING = "org.gateshipone.odyssey.mediascanner.cancel";

    private static final int NOTIFICATION_ID = 126;

    private static final String NOTIFICATION_CHANNEL_ID = "MediaScanner";

    /**
     * Defines how many tracks are sent at once to the MediaScanner. Should not be to big to avoid creating
     * to large objects for Binder IPC.
     */
    private static final int MEDIASCANNER_BUNCH_SIZE = 100;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private List<FileModel> mRemainingFiles;

    private int mFilesToScan;
    private int mScannedFiles;

    private MediaScannerService.ActionReceiver mBroadcastReceiver;

    private PowerManager.WakeLock mWakelock;

    private boolean mAbort;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        Log.v(TAG, "Calling super.onDestroy()");
        super.onDestroy();
        Log.v(TAG, "Called super.onDestroy()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(ACTION_START_MEDIASCANNING)) {
            mRemainingFiles = new ArrayList<>();

            mAbort = false;
            FileModel directory = null;

            // read path to directory from extras
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String startDirectory = extras.getString(BUNDLE_KEY_DIRECTORY);

                directory = new FileModel(startDirectory);
            }

            Log.v(TAG, "start mediascanning");

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Odyssey_Mediascanning");

            mWakelock.acquire();

            if (mBroadcastReceiver == null) {
                mBroadcastReceiver = new ActionReceiver();

                // Create a filter to only handle certain actions
                IntentFilter intentFilter = new IntentFilter();

                intentFilter.addAction(ACTION_CANCEL_MEDIASCANNING);

                registerReceiver(mBroadcastReceiver, intentFilter);
            }

            // create notification
            mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.mediascanner_notification_title))
                    .setProgress(0, 0, true)
                    .setSmallIcon(R.drawable.odyssey_notification);

            openChannel();

            mBuilder.setOngoing(true);

            // Cancel action
            Intent nextIntent = new Intent(MediaScannerService.ACTION_CANCEL_MEDIASCANNING);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            android.support.v7.app.NotificationCompat.Action cancelAction = new android.support.v7.app.NotificationCompat.Action.Builder(R.drawable.ic_close_24dp, getString(R.string.dialog_action_cancel), nextPendingIntent).build();

            mBuilder.addAction(cancelAction);

            Notification notification = mBuilder.build();
            startForeground(NOTIFICATION_ID, notification);
            mNotificationManager.notify(NOTIFICATION_ID, notification);

            // start scanning
            if (null != directory) {
                scanDirectory(this, directory);
            }
        }

        return START_NOT_STICKY;
    }

    private void updateNotification() {
        //  Updates the notification but only every 10 elements to reduce load on the notification view
        if (mScannedFiles % 10 == 0 && !mAbort) {
            mBuilder.setProgress(mFilesToScan, mScannedFiles, false);
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.mediascanner_notification_text, mScannedFiles, mFilesToScan)));
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    private void scanDirectory(final Context context, FileModel basePath) {
        new ListCreationTask(context).execute(basePath);
    }

    private void scanFileList(final Context context, List<FileModel> files) {
        mRemainingFiles = files;

        scanNextBunch(context);
    }

    /**
     * Proceeds to the next bunch of files to scan if any available.
     *
     * @param context Context used for scanning.
     */
    private void scanNextBunch(final Context context) {
        if (mRemainingFiles.isEmpty() || mAbort) {
            // No files left to scan, stop service (delayed to allow the ServiceConnection to the MediaScanner to close itself)
            Timer delayedStopTimer = new Timer();
            delayedStopTimer.schedule(new DelayedStopTask(), 100);
            return;
        }

        String[] bunch = new String[Math.min(MEDIASCANNER_BUNCH_SIZE, mRemainingFiles.size())];
        int i = 0;

        ListIterator<FileModel> listIterator = mRemainingFiles.listIterator();
        while (listIterator.hasNext() && i < MEDIASCANNER_BUNCH_SIZE) {
            bunch[i] = listIterator.next().getPath();
            listIterator.remove();
            i++;
        }
        MediaScannerConnection.scanFile(context, bunch, null, new MediaScanCompletedCallback(bunch.length, context));
    }

    private void finishService() {
        Log.v(TAG, "finish mediascanning");

        mNotificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);

        if (mWakelock.isHeld()) {
            mWakelock.release();
        }

        // Stop service.
        stopSelf();
    }

    /**
     * Opens a notification channel and disables the LED and vibration
     */
    private void openChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, this.getResources().getString(R.string.notification_channel_library_scanner), android.app.NotificationManager.IMPORTANCE_LOW);
            // Disable lights & vibration
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setVibrationPattern(null);

            // Register the channel
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private class MediaScanCompletedCallback implements MediaScannerConnection.OnScanCompletedListener {

        private final Context mContext;

        private final int mNumberOfFiles;

        private int mBunchScannedFiles;

        public MediaScanCompletedCallback(final int numberOfFiles, final Context context) {
            mContext = context;
            mNumberOfFiles = numberOfFiles;
            mBunchScannedFiles = 0;
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            Log.v(TAG, "scan completed: " + uri);

            mScannedFiles++;

            mBunchScannedFiles++;

            updateNotification();

            if (mBunchScannedFiles == mNumberOfFiles) {
                Log.v(TAG, "Bunch complete, proceed to next one");
                scanNextBunch(mContext);
            }
        }
    }

    private class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "Broadcast requested");
            if (intent.getAction().equals(ACTION_CANCEL_MEDIASCANNING)) {
                Log.e(TAG, "Cancel requested");
                // abort scan after finish scanning current folder
                mAbort = true;
                // cancel notification
                mNotificationManager.cancel(NOTIFICATION_ID);
                stopForeground(true);
            }
        }
    }

    private class ListCreationTask extends AsyncTask<FileModel, Integer, List<FileModel>> {

        Context mContext;

        public ListCreationTask(Context context) {
            mContext = context;
        }

        @Override
        protected List<FileModel> doInBackground(FileModel... params) {
            List<FileModel> files = FileExplorerHelper.getInstance().getMissingDBFiles(mContext, params[0]);
            Log.v(TAG, "Got missing tracks: " + files.size());
            mFilesToScan = files.size();
            scanFileList(mContext, files);
            return files;
        }
    }

    private class DelayedStopTask extends TimerTask {

        @Override
        public void run() {
            finishService();
        }
    }
}
