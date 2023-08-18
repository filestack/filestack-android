package com.filestack.android.samples.form;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.filestack.Config;
import com.filestack.FileLink;
import com.filestack.android.FsActivity;
import com.filestack.android.FsConstants;
import com.filestack.transforms.tasks.ResizeTask;
import com.squareup.picasso.Picasso;

// Contains a simple account creation form
public class FormFragment extends Fragment implements View.OnClickListener {

    private ImageView imageView;
    private TextView nameView;




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_form, container, false);
        imageView = root.findViewById(R.id.img);
        nameView = root.findViewById(R.id.name);
        root.findViewById(R.id.sel_img_btn).setOnClickListener(this);
        root.findViewById(R.id.crt_act_btn).setOnClickListener(this);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If MainActivity contains a FileLink when the fragment resumes, load the image from it
        // So in this case we check the activity for data, rather than have it push to the fragment
        FileLink fileLink = ((MainActivity) getActivity()).getFileLink();
        if (fileLink != null) {
            int dimen = getResources().getDimensionPixelSize(R.dimen.form_image);
            String url = getAdaptiveUrl(fileLink, dimen);
            Picasso.with(getContext()).load(url).into(imageView);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.sel_img_btn) {
            selectImage();
        } else if (id == R.id.crt_act_btn) {
            createAccount();
        }
    }




    // Creates a URL to an image sized appropriately for the form ImageView
    private String getAdaptiveUrl(FileLink fileLink, int dimen) {
        ResizeTask resizeTask = new ResizeTask.Builder()
                .fit("crop")
                .align("center")
                .width(dimen)
                .height(dimen)
                .build();

        return fileLink.imageTransform().addTask(resizeTask).url();
    }

    // Handles user clicking select button, launches the picker UI
    private void selectImage() {
        // Start picker activity

        // For simplicity we're loading credentials from a string res, don't do this in production
        String apiKey = getString(R.string.filestack_api_key);
        if (apiKey.equals("")) {
            throw new RuntimeException("Create a string res value for \"filestack_api_key\"");
        }

        Config config = new Config(apiKey, "https://form.samples.android.filestack.com");
        Context context = getContext();
        Intent pickerIntent = new Intent(context, FsActivity.class);
        pickerIntent.putExtra(FsConstants.EXTRA_CONFIG, config);
        // Restrict file selections to just images
        String[] mimeTypes = {"image/*"};
        pickerIntent.putExtra(FsConstants.EXTRA_MIME_TYPES, mimeTypes);
        context.startActivity(pickerIntent);

        // Show loading progress spinner
        ((MainActivity) getActivity()).setLoading(true);
    }

    // Handles user clicking create button, calls on activity to handle swapping fragments
    private void createAccount() {
        // TODO Validate form data and send off request

        String name = nameView.getText().toString();
        ((MainActivity) getActivity()).setComplete(name);
    }
}
