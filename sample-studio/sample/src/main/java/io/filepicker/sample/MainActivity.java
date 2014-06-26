package io.filepicker.sample;

import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import io.filepicker.FilePicker;
import io.filepicker.FilePickerAPI;

public class MainActivity extends Activity {

    //TODO : Enter your API key here.
    private static final String FILEPICKER_API_KEY = "A0Cz2me9eTsy3YNsgc5VQz";
    private static final String PARENT_APP = "I love FilePicker!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FilePickerAPI.setKey(FILEPICKER_API_KEY);
        FilePicker.setParentAppName(PARENT_APP);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void runFilePicker(View view) {
        Intent intent = new Intent(this, FilePicker.class);
        startActivityForResult(intent, FilePickerAPI.REQUEST_CODE_GETFILE);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == FilePickerAPI.REQUEST_CODE_GETFILE) {
            if (resultCode != RESULT_OK)
                //Result was cancelled by the user or there was an error
                return;

            String uri = data.getExtras().getString("fpurl");
            ImageView imageView = (ImageView) findViewById(R.id.imageResult);
            Picasso.with(getBaseContext()).load(uri).into(imageView);

        }
    }

}
