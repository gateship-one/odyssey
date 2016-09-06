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

import android.util.Pair;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

import org.gateshipone.odyssey.models.AlbumModel;

public class AlbumImageByteRequest extends Request<Pair<byte[], AlbumModel>> {

    private final Response.Listener<Pair<byte[], AlbumModel>> mListener;

    private AlbumModel mAlbum;


    public AlbumImageByteRequest(String url, AlbumModel album, Response.Listener<Pair<byte[], AlbumModel>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);

        mListener = listener;
        mAlbum = album;
    }

    @Override
    protected Response<Pair<byte[], AlbumModel>> parseNetworkResponse(NetworkResponse response) {
        return Response.success(new Pair<byte[], AlbumModel>(response.data, mAlbum), null);
    }

    @Override
    protected void deliverResponse(Pair<byte[], AlbumModel> response) {
        mListener.onResponse(response);
    }

}
