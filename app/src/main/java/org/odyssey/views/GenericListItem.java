package org.odyssey.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class GenericListItem extends LinearLayout {

    protected TextView mNumberView;
    protected TextView mTitleView;
    protected TextView mInformationView;
    protected TextView mDurationView;

    public GenericListItem(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(provideLayout(), this, true);

        mTitleView = provideTitleView();
        mNumberView = provideNumberView();
        mInformationView = provideInformationView();
        mDurationView = provideDurationView();
    }

    /* Methods needed to provide generic textviews
    and layout to inflate.
    */
    abstract TextView provideTitleView();
    abstract TextView provideNumberView();
    abstract TextView provideInformationView();
    abstract TextView provideDurationView();

    abstract int provideLayout();

    /*
    * Sets the title for the ListItem
    */
    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    /*
* Sets the number text for the ListItem
*/
    public void setNumber(String number) {
        mNumberView.setText(number);
    }

    /*
* Sets the additional information text for the ListItem
*/
    public void setAdditionalInformation(String information) {
        mInformationView.setText(information);
    }

    /*
* Sets the duration text for the ListItem
*/
    public void setDuration(String duration) {
        mDurationView.setText(duration);
    }
}
