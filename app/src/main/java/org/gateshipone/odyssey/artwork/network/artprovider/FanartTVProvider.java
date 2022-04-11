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

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.artwork.network.ArtworkRequestModel;
import org.gateshipone.odyssey.artwork.network.ImageResponse;
import org.gateshipone.odyssey.artwork.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artwork.network.requests.OdysseyByteRequest;
import org.gateshipone.odyssey.artwork.network.requests.OdysseyJsonObjectRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FanartTVProvider extends ArtProvider {

    private static final String TAG = FanartTVProvider.class.getSimpleName();

    /**
     * API-URL for MusicBrainz database. Used to resolve artist names to MBIDs
     */
    private static final String MUSICBRAINZ_API_URL = "https://musicbrainz.org/ws/2";

    /**
     * API-URL for fanart.tv itself.
     */
    private static final String FANART_TV_API_URL = "https://webservice.fanart.tv/v3/music";

    /**
     * constant API url part to instruct MB to return json format
     */
    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    /**
     * Limit the number of results to one. Used for resolving artist names to MBIDs
     */
    private static final int MUSICBRAINZ_LIMIT_RESULT_COUNT = 1;

    /**
     * Constant URL format to limit results
     */
    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + MUSICBRAINZ_LIMIT_RESULT_COUNT;

    /**
     * API-Key for used for fanart.tv.
     * THIS KEY IS ONLY INTENDED FOR THE USE BY GATESHIP-ONE APPLICATIONS. PLEASE RESPECT THIS.
     */
    private static final String API_KEY = "c0cc5d1b6e807ce93e49d75e0e5d371b";

    /**
     * {@link RequestQueue} used to handle the requests of this class.
     */
    private final RequestQueue mRequestQueue;

    /**
     * Singleton instance
     */
    private static FanartTVProvider mInstance;

    private FanartTVProvider(final Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context.getApplicationContext());
    }

    public static synchronized FanartTVProvider getInstance(final Context context) {
        if (mInstance == null) {
            mInstance = new FanartTVProvider(context);
        }
        return mInstance;
    }

    @Override
    public void fetchImage(final ArtworkRequestModel model, final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        switch (model.getType()) {
            case ALBUM:
                // not used for this provider
                break;
            case ARTIST:
                getArtists(model.getLuceneEscapedEncodedArtistName(),
                        response -> parseMusicBrainzArtistsJSON(model, response, listener, errorListener),
                        error -> errorListener.fetchVolleyError(model, error));
                break;
        }
    }

    /**
     * Method to parse the artist info json response.
     * The response will be used to get an image for the requested artist (via getArtistImageURL and getArtistImage).
     *
     * @param model         The model representing the artist for which an image was requested.
     * @param response      The artist info response as a {@link JSONObject}.
     * @param listener      Callback if an image could be loaded successfully.
     * @param errorListener Callback if an error occured.
     */
    private void parseMusicBrainzArtistsJSON(final ArtworkRequestModel model, final JSONObject response,
                                             final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        JSONArray artists;
        try {
            artists = response.getJSONArray("artists");

            if (!artists.isNull(0)) {
                JSONObject artistObj = artists.getJSONObject(0);

                // verify response
                final String artist = artistObj.getString("name");

                final boolean isMatching = compareArtistResponse(model.getArtistName(), artist);

                if (isMatching) {
                    final String artistMBId = artistObj.getString("id");

                    getArtistImageURL(artistMBId, response1 -> {
                        JSONArray thumbImages;
                        try {
                            thumbImages = response1.getJSONArray("artistthumb");

                            JSONObject firstThumbImage = thumbImages.getJSONObject(0);
                            model.setMBId(artistMBId);
                            getArtistImage(firstThumbImage.getString("url"), model, listener, error -> errorListener.fetchVolleyError(model, error));

                        } catch (JSONException e) {
                            errorListener.fetchJSONException(model, e);
                        }
                    }, error -> errorListener.fetchVolleyError(model, error));
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "Response ( " + artist + " )" + " doesn't match requested model: " +
                                "( " + model.getLoggingString() + " )");
                    }

                    errorListener.fetchVolleyError(model, null);
                }
            } else {
                errorListener.fetchError(model);
            }
        } catch (JSONException e) {
            errorListener.fetchJSONException(model, e);
        }
    }

    /**
     * Gets a list of possible artists from Musicbrainz database.
     *
     * @param artistName    Name of the artist to search for
     * @param listener      Response listener to handle the artist list
     * @param errorListener Error listener
     */
    private void getArtists(final String artistName, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + artistName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;

        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Requesting release mbid for: " + url);
        }

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Retrieves all available information (Artist image url, fanart url, ...) for an artist with an MBID of fanart.tv
     *
     * @param artistMBId    Artists MBID to query
     * @param listener      Response listener to handle the artists information from fanart.tv
     * @param errorListener Error listener
     */
    private void getArtistImageURL(final String artistMBId, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {

        String url = FANART_TV_API_URL + "/" + artistMBId + "?api_key=" + API_KEY;

        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Requesting artist image url for: " + url);
        }

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Raw download for an image-
     *
     * @param url           Final image URL to download
     * @param model         Artist associated with the image to download
     * @param listener      Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getArtistImage(final String url, final ArtworkRequestModel model,
                                final Response.Listener<ImageResponse> listener, final Response.ErrorListener errorListener) {
        final String httpsUrl = url.replace("http://", "https://");

        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Request artist image for: " + httpsUrl);
        }

        Request<ImageResponse> byteResponse = new OdysseyByteRequest(model, httpsUrl, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }
}
