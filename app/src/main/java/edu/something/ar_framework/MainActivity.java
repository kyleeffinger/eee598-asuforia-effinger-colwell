package edu.something.ar_framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.util.AttributeSet;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.android.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.core.Core.VERSION;
import static org.opencv.core.Core.VERSION_MAJOR;

import edu.something.ar_framework.ASUForia;

public class MainActivity extends AppCompatActivity {


    //TODO: For checkpoint purposes, check that OpenCV Compiles, log a tag, and print message to UI window
    // Tag used to find OpenCV logging in debug window
    private static final String CVTAG = "OpenCVTag";

    // Call OpenCVLoader to determine if OpenCV is compiling, and log the result
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(CVTAG, "OpenCV successfully compiled!");
        } else {
            Log.d(CVTAG, "OpenCV did not compile correctly...");
        }
    }

    // Get OpenCV version using built-in getVersion(), referenced as "VERSION" in org.opencv.core.Core
    // This version will be outputted to the camera preview when camera onOpened() within CameraDevice.StateCallback()
    private static String version = VERSION;


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

    // Define what happens when the application is first opened
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: Define PoseListener object to act as interface between ASUForia library and MainActivity
        final ASUForia.PoseListener myPoseListener = new ASUForia.PoseListener() {

            //TODO: Define PoseListener callback function, onPose() which will use OpenCV to draw cube on image
            public void onPose(Image cameraFrame, float[] rvec, float[] tvec) {

                //TODO: Paint cameraSurface with a cube as an overlay on the marker in the image using OpenCV.

            }

        };

        Image refImage = null;
        Surface cameraSurface = null;

        //TODO: Create an ASUForia object
        ASUForia asuForia = new ASUForia(myPoseListener, refImage, cameraSurface);


    }

    // Define what happens when our application enters and stays in the foreground
    @Override
    protected void onResume() {
        // call super constructor in order to apply onResume() to entire Activity
        super.onResume();

        //TODO: Call startEstimation() to setup camera and pass to onImageAvailable to nativePoseEstimation() to onPose()
        // startEstimation() only needs to be called here since onResume() will be called when the application
        // is first opened, and every time the app is brought into the foreground.

        ASUForia asuForia = new ASUForia();
        asuForia.startEstimation();

    }


    // Define what happens when app is put in the background
    @Override
    protected void onPause() {

        // Call super constructor to apply to entire activity
        super.onPause();

        //TODO: Call endEstimation() to stop the camera and free resources when camera is not visible
        ASUForia asuForia = new ASUForia();
        asuForia.endEstimation();

    }

    /********************************* End of Activity Lifecycle *******************************************/

}