package com.example.solution_color;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;


import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.library.bitmap_utilities.BitMap_Helpers;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    //these are constants and objects that I used, use them if you wish
    private static final String DEBUG_TAG = "CartoonActivity";
    private static final String ORIGINAL_FILE = "origfile.png";
    private static final String PROCESSED_FILE = "procfile.png";

    private static final int TAKE_PICTURE = 1;
    private static final double SCALE_FROM_0_TO_255 = 2.55;
    private static final int DEFAULT_COLOR_PERCENT = 3;
    private static final int DEFAULT_BW_PERCENT = 15;

    //preferences
    OnSharedPreferenceChangeListener listener = this;
    private SharedPreferences myPreference;

    private int saturation = DEFAULT_COLOR_PERCENT;
    private int bwPercent = DEFAULT_BW_PERCENT;
    private String shareSubject;
    private String shareText;

    //where images go
    private String originalImagePath;   //where orig image is
    private String processedImagePath;  //where processed image is
    private Uri outputFileUri;          //tells camera app where to store image
    //used to measure screen size
    int screenheight;
    int screenwidth;

    private ImageView myImage;

    //these guys will hog space
    Bitmap bmpOriginal;                 //original image
    Bitmap bmpThresholded;              //the black and white version of original image
    Bitmap bmpThresholdedColor;         //the colorized version of the black and white image

    // manage all the permissions you need
    private static int CAMERA_AND_STORAGE_PERMISSION = 0;
    private String[] allPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!verifyPermissions())
            requestPermissions();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //dont display these
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        FloatingActionButton fab = findViewById(R.id.buttonTakePicture);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doTakePicture();
            }
        });
        //get the default image
        myImage = (ImageView) findViewById(R.id.imageView1);

        // manage the preferences and the shared preference listenes
