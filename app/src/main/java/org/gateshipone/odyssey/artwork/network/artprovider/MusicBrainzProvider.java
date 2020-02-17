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

public class MusicBrainzProvider extends ArtProvider {

    private static final String TAG = MusicBrainzProvider.class.getSimpleName();

    private static final String MUSICBRAINZ_API_URL = "https://musicbrainz.org/ws/2";

    private static final String COVERART_ARCHIVE_API_URL = "https://coverartarchive.org";

    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    private static final int MUSICBRAINZ_LIMIT_RESULT_COUNT = 10;

    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + MUSICBRAINZ_LIMIT_RESULT_COUNT;

    /**
     * {@link RequestQueue} used to handle the requests of this class.
     */
    private RequestQueue mRequestQueue;

    /**
     * Singleton instance
     */
    private static MusicBrainzProvider mInstance;

    private MusicBrainzProvider(final Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context);
    }

    public static synchronized MusicBrainzProvider getInstance(final Context context) {
        if (mInstance == null) {
            mInstance = new MusicBrainzProvider(context);
        }
        return mInstance;
    }

    @Override
    public void fetchImage(final ArtworkRequestModel model, final Context context,
                           final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        switch (model.getType()) {
            case ALBUM:
                getAlbumMBID(model,
                        response -> parseMusicBrainzReleaseJSON(model, 0, response, context, listener, errorListener),
                        error -> errorListener.fetchVolleyError(model, context, error));
                break;
            case ARTIST:
                // not used for this provider
                break;
        }
    }

    /**
     * Wrapper to get an MBID out of an {@link ArtworkRequestModel}.
     *
     * @param model         Album to get the MBID for
     * @param listener      Response listener
     * @param errorListener Error listener
     */
    private void getAlbumMBID(final ArtworkRequestModel model, final Response.Listener<JSONObject> listener, final Response.ErrorListener errorListener) {
        final String albumName = model.getLuceneEscapedEncodedAlbumName();
        final String artistName = model.getLuceneEscapedEncodedArtistName();

        String url;
        if (!artistName.isEmpty()) {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + "%20AND%20artist:" + artistName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        } else {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        }

        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Requesting release mbid for: " + url);
        }

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Parses the JSON response and searches the image URL
     *
     * @param model         Album to check for an image
     * @param releaseIndex  Index of the requested release to check for an image
     * @param response      Response to check use to search for an image
     * @param context       Context used for lookup
     * @param listener      Callback to handle the response
     * @param errorListener Callback to handle errors
     */
    private void parseMusicBrainzReleaseJSON(final ArtworkRequestModel model, final int releaseIndex, final JSONObject response, final Context context,
                                             final Response.Listener<ImageResponse> listener, final ArtFetchError errorListener) {
        if (releaseIndex >= MUSICBRAINZ_LIMIT_RESULT_COUNT) {
            errorListener.fetchVolleyError(model, context, null);
            return;
        }

        try {
            final JSONArray releases = response.getJSONArray("releases");
            if (releases.length() > releaseIndex) {
                final JSONObject baseObj = releases.getJSONObject(releaseIndex);

                // verify response
                final String album = baseObj.getString("title");
                final String artist = baseObj.getJSONArray("artist-credit").getJSONObject(0).getString("name");

                final boolean isMatching = compareAlbumResponse(model.getAlbumName(), model.getArtistName(), album, artist);

                if (isMatching) {
                    final String mbid = releases.getJSONObject(releaseIndex).getString("id");
                    model.setMBID(mbid);

                    final String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + mbid + "/front-500";

                    getAlbumImage(url, model, listener, error -> {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "No image found for: " + model.getAlbumName() + " with release index: " + releaseIndex);
                        }

                        if (releaseIndex + 1 < releases.length()) {
                            parseMusicBrainzReleaseJSON(model, releaseIndex + 1, response, context, listener, errorListener);
                        } else {
                            errorListener.fetchVolleyError(model, context, error);
                        }
                    });
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "Response ( " + album + "-" + artist + " )" + " doesn't match requested model: " +
                                "( " + model.getLoggingString() + " )");
                    }

                    if (releaseIndex + 1 < releases.length()) {
                        parseMusicBrainzReleaseJSON(model, releaseIndex + 1, response, context, listener, errorListener);
                    } else {
                        errorListener.fetchVolleyError(model, context, null);
                    }
                }
            } else {
                errorListener.fetchVolleyError(model, context, null);
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
    private void getAlbumImage(final String url, final ArtworkRequestModel model,
                               final Response.Listener<ImageResponse> listener,
                               final Response.ErrorListener errorListener) {
        Request<ImageResponse> byteResponse = new OdysseyByteRequest(model, url, listener, errorListener);

        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Get image: " + url);
        }

        mRequestQueue.add(byteResponse);
    }
}
