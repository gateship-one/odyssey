/*
 * Copyright (C) 2018 Team Team Gateship-One
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

package org.gateshipone.odyssey.utils;

import android.os.AsyncTask;
import androidx.core.util.Pair;

import org.gateshipone.odyssey.models.GenericModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FilterTask<T extends GenericModel> extends AsyncTask<String, Void, Pair<List<T>, String>> {

    public interface SuccessCallback<T> {
        void onSuccess(final Pair<List<T>, String> result);
    }

    public interface FailureCallback {
        void onFailure();
    }

    public interface Filter<T> {
        boolean matchesFilter(final T elem, final String filterString);
    }

    private final Filter<T> mFilter;

    private final SuccessCallback<T> mSuccessCallback;

    private final FailureCallback mFailureCallback;

    private final WeakReference<List<T>> mModelDataRef;

    public FilterTask(final List<T> modelData, final Filter<T> filter, final SuccessCallback<T> successCallback, final FailureCallback failureCallback) {
        mModelDataRef = new WeakReference<>(modelData);
        mFilter = filter;
        mSuccessCallback = successCallback;
        mFailureCallback = failureCallback;
    }

    @Override
    protected Pair<List<T>, String> doInBackground(String... lists) {
        List<T> resultList = new ArrayList<>();

        String filterString = lists[0];
        for (T elem : mModelDataRef.get()) {
            // Check if task was cancelled from the outside.
            if (isCancelled()) {
                resultList.clear();
                return new Pair<>(resultList, filterString);
            }
            if (mFilter.matchesFilter(elem, filterString)) {
                resultList.add(elem);
            }
        }

        return new Pair<>(resultList, filterString);
    }

    public final AsyncTask<String, Void, Pair<List<T>,String>> execute(String filterString) {
        return super.execute(filterString);
    }

    @Override
    protected void onPostExecute(Pair<List<T>, String> result) {
        if (!isCancelled()) {
            mSuccessCallback.onSuccess(result);
        } else {
            mFailureCallback.onFailure();
        }
    }

    @Override
    protected void onCancelled() {
        mFailureCallback.onFailure();
    }
}
