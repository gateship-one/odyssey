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

package org.gateshipone.odyssey.artwork.network.artprovider;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

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
    private RequestQueue mRequestQueue;

    /**
     * Singleton instance
     */
    private static LastFMProvider mInstance;

    private LastFMProvider(final Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context);
    }

    public static synchronized LastFMProvider getInstance(final Context context) {
        if (mInstance == null) {
            mInstance = new LastFMProvider(context);
        }
        return mInstance;
    }

    @Override
    public void fetchImage(final ArtworkRequestModel model, final Context context,
                           final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        switch (model.getType()) {
            case ALBUM:
                getAlbumImageURL(model,
                        response -> parseJSONResponse(model, context, response, listener, errorListener),
                        error -> errorListener.fetchVolleyError(model, context, error));
                break;
            case ARTIST:
                getArtistImageURL(model.getEncodedArtistName(),
                        response -> parseJSONResponse(model, context, response, listener, errorListener),
                        error -> errorListener.fetchVolleyError(model, context, error));
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
    private void getAlbumImageURL(final ArtworkRequestModel model,
                                  final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {
        String albumName = model.getEncodedAlbumName();
        String artistName = model.getEncodedArtistName();

        if (albumName.isEmpty() || artistName.isEmpty()) {
            errorListener.onErrorResponse(new VolleyError("required arguments are empty"));
        } else {
            String url = LAST_FM_API_URL + "album.getinfo&album=" + albumName + "&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
            Log.v(TAG, url);

            OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(url, null, listener, errorListener);

            mRequestQueue.add(jsonObjectRequest);
        }
    }

    /**
     * Fetches the image URL for the raw image blob.
     *
     * @param artistName    Artist name to look for an image
     * @param listener      Callback listener to handle the response
     * @param errorListener Callback to handle a fetch error
     */
    private void getArtistImageURL(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {


        String url = LAST_FM_API_URL + "artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG, url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * FIXME ADD COMMENT
     *
     * @param model
     * @param context
     * @param response
     * @param listener
     * @param errorListener
     */
    private void parseJSONResponse(final ArtworkRequestModel model, final Context context, final JSONObject response,
                                   final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        try {
            String baseObjKey = "";

            switch (model.getType()) {
                case ALBUM:
                    baseObjKey = "album";
                    break;
                case ARTIST:
                    baseObjKey = "artist";
                    break;
            }
            JSONObject baseObj = response.getJSONObject(baseObjKey);
            JSONArray images = baseObj.getJSONArray("image");
            Log.v(TAG, "Found: " + images.length() + " images");
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = images.getJSONObject(i);
                if (image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                    String url = image.getString("#text");
                    if (!url.isEmpty()) {
                        getByteImage(image.getString("#text"), model, listener, error -> errorListener.fetchVolleyError(model, context, error));
                    } else {
                        errorListener.fetchVolleyError(model, context, null);
                    }
                }
            }
        } catch (JSONException e) {
            errorListener.fetchJSONException(model, context, e);
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
        Log.v(LastFMProvider.class.getSimpleName(), "Get byte image:" + url);

        Request<ImageResponse> byteResponse = new OdysseyByteRequest(model, url, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }
}
