package edu.something.ar_framework;

import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;


public class ASUForia extends AppCompatActivity {


    /**
     Load the 'native-lib' library on application startup. This library contains nativePoseEstimation(),
     which is used to get the image features/points and compare them to the reference image
     features/points. It also returns the rotation and translation vectors used in onPose() to draw
     the cube in the camera image.
     */
    static {
        System.loadLibrary("native-lib");
    }

    /*************************************** ASUForia Constructor ***************************************/
    ASUForia(PoseListener listener, Image referenceImage, Surface cameraSurface) {
    }



    /*************************************** Begin startEstimation() ************************************/
    //TODO: Create startEstimation() that will setup and open the camera
    public static void startEstimation() {

    }
    /*************************************** End startEstimation() **************************************/



    /*************************************** Begin onImageAvailable() ***********************************/
    //TODO: Create onImageAvailable() which will be used to pass the image to nativePoseEstimation()


    /*************************************** End onImageAvailable() *************************************/



    /*************************************** Begin endEstimation() **************************************/
    //TODO: Create endEstimation() that will close the camera
    public static void endEstimation() {

    }
    /*************************************** End endEstimation() ****************************************/







    /*************************************** PoseListener Definition ************************************/
    interface PoseListener {
        public void onPose(Image cameraFrame, float[] rvec, float[] tvec);
    }




    /*************************************** nativePoseEstimation Definition ****************************/
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String nativePoseEstimation();
}
