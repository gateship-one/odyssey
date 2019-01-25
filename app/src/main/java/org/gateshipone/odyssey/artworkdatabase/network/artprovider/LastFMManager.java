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

package org.gateshipone.odyssey.artworkdatabase.network.artprovider;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.gateshipone.odyssey.artworkdatabase.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artworkdatabase.network.requests.AlbumImageByteRequest;
import org.gateshipone.odyssey.artworkdatabase.network.requests.ArtistImageByteRequest;
import org.gateshipone.odyssey.artworkdatabase.network.requests.OdysseyJsonObjectRequest;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LastFMManager implements ArtistImageProvider, AlbumImageProvider {
    private static final String TAG = LastFMManager.class.getSimpleName();

    /**
     * Last.fm API url used for requests
     */
    private static final String LAST_FM_API_URL = "https://ws.audioscrobbler.com/2.0/?method=";

    /**
     * API-Key for used for last.fm
     * THIS KEY IS ONLY INTENDED FOR THE USE BY GATESHIP-ONE APPLICATIONS. PLEASE RESPECT THIS.
     */
    private static final String API_KEY = "8de46d96e49e78234f206fd9f21712de";

    /**
     * Constant to request JSON formatted responses
     */
    private static final String LAST_FM_FORMAT_JSON = "&format=json";

    /**
     * Default image download size. Should be around 500px * 500px
     */
    private static final String LAST_FM_REQUESTED_IMAGE_SIZE = "extralarge";
    /**
     * Private {@link RequestQueue} to use for internet requests.
     */
    private RequestQueue mRequestQueue;

    /**
     * Singleton instance
     */
    private static LastFMManager mInstance;

    private LastFMManager(Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context);
    }

    public static synchronized LastFMManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LastFMManager(context);
        }
        return mInstance;
    }

    /**
     * Fetch an image for an given {@link ArtistModel}. Make sure to provide response and error listener.
     * @param artist Artist to try to get an image for.
     * @param listener ResponseListener that reacts on successful retrieval of an image.
     * @param errorListener Error listener that is called when an error occurs.
     */
    public void fetchArtistImage(final ArtistModel artist, final Context context, final Response.Listener<ArtistImageResponse> listener, final ArtistFetchError errorListener) {
        String artistURLName = Uri.encode(artist.getArtistName().replaceAll("/", " "));

        getArtistImageURL(artistURLName, response -> {
            try {
                JSONObject artistObj = response.getJSONObject("artist");
                // FIXME optionally get mbid here without aborting the image fetch
                JSONArray images = artistObj.getJSONArray("image");
                Log.v(TAG, "Found: " + images.length() + "images");
                for (int i = 0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    if (image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                        String url = image.getString("#text");
                        if (!url.isEmpty()) {
                            getArtistImage(image.getString("#text"), artist, listener, error -> errorListener.fetchVolleyError(artist, context, error));
                        } else {
                            errorListener.fetchVolleyError(artist, context, null);
                        }
                    }
                }
            } catch (JSONException e) {
                errorListener.fetchJSONException(artist, context, e);
            }
        }, error -> errorListener.fetchVolleyError(artist, context, error));

    }

    @Override
    public void cancelAll() {
        mRequestQueue.cancelAll(request -> true);
    }


    /**
     * Fetches the image URL for the raw image blob.
     * @param artistName Artist name to look for an image
     * @param listener Callback listener to handle the response
     * @param errorListener Callback to handle a fetch error
     */
    private void getArtistImageURL(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {


        String url = LAST_FM_API_URL + "artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG, url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Raw download for an image
     * @param url Final image URL to download
     * @param artist Artist associated with the image to download
     * @param listener Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getArtistImage(String url, ArtistModel artist, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(LastFMManager.class.getSimpleName(), url);

        Request<ArtistImageResponse> byteResponse = new ArtistImageByteRequest(url, artist, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }

    /**
     * Public interface to get an image for an album.
     * @param album Album to check for an image
     * @param listener Callback to handle the fetched image
     * @param errorListener Callback to handle errors
     */
    @Override
    public void fetchAlbumImage(final AlbumModel album, final Context context, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {
        getAlbumImageURL(album, response -> {
            try {
                JSONObject albumObj = response.getJSONObject("album");
                JSONArray images = albumObj.getJSONArray("image");
                // FIXME optionally get mbid here without aborting the image fetch
                Log.v(TAG, "Found: " + images.length() + "images");
                for (int i = 0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    if (image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                        String url = image.getString("#text");
                        if (!url.isEmpty()) {
                            getAlbumImage(image.getString("#text"), album, listener, error -> errorListener.fetchVolleyError(album, context, error));
                        } else {
                            errorListener.fetchVolleyError(album, context, null);
                        }
                    }
                }
            } catch (JSONException e) {
                errorListener.fetchJSONException(album, context, e);
            }
        }, error -> errorListener.fetchVolleyError(album, context, error));
    }

    /**
     * Fetches the image URL for the raw image blob.
     * @param album Album to look for an image
     * @param listener Callback listener to handle the response
     * @param errorListener Callback to handle a fetch error
     */
    private void getAlbumImageURL(AlbumModel album, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String albumName = Uri.encode(album.getAlbumName());
        String artistName = Uri.encode(album.getArtistName());

        String url = LAST_FM_API_URL + "album.getinfo&album=" + albumName + "&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG, url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Raw download for an image
     * @param url Final image URL to download
     * @param album Album associated with the image to download
     * @param listener Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getAlbumImage(String url, AlbumModel album, Response.Listener<AlbumImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(LastFMManager.class.getSimpleName(), "Get album byte image:"  + url);

        Request<AlbumImageResponse> byteResponse = new AlbumImageByteRequest(url, album, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }
}
