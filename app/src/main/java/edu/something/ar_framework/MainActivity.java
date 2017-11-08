package edu.something.ar_framework;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private TextureView myTextureView;

    private final TextureView.SurfaceTextureListener mySurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            // When SurfaceTextureView is available, pass width and height to setupCamera(), which
            // contacts the camera service
            setupCamera(width, height);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };


    private CameraDevice myCameraDevice;

    private final CameraDevice.StateCallback myStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // Use returned cameraDevice corresponding to specified camera
            // by assigning to our created CameraDevice object
            myCameraDevice = cameraDevice;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            // Free up CameraDevice resources when not connected to specified camera
            cameraDevice.close();
            myCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            // Free up CameraDevice resources when not connected to specified camera due to error
            cameraDevice.close();
            myCameraDevice = null;
        }
    };


    private final ImageReader.OnImageAvailableListener myImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {

        }
    };

    private String mCameraID;

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        // Get a list of cameras contained in device
        try {
            // Traverse through all available cameraID's on device
            for (String cameraID : cameraManager.getCameraIdList()){
                // Get characteristics of each cameraID
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraID);
                // Only want to use rear facing camera - if front camera is selected first, skip it
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                // If cameraID is rear facing, then set current cameraID to our cameraId for use
                // and return out of method
                mCameraID = cameraID;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myTextureView = (TextureView) findViewById(R.id.textureview);

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
