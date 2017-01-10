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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.List;

public class MediaScannerService extends Service {
    private static final String TAG = MediaScannerService.class.getSimpleName();

    public static final String BUNDLE_KEY_DIRECTORY = "org.gateshipone.odyssey.mediascanner.directory";

    public static final String ACTION_START_MEDIASCANNING = "org.gateshipone.odyssey.mediascanner.start";
    public static final String ACTION_CANCEL_MEDIASCANNING = "org.gateshipone.odyssey.mediascanner.cancel";

    private static final int NOTIFICATION_ID = 126;

    private NotificationManager mNotificationManager;

    private List<FileModel> mRemainingFolders;

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
            mRemainingFolders = new ArrayList<>();

            mAbort = false;

            // read path to directory from extras
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String startDirectory = extras.getString(BUNDLE_KEY_DIRECTORY);

                FileModel directory = new FileModel(startDirectory);

                mRemainingFolders.add(directory);
            }

            if (mRemainingFolders.isEmpty()) {
                return START_NOT_STICKY;
            }

            Log.v(TAG, "start mediascanning");

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Odyssey_Mediascanning");

            // FIXME do some timeout checking. e.g. 5 minutes no new image then cancel the process
            mWakelock.acquire();

            if (mBroadcastReceiver == null) {
                mBroadcastReceiver = new ActionReceiver();

                // Create a filter to only handle certain actions
                IntentFilter intentFilter = new IntentFilter();

                intentFilter.addAction(ACTION_CANCEL_MEDIASCANNING);

                registerReceiver(mBroadcastReceiver, intentFilter);
            }

            // create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.mediascanner_notification_title))
                    .setProgress(0, 0, true)
                    .setSmallIcon(R.drawable.odyssey_notification);

            builder.setOngoing(true);

            // Cancel action
            Intent nextIntent = new Intent(MediaScannerService.ACTION_CANCEL_MEDIASCANNING);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            android.support.v7.app.NotificationCompat.Action cancelAction = new android.support.v7.app.NotificationCompat.Action.Builder(R.drawable.ic_close_24dp, getString(R.string.dialog_action_cancel), nextPendingIntent).build();

            builder.addAction(cancelAction);

            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
            mNotificationManager.notify(NOTIFICATION_ID, notification);

            // start scanning
            getNextFolder(getApplicationContext());
        }

        return START_STICKY;
    }

    private void getNextFolder(final Context context) {

        Log.v(TAG, "get next folder");

        if (mRemainingFolders.isEmpty() || mAbort) {
            // finish scanning if it was aborted or all folders were scanned
            finishService();
        } else {
            // get next directory
            FileModel currentDirectory = mRemainingFolders.get(0);
            mRemainingFolders.remove(0);

            scanDirectory(context, currentDirectory);
        }
    }

    private void scanDirectory(final Context context, final FileModel directory) {
        List<FileModel> files = PermissionHelper.getFilesForDirectory(context, directory);

        List<String> filePaths = new ArrayList<>();

        for (FileModel file : files) {
            if (file.isFile()) {
                // add file to the pathlist
                filePaths.add(file.getPath());
            } else {
                // add subdirectories to remaining folders list
                mRemainingFolders.add(file);
            }
        }

        if (!filePaths.isEmpty()) {
            // trigger mediascan for all current files
            MediaScannerConnection.scanFile(context, filePaths.toArray(new String[filePaths.size()]), null, new MediaScanCompletedCallback(filePaths.size(), context));
        } else {
            // if no files were found continue with the next folder
            getNextFolder(context);
        }
    }

    private void finishService() {
        Log.v(TAG, "finish mediascanning");
        mNotificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
        if (mWakelock.isHeld()) {
            mWakelock.release();
        }
    }

    private class MediaScanCompletedCallback implements MediaScannerConnection.OnScanCompletedListener {

        private final Context mContext;

        private final int mNumberOfFiles;

        private int mScannedFiles;

        public MediaScanCompletedCallback(final int numberOfFiles, final Context context) {
            mContext = context;
            mNumberOfFiles = numberOfFiles;
            mScannedFiles = 0;
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            // TODO check uri to give the user feedback
            Log.v(TAG, "scan completed: " + uri);

            mScannedFiles++;

            if (mScannedFiles == mNumberOfFiles) {
                // all files scanned so get next directory
                getNextFolder(mContext);
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
                finishService();
            }
        }
    }
}
