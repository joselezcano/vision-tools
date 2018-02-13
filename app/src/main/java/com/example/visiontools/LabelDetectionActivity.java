package com.example.visiontools;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

public class LabelDetectionActivity extends AppCompatActivity {

    private static final String CLOUD_VISION_API_KEY = "<put your Vision API key here>";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int maximumDimension = 1024;
    private String mCurrentPhotoPath;
    private ProgressBar mProgress;
    private TextView mTextViewProgress, mLabelsDetectedTextView, mAnnotationTextView;
    private DrawingImageView pictureImageView;
    private int bitmapHeight, bitmapWidth;
    private String labelAnnotation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_detection);
        mLabelsDetectedTextView = (TextView) findViewById(R.id.labelsDetectedTextView);
        mAnnotationTextView = (TextView) findViewById(R.id.labelsTextView);
        FloatingActionButton cameraFloatingActionButton = (FloatingActionButton) findViewById(R.id.labelCameraFloatingActionButton);
        cameraFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear previous detection results if present.
                pictureImageView.setImageDrawable(null);
                mLabelsDetectedTextView.setVisibility(View.GONE);
                mAnnotationTextView.setVisibility(View.GONE);
                dispatchTakePictureIntent();
            }
        });
        // Add empty image view
        pictureImageView = new DrawingImageView(this);
        pictureImageView.setAdjustViewBounds(true);
        LinearLayout layout = (LinearLayout) findViewById(R.id.labelImageViewLinearLayout);
        layout.addView(pictureImageView,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pictureImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pictureImageView.setPadding(0, 0, 0, 0);
        pictureImageView.setBackgroundColor(Color.WHITE);
    }

    // Call on activity result (called after picture intent)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Show progress bar while label detection is happening
            mProgress = (ProgressBar) findViewById(R.id.labelProgressBar);
            mProgress.setVisibility(View.VISIBLE);
            mTextViewProgress = (TextView) findViewById(R.id.labelProgressTextView);
            mTextViewProgress.setVisibility(View.VISIBLE);
            mTextViewProgress.setText("Label detection in progress ...");
            // Start label detection in a separate thread using AsyncTask
            new LabelDetection().execute();
        }
    }

    // Dispatch picture intent
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                System.out.println("ERROR: there was an IO Exception while creating the picture file");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                // NOTE: We are generating a content URI instead of a file URI for Android security reasons (changed in latest versions of Android, previously was Uri.fromFile(photoFile))
                Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), "com.example.visiontools.fileprovider", photoFile);
                // Add extra URI to save full picture
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                // Start photo activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                System.out.println("ERROR: Photo file is null");
            }
        }
    }

    // Create photo URI (and JPG directory, if it does not exist)
    private File createImageFile() throws IOException {
        // Create an image file name (we'll save the picture into the same file and save memory)
        String imageFileName = "label-detection";
        File imageFile = null;
        File folder = new File(getFilesDir() + File.separator + "jpg");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (success) {
            // Get storage directory
            File storageDir = new File(getFilesDir() + File.separator + "jpg");
            // Create file
            imageFile = File.createTempFile(
                    imageFileName,  // prefix
                    ".jpg",         // suffix
                    storageDir      // directory
            );
            // Absolute paths for different image formats
            mCurrentPhotoPath = imageFile.getAbsolutePath();
        }
        return imageFile;
    }

    // AsyncTask to upload the picture and perform detection in a background thread
    private class LabelDetection extends AsyncTask<Void, Void, String> {

        Bitmap bitmap = null;

        protected String doInBackground(Void... params) {
            // Initialize detection request to be sent to Google Vision API
            VisionRequestInitializer requestInitializer =
                    new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                        /**
                         * We override this so we can inject important identifying fields into the HTTP
                         * headers. This enables use of a restricted cloud platform API key.
                         */
                        @Override
                        protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                throws IOException {
                            super.initializeVisionRequest(visionRequest);
                            String packageName = getPackageName();
                            visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);
                            String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);
                            visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                        }
                    };
            Vision.Builder visionBuilder = new Vision.Builder(
                    new NetHttpTransport(),
                    new AndroidJsonFactory(),
                    null);
            visionBuilder.setVisionRequestInitializer(requestInitializer);
            Vision vision = visionBuilder.build();
            String labels = "";
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mCurrentPhotoPath, options);
                bitmapHeight = options.outHeight;
                bitmapWidth = options.outWidth;
                int maxDim = Math.max(bitmapHeight,bitmapWidth);
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options);
                // Reduce maximum dimension of the picture so that uploading and detection will be quick
                if(maxDim > maximumDimension) {
                    bitmap = scaleBitmapDown(bitmap, maximumDimension);
                }
                // Convert the bitmap to a JPEG
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                byte[] photoData = byteArrayOutputStream.toByteArray();
                Image inputImage = new Image();
                inputImage.encodeContent(photoData);
                // Set the desired detection that Vision API will perform
                Feature desiredFeature = new Feature();
                desiredFeature.setType("LABEL_DETECTION");
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature));
                BatchAnnotateImagesRequest batchRequest =
                        new BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));
                Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                // Due to a bug: requests to Vision API containing large images fail when GZipped.
                annotateRequest.setDisableGZipContent(true);
                BatchAnnotateImagesResponse batchResponse = annotateRequest.execute();
                int index = 0;
                for (EntityAnnotation annotation : batchResponse.getResponses()
                        .get(0).getLabelAnnotations()) {
                    labels += "Label " + String.valueOf(index) + "\n";
                    labels += "description: " + annotation.getDescription() + "\n";
                    labels += "score: " + annotation.getScore() + "\n\n";
                    index++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return labels;
        }

        protected void onProgressUpdate(Void... progress) {
        }

        protected void onPostExecute(final String labels) {
            if(bitmap != null && labels.length() > 0) {
                labelAnnotation = labels;
                mTextViewProgress.setVisibility(View.GONE);
                mProgress.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Detection completed!", Toast.LENGTH_LONG).show();
                pictureImageView.setImageBitmap(bitmap);
                mLabelsDetectedTextView.setVisibility(View.VISIBLE);
                mAnnotationTextView.setVisibility(View.VISIBLE);
                mAnnotationTextView.setText(labels);
            } else {
                mTextViewProgress.setText("There was an error. Please try again.");
                mProgress.setVisibility(View.GONE);
                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        mTextViewProgress.setVisibility(View.GONE);
                    }
                }, 3000);
            }
        }
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        bitmapWidth = resizedWidth;
        bitmapHeight = resizedHeight;
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

}
