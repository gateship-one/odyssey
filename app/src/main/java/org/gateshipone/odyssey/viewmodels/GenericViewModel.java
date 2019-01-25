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

package org.gateshipone.odyssey.viewmodels;

import android.app.Application;

import org.gateshipone.odyssey.models.GenericModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public abstract class GenericViewModel<T extends GenericModel> extends AndroidViewModel {

    private MutableLiveData<List<T>> mData;

    abstract void loadData();

    GenericViewModel(@NonNull final Application application) {
        super(application);

        mData = new MutableLiveData<>();
    }

    public LiveData<List<T>> getData() {
        return mData;
    }

    public void reloadData() {
        loadData();
    }

    public void clearData() {
        mData.setValue(null);
    }

    protected void setData(final List<T> data) {
        mData.setValue(data);
    }
}
