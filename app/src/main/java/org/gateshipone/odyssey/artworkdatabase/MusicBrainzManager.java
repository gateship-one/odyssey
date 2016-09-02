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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MusicBrainzManager {

    private static final String mApiURL = "http://musicbrainz.org/ws/2";

    private RequestQueue mRequestQueue;

    private Context mContext;

    private static MusicBrainzManager mInstance;

    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    private MusicBrainzManager(Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized MusicBrainzManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MusicBrainzManager(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void fetchImage(String artistName, final Response.Listener<byte[]> listener) {

        String artistURLName = Uri.encode(artistName);

        getArtists(artistURLName, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray artists = null;
                try {
                    artists = response.getJSONArray("artists");

                    if (!artists.isNull(0)) {
                        JSONObject artist = artists.getJSONObject(0);
                        String artistMBID = artist.getString("id");

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

                                            getImage(url.getString("resource"), listener, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    // FIXME error handling
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
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // FIXME error handling
            }
        });
    }

    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistName);

        String url = mApiURL + "/" + "artist/?query=artist:" + artistName + MUSICBRAINZ_FORMAT_JSON;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImageURL(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistMBID);

        String url = mApiURL + "/" + "artist/" + artistMBID + "?inc=url-rels" + MUSICBRAINZ_FORMAT_JSON;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getImage(String url, Response.Listener<byte[]> listener, Response.ErrorListener errorListener) {
        Log.v(MusicBrainzManager.class.getSimpleName(), url);

        Request<byte[]> byteResponse = new ByteRequest(url, listener, errorListener);

        addToRequestQueue(byteResponse);
    }
}
