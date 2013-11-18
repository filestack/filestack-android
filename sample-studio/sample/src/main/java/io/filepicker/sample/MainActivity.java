package io.filepicker.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import io.filepicker.FilePicker;
import io.filepicker.FilePickerAPI;

public class MainActivity extends Activity {

    //TODO : Enter your API key here.
    private static final String FILEPICKER_API_KEY = "your_api_key_here";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FilePickerAPI.setKey(FILEPICKER_API_KEY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onClick(View view) {
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
            Uri uri = data.getData();
            System.out.println("File path is " + uri.toString());
            ((TextView)findViewById(R.id.filePathtextView)).setText(uri.toString());
            System.out.println("Ink file URL: " + data.getExtras().getString("fpurl"));
            ((TextView)findViewById(R.id.inkfileUrltextView)).setText(data.getExtras().getString("fpurl"));
        }
    }

}
