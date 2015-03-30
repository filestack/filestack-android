package io.filepicker.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

        // MANDATORY
        Filepicker.setKey(FILEPICKER_API_KEY);
        Filepicker.setAppName(PARENT_APP);

        Intent intent = new Intent(this, Filepicker.class);

        // Store options
        //   intent.putExtra("location", "S3");
        //   intent.putExtra("path", "/example/123.png");
        //   intent.putExtra("container", "example_bucket");
        //   intent.putExtra("access", "public");

        // Choose services
        //   String[] services = {"FACEBOOK", "CAMERA", "GMAIL"};
        //   intent.putExtra("services", services);

        // Allow getting multiple files
        //   intent.putExtra("multiple", true);

        // Choose mimetypes
        //   String[] mimetypes = {"image/*"};
        //   intent.putExtra("mimetype", mimetypes);

        // Choose max number of files
        //   intent.putExtra("maxFiles", 0);


        // Security options
        // https://developers.filepicker.io/docs/security/

        //   String appSecret = "LDIUJKQGDZCVTBV7ADUPKR2UKE";
        //   intent.putExtra("app_secret", appSecret);

        //   String[] calls = new String[]{"pick", "read"};
        //   intent.putExtra("policy_calls", calls);

        //   String handle = "SET HANDLE";
        //   intent.putExtra("policy_handle", handle);

        // NOTE: Expiry states for how long the policy is valid (the value is added to the request's time)
        //   int expiry = 60*60;
        //   intent.putExtra("policy_expiry", expiry);

        //   int maxSize = 100;
        //   intent.putExtra("policy_max_size", maxSize);

        //   int minSize = 100;
        //   intent.putExtra("policy_min_size", maxSize);

        //   String policyPath = "SET PATH";
        //   intent.putExtra("policy_path", policyPath);

        //   String policyContainer = "SET CONTAINER";
        //   intent.putExtra("policy_container", policyContainer);


        startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == Filepicker.REQUEST_CODE_GETFILE) {
            if (resultCode != RESULT_OK)
                //Result was cancelled by the user or there was an error
                return;

            // Filepicker always returns array of FPFile objects
            ArrayList<FPFile> fpFiles = data.getParcelableArrayListExtra(Filepicker.FPFILES_EXTRA);

            // Get first object (use if multiple option is not set)
            FPFile file = fpFiles.get(0);

            // Load image using Picasso library
            ImageView imageView = (ImageView) findViewById(R.id.imageResult);
            Picasso.with(getBaseContext()).load(file.getUrl()).into(imageView);
        }
    }
}
