package io.filepicker.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.filepicker.Filepicker;
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

    }

    public void runFilepicker(View view) {

        Filepicker.setKey(FILEPICKER_API_KEY);
        Filepicker.setAppName(PARENT_APP);

        Intent intent = new Intent(this, Filepicker.class);

        startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
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
