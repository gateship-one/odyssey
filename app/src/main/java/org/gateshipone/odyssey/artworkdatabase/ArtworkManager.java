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

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.odyssey.R;
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
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ArtworkManager implements ArtistFetchError, AlbumFetchError {
    private static final String TAG = ArtworkManager.class.getSimpleName();
    private static final int MAXIMUM_IMAGE_SIZE = 500;
    private static final int IMAGE_COMPRESSION_SETTING = 80;

    private ArtworkDatabaseManager mDBManager;
    private final ArrayList<onNewArtistImageListener> mArtistListeners;

    private final ArrayList<onNewAlbumImageListener> mAlbumListeners;

    private static ArtworkManager mInstance;

    private final List<AlbumModel> mAlbumList = new ArrayList<>();
    private final List<ArtistModel> mArtistList = new ArrayList<>();

    private AlbumModel mCurrentBulkAlbum = null;
    private ArtistModel mCurrentBulkArtist = null;

    private BulkLoadingProgressCallback mBulkProgressCallback;

    private String mArtistProvider;

    private String mAlbumProvider;

    private boolean mWifiOnly;

    /*
     * Broadcast constants
     */
    public static final String ACTION_NEW_ARTWORK_READY = "org.gateshipone.odyssey.action_new_artwork_ready";

    public static final String INTENT_EXTRA_KEY_ARTIST_ID = "org.gateshipone.odyssey.extra.artist_id";
    public static final String INTENT_EXTRA_KEY_ARTIST_NAME = "org.gateshipone.odyssey.extra.artist_name";

    public static final String INTENT_EXTRA_KEY_ALBUM_ID = "org.gateshipone.odyssey.extra.album_id";
    public static final String INTENT_EXTRA_KEY_ALBUM_NAME = "org.gateshipone.odyssey.extra.album_name";
    public static final String INTENT_EXTRA_KEY_ALBUM_KEY = "org.gateshipone.odyssey.extra.album_key";

    private ArtworkManager(Context context) {

        mDBManager = ArtworkDatabaseManager.getInstance(context);

        mArtistListeners = new ArrayList<>();
        mAlbumListeners = new ArrayList<>();

        ConnectionStateReceiver receiver = new ConnectionStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, filter);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mArtistProvider = sharedPref.getString(context.getString(R.string.pref_artist_provider_key), context.getString(R.string.pref_artwork_provider_artist_default));
        mAlbumProvider = sharedPref.getString(context.getString(R.string.pref_album_provider_key), context.getString(R.string.pref_artwork_provider_album_default));
        mWifiOnly = sharedPref.getBoolean(context.getString(R.string.pref_download_wifi_only_key), context.getResources().getBoolean(R.bool.pref_download_wifi_default));
    }

    public static synchronized ArtworkManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ArtworkManager(context);
        }
        return mInstance;
    }

    public void setWifiOnly(boolean wifiOnly) {
        mWifiOnly = wifiOnly;
    }

    public void setAlbumProvider(String albumProvider) {
        mAlbumProvider = albumProvider;
    }

    public void setArtistProvider(String artistProvider) {
        mArtistProvider = artistProvider;
    }

    public void initialize(String artistProvider, String albumProvider, boolean wifiOnly) {
        mArtistProvider = artistProvider;
        mAlbumProvider = albumProvider;
        mWifiOnly = wifiOnly;
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

    public Bitmap getAlbumImage(final TrackModel track) throws ImageNotFoundException {
        if (null == track) {
            return null;
        }


        byte[] image;

        // FIXME use album artist as well.
        image = mDBManager.getAlbumImage(track.getTrackAlbumName());

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
    public void fetchArtistImage(final ArtistModel artist, final Context context) {
        if (!isDownloadAllowed(context)) {
            return;
        }

        if (mArtistProvider.equals(context.getString(R.string.pref_artwork_provider_lastfm_key))) {
            LastFMManager.getInstance(context).fetchArtistImage(artist, context, new Response.Listener<ArtistImageResponse>() {
                @Override
                public void onResponse(ArtistImageResponse response) {
                    new InsertArtistImageTask(context).execute(response);
                }
            }, this);
        } else if (mArtistProvider.equals(context.getString(R.string.pref_artwork_provider_fanarttv_key))) {
            FanartTVManager.getInstance(context).fetchArtistImage(artist, context, new Response.Listener<ArtistImageResponse>() {
                @Override
                public void onResponse(ArtistImageResponse response) {
                    new InsertArtistImageTask(context).execute(response);
                }
            }, this);
        }
    }

    /**
     * Starts an asynchronous fetch for the image of the given album
     *
     * @param album Album to fetch an image for.
     */
    public void fetchAlbumImage(final AlbumModel album, final Context context) {
        if (!isDownloadAllowed(context)) {
            return;
        }

        if (mAlbumProvider.equals(context.getString(R.string.pref_artwork_provider_musicbrainz_key))) {
            MusicBrainzManager.getInstance(context).fetchAlbumImage(album, context, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask(context).execute(response);
                }
            }, this);
        } else if (mAlbumProvider.equals(context.getString(R.string.pref_artwork_provider_lastfm_key))) {
            LastFMManager.getInstance(context).fetchAlbumImage(album, context, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask(context).execute(response);
                }
            }, this);
        }
    }

    /**
     * Starts an asynchronous fetch for the image of the given album
     *
     * @param track Track to be used for image fetching
     */
    public void fetchAlbumImage(final TrackModel track, final Context context) {
        if (!isDownloadAllowed(context)) {
            return;
        }

        // Create a dummy album
        AlbumModel album = new AlbumModel(track.getTrackAlbumName(), null, track.getTrackArtistName(),
                track.getTrackAlbumKey(), MusicLibraryHelper.getAlbumIDFromKey(track.getTrackAlbumKey(), context));

        if (mAlbumProvider.equals(context.getString(R.string.pref_artwork_provider_musicbrainz_key))) {
            MusicBrainzManager.getInstance(context).fetchAlbumImage(album, context, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask(context).execute(response);
                }
            }, this);
        } else if (mAlbumProvider.equals(context.getString(R.string.pref_artwork_provider_lastfm_key))) {
            LastFMManager.getInstance(context).fetchAlbumImage(album, context, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask(context).execute(response);
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
     * Interface implementation to handle errors during fetching of album images
     *
     * @param album Album that resulted in a fetch error
     */
    @Override
    public void fetchJSONException(AlbumModel album, Context context, JSONException exception) {
        Log.e(TAG, "JSONException for album: " + album.getAlbumName() + "-" + album.getArtistName());
        AlbumImageResponse imageResponse = new AlbumImageResponse();
        imageResponse.album = album;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertAlbumImageTask(context).execute(imageResponse);
    }

    /**
     * Interface implementation to handle errors during fetching of album images
     *
     * @param album Album that resulted in a fetch error
     */
    @Override
    public void fetchVolleyError(AlbumModel album, Context context, VolleyError error) {
        Log.e(TAG, "VolleyError for album: " + album.getAlbumName() + "-" + album.getArtistName());

        if (error != null) {
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse != null && networkResponse.statusCode == 503) {
                mAlbumList.clear();
                cancelAllRequests(context);
                boolean isEmpty;
                synchronized (mArtistList) {
                    isEmpty = mArtistList.isEmpty();
                }
                if (isEmpty && mBulkProgressCallback != null) {
                    mBulkProgressCallback.finishedLoading();
                }
                return;
            }
        }

        AlbumImageResponse imageResponse = new AlbumImageResponse();
        imageResponse.album = album;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertAlbumImageTask(context).execute(imageResponse);
    }

    /**
     * Interface implementation to handle errors during fetching of artist images
     *
     * @param artist Artist that resulted in a fetch error
     */
    @Override
    public void fetchJSONException(ArtistModel artist, Context context, JSONException exception) {
        Log.e(TAG, "JSONException fetching: " + artist.getArtistName());
        ArtistImageResponse imageResponse = new ArtistImageResponse();
        imageResponse.artist = artist;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertArtistImageTask(context).execute(imageResponse);
    }

    /**
     * Interface implementation to handle errors during fetching of artist images
     *
     * @param artist Artist that resulted in a fetch error
     */
    @Override
    public void fetchVolleyError(ArtistModel artist, Context context, VolleyError error) {
        Log.e(TAG, "VolleyError fetching: " + artist.getArtistName());

        if (error != null) {
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse != null && networkResponse.statusCode == 503) {
                mArtistList.clear();
                cancelAllRequests(context);
                boolean isEmpty;
                synchronized (mAlbumList) {
                    isEmpty = mAlbumList.isEmpty();
                }
                if (isEmpty && mBulkProgressCallback != null) {
                    mBulkProgressCallback.finishedLoading();
                }
                return;
            }
        }

        ArtistImageResponse imageResponse = new ArtistImageResponse();
        imageResponse.artist = artist;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertArtistImageTask(context).execute(imageResponse);
    }

    /**
     * Checks the current network state if an artwork download is allowed.
     *
     * @param context The current context to resolve the networkinfo
     * @return true if a download is allowed else false
     */
    private boolean isDownloadAllowed(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo == null) {
            return false;
        } else {
            boolean isWifi = cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI || cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_ETHERNET;

            return !(mWifiOnly && !isWifi);
        }
    }

    /**
     * AsyncTask to insert the images to the SQLdatabase. This is necessary as the Volley response
     * is handled in the UI thread.
     */
    private class InsertArtistImageTask extends AsyncTask<ArtistImageResponse, Object, ArtistModel> {

        private final Context mContext;

        public InsertArtistImageTask(Context context) {
            mContext = context;
        }

        /**
         * Inserts the image to the database.
         *
         * @param params Pair of byte[] (containing the image itself) and ArtistModel for which the image is for
         * @return the artist model that was inserted to the database.
         */
        @Override
        protected ArtistModel doInBackground(ArtistImageResponse... params) {
            ArtistImageResponse response = params[0];

            if (mCurrentBulkArtist == response.artist) {
                fetchNextBulkArtist(mContext);
            }
            if (response.image == null) {
                mDBManager.insertArtistImage(response.artist, null, mContext);
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
                Bitmap.createScaledBitmap(bm, MAXIMUM_IMAGE_SIZE, MAXIMUM_IMAGE_SIZE, true).compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);
                mDBManager.insertArtistImage(response.artist, byteStream.toByteArray(), mContext);
            } else {
                mDBManager.insertArtistImage(response.artist, response.image, mContext);
            }

            broadcastNewArtistImageInfo(response, mContext);

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

        private final Context mContext;

        public InsertAlbumImageTask(Context context) {
            mContext = context;
        }

        /**
         * Inserts the image to the database.
         *
         * @param params Pair of byte[] (containing the image itself) and AlbumModel for which the image is for
         * @return the album model that was inserted to the database.
         */
        @Override
        protected AlbumModel doInBackground(AlbumImageResponse... params) {
            AlbumImageResponse response = params[0];

            if (mCurrentBulkAlbum == response.album) {
                fetchNextBulkAlbum(mContext);
            }
            if (response.image == null) {
                mDBManager.insertAlbumImage(response.album, null);
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
                Bitmap.createScaledBitmap(bm, MAXIMUM_IMAGE_SIZE, MAXIMUM_IMAGE_SIZE, true).compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);
                mDBManager.insertAlbumImage(response.album, byteStream.toByteArray());
            } else {
                mDBManager.insertAlbumImage(response.album, response.image);
            }

            broadcastNewAlbumImageInfo(response, mContext);

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

    private void broadcastNewArtistImageInfo(ArtistImageResponse artistImage, Context context) {
        Intent newImageIntent = new Intent(ACTION_NEW_ARTWORK_READY);

        newImageIntent.putExtra(INTENT_EXTRA_KEY_ARTIST_ID, artistImage.artist.getArtistID());
        newImageIntent.putExtra(INTENT_EXTRA_KEY_ARTIST_NAME, artistImage.artist.getArtistName());

        context.sendBroadcast(newImageIntent);
    }

    private void broadcastNewAlbumImageInfo(AlbumImageResponse albumImage, Context context) {
        Intent newImageIntent = new Intent(ACTION_NEW_ARTWORK_READY);

        newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_ID, albumImage.album.getAlbumID());
        newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_KEY, albumImage.album.getAlbumKey());
        newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_NAME, albumImage.album.getAlbumName());

        context.sendBroadcast(newImageIntent);
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
            if (!isDownloadAllowed(context)) {
                // Cancel all downloads
                Log.v(TAG, "Cancel all downloads because of connection change");
                cancelAllRequests(context);
            }
        }
    }

    /**
     * This will cancel the last used album/artist image providers. To make this useful on connection change
     * it is important to cancel all requests when changing the provider in settings.
     */
    public void cancelAllRequests(Context context) {
        LimitingRequestQueue.getInstance(context).cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    public void bulkLoadImages(BulkLoadingProgressCallback progressCallback, Context context) {
        if (progressCallback == null) {
            return;
        }
        mBulkProgressCallback = progressCallback;
        mArtistList.clear();
        mAlbumList.clear();
        Log.v(TAG, "Start bulk loading");
        if (!mAlbumProvider.equals(context.getString((R.string.pref_artwork_provider_none_key)))) {
            List<AlbumModel> albums = MusicLibraryHelper.getAllAlbums(context);
            new ParseAlbumListTask(context).execute(albums);
        }

        if (!mArtistProvider.equals(context.getString((R.string.pref_artwork_provider_none_key)))) {
            List<ArtistModel> artists = MusicLibraryHelper.getAllArtists(false, context);
            new ParseArtistListTask(context).execute(artists);
        }
    }

    private class ParseAlbumListTask extends AsyncTask<List<AlbumModel>, Object, Object> {

        private final Context mContext;

        public ParseAlbumListTask(Context context) {
            mContext = context;
        }

        @SafeVarargs
        @Override
        protected final Object doInBackground(List<AlbumModel>... lists) {
            List<AlbumModel> albumList = lists[0];

            mBulkProgressCallback.startAlbumLoading(albumList.size());

            Log.v(TAG, "Received " + albumList.size() + " albums for bulk loading");
            synchronized (mAlbumList) {
                mAlbumList.clear();
                mAlbumList.addAll(albumList);
            }

            fetchNextBulkAlbum(mContext);

            return null;
        }
    }

    private class ParseArtistListTask extends AsyncTask<List<ArtistModel>, Object, Object> {

        private final Context mContext;

        public ParseArtistListTask(Context context) {
            mContext = context;
        }

        @SafeVarargs
        @Override
        protected final Object doInBackground(List<ArtistModel>... lists) {
            List<ArtistModel> artistList = lists[0];

            Log.v(TAG, "Received " + artistList.size() + " artists for bulk loading");
            mBulkProgressCallback.startArtistLoading(artistList.size());
            synchronized (mArtistList) {
                mArtistList.clear();
                mArtistList.addAll(artistList);
            }

            fetchNextBulkArtist(mContext);

            return null;
        }
    }

    private void fetchNextBulkAlbum(Context context) {
        boolean isEmpty;
        synchronized (mAlbumList) {
            isEmpty = mAlbumList.isEmpty();
        }

        while (!isEmpty) {
            AlbumModel album;
            synchronized (mAlbumList) {
                album = mAlbumList.remove(0);
                Log.v(TAG, "Bulk load next album: " + album.getAlbumName() + ":" + album.getArtistName() + " remaining: " + mAlbumList.size());
                mBulkProgressCallback.albumsRemaining(mAlbumList.size());
            }
            mCurrentBulkAlbum = album;

            if (album.getAlbumArtURL() == null || album.getAlbumArtURL().isEmpty()) {
                // Check if image already there
                try {
                    if (album.getAlbumID() != -1) {
                        mDBManager.getAlbumImage(album.getAlbumID());
                    } else {
                        mDBManager.getAlbumImage(album.getAlbumName());
                    }
                    // If this does not throw the exception it already has an image.
                } catch (ImageNotFoundException e) {
                    fetchAlbumImage(album, context);
                    return;
                }
            }

            synchronized (mAlbumList) {
                isEmpty = mAlbumList.isEmpty();
            }
        }

        if (mArtistList.isEmpty()) {
            mBulkProgressCallback.finishedLoading();
        }
    }

    private void fetchNextBulkArtist(Context context) {
        boolean isEmpty;
        synchronized (mArtistList) {
            isEmpty = mArtistList.isEmpty();
        }

        while (!isEmpty) {
            ArtistModel artist;
            synchronized (mArtistList) {
                artist = mArtistList.remove(0);
                Log.v(TAG, "Bulk load next artist: " + artist.getArtistName() + " remaining: " + mArtistList.size());
                mBulkProgressCallback.artistsRemaining(mArtistList.size());
            }
            mCurrentBulkArtist = artist;

            // Check if image already there
            try {
                if (artist.getArtistID() != -1) {
                    mDBManager.getArtistImage(artist.getArtistID());
                } else {
                    mDBManager.getArtistImage(artist.getArtistName());
                }
                // If this does not throw the exception it already has an image.
            } catch (ImageNotFoundException e) {
                fetchArtistImage(artist, context);
                return;
            }

            synchronized (mArtistList) {
                isEmpty = mArtistList.isEmpty();
            }
        }

        if (mAlbumList.isEmpty()) {
            mBulkProgressCallback.finishedLoading();
        }
    }


    public interface BulkLoadingProgressCallback {
        void startAlbumLoading(int albumCount);

        void startArtistLoading(int artistCount);

        void albumsRemaining(int remainingAlbums);

        void artistsRemaining(int remainingArtists);

        void finishedLoading();
    }
}
