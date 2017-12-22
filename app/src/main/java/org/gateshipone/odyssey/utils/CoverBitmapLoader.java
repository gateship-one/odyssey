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
import org.gateshipone.odyssey.artworkdatabase.BitmapCache;
import org.gateshipone.odyssey.artworkdatabase.ImageNotFoundException;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.TrackModel;

// FIXME clean up where things are stored. mTrack, mContext,...!!!
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
    public void getImage(TrackModel track, int width, int height) {
        if (track != null && !track.getTrackAlbumKey().isEmpty()) {
            mTrack = track;
            // start the loader thread to load the image async
            Thread loaderThread = new Thread(new TrackAlbumImageRunner(width,height));
            loaderThread.start();
        }
    }

    public void getArtistImage(ArtistModel artist, int width, int height) {
        if ( artist == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new ArtistImageRunner(artist, width, height));
        loaderThread.start();
    }

    public void getAlbumImage(AlbumModel album, int width, int height) {
        if ( album == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new AlbumImageRunner(album, mContext, width, height));
        loaderThread.start();
    }

    public void getArtistImage(TrackModel track, int width, int height) {
        if (track==null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new TrackArtistImageRunner(track, width, height));
        loaderThread.start();
    }

    private class TrackAlbumImageRunner implements Runnable {
        private int mWidth;
        private int mHeight;

        public TrackAlbumImageRunner(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            // At first get image independent of resolution (can be replaced later with higher resolution)
            AlbumModel album = MusicLibraryHelper.createAlbumModelFromKey(mTrack.getTrackAlbumKey(), mContext);

            Bitmap image = BitmapCache.getInstance().requestAlbumBitmap(album);
            if(image != null) {
                mListener.receiveBitmap(image, IMAGE_TYPE.ALBUM_IMAGE);
            }

            try {
                // If image was to small get it in the right resolution
                if (image == null || !(mWidth <= image.getWidth() && mHeight <= image.getHeight())) {
                    image = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImage(mContext, album, mWidth, mHeight, true);
                    mListener.receiveBitmap(image,IMAGE_TYPE.ALBUM_IMAGE);
                    // Replace image with higher resolution one
                    BitmapCache.getInstance().putAlbumBitmap(album, image);
                }
            } catch (ImageNotFoundException e) {
                // Try to fetch the image here
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mTrack, mContext);
            }
        }
    }

    private class ArtistImageRunner implements Runnable {
        private int mWidth;
        private int mHeight;

        private ArtistModel mArtist;

        public ArtistImageRunner(ArtistModel artist, int width, int height) {
            mArtist = artist;
            mWidth = width;
            mHeight = height;
        }

        /**
         * Load the image for the given artist from the mediastore.
         */
        @Override
        public void run() {
            // At first get image independent of resolution (can be replaced later with higher resolution)
            Bitmap image = BitmapCache.getInstance().requestArtistImage(mArtist);
            mListener.receiveBitmap(image,IMAGE_TYPE.ARTIST_IMAGE);

            // If image was to small get it in the right resolution
            if (image == null || !(mWidth <= image.getWidth() && mHeight <= image.getHeight())) {
                try {
                    Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getArtistImage(mContext, mArtist, mWidth, mHeight, true);
                    mListener.receiveBitmap(artistImage, IMAGE_TYPE.ARTIST_IMAGE);
                    // Replace image with higher resolution one
                    BitmapCache.getInstance().putArtistImage(mArtist, image);
                } catch (ImageNotFoundException e) {
                    ArtworkManager.getInstance(mContext.getApplicationContext()).fetchArtistImage(mArtist, mContext);
                }
            }
        }
    }

    private class TrackArtistImageRunner implements Runnable {
        private int mWidth;
        private int mHeight;

        private ArtistModel mArtist;

        public TrackArtistImageRunner(TrackModel trackModel, int width, int height) {
            long artistID = MusicLibraryHelper.getArtistIDFromName(trackModel.getTrackArtistName(), mContext);
            mArtist = new ArtistModel(trackModel.getTrackArtistName(), artistID );
            mWidth = width;
            mHeight = height;
        }

        /**
         * Load the image for the given artist from the mediastore.
         */
        @Override
        public void run() {
            // At first get image independent of resolution (can be replaced later with higher resolution)
            Bitmap image = BitmapCache.getInstance().requestArtistImage(mArtist);
            mListener.receiveBitmap(image,IMAGE_TYPE.ARTIST_IMAGE);

            // If image was to small get it in the right resolution
            if (image == null || !(mWidth <= image.getWidth() && mHeight <= image.getHeight())) {
                try {
                    Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getArtistImage(mContext, mArtist, mWidth, mHeight,true);
                    mListener.receiveBitmap(artistImage, IMAGE_TYPE.ARTIST_IMAGE);
                    // Replace image with higher resolution one
                    BitmapCache.getInstance().putArtistImage(mArtist, image);
                } catch (ImageNotFoundException e) {
                    ArtworkManager.getInstance(mContext.getApplicationContext()).fetchArtistImage(mArtist, mContext);
                }
            }
        }
    }

    private class AlbumImageRunner implements Runnable {
        private int mWidth;
        private int mHeight;
        private AlbumModel mAlbum;

        private final Context mContext;

        public AlbumImageRunner(AlbumModel album, Context context, int width, int height) {
            mAlbum = album;
            mContext = context;
            mWidth = width;
            mHeight = height;
        }

        /**
         * Load the image for the given album from the mediastore.
         */
        @Override
        public void run() {
            // At first get image independent of resolution (can be replaced later with higher resolution)
            Bitmap image = BitmapCache.getInstance().requestAlbumBitmap(mAlbum);
            if(image != null) {
                mListener.receiveBitmap(image, IMAGE_TYPE.ALBUM_IMAGE);
            }

            try {
                // If image was to small get it in the right resolution
                if (image == null || !(mWidth <= image.getWidth() && mHeight <= image.getHeight())) {
                    image = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImage(mContext, mAlbum, mWidth, mHeight,true);
                    mListener.receiveBitmap(image,IMAGE_TYPE.ALBUM_IMAGE);
                    // Replace image with higher resolution one
                    BitmapCache.getInstance().putAlbumBitmap(mAlbum, image);
                }
            } catch (ImageNotFoundException e) {
                // Try to fetch the image here
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mTrack, mContext);
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