//        settings = getSharedPreferences(MY_PREF,MODE_PRIVATE);
        myPreference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        getPrefValues(myPreference);
        myPreference.registerOnSharedPreferenceChangeListener(listener);

        //  and get the values already there getPrefValues(settings);
        // use getPrefValues(SharedPreferences settings)
        // Fetch screen height and width,
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        screenheight = metrics.heightPixels;
        screenwidth = metrics.widthPixels;

        setUpFileSystem();
    }

    private void setImage() {
        //prefer to display processed image if available
        bmpThresholded = Camera_Helpers.loadAndScaleImage(processedImagePath, screenheight, screenwidth);
        if (bmpThresholded != null) {
            myImage.setImageBitmap(bmpThresholded);
            Log.d(DEBUG_TAG, "setImage: myImage.setImageBitmap(bmpThresholded) set");
            return;
        }

        //otherwise fall back to unprocessd photo
        bmpOriginal = Camera_Helpers.loadAndScaleImage(originalImagePath, screenheight, screenwidth);
        if (bmpOriginal != null) {
            myImage.setImageBitmap(bmpOriginal);
            Log.d(DEBUG_TAG, "setImage: myImage.setImageBitmap(bmpOriginal) set");
            return;
        }

        //worst case get from default image
        //save this for restoring
        bmpOriginal = BitMap_Helpers.copyBitmap(myImage.getDrawable());
        Log.d(DEBUG_TAG, "setImage: bmpOriginal copied");
    }

    // use this to set the following member preferences whenever preferences are changed.
    // Please ensure that this function is called by your preference change listener
    private void getPrefValues(SharedPreferences settings) {
        // should track shareSubject, shareText, saturation, bwPercent
//        if (settings == null)
//            settings = getSharedPreferences(MY_PREF,MODE_PRIVATE);
        saturation = settings.getInt("saturation", DEFAULT_COLOR_PERCENT);
        bwPercent = settings.getInt("bw", DEFAULT_BW_PERCENT);
        shareSubject = settings.getString("shareSubject", "Welcome to CNU from the PCSE department!");
        shareText = settings.getString("shareText", "This marvelous colorized image" +
                " was generated by a project from CPSC 475, Android mobile programming. " +
                "Please contact Prof. Keith Perkins at keith.perkins@cnu.edu " +
                "if you have any questions about our department or the classes we offer or technology in general.");

        File photoFile = createImageFile(PROCESSED_FILE);

            if (photoFile != null) {
                processedImagePath = settings.getString("preocessedFile", photoFile.getAbsolutePath());
                outputFileUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.example.solution_color.fileprovider", photoFile);
                bmpOriginal = BitmapFactory.decodeFile(processedImagePath);
            }
        if (bmpOriginal == null){
            bmpOriginal = BitMap_Helpers.copyBitmap(myImage.getDrawable());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private void setUpFileSystem() {
        if (verifyPermissions()) {
            File photoFile = createImageFile(ORIGINAL_FILE);
            originalImagePath = photoFile.getAbsolutePath();
            File processedfile = createImageFile(PROCESSED_FILE);
            processedImagePath = processedfile.getAbsolutePath();

            //worst case get from default image
            //save this for restoring
            if (bmpOriginal == null)
                bmpOriginal = BitMap_Helpers.copyBitmap(myImage.getDrawable());
            setImage();
        }

    }

    private File createImageFile(final String fn) {
        try {
            File[] storageDir = getExternalMediaDirs();

            File imagefile = new File(storageDir[0], fn);

            if (!storageDir[0].exists()) {
                if (!storageDir[0].mkdirs()) {
                    Log.e(DEBUG_TAG, "Failed to create file in: " + storageDir[0]);
                    return null;
                }
            }
            imagefile.createNewFile();

            processedImagePath = imagefile.getAbsolutePath();
            return imagefile;
        } catch(IOException ex){
            // Error occurred while creating the File
            Toast.makeText(this, "IOException occurred", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    //DUMP for students
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // permissions

    /***
     * callback from requestPermissions
     * @param permsRequestCode  user defined code passed to requestpermissions used to identify what callback is coming in
     * @param permissions       list of permissions requested
     * @param grantResults      //results of those requests
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        if (permsRequestCode == CAMERA_AND_STORAGE_PERMISSION) {
            boolean storagePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean cameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if (storagePermission && cameraPermission) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_LONG).show();
            } else if (storagePermission) {
                Toast.makeText(this, "Storage Permission granted, Camera Perrmission Denied", Toast.LENGTH_LONG).show();
            } else if (cameraPermission) {
                Toast.makeText(this, "Camera Permission granted, Storage Permission Denied", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    //DUMP for students

    /**
     * Verify that the specific list of permisions requested have been granted, otherwise ask for
     * these permissions.  Note this is coarse in that I assumme I need them all
     */
    private boolean verifyPermissions() {
        return hasPerrmissions(this, allPermissions);
    }

    private static boolean hasPerrmissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requestPermissions() {
        if (!hasPerrmissions(this, allPermissions)) {
            ActivityCompat.requestPermissions(this, allPermissions, CAMERA_AND_STORAGE_PERMISSION);
        }
    }


    //take a picture and store it on external storage
    public void doTakePicture() {
        // verify that app has permission to use camera
        if (!hasPerrmissions(this, allPermissions)) {
            requestPermissions();
        }
        // manage launching intent to take a picture
        else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (intent.resolveActivity(getPackageManager()) != null) {
                File photoFile = createImageFile(PROCESSED_FILE);
                if (photoFile != null) {
                    outputFileUri = FileProvider.getUriForFile(MainActivity.this,
                            "com.example.solution_color.fileprovider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(intent, TAKE_PICTURE);
                    }
                else
                    Toast.makeText(this,"No camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // manage return from camera and other activities
    //  handle edge cases as well (no pic taken)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // get photo
        // set the myImage equal to the camera image returned
        // tell scanner to pic up this unaltered image
        // save anything needed for later
        if (requestCode == TAKE_PICTURE) {
            if (resultCode == RESULT_OK) {
                bmpOriginal = BitmapFactory.decodeFile(processedImagePath);
                Camera_Helpers.saveProcessedImage(bmpOriginal, originalImagePath);
                Camera_Helpers.saveProcessedImage(bmpOriginal, processedImagePath);
                scanSavedMediaFile(processedImagePath);
                myImage.setImageBitmap(bmpOriginal);
            }
        }
    }

    /**
     * delete original and processed images, then rescan media paths to pick up that they are gone.
     */
    private void doReset() {
        // verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            requestPermissions();
        } else {
            //delete the files
            Camera_Helpers.delSavedImage(originalImagePath);
            Camera_Helpers.delSavedImage(processedImagePath);
            bmpThresholded = null;
            bmpOriginal = null;

            myImage.setImageResource(R.drawable.gutters);
            myImage.setScaleType(ImageView.ScaleType.FIT_CENTER);//what the hell? why both
            myImage.setScaleType(ImageView.ScaleType.FIT_XY);

            //worst case get from default image
            //save this for restoring
            bmpOriginal = BitMap_Helpers.copyBitmap(myImage.getDrawable());

            // make media scanner pick up that images are gone
            scanSavedMediaFile(processedImagePath);
        }
    }

    public void doSketch() {
        // verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            requestPermissions();         // do colorize
        }

        //sketchify the image
        if (bmpOriginal == null) {
            Log.e(DEBUG_TAG, "doSketch: bmpOriginal = null");
            return;
        }

        bmpThresholded = BitMap_Helpers.thresholdBmp(bmpOriginal, bwPercent);

        //set image
        myImage.setImageBitmap(bmpThresholded);

        //save to file for possible email
        Camera_Helpers.saveProcessedImage(bmpThresholded, processedImagePath);
        scanSavedMediaFile(processedImagePath);
    }

    public void doColorize() {
        // verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            requestPermissions();
        }

        //colorize the image
        if (bmpOriginal == null) {
            Log.e(DEBUG_TAG, "doColorize: bmpOriginal = null");
            return;
        }
        //if not thresholded yet then do nothin
        if (bmpThresholded == null) {
            Log.e(DEBUG_TAG, "doColorize: bmpThresholded not thresholded yet");
            return;
        }

        //otherwise color the bitmap

        bmpThresholdedColor = BitMap_Helpers.colorBmp(bmpOriginal, saturation);

        //takes the thresholded image and overlays it over the color one
        //so edges are well defined
        BitMap_Helpers.merge(bmpThresholdedColor, bmpThresholded);

        //set background to new image
        myImage.setImageBitmap(bmpThresholdedColor);

        //save to file for possible email
        Camera_Helpers.saveProcessedImage(bmpThresholdedColor, processedImagePath);
        scanSavedMediaFile(processedImagePath);
    }

    public void doShare() {
        // verify that app has permission to use file system
        //do we have needed permissions?
        if (!verifyPermissions()) {
            requestPermissions();
        }
        // share the processed image with appropriate subject, text and file URI
        // the subject and text should come from the preferences set in the Settings Activity
        else {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            if (outputFileUri != null) {
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, outputFileUri);
            }
            startActivity(shareIntent);
        }
    }

    // set this up
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle all of the appbar button clicks
        switch (item.getItemId()) {
            case R.id.action_colorize: {
                doColorize();
                return true;
            }
            case R.id.action_sketch: {
                doSketch();
                return true;
            }
            case R.id.action_reset: {
                doReset();
                return true;
            }
            case R.id.action_share: {
                doShare();
                return true;
            }
            case R.id.action_settings: {
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        Map<String, ?> all = pref.getAll();
        Object value = all.get(key);

        if (key.equals("saturation_bar")) {
            Toast.makeText(this, "Handle Key = " + key + "Value =" + String.valueOf(value), Toast.LENGTH_LONG).show();
            saturation = Integer.valueOf(String.valueOf(value));
        }
        else if (key.equals("sketchiness_bar")){
            Toast.makeText(this, "Handle Key = " + key + "Value =" + String.valueOf(value), Toast.LENGTH_LONG).show();
            bwPercent = Integer.valueOf(String.valueOf(value));
        }
        else if (key.equals("shareSubject"))
            shareSubject = String.valueOf(value);
        else if (key.equals("shareText"))
            shareText = String.valueOf(value);

    }

        /**
         * Notifies the OS to index the new image, so it shows up in Gallery.
         * see https://www.programcreek.com/java-api-examples/index.php?api=android.media.MediaScannerConnection
         */
        private void scanSavedMediaFile ( final String path){
            // silly array hack so closure can reference scannerConnection[0] before it's created
            final MediaScannerConnection[] scannerConnection = new MediaScannerConnection[1];
            try {
                MediaScannerConnection.MediaScannerConnectionClient scannerClient = new MediaScannerConnection.MediaScannerConnectionClient() {
                    public void onMediaScannerConnected() {
                        scannerConnection[0].scanFile(path, null);
                    }

                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }

                };
                scannerConnection[0] = new MediaScannerConnection(this, scannerClient);
                scannerConnection[0].connect();
            } catch (Exception ignored) {
            }
        }
    }

