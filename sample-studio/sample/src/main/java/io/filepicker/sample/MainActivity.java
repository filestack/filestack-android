package io.filepicker.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.filepicker.Filepicker;
import io.filepicker.FilepickerCallback;
import io.filepicker.models.FPFile;

public class MainActivity extends Activity {


    // REQUIRED FIELD
    private static final String FILEPICKER_API_KEY = "PUT YOUR API KEY HERE";

    // OPTIONAL FIELD
    private static final String PARENT_APP = "PUT YOUR APP NAME HERE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Filepicker.setKey(FILEPICKER_API_KEY);
        Filepicker.setAppName(PARENT_APP);
    }

    public void getFromAll(View view) {
        Intent intent = new Intent(this, Filepicker.class);
        startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
    }

    public void getFromFacebook(View view) {
        Intent intent = new Intent(this, Filepicker.class);

        String[] services = {"FACEBOOK"};
        intent.putExtra("services", services);

        startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
    }

    public void getFromCamera(View view) {
        Intent intent = new Intent(this, Filepicker.class);

        String[] services = {"CAMERA"};
        intent.putExtra("services", services);

        startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
    }

    public void getMultiple(View view) {
        Intent intent = new Intent(this, Filepicker.class);
        intent.putExtra("multiple", true);
        startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
    }

    public void uploadLocalFile(View view) {
        final String url = "PUT PATH TO LOCAL FILE HERE - something like content://...";

        Filepicker.uploadLocalFile(Uri.parse(url), this, new FilepickerCallback() {
            @Override
            public void onFileUploadSuccess(FPFile fpFile) {
                // Do something on success
            }

            @Override
            public void onFileUploadError(Throwable error) {
                // Do something on error
            }

            @Override
            public void onFileUploadProgress(Uri uri, float progress) {
                // Do something on progress
            }
        });
    }

    @Override
    protected void onDestroy() {
        Filepicker.unregistedLocalFileUploadCallbacks();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == Filepicker.REQUEST_CODE_GETFILE) {
            if (resultCode == RESULT_OK) {
                // Filepicker always returns array of FPFile objects
                ArrayList<FPFile> fpFiles = data.getParcelableArrayListExtra(Filepicker.FPFILES_EXTRA);

                // Get first object (use if multiple option is not set)
                FPFile file = fpFiles.get(0);

                // Load image using Picasso library
                ImageView imageView = (ImageView) findViewById(R.id.imageResult);
                Picasso.with(getBaseContext()).load(file.getUrl()).into(imageView);
            } else if(resultCode == RESULT_CANCELED && data != null) {
                Uri fileUri = data.getData();

                Filepicker.uploadLocalFile(fileUri, this);
            }
        }
    }
}
