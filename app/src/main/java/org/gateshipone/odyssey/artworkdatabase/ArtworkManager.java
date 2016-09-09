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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.gateshipone.odyssey.artworkdatabase.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artworkdatabase.network.artprovider.FanartTVManager;
import org.gateshipone.odyssey.artworkdatabase.network.artprovider.LastFMManager;
import org.gateshipone.odyssey.artworkdatabase.network.artprovider.MusicBrainzManager;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class ArtworkManager implements ArtistFetchError, AlbumFetchError {
    private static final String TAG = ArtworkManager.class.getSimpleName();
    private static final int MAXIMUM_IMAGE_SIZE = 500;
    private static final int IMAGE_COMPRESSION_SETTING = 80;

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

        ConnectionStateReceiver receiver = new ConnectionStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(receiver, filter);
    }

    public static synchronized ArtworkManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ArtworkManager(context);
        }
        return mInstance;
    }

    public Bitmap getArtistImage(final ArtistModel artist) throws ImageNotFoundException {
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
            // Create a bitmap from the data blob in the database
            return BitmapFactory.decodeByteArray(image, 0, image.length);
        }
        return null;
    }

    public Bitmap getAlbumImage(final AlbumModel album) throws ImageNotFoundException {
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
            // Create a bitmap from the data blob in the database
            return BitmapFactory.decodeByteArray(image, 0, image.length);

        }
        return null;
    }

    /**
     * Starts an asynchronous fetch for the image of the given artist.
     *
     * @param artist Artist to fetch an image for.
     */
    public void fetchArtistImage(final ArtistModel artist) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String artistProvider = sharedPref.getString("pref_artist_provider", "last_fm");

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean wifiOnly = sharedPref.getBoolean("pref_download_wifi_only", true);

        boolean isWifi = cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI || cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_ETHERNET;

        if (wifiOnly && !isWifi) {
            return;
        }

        if (artistProvider.equals("last_fm")) {
            LastFMManager.getInstance(mContext).fetchArtistImage(artist, new Response.Listener<ArtistImageResponse>() {
                @Override
                public void onResponse(ArtistImageResponse response) {
                    new InsertArtistImageTask().execute(response);
                }
            }, this);
        } else if (artistProvider.equals("fanart_tv")) {
            FanartTVManager.getInstance(mContext).fetchArtistImage(artist, new Response.Listener<ArtistImageResponse>() {
                @Override
                public void onResponse(ArtistImageResponse response) {
                    new InsertArtistImageTask().execute(response);
                }
            }, this);
        }
    }

    /**
     * Starts an asynchronous fetch for the image of the given album
     *
     * @param album Album to fetch an image for.
     */
    public void fetchAlbumImage(final AlbumModel album) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String albumProvider = sharedPref.getString("pref_album_provider", "musicbrainz");

        boolean wifiOnly = sharedPref.getBoolean("pref_download_wifi_only", true);

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifi = cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI || cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_ETHERNET;

        if (wifiOnly && !isWifi) {
            return;
        }

        if (albumProvider.equals("musicbrainz")) {
            MusicBrainzManager.getInstance(mContext).fetchAlbumImage(album, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask().execute(response);
                }
            }, this);
        } else if (albumProvider.equals("last_fm")) {
            LastFMManager.getInstance(mContext).fetchAlbumImage(album, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask().execute(response);
                }
            }, this);
        }
    }

    /**
     * Registers a listener that gets notified when a new artist image was added to the dataset.
     *
     * @param listener Listener to register
     */
    public void registerOnNewArtistImageListener(onNewArtistImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mArtistListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a listener that got notified when a new artist image was added to the dataset.
     *
     * @param listener Listener to unregister
     */
    public void unregisterOnNewArtistImageListener(onNewArtistImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mArtistListeners.remove(listener);
            }
        }
    }

    /**
     * Registers a listener that gets notified when a new album image was added to the dataset.
     *
     * @param listener Listener to register
     */
    public void registerOnNewAlbumImageListener(onNewAlbumImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mAlbumListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a listener that got notified when a new album image was added to the dataset.
     *
     * @param listener Listener to unregister
     */
    public void unregisterOnNewAlbumImageListener(onNewAlbumImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mAlbumListeners.remove(listener);
            }
        }
    }

    /**
     * Interface implementation to handle errors during fetching of artist images
     *
     * @param artist Artist that resulted in a fetch error
     */
    @Override
    public void fetchError(ArtistModel artist) {
        Log.e(TAG, "Error fetching: " + artist.getArtistName());
        ArtistImageResponse imageResponse = new ArtistImageResponse();
        imageResponse.artist = artist;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertArtistImageTask().execute(imageResponse);
    }

    /**
     * Interface implementation to handle errors during fetching of album images
     *
     * @param album Album that resulted in a fetch error
     */
    @Override
    public void fetchError(AlbumModel album) {
        Log.e(TAG, "Fetch error for album: " + album.getAlbumName() + "-" + album.getArtistName());
        AlbumImageResponse imageResponse = new AlbumImageResponse();
        imageResponse.album = album;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertAlbumImageTask().execute(imageResponse);
    }

    /**
     * AsyncTask to insert the images to the SQLdatabase. This is necessary as the Volley response
     * is handled in the UI thread.
     */
    private class InsertArtistImageTask extends AsyncTask<ArtistImageResponse, Object, ArtistModel> {

        /**
         * Inserts the image to the database.
         *
         * @param params Pair of byte[] (containing the image itself) and ArtistModel for which the image is for
         * @return the artist model that was inserted to the database.
         */
        @Override
        protected ArtistModel doInBackground(ArtistImageResponse... params) {
            ArtistImageResponse response = params[0];

            if ( response.image == null ){
                mDBManager.insertArtistImage(response.artist, response.image);
                return response.artist;
            }

            // Rescale them if to big
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
            if ((options.outHeight > MAXIMUM_IMAGE_SIZE || options.outWidth > MAXIMUM_IMAGE_SIZE)) {
                options.inJustDecodeBounds = false;
                Bitmap bm = BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                bm.createScaledBitmap(bm, MAXIMUM_IMAGE_SIZE, MAXIMUM_IMAGE_SIZE, true).compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);
                mDBManager.insertArtistImage(response.artist, byteStream.toByteArray());
            } else {
                mDBManager.insertArtistImage(response.artist, response.image);
            }

            return response.artist;
        }

        /**
         * Notifies the listeners about a change in the image dataset. Called in the UI thread.
         *
         * @param result Artist that was inserted in the database
         */
        protected void onPostExecute(ArtistModel result) {
            synchronized (mArtistListeners) {
                for (onNewArtistImageListener artistListener : mArtistListeners) {
                    artistListener.newArtistImage(result);
                }
            }
        }

    }

    /**
     * AsyncTask to insert the images to the SQLdatabase. This is necessary as the Volley response
     * is handled in the UI thread.
     */
    private class InsertAlbumImageTask extends AsyncTask<AlbumImageResponse, Object, AlbumModel> {

        /**
         * Inserts the image to the database.
         *
         * @param params Pair of byte[] (containing the image itself) and AlbumModel for which the image is for
         * @return the album model that was inserted to the database.
         */
        @Override
        protected AlbumModel doInBackground(AlbumImageResponse... params) {
            AlbumImageResponse response = params[0];

            if ( response.image == null ){
                mDBManager.insertAlbumImage(response.album, response.image);
                return response.album;
            }

            // Rescale them if to big
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
            if ((options.outHeight > MAXIMUM_IMAGE_SIZE || options.outWidth > MAXIMUM_IMAGE_SIZE)) {
                options.inJustDecodeBounds = false;
                Bitmap bm = BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                bm.createScaledBitmap(bm, MAXIMUM_IMAGE_SIZE, MAXIMUM_IMAGE_SIZE, true).compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);
                mDBManager.insertAlbumImage(response.album, byteStream.toByteArray());
            } else {
                mDBManager.insertAlbumImage(response.album, response.image);
            }

            return response.album;
        }

        /**
         * Notifies the listeners about a change in the image dataset. Called in the UI thread.
         *
         * @param result Album that was inserted in the database
         */
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

    private class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);

            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if ( null == netInfo) {
                return;
            }
            boolean wifiOnly = sharedPref.getBoolean("pref_download_wifi_only", true);
            boolean isWifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

            if ( wifiOnly && !isWifi)  {
                // Cancel all downloads
                Log.v(TAG,"Cancel all downloads because of connection change");
                cancelAllRequests();
            }

        }
    }

    /**
     * This will cancel the last used album/artist image providers. To make this useful on connection change
     * it is important to cancel all requests when changing the provider in settings.
     */
    public void cancelAllRequests() {
        LimitingRequestQueue.getInstance(mContext).cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }
}
