package com.filestack.android;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AuthFragment extends Fragment implements View.OnClickListener {
    private final static String ARG_CLOUD_INFO_ID = "cloudInfoId";
    private final static String ARG_AUTH_URL = "authUrl";

    private final static int[] TEXT_VIEW_IDS = {
            R.id.title, R.id.description, R.id.button, R.id.action };

    private SourceInfo sourceInfo;
    private String authUrl;

    public static AuthFragment create(int cloudInfoId, String authUrl) {
        AuthFragment fragment = new AuthFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLOUD_INFO_ID, cloudInfoId);
        args.putString(ARG_AUTH_URL, authUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        sourceInfo = Util.getSourceInfo(args.getInt(ARG_CLOUD_INFO_ID));
        authUrl = args.getString(ARG_AUTH_URL);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View baseView = inflater.inflate(R.layout.fragment_auth, container, false);

        ImageView iconView = baseView.findViewById(R.id.icon);
        iconView.setImageResource(sourceInfo.getIconId());

        // Replace placeholder text with actual cloud name
        String target = "Cloud";
        String replacement = getString(sourceInfo.getTextId());
        for (int id : TEXT_VIEW_IDS) {
            TextView textView = baseView.findViewById(id);
            Util.textViewReplace(textView, target, replacement);
        }

        Button button = baseView.findViewById(R.id.button);
        button.setOnClickListener(this);

        return baseView;
    }

    @Override
    public void onClick(View view) {
//        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
//        CustomTabsIntent customTabsIntent = builder.build();
//        customTabsIntent.launchUrl(getContext(), Uri.parse(authUrl));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(authUrl));
        startActivity(intent);
    }
}
