package com.project.edgedetector;

import static com.google.android.gms.cast.framework.media.ImagePicker.*;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.AsyncTaskLoader;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.drjacky.imagepicker.provider.*;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.ImagePicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private Button uploadImage;
    private Button startProcessing;
    private Uri uri = null;
    private EditText editText;
    private Handler handler = new Handler();
    private Button fetchImage;
    ProgressDialog progressDialog;

    private ImageView input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();


        uploadImage = findViewById(R.id.upload_image);
        fetchImage = findViewById(R.id.fetch_image_button);

        startProcessing = findViewById(R.id.processing);
        input = findViewById(R.id.inputImage);
        editText = findViewById(R.id.url_text);


        startProcessing.setEnabled(false);
        startProcessing.setBackgroundColor(Color.GRAY);
        TextView linkTextView = findViewById(R.id.activity_main_link);
        linkTextView.setMovementMethod(LinkMovementMethod.getInstance());

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                com.github.dhaval2404.imagepicker.ImagePicker.Companion.with(MainActivity.this).crop().start();

            }
        });


        fetchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String url = editText.getText().toString();

                if (url.equals("")) {
                    Toast.makeText(MainActivity.this, "please enter the URL first", Toast.LENGTH_SHORT).show();
                } else {
                    new FetchImage(url).start();
                }
            }
        });


        startProcessing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uriString = uri.toString();

                Intent intent = new Intent(MainActivity.this, Processing.class);
                intent.putExtra("key", uriString);
                startActivity(intent);

            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uri = data.getData();
        input.setImageURI(uri);
        if (uri != null) {
            startProcessing.setEnabled(true);
            startProcessing.setBackgroundColor(Color.BLACK);
        }

    }

    // for fetching the image --- background thread
    class FetchImage extends Thread {

        String url;
        Bitmap bitmap;

        public FetchImage(String url) {
            this.url = url;

        }

        @Override
        public void run() {

            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressDialog = new ProgressDialog(MainActivity.this);
                    progressDialog.setMessage("Getting your image ...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });


            InputStream inputStream = null;
            try {
                inputStream = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);

            } catch (IOException e) {
                e.printStackTrace();
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();

                    input.setImageBitmap(bitmap);

                    // converting bitmap to uri
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    String path = MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), bitmap, "val", null);
                    uri = Uri.parse(path);


                    startProcessing.setEnabled(true);
                    startProcessing.setBackgroundColor(Color.BLACK);
                    editText.setText("");

                }
            });

        }
    }
}