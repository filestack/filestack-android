package io.filepicker.sample;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.filepicker.Filepicker;
import io.filepicker.models.FPFile;
import io.filepicker.old.FilePicker;
import io.filepicker.old.FilePickerAPI;


public class MainActivity extends Activity {

    //TODO : Enter your API key here.
    private static final String FILEPICKER_API_KEY = "AcD6WqSDZTuumV0HMhYUez";
    private static final String PARENT_APP = "whatever";

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

    public void runNewFilePicker(View view) {
        Filepicker.setKey(FILEPICKER_API_KEY);
        Filepicker.setAppName(PARENT_APP);

        Intent intent = new Intent(this, Filepicker.class);

        // This way user can choose services
//        String[] services = {"FACEBOOK", "CAMERA", "GMAIL"};
//        intent.putExtra("services", services);

        intent.putExtra("multiple", true);
        startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == FilePickerAPI.REQUEST_CODE_GETFILE) {
            if (resultCode != RESULT_OK)
                //Result was cancelled by the user or there was an error
                return;

            ArrayList<FPFile> fpFiles = data.getParcelableArrayListExtra(Filepicker.FPFILES_EXTRA);

            for(FPFile fpFile : fpFiles) {
                Log.d("RECEIVED ", fpFile.getFilename());
            }
            FPFile file = fpFiles.get(0);

            ImageView imageView = (ImageView) findViewById(R.id.imageResult);
            Picasso.with(getBaseContext()).load(file.getUrl()).into(imageView);

            ((TextView)findViewById(R.id.tvUrl)).setText(file.getUrl());
        }
    }
}
