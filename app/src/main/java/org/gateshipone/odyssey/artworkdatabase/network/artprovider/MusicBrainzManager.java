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

package org.gateshipone.odyssey.artworkdatabase.network.artprovider;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.requests.OdysseyJsonObjectRequest;
import org.gateshipone.odyssey.artworkdatabase.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artworkdatabase.network.requests.AlbumImageByteRequest;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MusicBrainzManager implements ArtistImageProvider, AlbumImageProvider {
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


    public <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
    }

    public void fetchArtistImage(final ArtistModel artist, final Response.Listener<ArtistImageResponse> listener, final ArtistFetchError errorListener) {

        String artistURLName = Uri.encode(artist.getArtistName());

        getArtists(artistURLName, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray artists = null;
                try {
                    artists = response.getJSONArray("artists");

                    if (!artists.isNull(0)) {
                        JSONObject artistObj = artists.getJSONObject(0);
                        String artistMBID = artistObj.getString("id");

                        getArtistImageURL(artistMBID, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONArray relations = null;
                                try {
                                    relations = response.getJSONArray("relations");
                                    for (int i = 0; i < relations.length(); i++) {
                                        JSONObject obj = relations.getJSONObject(i);

                                        if (obj.getString("type").equals("image")) {
                                            JSONObject url = obj.getJSONObject("url");

                                            getArtistImage(url.getString("resource"), listener, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    error.printStackTrace();
                                                    errorListener.fetchError(artist);
                                                }
                                            });
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                error.printStackTrace();
                                errorListener.fetchError(artist);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                errorListener.fetchError(artist);
            }
        });
    }

    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistName);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + artistName + MUSICBRAINZ_FORMAT_JSON;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImageURL(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistMBID);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/" + artistMBID + "?inc=url-rels" + MUSICBRAINZ_FORMAT_JSON;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImage(String url, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(MusicBrainzManager.class.getSimpleName(), url);

//        Request<byte[]> byteResponse = new ArtistImageByteRequest(url, listener, errorListener);

//        addToRequestQueue(byteResponse);
    }

    @Override
    public void fetchAlbumImage(final AlbumModel album, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {

        getAlbumMBID(album, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                parseMusicBrainzReleaseJSON(album, 0, response, listener, errorListener);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                errorListener.fetchError(album);
            }
        });
    }

    private void parseMusicBrainzReleaseJSON(final AlbumModel album, final int releaseIndex, final JSONObject response, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {
        if (releaseIndex >= MUSICBRAINZ_LIMIT_RESULT_COUNT) {
            return;
        }

        try {
            JSONArray releases = response.getJSONArray("releases");
            if ( releases.length() > releaseIndex) {
                String mbid = releases.getJSONObject(releaseIndex).getString("id");
                album.setMBID(mbid);

                String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + mbid + "/front-500";

                getAlbumImage(url, album, listener, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v(TAG,"No image found for: " + album.getAlbumName() + " with release index: " + releaseIndex);
                        parseMusicBrainzReleaseJSON(album, releaseIndex+1, response, listener, errorListener);
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void getAlbumMBID(AlbumModel album, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String albumName = Uri.encode(album.getAlbumName());
        String artistName = Uri.encode(album.getArtistName());
        String url;
        if (!artistName.isEmpty()) {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + "%20AND%20artist:" + artistName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        } else {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        }

        Log.v(TAG, "Requesting release mbid for: " + url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getAlbumImageURL(String releaseMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + releaseMBID;

        Log.v(TAG, "Requesting release image urls for: " + url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getAlbumImage(String url, AlbumModel album, Response.Listener<AlbumImageResponse> listener, Response.ErrorListener errorListener) {
        Request<AlbumImageResponse> byteResponse = new AlbumImageByteRequest(url, album, listener, errorListener);
        Log.v(TAG,"Get image: " + url);
        addToRequestQueue(byteResponse);
    }

    @Override
    public void cancelAll() {
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

}
