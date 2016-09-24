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

package org.gateshipone.odyssey.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;

import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.artworkdatabase.ImageNotFoundException;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.TrackModel;

public class CoverBitmapLoader {
    private final CoverBitmapListener mListener;
    private final Context mContext;
    private TrackModel mTrack;

    public CoverBitmapLoader(Context context, CoverBitmapListener listener) {
        mContext = context;
        mListener = listener;
    }

    /**
     * Load the image for the given track from the mediastore.
     */
    public void getImage(TrackModel track) {
        if (track != null) {
            mTrack = track;
            // start the loader thread to load the image async
            Thread loaderThread = new Thread(new TrackAlbumImageRunner());
            loaderThread.start();
        }
    }

    public void getArtistImage(ArtistModel artist) {
        if ( artist == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new ArtistImageRunner(artist));
        loaderThread.start();
    }

    public void getAlbumImage(AlbumModel album) {
        if ( album == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new AlbumImageRunner(album));
        loaderThread.start();
    }

    private class TrackAlbumImageRunner implements Runnable {

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            String where = android.provider.MediaStore.Audio.Albums.ALBUM_KEY + "=?";

            String whereVal[] = { mTrack.getTrackAlbumKey() };

            Cursor cursor = PermissionHelper.query(mContext, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Albums.ALBUM_ART}, where, whereVal, "");

            if(cursor != null) {
                String coverPath = null;
                if (cursor.moveToFirst()) {
                    coverPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                }
                if (coverPath != null) {
                    Bitmap cover = (Bitmap) BitmapFactory.decodeFile(coverPath);
                    mListener.receiveBitmap(cover);
                }

                cursor.close();
            }

            // If we reach this, we obviously don't have a local image. Try the database of downloaded images
            try {
                Bitmap image = ArtworkManager.getInstance(mContext).getAlbumImage(mTrack);
                mListener.receiveBitmap(image);
            } catch (ImageNotFoundException e) {
                // Try to fetch the image here
                ArtworkManager.getInstance(mContext).fetchAlbumImage(mTrack);
            }
        }
    }

    private class ArtistImageRunner implements Runnable {

        private ArtistModel mArtist;

        public ArtistImageRunner(ArtistModel artist) {
            mArtist = artist;
        }

        /**
         * Load the image for the given artist from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap artistImage = ArtworkManager.getInstance(mContext).getArtistImage(mArtist);
                mListener.receiveBitmap(artistImage);
            } catch (ImageNotFoundException e) {
                ArtworkManager.getInstance(mContext).fetchArtistImage(mArtist);
            }
        }
    }

    private class AlbumImageRunner implements Runnable {

        private AlbumModel mAlbum;

        public AlbumImageRunner(AlbumModel album) {
            mAlbum = album;
        }

        /**
         * Load the image for the given album from the mediastore.
         */
        @Override
        public void run() {
            try {
                // Check if local image (tagged in album) is available
                if ( mAlbum.getAlbumArtURL() != null && !mAlbum.getAlbumArtURL().isEmpty() ) {
                    Bitmap cover = (Bitmap) BitmapFactory.decodeFile(mAlbum.getAlbumArtURL());
                    mListener.receiveBitmap(cover);
                } else {
                    if ( mAlbum.getAlbumID() == -1 ) {
                        mAlbum.setAlbumID(MusicLibraryHelper.getAlbumIDFromKey(mAlbum.getAlbumKey(), mContext));
                    }
                    // No tagged album image available, check download database
                    Bitmap artistImage = ArtworkManager.getInstance(mContext).getAlbumImage(mAlbum);
                    mListener.receiveBitmap(artistImage);
                }
            } catch (ImageNotFoundException e) {
                ArtworkManager.getInstance(mContext).fetchAlbumImage(mAlbum);
            }
        }
    }

    /**
     * Callback if image was loaded.
     */
    public interface CoverBitmapListener {
        void receiveBitmap(Bitmap bm);
    }
}