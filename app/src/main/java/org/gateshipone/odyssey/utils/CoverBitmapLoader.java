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

package org.gateshipone.odyssey.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;

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
     * Enum to define the type of the image that was retrieved
     */
    public enum IMAGE_TYPE {
        ALBUM_IMAGE,
        ARTIST_IMAGE,
    }

    /**
     * Load the image for the given track from the mediastore.
     */
    public void getImage(TrackModel track) {
        if (track != null && !track.getTrackAlbumKey().isEmpty()) {
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
        Thread loaderThread = new Thread(new AlbumImageRunner(album, mContext));
        loaderThread.start();
    }

    public void getArtistImage(TrackModel track) {
        if (track==null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new TrackArtistImageRunner(track));
        loaderThread.start();
    }

    private class TrackAlbumImageRunner implements Runnable {

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap image = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImage(mContext, mTrack);
                mListener.receiveBitmap(image,IMAGE_TYPE.ALBUM_IMAGE);
            } catch (ImageNotFoundException e) {
                // Try to fetch the image here
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mTrack, mContext);
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
                Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getArtistImage(mContext, mArtist);
                mListener.receiveBitmap(artistImage, IMAGE_TYPE.ARTIST_IMAGE);
            } catch (ImageNotFoundException e) {
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchArtistImage(mArtist, mContext);
            }
        }
    }

    private class TrackArtistImageRunner implements Runnable {

        private ArtistModel mArtist;

        public TrackArtistImageRunner(TrackModel trackModel) {
            long artistID = MusicLibraryHelper.getArtistIDFromName(trackModel.getTrackArtistName(), mContext);
            mArtist = new ArtistModel(trackModel.getTrackArtistName(), artistID );
        }

        /**
         * Load the image for the given artist from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getArtistImage(mContext, mArtist);
                mListener.receiveBitmap(artistImage,IMAGE_TYPE.ARTIST_IMAGE);
            } catch (ImageNotFoundException e) {
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchArtistImage(mArtist, mContext);
            }
        }
    }

    private class AlbumImageRunner implements Runnable {

        private AlbumModel mAlbum;

        private final Context mContext;

        public AlbumImageRunner(AlbumModel album, Context context) {
            mAlbum = album;
            mContext = context;
        }

        /**
         * Load the image for the given album from the mediastore.
         */
        @Override
        public void run() {
            try {
                // Check if local image (tagged in album) is available
                if ( mAlbum.getAlbumArtURL() != null && !mAlbum.getAlbumArtURL().isEmpty() ) {
                    Bitmap cover = BitmapFactory.decodeFile(mAlbum.getAlbumArtURL());
                    mListener.receiveBitmap(cover,IMAGE_TYPE.ALBUM_IMAGE);
                } else {
                    if ( mAlbum.getAlbumID() == -1 ) {
                        mAlbum.setAlbumID(MusicLibraryHelper.getAlbumIDFromKey(mAlbum.getAlbumKey(), mContext));
                    }
                    // No tagged album image available, check download database
                    Bitmap albumImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImage(mContext, mAlbum);
                    mListener.receiveBitmap(albumImage, IMAGE_TYPE.ALBUM_IMAGE);
                }
            } catch (ImageNotFoundException e) {
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mAlbum, mContext);
            }
        }
    }

    /**
     * Callback if image was loaded.
     */
    public interface CoverBitmapListener {
        void receiveBitmap(Bitmap bm, IMAGE_TYPE type);
    }
}