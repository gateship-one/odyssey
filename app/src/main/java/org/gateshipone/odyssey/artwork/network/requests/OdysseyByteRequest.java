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

package org.gateshipone.odyssey.artwork.network.requests;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

import org.gateshipone.odyssey.BuildConfig;
import org.gateshipone.odyssey.artwork.network.ArtworkRequestModel;
import org.gateshipone.odyssey.artwork.network.ImageResponse;

import java.util.HashMap;
import java.util.Map;

public class OdysseyByteRequest extends Request<ImageResponse> {

    private final Response.Listener<ImageResponse> mListener;

    private ArtworkRequestModel mModel;

    public OdysseyByteRequest(ArtworkRequestModel model, String url, Response.Listener<ImageResponse> listener, @Nullable Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);

        mModel = model;
        mListener = listener;
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-agent", "Application Odyssey/" + BuildConfig.VERSION_NAME + " (https://github.com/gateship-one/odyssey)");
        return headers;
    }

    @Override
    protected Response<ImageResponse> parseNetworkResponse(NetworkResponse response) {
        ImageResponse imageResponse = new ImageResponse();
        imageResponse.model = mModel;
        imageResponse.url = getUrl();
        imageResponse.image = response.data;
        return Response.success(imageResponse, null);
    }

    @Override
    protected void deliverResponse(ImageResponse response) {
        mListener.onResponse(response);
    }
}
