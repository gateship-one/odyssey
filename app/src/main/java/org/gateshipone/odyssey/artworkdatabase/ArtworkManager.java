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

package org.gateshipone.odyssey.artworkdatabase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Response;

import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class ArtworkManager implements ArtistFetchError, AlbumFetchError {
    private static final String TAG = ArtworkManager.class.getSimpleName();

    private ArtworkDatabaseManager mDBManager;
    private final ArrayList<onNewArtistImageListener> mArtistListeners;

    private final ArrayList<onNewAlbumImageListener> mAlbumListeners;

    private static ArtworkManager mInstance;
    private Context mContext;

    private ArtworkManager(Context context) {

        mDBManager = ArtworkDatabaseManager.getInstance(context);

        mArtistListeners = new ArrayList<>();
        mAlbumListeners = new ArrayList<>();

        mContext = context;
    }

    public static synchronized ArtworkManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ArtworkManager(context);
        }
        return mInstance;
    }

    public Bitmap getArtistImage(final ArtistModel artist) throws ImageNotInDatabaseException {
        if (null == artist) {
            return null;
        }

        long artistID = artist.getArtistID();

        byte[] image;

        /**
         * If no artist id is set for the album (possible with data set of Odyssey) check
         * the artist with name instead of id.
         */
        if (artistID == -1) {
            image = mDBManager.getArtistImage(artist.getArtistName());
        } else {
            image = mDBManager.getArtistImage(artistID);
        }

        // Checks if the database has an image for the requested artist
        if (null != image) {
            // If image is existing or blocked for the artist. (blocked: not found before)
            if (image.length != 0) {
                // Create a bitmap from the data blob in the database
                return BitmapFactory.decodeByteArray(image, 0, image.length);
            }
        } else {
            // Image not in database throw exception
            throw new ImageNotInDatabaseException();
        }
        return null;
    }

    public Bitmap getAlbumImage(final AlbumModel album) throws ImageNotInDatabaseException {
        if (null == album) {
            return null;
        }

        // Get the id for the album, used to check in database
        long albumID = album.getAlbumID();

        byte[] image;

        if (albumID == -1) {
            // Check if ID is available (should be the case). If not use the album name for
            // lookup.
            // FIXME use artistname also
            image = mDBManager.getAlbumImage(album.getAlbumName());
        } else {
            // If id is available use it.
            image = mDBManager.getAlbumImage(album.getAlbumID());
        }

        // Checks if the database has an image for the requested album
        if (null != image) {
            // If image is existing or blocked for the artist. (blocked: not found before)
            if (image.length != 0) {
                // Create a bitmap from the data blob in the database
                return BitmapFactory.decodeByteArray(image, 0, image.length);
            }
        } else {
            // Image not in database throw exception
            throw new ImageNotInDatabaseException();
        }
        return null;
    }

    /**
     * Starts an asynchronous fetch for the image of the given artist.
     * @param artist Artist to fetch an image for.
     */
    public void fetchArtistImage(final ArtistModel artist) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifi = cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;

        if (!isWifi) {
            return;
        }

        LastFMManager.getInstance().fetchArtistImage(artist, new Response.Listener<Pair<byte[], ArtistModel>>() {
            @Override
            public void onResponse(Pair<byte[], ArtistModel> response) {
                new InsertArtistImageTask().execute(response);
            }
        }, this);
    }

    /**
     * Starts an asynchronous fetch for the image of the given album
     * @param album Album to fetch an image for.
     */
    public void fetchAlbumImage(final AlbumModel album) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifi = cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;

        if (!isWifi) {
            return;
        }

        MusicBrainzManager.getInstance().fetchAlbumImage(album, new Response.Listener<Pair<byte[], AlbumModel>>() {
            @Override
            public void onResponse(Pair<byte[], AlbumModel> response) {
                new InsertAlbumImageTask().execute(response);
            }
        }, this);
    }

    public void  registerOnNewArtistImageListener(onNewArtistImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mArtistListeners.add(listener);
            }
        }
    }

    public void unregisterOnNewArtistImageListener(onNewArtistImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mArtistListeners.remove(listener);
            }
        }
    }

    public void  registerOnNewAlbumImageListener(onNewAlbumImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mAlbumListeners.add(listener);
            }
        }
    }

    public void unregisterOnNewAlbumImageListener(onNewAlbumImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mAlbumListeners.remove(listener);
            }
        }
    }

    @Override
    public void fetchError(ArtistModel artist) {
        Log.e(TAG, "Error fetching: " + artist.getArtistName());
        // FIXME check if retrying again and again is a problem
//        mDBManager.insertArtistImage(artist, new byte[0]);
    }

    @Override
    public void fetchError(AlbumModel album) {
        Log.e(TAG,"Fetch error for album: " + album.getAlbumName() + "-" + album.getArtistName());
    }



    public class ImageNotInDatabaseException extends Exception {

    }

    private class InsertArtistImageTask extends AsyncTask<Pair<byte[], ArtistModel>, Object, ArtistModel> {

        @Override
        protected ArtistModel doInBackground(Pair<byte[], ArtistModel>... params) {
            Pair<byte[], ArtistModel> response = params[0];

            Log.v(TAG, "Received image with: " + response.first.length + "bytes for artist: " + response.second.getArtistName() +
                    " with MBID: " + response.second.getMBID());

            mDBManager.insertArtistImage(response.second, response.first);


            return response.second;
        }

        protected void onPostExecute(ArtistModel result) {
            synchronized (mArtistListeners) {
                for (onNewArtistImageListener artistListener : mArtistListeners) {
                    artistListener.newArtistImage(result);
                }
            }
        }

    }

    private class InsertAlbumImageTask extends AsyncTask<Pair<byte[], AlbumModel>, Object, AlbumModel> {

        @Override
        protected AlbumModel doInBackground(Pair<byte[], AlbumModel>... params) {
            Pair<byte[], AlbumModel> response = params[0];

            Log.v(TAG, "Received image with: " + response.first.length + "bytes for album: " + response.second.getAlbumName() +
                    " with MBID: " + response.second.getMBID());

            mDBManager.insertAlbumImage(response.second, response.first);


            return response.second;
        }

        protected void onPostExecute(AlbumModel result) {
            synchronized (mAlbumListeners) {
                for (onNewAlbumImageListener albumListener : mAlbumListeners) {
                    albumListener.newAlbumImage(result);
                }
            }
        }

    }

    /**
     * Interface used for adapters to be notified about data set changes
     */
    public interface onNewArtistImageListener {
        void newArtistImage(ArtistModel artist);
    }

    /**
     * Interface used for adapters to be notified about data set changes
     */
    public interface onNewAlbumImageListener {
        void newAlbumImage(AlbumModel album);
    }
}
