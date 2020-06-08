package com.ubiquid.ubiquidtest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Display;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Activity used to display camera image and detect barcodes through two modes: normal and evaluation
 * Results from the two modes are displayed in two different activities
 */
public class MainActivity extends AppCompatActivity {

    public static final int CAMERA_PERMISSION_REQUEST_CODE = 1839;
    public static final String CODE_CONTENT = "cct";
    public static final String CODE_QR_NUMBER = "cqn";
    public static final String CODE_DATA_MATRIX_NUMBER = "cdmn";

    private SurfaceView mSurfaceView;
    private CameraSource mCameraSource;
    private BarcodeDetector mBarcodeDetector;
    private boolean launching_activity = false;

    private BottomNavigationView bottomNavigationView;
    private List<String> detected_codes;
    private int codeQRCount;
    private int dataMatrixCount;
    private TextView codeCountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean askedPermission = askPermissionsIfNeeded();

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigationView);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        codeCountTextView = (TextView) findViewById(R.id.codeCount);

        //Creating a barcodeDetector to detect QR codes and data matrix
        mBarcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.QR_CODE | Barcode.DATA_MATRIX)
                .build();
        if (!mBarcodeDetector.isOperational()) {
            Toast.makeText(getApplicationContext(), "Barcode is not operational", Toast.LENGTH_LONG).show();
            finish();
        }

        //Setting processor to determine what to do when detecting codes
        changeToSimpleDetectorProcessor();

        //Linking camera to the barcode
        mCameraSource = new CameraSource.Builder(getApplicationContext(), mBarcodeDetector).setAutoFocusEnabled(true).build();

        //Linking camera to the surface view
        if(!askedPermission)
            buildLayout();


    }

    @Override
    protected void onResume() {
        super.onResume();

        //No other activity is running, setting variable back to false
        launching_activity= false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //If focused, setting the activity to be immersive and fullscreen
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    /**
     * Ask permission to access camera if it was not granted
     */
    private boolean askPermissionsIfNeeded() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length >0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
            buildLayout();
        }
        else if(requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            Toast.makeText(getApplicationContext(), "You need to give camera permission to use this app", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Change the barcodeDetector processor to open an activity and display its value when a QRCode is detected
     */
    private void changeToSimpleDetectorProcessor() {
        if(mBarcodeDetector!=null && mBarcodeDetector.isOperational()) {
            codeCountTextView.setVisibility(View.GONE);
            mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<Barcode> detections) {
                    SparseArray<Barcode> codesDetected = detections.getDetectedItems();
                    if (codesDetected.size() > 0  && !launching_activity) {
                        launching_activity = true;
                        Intent i = new Intent(getApplicationContext(), DisplayCodeValuesActivity.class);
                        String value = codesDetected.valueAt(0).displayValue;
                        i.putExtra(CODE_CONTENT, value);
                        startActivity(i);
                    }
                }
            });
        }
    }

    /**
     * Change the barcodeDetector processor to register every QRCode detected and only scan new ones
     */
    private void changeToEvaluatingDetectorProcessor() {
        if(mBarcodeDetector!=null && mBarcodeDetector.isOperational()) {
            detected_codes = new ArrayList<>();
            codeCountTextView.setText("0");
            codeCountTextView.setVisibility(View.VISIBLE);
            codeQRCount = 0;
            dataMatrixCount = 0;
            mBarcodeDetector.setProcessor(new Detector.Processor<Barcode>() {

                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<Barcode> detections) {
                    SparseArray<Barcode> codesDetected = detections.getDetectedItems();
                    if (codesDetected.size() > 0) {
                        for(int i = 0; i<codesDetected.size(); i++) {
                            if(!detected_codes.contains(codesDetected.valueAt(i).displayValue)) {
                                detected_codes.add(codesDetected.valueAt(i).displayValue);
                                if(codesDetected.valueAt(i).format==Barcode.QR_CODE)
                                    codeQRCount++;
                                else
                                    dataMatrixCount++;

                                new Handler(getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        blinkGreen();
                                        notifSound();
                                    }
                                });
                            }
                        }
                        codeCountTextView.setText(Integer.toString(detected_codes.size()));
                    }
                }
            });
        }
    }

    /**
     * Prepare the surfaceView and set the bottomNavigationView Listener when an item is selected
     */
    private void buildLayout() {

        //Configuring surfaceview to display camera source
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "You need to give camera permission to use this app", Toast.LENGTH_LONG).show();
                }
                else {
                    try {
                        mCameraSource.start(holder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }
        });

        //Configuring surfaceview to use optimal size from camera resolution
        Size optimalSurfaceSize = getOptimalSurfaceSize();
        if(optimalSurfaceSize!=null)
            mSurfaceView.setLayoutParams(new ConstraintLayout.LayoutParams(optimalSurfaceSize.getWidth(), optimalSurfaceSize.getHeight()));

        //Setting the navigationView callback to change mode
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()){
                    case R.id.code_reading:
                        if(!item.isChecked()) {
                            changeToSimpleDetectorProcessor();
                            bottomNavigationView.getMenu().getItem(0).setChecked(true);
                        }
                        break;

                    case R.id.evaluate_detection:
                        if(!item.isChecked()) {
                            launching_activity = true;  //Prevent the app to change activity during the dialog
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                            alertDialog.setTitle("Lancement de l'évaluation");
                            alertDialog.setMessage("Vous êtes sur le point de lancer une évaluation.\nVous aurez 15 secondes pour scanner le plus de QR code avant d'être redirigé vers les résultats du test.");
                            //Function called to initiate the evaluation mode
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    bottomNavigationView.getMenu().getItem(1).setChecked(true);

                                    changeToEvaluatingDetectorProcessor();

                                    //Preparing the runnable to use after 15s to display the results
                                    new Handler(getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent i = new Intent(getApplicationContext(), EvaluationDisplayActivity.class);
                                            i.putExtra(CODE_CONTENT, detected_codes.toArray(new String[0]));
                                            i.putExtra(CODE_QR_NUMBER, codeQRCount);
                                            i.putExtra(CODE_DATA_MATRIX_NUMBER, dataMatrixCount);

                                            changeToSimpleDetectorProcessor();

                                            startActivity(i);
                                            bottomNavigationView.getMenu().getItem(0).setEnabled(true);
                                            bottomNavigationView.getMenu().getItem(1).setEnabled(true);
                                            bottomNavigationView.getMenu().getItem(0).setChecked(true);
                                        }
                                    }, 15000);
                                    //Disabling the navigationView to avoid changing mode during the evaluation
                                    bottomNavigationView.getMenu().getItem(0).setEnabled(false);
                                    bottomNavigationView.getMenu().getItem(1).setEnabled(false);
                                }
                            });
                            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "ANNULER", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    launching_activity = false; //No need to prevent changing activity anymore
                                    dialog.dismiss();
                                }
                            });
                            alertDialog.show();
                        }
                        break;
                }
                return false;
            }
        });
    }

    /**
     * Getting back camera resolution from camera manager
     * @return  Size cameraSize - null if not available
     */
    private Size getCameraSize() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    Size size = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    return size;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Getting screen size from default display
     * @return  Size screenSize
     */
    private Size getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return new Size(size.x, size.y);
    }

    /**
     * Compute optimal size of the surface view to get a non-distorted image from the camera
     * @return  Size optimalSurfaceSize
     */
    private Size getOptimalSurfaceSize() {
        Size cameraSize = getCameraSize();
        Size screenSize = getScreenSize();
        Size optimalSurfaceSize = null;
        //Can't continue if the camera size was not available
        if(screenSize == null)
            return null;

        Log.d("SVSize", "CameraSize: " + cameraSize.getWidth() + "w " +  cameraSize.getHeight() + "h");
        Log.d("SVSize", "ScreenSize: " + screenSize.getWidth() + "w " +  screenSize.getHeight() + "h");

        int optimalWidth, optimalHeight;
        float ratio;

        //Getting display ratio from camera
        if(cameraSize.getWidth() > cameraSize.getHeight())
            ratio = ((float)cameraSize.getWidth())/cameraSize.getHeight();
        else
            ratio = ((float)cameraSize.getHeight())/cameraSize.getWidth();

        //Computing optimal size from ratio
        if(screenSize.getWidth() > screenSize.getHeight())
            optimalSurfaceSize = new Size(screenSize.getHeight(), (int)(screenSize.getHeight()*ratio));
        else
            optimalSurfaceSize = new Size(screenSize.getWidth(), (int)(screenSize.getWidth()*ratio));

        Log.d("SVSize", "LayoutSize: " + optimalSurfaceSize.getWidth() + "w " +  optimalSurfaceSize.getHeight() + "h");

        return optimalSurfaceSize;
    }

    /**
     * Used to play a notification sound when a code is detected during evaluation mode
     */
    private void notifSound() {
        RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play();
    }

    /**
     * Used to make the background blink in green when a code is detected during evaluation mode
     */
    private void blinkGreen() {
        ConstraintLayout lyt = (ConstraintLayout) findViewById(R.id.mainLayout);
        ObjectAnimator anim = ObjectAnimator.ofInt(lyt, "backgroundColor", Color.BLACK, Color.GREEN, Color.BLACK);
        anim.setDuration(300);
        anim.setEvaluator(new ArgbEvaluator());
        anim.setRepeatCount(1);
        anim.start();
    }
}