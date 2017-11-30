package edu.something.ar_framework;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import static org.opencv.core.Core.VERSION;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity {


    //TODO: For checkpoint purposes, check that OpenCV Compiles, log a tag, and print message to UI window
    // Tag used to find OpenCV logging in debug window
    private static final String CVTAG = "OpenCVTag";

    // Get OpenCV version using built-in getVersion(), referenced as "VERSION" in org.opencv.core.Core
    // This version will be outputted to the camera preview when camera onOpened() within CameraDevice.StateCallback()
    private static String version = VERSION;

    // Call OpenCVLoader to determine if OpenCV is compiling, and log the result
    static {
        if (version.equals("3.3.1")) {
            Log.d(CVTAG, "OpenCV successfully compiled!");
        } else {
            Log.d(CVTAG, "OpenCV did not compile correctly...");
        }
    }


    /******************************* Start of Activity Lifecycle *******************************************/


    /**
     * As a user opens, navigates through, leaves, and returns to our application, our Activity instance goes
     * through many different states in what Google calls the Activity Lifecycle. When the Activity transitions
     * into a new state, the Android system invokes the corresponding callback. There are 6 core callbacks,
     * referred to as onCreate(), onStart(), onResume(), onPause(), onStop(), and onDestroy(). Of particular
     * importance to us are onCreate(), onResume(), and onPause(). onCreate is called when the Activity is
     * first launched, meaning that it is not already in memory. This must be implemented, or the application
     * will not run. In this method, we want to perform the actions and logic that should only happen once
     * throughout the lifecycle of the Activity (like initialize objects and variables). The Activity does not
     * stay in this state after completing these tasks. The Activity interacts with the user in the onResume()
     * state. The app stays in this state until something takes the focus away from the app (such as opening
     * another app or going to the home screen). When the application goes to the background like this, the
     * app will transition into the onPause() state. In our case, the onCreate() state will be used to setup
     * the interface between the ASUForia library and the MainActivity, as well as define onPose() to perform
     * the cube drawing. onResume() and onPause() will be used to ensure that pose estimation starts when the
     * app comes to the foreground and ends when the app goes to the background.
     */

    ASUForia asuforia;
    TextureView myTextureView;
    Bitmap bmpIn;

    // Define what happens when the application is first opened
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
        Import reference jpg image as Bitmap (3 lines of code below code copied and pasted from project 1)
         */
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888; // Each pixel is 4 bytes: Alpha, Red, Green, Blue
        bmpIn = BitmapFactory.decodeResource(getResources(), R.drawable.referenceimage, opts);

        //TODO: Define PoseListener object to act as interface between ASUForia library and MainActivity
        final ASUForia.PoseListener myPoseListener = new ASUForia.PoseListener() {

            //TODO: Define PoseListener callback function, onPose() which will use OpenCV to draw cube on image
            public void onPose(Image cameraFrame, float[] rvec, float[] tvec) {

                //TODO: Paint cameraSurface with a cube as an overlay on the marker in the image using OpenCV.

            }

        };
//
        Mat mat = new Mat(bmpIn.getHeight(), bmpIn.getWidth(), CvType.CV_8UC3);
        Utils.bitmapToMat(bmpIn, mat);

        Image refImage = null;
        Surface cameraSurface = null;

        //TODO: Create an ASUForia object
       asuforia = new ASUForia(myPoseListener, refImage, cameraSurface, this);



    }

    // Define what happens when our application enters and stays in the foreground
    @Override
    protected void onResume() {
        // call super constructor in order to apply onResume() to entire Activity
        super.onResume();

        //TODO: Call startEstimation() to setup camera and pass to onImageAvailable to nativePoseEstimation() to onPose()
        // startEstimation() only needs to be called here since onResume() will be called when the application
        // is first opened, and every time the app is brought into the foreground.
        myTextureView = (TextureView) findViewById(R.id.cameraPreview);

        asuforia.startEstimation(myTextureView);
    }


    // Define what happens when app is put in the background
    @Override
    protected void onPause() {

        // Call super constructor to apply to entire activity
        super.onPause();

        //TODO: Call endEstimation() to stop the camera and free resources when camera is not visible
        asuforia.endEstimation();

    }

    /********************************* End of Activity Lifecycle *******************************************/

}