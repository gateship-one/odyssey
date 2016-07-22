package org.odyssey.utils;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileExplorerHelper {

    private static FileExplorerHelper mInstance = null;
    private Context mContext = null;
    private List<String> mStorageVolumesList;

    private FileExplorerHelper(Context context) {
        mContext = context;

        // create storage volume list
        mStorageVolumesList = new ArrayList<>();

        File[] storageList = ContextCompat.getExternalFilesDirs(mContext, null);
        for (File storageFile : storageList ) {
            mStorageVolumesList.add(storageFile.getAbsolutePath().replaceAll("/Android/data/" + mContext.getPackageName() + "/files", ""));
        }
    }

    public static synchronized FileExplorerHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FileExplorerHelper(context);
        }

        return mInstance;
    }

    public List<String> getStorageVolumes() {
        return mStorageVolumesList;
    }

}
