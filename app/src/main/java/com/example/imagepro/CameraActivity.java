package com.example.imagepro;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView translate_button;
    private ImageView take_picture_button;
    private ImageView show_image_button;

    private ImageView current_image;
    private TextView textview;

    // Define TextRecognizer
    private TextRecognizer textRecognizer;
    // Define a string to determine mode
    private String Camera_or_recognizeText = "Camera";
    // Define a bitmap to store current image
    private Bitmap bitmap = null;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV is loaded");
                    mOpenCvCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA = 0;
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // Initialize the TextRecognizer
        textRecognizer = TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());

        textview = findViewById(R.id.textview);
        textview.setVisibility(View.GONE);

        take_picture_button = findViewById(R.id.take_picture_button);
        take_picture_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    take_picture_button.setColorFilter(Color.DKGRAY); // Change color filter on touch down
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (Camera_or_recognizeText.equals("Camera")) {
                        take_picture_button.setColorFilter(null); // Reset color filter
                        Mat a = mRgba.t();
                        Core.flip(a, mRgba, 1);
                        a.release();
                        bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mRgba, bitmap);
                        mOpenCvCameraView.disableView();
                        Camera_or_recognizeText = "recognizeText";
                    } else {
                        take_picture_button.setColorFilter(Color.WHITE); // Reset the color filter
                        textview.setVisibility(View.GONE);
                        current_image.setVisibility(View.GONE);
                        mOpenCvCameraView.enableView();
                        textview.setText(""); // Corrected textView to textview
                        Camera_or_recognizeText = "Camera";
                    }
                    return true;
                }
                return false;
            }
        });

        translate_button = findViewById(R.id.translate_button);
        translate_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    translate_button.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    translate_button.setColorFilter(Color.WHITE); // Reset the color filter
                    if (Camera_or_recognizeText.equals("recognizeText")) {
                        textview.setVisibility(View.VISIBLE);
                        InputImage image = InputImage.fromBitmap(bitmap, 0);
                        @SuppressLint("ClickableViewAccessibility") Task<Text> result = textRecognizer.process(image)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text text) {
                                        textview.setText(text.getText());
                                        Log.d(TAG, "Recognized text: " + text.getText());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Text recognition failed", e);
                                    }
                                });
                    } else {
                        Toast.makeText(CameraActivity.this, "Please take a picture", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });

        show_image_button = findViewById(R.id.show_image_button);
        show_image_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    show_image_button.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    show_image_button.setColorFilter(Color.WHITE); // Reset the color filter
                    if (bitmap != null) {
                        current_image.setImageBitmap(bitmap); // Show the image
                        current_image.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(CameraActivity.this, "No image to show", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "OpenCV is not loaded. Trying again...");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        return mRgba;
    }
}
