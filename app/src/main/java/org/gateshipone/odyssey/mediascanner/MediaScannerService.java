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

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import org.gateshipone.odyssey.models.FileModel;
import org.gateshipone.odyssey.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.List;

public class MediaScannerService extends Service {

    public static final String BUNDLE_KEY_DIRECTORY = "org.gateshipone.odyssey.mediascan.directory";

    private static final int NOTIFICATION_ID = 126;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private List<FileModel> mRemainingFolders;

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
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mRemainingFolders = new ArrayList<>();

        return START_STICKY;
    }

    private void getNextFolder(final Context context) {

        if (mRemainingFolders.isEmpty()) {
            // TODO finish service
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

        // trigger mediascan
        MediaScannerConnection.scanFile(context, filePaths.toArray(new String[filePaths.size()]), null, new MediaScanCompletedCallback(filePaths.size(), context));
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

            mScannedFiles++;

            if (mScannedFiles == mNumberOfFiles) {
                // all files scanned so get next directory
                getNextFolder(mContext);
            }
        }
    }
}
