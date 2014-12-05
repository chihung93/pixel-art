package com.jaween.pixelart.ui.animation;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jaween.pixelart.R;

/**
 * Created by ween on 12/6/14.
 */
public class AnimationFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.animation_fragment, null);

        initialiseViews(view);

        return view;
    }

    private void initialiseViews(View v) {
        // TODO: Animation Fragment
    }
}
