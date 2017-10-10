package com.filestack.android;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class CloudListFragment extends Fragment {
    private final static String ARG_CLOUD_INFO_ID = "cloudInfoId";

    private CloudInfo cloudInfo;

    public static CloudListFragment create(int cloudInfoId) {
        CloudListFragment fragment = new CloudListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLOUD_INFO_ID, cloudInfoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        cloudInfo = Util.getCloudInfo(args.getInt(ARG_CLOUD_INFO_ID));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View baseView = inflater.inflate(R.layout.fragment_cloud_list, container, false);

        // TODO Temporary until we set actual icons
        Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_square);
        ImageView iconView = baseView.findViewById(R.id.icon);
        drawable.setColorFilter(cloudInfo.getIconId(), PorterDuff.Mode.MULTIPLY);
        iconView.setImageDrawable(drawable);

        // Replace placeholder text with actual cloud name
        String target = "Cloud";
        String replacement = getString(cloudInfo.getTextId());
        TextView textView = baseView.findViewById(R.id.title);
        Util.textViewReplace(textView, target, replacement);

        return baseView;
    }
}
