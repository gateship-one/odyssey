package org.odyssey.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;

import org.odyssey.models.TrackModel;

public class CoverBitmapGenerator {
    private CoverBitmapListener mListener;
    private Context mContext;
    private TrackModel mTrack;
    private Thread mGeneratorThread;

    public CoverBitmapGenerator(Context context, CoverBitmapListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void getImage(TrackModel track) {
        if (track != null) {
            mTrack = track;
            // Create generator thread
            mGeneratorThread = new Thread(new DownloadRunner());
            mGeneratorThread.start();
        }
    }

    private class DownloadRunner implements Runnable {

        @Override
        public void run() {
            String where = android.provider.MediaStore.Audio.Albums.ALBUM_KEY + "=?";

            String whereVal[] = { mTrack.getTrackAlbumKey() };

            Cursor cursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[] { MediaStore.Audio.Albums.ALBUM_ART }, where, whereVal, "");

            String coverPath = null;
            if (cursor.moveToFirst()) {
                coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            }
            if (coverPath != null) {
                BitmapDrawable cover = (BitmapDrawable) BitmapDrawable.createFromPath(coverPath);
                mListener.receiveBitmap(cover);
            }

            cursor.close();
        }
    }

    public interface CoverBitmapListener {
        void receiveBitmap(BitmapDrawable bm);
    }
}