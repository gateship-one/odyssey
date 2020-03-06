/*
 * Copyright (C) 2020 Team Gateship-One
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

package org.gateshipone.odyssey.artwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artwork.network.ArtworkRequestModel;
import org.gateshipone.odyssey.artwork.network.ImageResponse;
import org.gateshipone.odyssey.artwork.network.InsertImageTask;
import org.gateshipone.odyssey.artwork.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artwork.network.artprovider.ArtProvider;
import org.gateshipone.odyssey.artwork.network.artprovider.FanartTVProvider;
import org.gateshipone.odyssey.artwork.network.artprovider.LastFMProvider;
import org.gateshipone.odyssey.artwork.network.artprovider.MusicBrainzProvider;
import org.gateshipone.odyssey.artwork.storage.ArtworkDatabaseManager;
import org.gateshipone.odyssey.artwork.storage.ImageNotFoundException;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.models.TrackModel;
import org.gateshipone.odyssey.utils.BitmapUtils;
import org.gateshipone.odyssey.utils.MusicLibraryHelper;
import org.gateshipone.odyssey.utils.NetworkUtils;
import org.gateshipone.odyssey.utils.PermissionHelper;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import androidx.preference.PreferenceManager;

public class ArtworkManager implements ArtProvider.ArtFetchError, InsertImageTask.ImageSavedCallback {

    private static final String TAG = ArtworkManager.class.getSimpleName();

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

    /**
     * Broadcast constants
     */
    public static final String ACTION_NEW_ARTWORK_READY = "org.gateshipone.odyssey.action_new_artwork_ready";

    public static final String INTENT_EXTRA_KEY_ALBUM_KEY = "org.gateshipone.odyssey.extra.album_key";

    private static final String INTENT_EXTRA_KEY_ALBUM_ID = "org.gateshipone.odyssey.extra.album_id";

    private static final String INTENT_EXTRA_KEY_ALBUM_NAME = "org.gateshipone.odyssey.extra.album_name";

    private static final String INTENT_EXTRA_KEY_ARTIST_ID = "org.gateshipone.odyssey.extra.artist_id";

    private static final String INTENT_EXTRA_KEY_ARTIST_NAME = "org.gateshipone.odyssey.extra.artist_name";

    /**
     * The list of supported artwork filenames. This will be used to check if a local cover exists.
     */
    private static final List<String> ALLOWED_ARTWORK_FILENAMES = new ArrayList<>(Arrays.asList("cover.jpg", "cover.jpeg", "cover.png", "folder.jpg", "folder.jpeg", "folder.png", "artwork.jpg", "artwork.jpeg", "artwork.png"));

    /**
     * Private static singleton instance that can be used by other classes via the
     * getInstance method.
     */
    private static ArtworkManager mInstance;

    /**
     * Settings string which artist download provider to use
     */
    private String mArtistProvider;

    /**
     * Settings string which album download provider to use
     */
    private String mAlbumProvider;

    /**
     * Settings value if artwork download is only allowed via wifi/wired connection.
     */
    private boolean mWifiOnly;

    /**
     * Flag if local covers should be used instead of the android media database.
     */
    private boolean mUseLocalImages;

    /**
     * Manager for the SQLite database handling
     */
    private ArtworkDatabaseManager mDBManager;

    /**
     * List of observers that needs updating if a new ArtistImage is downloaded.
     */
    private final ArrayList<ArtworkManager.onNewArtistImageListener> mArtistListeners;

    /**
     * List of observers that needs updating if a new AlbumImage is downloaded.
     */
    private final ArrayList<ArtworkManager.onNewAlbumImageListener> mAlbumListeners;

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
        mUseLocalImages = sharedPref.getBoolean(context.getString(R.string.pref_artwork_use_local_images_key), context.getResources().getBoolean(R.bool.pref_artwork_use_local_images_default));
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

    public void initialize(String artistProvider, String albumProvider, boolean wifiOnly, boolean useLocalImages) {
        mArtistProvider = artistProvider;
        mAlbumProvider = albumProvider;
        mWifiOnly = wifiOnly;
        mUseLocalImages = useLocalImages;
    }

    /**
     * Removes the image for the album and tries to reload it from the internet
     *
     * @param album {@link AlbumModel} to reload the image for
     */
    public void resetImage(final AlbumModel album, final Context context) {
        if (null == album) {
            return;
        }

        // Clear the old image
        mDBManager.removeAlbumImage(context, album);

        // Clear the old image from the cache
        BitmapCache.getInstance().removeAlbumBitmap(album);

        // Reload the image from the internet
        fetchImage(album, context);
    }


    /**
     * Removes the image for the artist and tries to reload it from the internet
     *
     * @param artist {@link ArtistModel} to reload the image for
     */
    public void resetImage(final ArtistModel artist, final Context context) {
        if (null == artist) {
            return;
        }

        // Clear the old image
        mDBManager.removeArtistImage(context, artist);

        // Clear the old image from the cache
        BitmapCache.getInstance().removeArtistImage(artist);

        // Reload the image from the internet
        fetchImage(artist, context);
    }

    public Bitmap getImage(final ArtistModel artist, int width, int height, boolean skipCache, final Context context) throws ImageNotFoundException {
        if (null == artist) {
            return null;
        }

        if (!skipCache) {
            // Try cache first
            Bitmap cacheImage = BitmapCache.getInstance().requestArtistImage(artist);
            if (cacheImage != null && width <= cacheImage.getWidth() && height <= cacheImage.getWidth()) {
                return cacheImage;
            }
        }

        final String image = mDBManager.getArtistImage(context, artist);

        // Checks if the database has an image for the requested artist
        if (null != image) {
            // Create a bitmap from the data blob in the database
            Bitmap bm = BitmapUtils.decodeSampledBitmapFromFile(image, width, height);
            BitmapCache.getInstance().putArtistImage(artist, bm);
            return bm;
        }
        return null;
    }

    public Bitmap getImage(final AlbumModel album, int width, int height, boolean skipCache, final Context context) throws ImageNotFoundException {
        if (null == album) {
            return null;
        }

        if (!skipCache) {
            // Try cache first
            Bitmap cacheBitmap = BitmapCache.getInstance().requestAlbumBitmap(album);
            if (cacheBitmap != null && width <= cacheBitmap.getWidth() && height <= cacheBitmap.getWidth()) {
                return cacheBitmap;
            }
        }

        // Check local artwork database
        String albumURL = album.getAlbumArtURL();
        if (!mUseLocalImages && albumURL != null && !albumURL.isEmpty()) {
            // Local album art found (android database)
            Bitmap bm = BitmapUtils.decodeSampledBitmapFromFile(albumURL, width, height);
            BitmapCache.getInstance().putAlbumBitmap(album, bm);
            return bm;
        }

        final String image = mDBManager.getAlbumImage(context, album);

        // Checks if the database has an image for the requested album
        if (null != image) {
            // Create a bitmap from the data blob in the database
            Bitmap bm = BitmapUtils.decodeSampledBitmapFromFile(image, width, height);
            BitmapCache.getInstance().putAlbumBitmap(album, bm);
            return bm;
        }
        return null;
    }

    public Bitmap getImage(final TrackModel track, int width, int height, boolean skipCache, final Context context) throws ImageNotFoundException {
        if (null == track) {
            return null;
        }

        // get album information for the current track
        AlbumModel album = MusicLibraryHelper.createAlbumModelFromKey(track.getTrackAlbumKey(), context);
        if (album == null) {
            return null;
        }

        return getImage(album, width, height, skipCache, context);
    }

    /**
     * Starts an asynchronous fetch for the image of the given artist.
     *
     * @param artistModel        Artist to fetch an image for.
     * @param context            The application context.
     * @param imageSavedCallback Callback if an image was saved.
     * @param errorCallback      Callback if an error occured.
     */
    void fetchImage(final ArtistModel artistModel, final Context context,
                    final InsertImageTask.ImageSavedCallback imageSavedCallback,
                    final ArtProvider.ArtFetchError errorCallback) {
        if (!NetworkUtils.isDownloadAllowed(context, mWifiOnly)) {
            return;
        }

        final ArtworkRequestModel requestModel = new ArtworkRequestModel(artistModel);

        if (mArtistProvider.equals(context.getString(R.string.pref_artwork_provider_fanarttv_key))) {
            FanartTVProvider.getInstance(context).fetchImage(requestModel, context,
                    response -> new InsertImageTask(context, imageSavedCallback).execute(response),
                    errorCallback);
        }
    }

    /**
     * Starts an asynchronous fetch for the image of the given artist.
     * This method will use internal callbacks.
     *
     * @param artistModel Artist to fetch an image for.
     * @param context     The application context.
     */
    public void fetchImage(final ArtistModel artistModel, final Context context) {
        fetchImage(artistModel, context, this, this);
    }

    /**
     * Starts an asynchronous fetch for the image of the given album.
     *
     * @param albumModel         Album to fetch an image for.
     * @param context            The application context.
     * @param imageSavedCallback Callback if an image was saved.
     * @param errorCallback      Callback if an error occured.
     */
    void fetchImage(final AlbumModel albumModel, final Context context,
                    final InsertImageTask.ImageSavedCallback imageSavedCallback,
                    final ArtProvider.ArtFetchError errorCallback) {
        if (mUseLocalImages) {
            final Set<String> storageLocations = MusicLibraryHelper.getTrackStorageLocationsForAlbum(albumModel.getAlbumKey(), context);

            for (final String location : storageLocations) {
                final List<File> artworkFiles = PermissionHelper.getFilesForDirectory(context, location, (dir, name) -> ALLOWED_ARTWORK_FILENAMES.contains(name.toLowerCase()));

                if (!artworkFiles.isEmpty()) {
                    // use the first valid cover file
                    final File coverFile = artworkFiles.get(0);

                    final ArtworkRequestModel requestModel = new ArtworkRequestModel(albumModel);

                    ImageResponse response = new ImageResponse();
                    response.model = requestModel;
                    response.image = null;
                    response.url = null;
                    response.localArtworkPath = coverFile.getAbsolutePath();

                    new InsertImageTask(context, imageSavedCallback).execute(response);

                    return;
                }
            }
        }

        if (!NetworkUtils.isDownloadAllowed(context, mWifiOnly)) {
            return;
        }

        ArtworkRequestModel requestModel = new ArtworkRequestModel(albumModel);

        if (mAlbumProvider.equals(context.getString(R.string.pref_artwork_provider_musicbrainz_key))) {
            MusicBrainzProvider.getInstance(context).fetchImage(requestModel, context,
                    response -> new InsertImageTask(context, imageSavedCallback).execute(response),
                    errorCallback);
        } else if (mAlbumProvider.equals(context.getString(R.string.pref_artwork_provider_lastfm_key))) {
            LastFMProvider.getInstance(context).fetchImage(requestModel, context,
                    response -> new InsertImageTask(context, imageSavedCallback).execute(response),
                    errorCallback);
        }
    }

    /**
     * Starts an asynchronous fetch for the image of the given album.
     * This method will use internal callbacks.
     *
     * @param albumModel Album to fetch an image for.
     * @param context    The application context.
     */
    public void fetchImage(final AlbumModel albumModel, final Context context) {
        fetchImage(albumModel, context, this, this);
    }

    /**
     * Starts an asynchronous fetch for the image of the given album
     *
     * @param trackModel Track to be used for image fetching
     */
    public void fetchImage(final TrackModel trackModel, final Context context) {
        // Create a dummy album
        AlbumModel album = new AlbumModel(trackModel.getTrackAlbumName(), null,
                trackModel.getTrackArtistName(), trackModel.getTrackAlbumKey(),
                MusicLibraryHelper.getAlbumIDFromKey(trackModel.getTrackAlbumKey(), context));

        fetchImage(album, context);
    }

    /**
     * Registers a listener that gets notified when a new artist image was added to the dataset.
     *
     * @param listener Listener to register
     */
    public void registerOnNewArtistImageListener(ArtworkManager.onNewArtistImageListener listener) {
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
    public void unregisterOnNewArtistImageListener(ArtworkManager.onNewArtistImageListener listener) {
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
    public void registerOnNewAlbumImageListener(ArtworkManager.onNewAlbumImageListener listener) {
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
    public void unregisterOnNewAlbumImageListener(ArtworkManager.onNewAlbumImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mAlbumListeners.remove(listener);
            }
        }
    }

    @Override
    public void onImageSaved(final ArtworkRequestModel artworkRequestModel, final Context applicationContext) {
        broadcastNewArtwokInfo(artworkRequestModel, applicationContext);

        switch (artworkRequestModel.getType()) {
            case ALBUM:
                synchronized (mAlbumListeners) {
                    for (onNewAlbumImageListener albumListener : mAlbumListeners) {
                        albumListener.newAlbumImage(((AlbumModel) artworkRequestModel.getGenericModel()));
                    }
                }
                break;
            case ARTIST:
                synchronized (mArtistListeners) {
                    for (onNewArtistImageListener artistListener : mArtistListeners) {
                        artistListener.newArtistImage((ArtistModel) artworkRequestModel.getGenericModel());
                    }
                }
                break;
        }
    }

    @Override
    public void fetchJSONException(ArtworkRequestModel model, Context context, JSONException exception) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "JSONException fetching: " + model.getLoggingString());
        }

        ImageResponse imageResponse = new ImageResponse();
        imageResponse.model = model;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertImageTask(context, this).execute(imageResponse);
    }

    @Override
    public void fetchVolleyError(ArtworkRequestModel model, Context context, VolleyError error) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "VolleyError for request: " + model.getLoggingString());
        }

        if (error != null) {
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse != null && networkResponse.statusCode == 503) {
                cancelAllRequests(context);
                return;
            }
        }

        ImageResponse imageResponse = new ImageResponse();
        imageResponse.model = model;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertImageTask(context, this).execute(imageResponse);
    }

    public void fetchError(ArtworkRequestModel model, Context context) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "Error fetching: " + model.getLoggingString());
        }

        ImageResponse imageResponse = new ImageResponse();
        imageResponse.model = model;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertImageTask(context, this).execute(imageResponse);
    }

    /**
     * This will cancel the last used album/artist image providers. To make this useful on connection change
     * it is important to cancel all requests when changing the provider in settings.
     */
    public void cancelAllRequests(Context context) {
        LimitingRequestQueue.getInstance(context).cancelAll(request -> true);
    }

    /**
     * Used to broadcast information about new available artwork to {@link BroadcastReceiver} like
     * the {@link org.gateshipone.odyssey.widget.OdysseyWidgetProvider} to reload its artwork.
     *
     * @param model   The model that an image was inserted for.
     * @param context Context used for broadcasting
     */
    private void broadcastNewArtwokInfo(ArtworkRequestModel model, Context context) {
        Intent newImageIntent = new Intent(ACTION_NEW_ARTWORK_READY);

        switch (model.getType()) {
            case ALBUM:
                AlbumModel albumModel = (AlbumModel) model.getGenericModel();
                newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_ID, albumModel.getAlbumID());
                newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_KEY, albumModel.getAlbumKey());
                newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_NAME, albumModel.getAlbumName());
                break;
            case ARTIST:
                ArtistModel artistModel = (ArtistModel) model.getGenericModel();
                newImageIntent.putExtra(INTENT_EXTRA_KEY_ARTIST_ID, artistModel.getArtistID());
                newImageIntent.putExtra(INTENT_EXTRA_KEY_ARTIST_NAME, artistModel.getArtistName());
                break;
        }

        context.sendBroadcast(newImageIntent);
    }

    private class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!NetworkUtils.isDownloadAllowed(context, mWifiOnly)) {
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Cancel all downloads because of connection change");
                }

                // Cancel all downloads
                cancelAllRequests(context);
            }
        }
    }
}
