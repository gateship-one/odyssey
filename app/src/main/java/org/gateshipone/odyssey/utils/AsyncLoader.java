/*
 * Copyright (C) 2019 Team Gateship-One
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
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Pair;

import org.gateshipone.odyssey.adapter.ScrollSpeedAdapter;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.artworkdatabase.ImageNotFoundException;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.GenericModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.viewitems.CoverLoadable;

/**
 * Loader class for covers
 */
public class AsyncLoader extends AsyncTask<AsyncLoader.CoverViewHolder, Void, Bitmap> {
    private static final String TAG = AsyncLoader.class.getSimpleName();
    private CoverViewHolder mCover;

    private final Context mContext;

    private long mStartTime;

    public AsyncLoader(Context context) {
        mContext = context;
    }

    /**
     * Wrapper class for covers
     */
    public static class CoverViewHolder {
        public Pair<Integer, Integer> imageDimension;
        public CoverLoadable coverLoadable;
        public ArtworkManager artworkManager;
        public GenericModel modelItem;
        public ScrollSpeedAdapter mAdapter;
    }

    @Override
    protected Bitmap doInBackground(CoverViewHolder... params) {
        // Save the time when loading started for later duration calculation
        mStartTime = System.currentTimeMillis();

        mCover = params[0];
        Bitmap image = null;
        // Check if model item is artist or album
        if (mCover.modelItem instanceof ArtistModel) {
            ArtistModel artist = (ArtistModel) mCover.modelItem;

            try {
                // Check if image is available. If it is not yet fetched it will throw an exception
                // If it was already searched for and not found, this will be null.
                image = mCover.artworkManager.getArtistImage(mContext, artist, mCover.imageDimension.first, mCover.imageDimension.second, false);
            } catch (ImageNotFoundException e) {
                // Check if fetching for this item is already ongoing
                if (!artist.getFetching()) {
                    // If not set it as ongoing and request the image fetch.
                    mCover.artworkManager.fetchArtistImage(artist, mContext);
                    artist.setFetching(true);
                }
            }
        } else if (mCover.modelItem instanceof AlbumModel) {
            AlbumModel album = (AlbumModel) mCover.modelItem;

            try {
                // Check if image is available. If it is not yet fetched it will throw an exception.
                // If it was already searched for and not found, this will be null.
                image = mCover.artworkManager.getAlbumImage(mContext, album, mCover.imageDimension.first, mCover.imageDimension.second, false);
            } catch (ImageNotFoundException e) {
                // Check if fetching for this item is already ongoing
                if (!album.getFetching()) {
                    // If not set it as ongoing and request the image fetch.
                    mCover.artworkManager.fetchAlbumImage(album, mContext);
                    album.setFetching(true);
                }
            }
        } else if (mCover.modelItem instanceof TrackModel) {
            TrackModel track = (TrackModel) mCover.modelItem;

            try {
                // Check if image is available. If it is not yet fetched it will throw an exception.
                // If it was already searched for and not found, this will be null.
                image = mCover.artworkManager.getAlbumImage(mContext, track, mCover.imageDimension.first, mCover.imageDimension.second, false);
            } catch (ImageNotFoundException e) {
                // If not set it as ongoing and request the image fetch.
                mCover.artworkManager.fetchAlbumImage(track, mContext);
            }
        }
        return image;
    }



    @Override
    protected void onPostExecute(Bitmap result) {

        super.onPostExecute(result);

        // set mCover if exists
        if (result != null) {
            if (mCover.mAdapter != null) {
                mCover.mAdapter.addImageLoadTime(System.currentTimeMillis() - mStartTime);
            }
            mCover.coverLoadable.setImage(result);
        }
    }
}