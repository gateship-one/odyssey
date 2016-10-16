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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Pair;

import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.artworkdatabase.ImageNotFoundException;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.views.GridViewItem;

/**
 * Loader class for covers
 */
public class AsyncLoader extends AsyncTask<AsyncLoader.CoverViewHolder, Void, Bitmap> {
    private static final String TAG = AsyncLoader.class.getSimpleName();
    private CoverViewHolder mCover;

    /**
     * Wrapper class for covers
     */
    public static class CoverViewHolder {
        public String imagePath;
        public Pair<Integer, Integer> imageDimension;
        public GridViewItem gridItem;
        public ArtworkManager artworkManager;
        public GenericModel modelItem;
    }

    @Override
    protected Bitmap doInBackground(CoverViewHolder... params) {
        mCover = params[0];
            Bitmap image = null;
            // Check if model item is artist or album
            if (mCover.modelItem instanceof ArtistModel) {
                ArtistModel artist = (ArtistModel)mCover.modelItem;
                String artistURL = artist.getArtistURL();
                if ( null != artistURL ) {
                    return decodeSampledBitmapFromResource(artistURL, mCover.imageDimension.first, mCover.imageDimension.second);
                }

                try {
                    // Check if image is available. If it is not yet fetched it will throw an exception
                    // If it was already searched for and not found, this will be null.
                    image = mCover.artworkManager.getArtistImage(artist);
                } catch (ImageNotFoundException e) {
                    // Check if fetching for this item is already ongoing
                    if (!artist.getFetching()) {
                        // If not set it as ongoing and request the image fetch.
                        mCover.artworkManager.fetchArtistImage(artist);
                        artist.setFetching(true);
                    }
                }
            } else if (mCover.modelItem instanceof AlbumModel) {
                AlbumModel album = (AlbumModel)mCover.modelItem;
                String albumURL = album.getAlbumArtURL();
                if ( null != albumURL ) {
                    return decodeSampledBitmapFromResource(albumURL, mCover.imageDimension.first, mCover.imageDimension.second);
                }
                try {
                    // Check if image is available. If it is not yet fetched it will throw an exception.
                    // If it was already searched for and not found, this will be null.
                    image = mCover.artworkManager.getAlbumImage(album);
                } catch (ImageNotFoundException e) {
                    // Check if fetching for this item is already ongoing
                    if (!album.getFetching()) {
                        // If not set it as ongoing and request the image fetch.
                        mCover.artworkManager.fetchAlbumImage(album);
                        album.setFetching(true);
                    }
                }
            }
            return image;

    }

    /**
     * Resize retrieved bitmap if necessary
     */
    private Bitmap decodeSampledBitmapFromResource(String pathName, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        if (reqWidth == 0 && reqHeight == 0) {
            // check if the layout of the view already set
            options.inSampleSize = 1;
        } else {
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    /**
     * Calculate sample size to resize the bitmap
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onPostExecute(Bitmap result) {

        super.onPostExecute(result);

        // set mCover if exists
        if ( null != result ) {
            mCover.gridItem.setImage(result);
        }
    }
}