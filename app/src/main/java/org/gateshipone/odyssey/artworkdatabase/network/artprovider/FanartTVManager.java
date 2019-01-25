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
import org.gateshipone.odyssey.artworkdatabase.network.requests.ArtistImageByteRequest;
import org.gateshipone.odyssey.artworkdatabase.network.requests.OdysseyJsonObjectRequest;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Artwork downloading class for http://fanart.tv. This class provides an interface
 * to download artist images and artist fanart images.
 */
public class FanartTVManager implements ArtistImageProvider {
    private static final String TAG = FanartTVManager.class.getSimpleName();

    /**
     * API-URL for MusicBrainz database. Used to resolve artist names to MBIDs
     */
    private static final String MUSICBRAINZ_API_URL = "https://musicbrainz.org/ws/2";

    /**
     * API-URL for fanart.tv itself.
     */
    private static final String FANART_TV_API_URL = "https://webservice.fanart.tv/v3/music";

    /**
     * {@link RequestQueue} used to handle the requests of this class.
     */
    private RequestQueue mRequestQueue;

    /**
     * Singleton instance
     */
    private static FanartTVManager mInstance;

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
    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + String.valueOf(MUSICBRAINZ_LIMIT_RESULT_COUNT);

    /**
     * API-Key for used for fanart.tv.
     * THIS KEY IS ONLY INTENDED FOR THE USE BY GATESHIP-ONE APPLICATIONS. PLEASE RESPECT THIS.
     */
    private static final String API_KEY = "c0cc5d1b6e807ce93e49d75e0e5d371b";

    private FanartTVManager(Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context);
    }

    public static synchronized FanartTVManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FanartTVManager(context);
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

        getArtists(artistURLName, response -> {
            JSONArray artists;
            try {
                artists = response.getJSONArray("artists");

                if (!artists.isNull(0)) {
                    JSONObject artistObj = artists.getJSONObject(0);
                    final String artistMBID = artistObj.getString("id");

                    getArtistImageURL(artistMBID, response1 -> {
                        JSONArray thumbImages;
                        try {
                            thumbImages = response1.getJSONArray("artistthumb");

                            JSONObject firstThumbImage = thumbImages.getJSONObject(0);
                            artist.setMBID(artistMBID);
                            getArtistImage(firstThumbImage.getString("url"), artist, listener, error -> errorListener.fetchVolleyError(artist, context, error));

                        } catch (JSONException e) {
                            errorListener.fetchJSONException(artist, context, e);
                        }
                    }, error -> errorListener.fetchVolleyError(artist, context, error));
                }
            } catch (JSONException e) {
                errorListener.fetchJSONException(artist, context, e);
            }
        }, error -> errorListener.fetchVolleyError(artist, context, error));
    }

    /**
     * Gets a list of possible artists from Musicbrainz database.
     * @param artistName Name of the artist to search for
     * @param listener Response listener to handle the artist list
     * @param errorListener Error listener
     */
    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(FanartTVManager.class.getSimpleName(), artistName);

        String queryArtistname = FormatHelper.escapeSpecialCharsLucene(artistName);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + queryArtistname + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Retrieves all available information (Artist image url, fanart url, ...) for an artist with an MBID of fanart.tv
     * @param artistMBID Artists MBID to query
     * @param listener Response listener to handle the artists information from fanart.tv
     * @param errorListener Error listener
     */
    private void getArtistImageURL(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(FanartTVManager.class.getSimpleName(), artistMBID);

        String url = FANART_TV_API_URL + "/" + artistMBID + "?api_key=" + API_KEY;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Raw download for an image-
     * @param url Final image URL to download
     * @param artist Artist associated with the image to download
     * @param listener Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getArtistImage(String url, ArtistModel artist, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(FanartTVManager.class.getSimpleName(), url);

        Request<ArtistImageResponse> byteResponse = new ArtistImageByteRequest(url, artist, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }

    @Override
    public void cancelAll() {
        mRequestQueue.cancelAll(request -> true);
    }

}
