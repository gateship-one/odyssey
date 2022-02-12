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

package org.gateshipone.odyssey.artwork.network.artprovider;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.artwork.network.ArtworkRequestModel;
import org.gateshipone.odyssey.artwork.network.ImageResponse;
import org.gateshipone.odyssey.artwork.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artwork.network.requests.OdysseyByteRequest;
import org.gateshipone.odyssey.artwork.network.requests.OdysseyJsonObjectRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LastFMProvider extends ArtProvider {

    private static final String TAG = LastFMProvider.class.getSimpleName();

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
    private final RequestQueue mRequestQueue;

    /**
     * Singleton instance
     */
    private static LastFMProvider mInstance;

    private LastFMProvider(final Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context.getApplicationContext());
    }

    public static synchronized LastFMProvider getInstance(final Context context) {
        if (mInstance == null) {
            mInstance = new LastFMProvider(context);
        }
        return mInstance;
    }

    @Override
    public void fetchImage(final ArtworkRequestModel model, final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        switch (model.getType()) {
            case ALBUM:
                getAlbumImageURL(model,
                        response -> parseJSONResponse(model, response, listener, errorListener),
                        error -> errorListener.fetchVolleyError(model, error));
                break;
            case ARTIST:
                // not used any more for this provider (api changed no valid artist images any more, only stars)
                break;
        }
    }

    /**
     * Fetches the image URL for the raw image blob.
     *
     * @param model         Album to look for an image
     * @param listener      Callback listener to handle the response
     * @param errorListener Callback to handle a fetch error
     */
    private void getAlbumImageURL(final ArtworkRequestModel model, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {
        String albumName = model.getEncodedAlbumName();
        String artistName = model.getEncodedArtistName();

        if (albumName.isEmpty() || artistName.isEmpty()) {
            errorListener.onErrorResponse(new VolleyError("required arguments are empty"));
        } else {
            String url = LAST_FM_API_URL + "album.getinfo&album=" + albumName + "&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;

            if (BuildConfig.DEBUG) {
                Log.v(TAG, url);
            }

            OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(url, null, listener, errorListener);

            mRequestQueue.add(jsonObjectRequest);
        }
    }

    /**
     * Method to parse the album info json response.
     * The response will be used to get an image for the requested album.
     *
     * @param model         The model representing the album for which an image was requested.
     * @param response      The album info response as a {@link JSONObject}.
     * @param listener      Callback if an image could be loaded successfully.
     * @param errorListener Callback if an error occured.
     */
    private void parseJSONResponse(final ArtworkRequestModel model, final JSONObject response,
                                   final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        try {
            final JSONObject baseObj = response.getJSONObject("album");

            // verify response
            final String album = baseObj.getString("name");
            final String artist = baseObj.getString("artist");

            final boolean isMatching = compareAlbumResponse(model.getAlbumName(), model.getArtistName(), album, artist);

            if (isMatching) {
                final JSONArray images = baseObj.getJSONArray("image");

                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Found: " + images.length() + " images");
                }

                for (int i = 0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);
                    if (image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                        String url = image.getString("#text");
                        if (!url.isEmpty()) {
                            getByteImage(image.getString("#text"), model, listener, error -> errorListener.fetchVolleyError(model, error));
                        } else {
                            errorListener.fetchVolleyError(model, null);
                        }
                    }
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "Response ( " + album + "-" + artist + " )" + " doesn't match requested model: " +
                            "( " + model.getLoggingString() + " )");
                }

                errorListener.fetchVolleyError(model, null);
            }
        } catch (JSONException e) {
            errorListener.fetchJSONException(model, e);
        }
    }

    /**
     * Raw download for an image
     *
     * @param url           Final image URL to download
     * @param model         Album associated with the image to download
     * @param listener      Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getByteImage(final String url, final ArtworkRequestModel model,
                              final Response.Listener<ImageResponse> listener, final Response.ErrorListener errorListener) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Get byte image:" + url);
        }

        Request<ImageResponse> byteResponse = new OdysseyByteRequest(model, url, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }
}
