package org.odyssey.listener;

import org.odyssey.fragments.SaveDialog;

public interface OnSaveDialogListener {
    void onSaveObject(String title, SaveDialog.OBJECTTYPE type);
}
