/*
 * Copyright (C) 2018 Team Gateship-One
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
import org.gateshipone.odyssey.artworkdatabase.network.requests.OdysseyJsonObjectRequest;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.FormatHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * FIXME:
 * ArtistImageProvider currently NOT IMPLEMENTED!!!
 */
public class MusicBrainzManager implements AlbumImageProvider {
    private static final String TAG = MusicBrainzManager.class.getSimpleName();

    private static final String MUSICBRAINZ_API_URL = "http://musicbrainz.org/ws/2";
    private static final String COVERART_ARCHIVE_API_URL = "http://coverartarchive.org";

    private RequestQueue mRequestQueue;

    private static MusicBrainzManager mInstance;

    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    private static final int MUSICBRAINZ_LIMIT_RESULT_COUNT = 10;
    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + String.valueOf(MUSICBRAINZ_LIMIT_RESULT_COUNT);

    private MusicBrainzManager(Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context);
    }

    public static synchronized MusicBrainzManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MusicBrainzManager(context);
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

        String artistURLName = Uri.encode(artist.getArtistName());

        getArtists(artistURLName, response -> {
            JSONArray artists;
            try {
                artists = response.getJSONArray("artists");

                if (!artists.isNull(0)) {
                    JSONObject artistObj = artists.getJSONObject(0);
                    String artistMBID = artistObj.getString("id");

                    getArtistImageURL(artistMBID, response1 -> {
                        JSONArray relations;
                        try {
                            relations = response1.getJSONArray("relations");
                            for (int i = 0; i < relations.length(); i++) {
                                JSONObject obj = relations.getJSONObject(i);

                                if (obj.getString("type").equals("image")) {
                                    JSONObject url = obj.getJSONObject("url");

                                    getArtistImage(url.getString("resource"), listener, error -> errorListener.fetchVolleyError(artist, context, error));
                                }
                            }
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
     * Searches for the artist with the given artist name and tries to manually get an MBID
     * @param artistName Artist name to search for
     * @param listener Callback to handle the response
     * @param errorListener Callback to handle errors
     */
    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistName);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + artistName + MUSICBRAINZ_FORMAT_JSON;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Fetches the image URL for the raw image blob.
     * @param artistMBID Artist mbid to look for an image
     * @param listener Callback listener to handle the response
     * @param errorListener Callback to handle a fetch error
     */
    private void getArtistImageURL(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistMBID);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/" + artistMBID + "?inc=url-rels" + MUSICBRAINZ_FORMAT_JSON;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Raw download for an image
     * @param url Final image URL to download
     * @param listener Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getArtistImage(String url, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(MusicBrainzManager.class.getSimpleName(), url);

        // FIXME not implemented yet

//        Request<byte[]> byteResponse = new ArtistImageByteRequest(url, listener, errorListener);

//        addToRequestQueue(byteResponse);
    }

    /**
     * Public interface to get an image for an album.
     * @param album Album to check for an image
     * @param listener Callback to handle the fetched image
     * @param errorListener Callback to handle errors
     */
    @Override
    public void fetchAlbumImage(final AlbumModel album, final Context context, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {

        getAlbumMBID(album, response -> parseMusicBrainzReleaseJSON(album, 0, response, context, listener, errorListener), error -> errorListener.fetchVolleyError(album, context, error));
    }

    /**
     * Parses the JSON response and searches the image URL
     * @param album Album to check for an image
     * @param releaseIndex Index of the requested release to check for an image
     * @param response Response to check use to search for an image
     * @param context Context used for lookup
     * @param listener Callback to handle the response
     * @param errorListener Callback to handle errors
     */
    private void parseMusicBrainzReleaseJSON(final AlbumModel album, final int releaseIndex, final JSONObject response, final Context context, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {
        if (releaseIndex >= MUSICBRAINZ_LIMIT_RESULT_COUNT) {
            return;
        }

        try {
            final JSONArray releases = response.getJSONArray("releases");
            if (releases.length() > releaseIndex) {
                String mbid = releases.getJSONObject(releaseIndex).getString("id");
                album.setMBID(mbid);

                String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + mbid + "/front-500";

                getAlbumImage(url, album, listener, error -> {
                    Log.v(TAG, "No image found for: " + album.getAlbumName() + " with release index: " + releaseIndex);
                    if (releaseIndex + 1 < releases.length()) {
                        parseMusicBrainzReleaseJSON(album, releaseIndex + 1, response, context, listener, errorListener);
                    } else {
                        errorListener.fetchVolleyError(album, context, error);
                    }
                });
            } else {
                errorListener.fetchVolleyError(album, context, null);
            }
        } catch (JSONException e) {
            errorListener.fetchJSONException(album, context, e);
        }
    }

    /**
     * Wrapper to get an MBID out of an {@link AlbumModel}.
     * @param album Album to get the MBID for
     * @param listener Response listener
     * @param errorListener Error listener
     */
    private void getAlbumMBID(AlbumModel album, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String albumName = Uri.encode(album.getAlbumName());
        albumName = FormatHelper.escapeSpecialCharsLucene(albumName);
        String artistName = Uri.encode(album.getArtistName());
        artistName = FormatHelper.escapeSpecialCharsLucene(artistName);
        String url;
        if (!artistName.isEmpty()) {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + "%20AND%20artist:" + artistName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        } else {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        }

        Log.v(TAG, "Requesting release mbid for: " + url);

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
        Request<AlbumImageResponse> byteResponse = new AlbumImageByteRequest(url, album, listener, errorListener);
        Log.v(TAG, "Get image: " + url);
        mRequestQueue.add(byteResponse);
    }

    @Override
    public void cancelAll() {
        mRequestQueue.cancelAll(request -> true);
    }

}
