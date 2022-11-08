package com.project.edgedetector;


import android.content.Context;
import android.graphics.Bitmap;

import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Processing extends AppCompatActivity {
    private Uri uri = null;
    private Uri resultUri = null;
    private final Handler handler = new Handler();
    ArrayList<Uri> images = new ArrayList<Uri>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_processing);

        if (!OpenCVLoader.initDebug())
            Log.e("OpenCV", "Failed");
        else
            Log.d("OpenCV", "Successfully!");

        ImageView imagemOriginal = (ImageView) findViewById(R.id.inputImage);
        ImageView imagemResult = (ImageView) findViewById(R.id.outputImage);

        Button saveToFirebase = (Button) findViewById(R.id.save_to_firebase);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            String value = extras.getString("key");
            uri = Uri.parse(value);

            Bitmap bitmapOriginal = null;

            try {
                bitmapOriginal = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            imagemOriginal.setImageBitmap(resizeImage(getApplicationContext(), bitmapOriginal, 300,250));

            Bitmap bitmap = ((BitmapDrawable) imagemOriginal.getDrawable()).getBitmap();

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Bitmap bitmapResult = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);
            Size size = new Size(7, 7);

            Mat blurredImage = new Mat();
            Imgproc.GaussianBlur(mat, blurredImage, size, 0, 0);

            Mat gray = new Mat();
            Imgproc.cvtColor(blurredImage, gray, Imgproc.COLOR_RGB2GRAY);


            Mat grad_x = new Mat();
            Imgproc.Sobel(gray, grad_x, CvType.CV_16S, 1, 0, 3, 1, 0);


            Mat grad_y = new Mat();
            Imgproc.Sobel(gray, grad_y, CvType.CV_16S, 0, 1, 3, 1, 0);


            Mat abs_grad_x = new Mat();
            Mat abs_grad_y = new Mat();
            Core.convertScaleAbs(grad_x, abs_grad_x);
            Core.convertScaleAbs(grad_y, abs_grad_y);


            Mat sobel = new Mat();
            Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 1, sobel);

            Utils.matToBitmap(sobel, bitmapResult);

            imagemResult.setImageBitmap(resizeImage(this, bitmapResult, 300, 250));

            new ConvertBitmapToUri(bitmapResult).start();


        }

        saveToFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (uri != null && resultUri != null) {
                    saveImagesToFirebase();
                    Toast.makeText(Processing.this, "saved successfully to firebase", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(Processing.this, "failed to save", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void saveImagesToFirebase() {


        images.add(uri);
        images.add(resultUri);
        StorageReference ImageFolder = FirebaseStorage.getInstance().getReference().child("ImageFolder");

        for (int i = 0; i < 2; i++) {
            Uri image = images.get(i);
            StorageReference ImageName = ImageFolder.child("Image" + image.getLastPathSegment());
            ImageName.putFile(image).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    ImageName.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String url = String.valueOf(uri);
                            StoreLink(url);

                        }
                    });
                }


            });
        }


    }

    private void StoreLink(String url) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("ImageLinks");
        HashMap<String, String> map = new HashMap<>();
        map.put("imagelink", url);
        databaseReference.push().setValue(map);
    }


    public Bitmap resizeImage(Context context, Bitmap bmpOriginal,
                              float newWidth, float newHeight) {

        Bitmap novoBmp = null;

        int w = bmpOriginal.getWidth();
        int h = bmpOriginal.getHeight();

        float densityFactor = context.getResources().getDisplayMetrics().density;
        float novoW = newWidth * densityFactor;
        float novoH = newHeight * densityFactor;

        float scalaW = novoW / w;
        float scalaH = novoH / h;

        Matrix matrix = new Matrix();
        matrix.postScale(scalaW, scalaH);
        novoBmp = Bitmap.createBitmap(bmpOriginal, 0, 0, w, h, matrix, true);

        return novoBmp;
    }


    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    class ConvertBitmapToUri extends Thread {
        Bitmap bitmap;

        public ConvertBitmapToUri(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public void run() {


            handler.post(new Runnable() {
                @Override
                public void run() {


                    // converting bitmap to uri
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    String path = MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), bitmap, "val", null);
                    resultUri = Uri.parse(path);


                }
            });
        }
    }


}


