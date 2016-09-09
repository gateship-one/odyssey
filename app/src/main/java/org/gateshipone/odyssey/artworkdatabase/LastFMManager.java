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

package org.gateshipone.odyssey.artworkdatabase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;

import org.gateshipone.odyssey.artworkdatabase.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LastFMManager implements ArtistImageProvider, AlbumImageProvider {
    private static final String TAG = LastFMManager.class.getSimpleName();

    private static final String LAST_FM_API_URL = "http://ws.audioscrobbler.com/2.0/?method=";
    private static final String API_KEY = "8de46d96e49e78234f206fd9f21712de";

    private static final String LAST_FM_FORMAT_JSON = "&format=json";

    private static final String LAST_FM_REQUESTED_IMAGE_SIZE = "extralarge";

    private RequestQueue mRequestQueue;


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


    public <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
    }

    public void fetchArtistImage(final ArtistModel artist, final Response.Listener<ArtistImageResponse> listener, final ArtistFetchError errorListener) {


        String artistURLName = Uri.encode(artist.getArtistName().replaceAll("/"," "));

        getArtistImageURL(artistURLName, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject artistObj = response.getJSONObject("artist");
                    // FIXME optionally get mbid here without aborting the image fetch
                    JSONArray images = artistObj.getJSONArray("image");
                    Log.v(TAG,"Found: " + images.length() + "images");
                    for ( int i = 0; i < images.length(); i++ ) {
                        JSONObject image = images.getJSONObject(i);
                        if ( image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                            getArtistImage(image.getString("#text"), artist, listener, new Response.ErrorListener() {
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
                    errorListener.fetchError(artist);
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

    @Override
    public void cancelAll() {
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }


    private void getArtistImageURL(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {


        String url = LAST_FM_API_URL + "artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG,url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImage(String url, ArtistModel artist, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(LastFMManager.class.getSimpleName(), url);

        Request<ArtistImageResponse> byteResponse = new ArtistImageByteRequest(url, artist, listener, errorListener);

        addToRequestQueue(byteResponse);
    }


    @Override
    public void fetchAlbumImage(final AlbumModel album, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {
        getAlbumImageURL(album, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject albumObj = response.getJSONObject("album");
                    JSONArray images = albumObj.getJSONArray("image");
                    // FIXME optionally get mbid here without aborting the image fetch
                    Log.v(TAG,"Found: " + images.length() + "images");
                    for ( int i = 0; i < images.length(); i++ ) {
                        JSONObject image = images.getJSONObject(i);
                        if ( image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                            getAlbumImage(image.getString("#text"), album, listener, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    error.printStackTrace();
                                    errorListener.fetchError(album);
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

            }
        });
    }

    private void getAlbumImageURL(AlbumModel album, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String albumName = Uri.encode(album.getAlbumName());
        String artistName = Uri.encode(album.getArtistName());

        String url = LAST_FM_API_URL + "album.getinfo&album=" + albumName + "&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG,url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getAlbumImage(String url, AlbumModel album, Response.Listener<AlbumImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(LastFMManager.class.getSimpleName(), url);

        Request<AlbumImageResponse> byteResponse = new AlbumImageByteRequest(url, album, listener, errorListener);

        addToRequestQueue(byteResponse);
    }
}
