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

import org.gateshipone.odyssey.models.ArtistModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LastFMManager implements ArtistImageProvider {
    private static final String TAG = LastFMManager.class.getSimpleName();

    private static final String LAST_FM_API_URL = "http://ws.audioscrobbler.com/2.0/?method=";
    private static final String API_KEY = "API_KEY_MISSING";

    private static final String LAST_FM_FORMAT_JSON = "&format=json";

    private static final String LAST_FM_REQUESTED_IMAGE_SIZE = "extralarge";

    private RequestQueue mRequestQueue;


    private static LastFMManager mInstance;

    private LastFMManager() {
        mRequestQueue = getRequestQueue();
    }

    public static synchronized LastFMManager getInstance() {
        if (mInstance == null) {
            mInstance = new LastFMManager();
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            Cache cache = new NoCache();
            Network nw = new BasicNetwork(new HurlStack());
            mRequestQueue = new RequestQueue(cache, nw,1);
            mRequestQueue.start();
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void fetchArtistImage(final ArtistModel artist, final Response.Listener<Pair<byte[], ArtistModel>> listener, final ArtistFetchError errorListener) {


        String artistURLName = Uri.encode(artist.getArtistName().replaceAll("/"," "));

        getArtistImageURL(artistURLName, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject artistObj = response.getJSONObject("artist");

                    JSONArray images = artistObj.getJSONArray("image");
                    String mbid = artistObj.getString("mbid");
                    artist.setMBID(mbid);
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


    private void getArtistImageURL(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {


        String url = LAST_FM_API_URL + "artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG,url);

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImage(String url, ArtistModel artist, Response.Listener<Pair<byte[],ArtistModel>> listener, Response.ErrorListener errorListener) {
        Log.v(LastFMManager.class.getSimpleName(), url);

        Request<Pair<byte[], ArtistModel>> byteResponse = new ArtistImageByteRequest(url, artist, listener, errorListener);

        addToRequestQueue(byteResponse);
    }
}
