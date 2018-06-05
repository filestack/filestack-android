package com.filestack.android.samples.form;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

// Shows a simple welcome screen to the user to indicate the create flow is complete
public class CompleteFragment extends Fragment implements View.OnClickListener {

    private static final String KEY_NAME = "keyName";




    public static CompleteFragment create(String name) {
        Bundle args = new Bundle();
        args.putString(KEY_NAME, name);
        CompleteFragment fragment = new CompleteFragment();
        fragment.setArguments(args);
        return fragment;
    }




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_complete, container, false);

        // Personalize the welcome message
        TextView messageView = root.findViewById(R.id.msg);
        String name = getArguments().getString(KEY_NAME);
        name = name.split(" ")[0];
        String message = String.format("Welcome to Acme %s!\nLet's get started!", name);
        messageView.setText(message);

        // Wire up the button
        Button button = root.findViewById(R.id.ok);
        button.setOnClickListener(this);

        return root;
    }

    @Override
    public void onClick(View v) {
        // Call out to the activity to reset state and swap fragments
        ((MainActivity) getActivity()).reset();
    }

}
