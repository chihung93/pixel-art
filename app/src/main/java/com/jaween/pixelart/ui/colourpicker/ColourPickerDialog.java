package com.jaween.pixelart.ui.colourpicker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.widget.Toast;

/**
 * Created by ween on 11/20/14.
 */
public class ColourPickerDialog extends DialogFragment implements
        DialogInterface.OnClickListener {

    private ColourPickerFragment colourPickerFragment;

    private static final String TAG_COLOUR_PICKER_FRAGMENT = "colour_picker_fragment";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();


        FragmentManager fragmentManager = getChildFragmentManager();
        if (savedInstanceState != null) {
            colourPickerFragment = (ColourPickerFragment) fragmentManager.findFragmentByTag(TAG_COLOUR_PICKER_FRAGMENT);
        } else {
            colourPickerFragment = new ColourPickerFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(colourPickerFragment, TAG_COLOUR_PICKER_FRAGMENT);
            fragmentTransaction.commit();
        }

        // Sets up the dialog components
        builder.setView(colourPickerFragment.getView())
                .setPositiveButton("Done", this)
                .setNegativeButton("Cancel", this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        Toast.makeText(getActivity(), i + " clicked!", Toast.LENGTH_SHORT).show();
    }
}
