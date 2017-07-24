package com.ahmednts.googlevisiontest;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.ahmednts.googlevisiontest.camera.CameraSourcePreview;
import com.ahmednts.googlevisiontest.camera.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;
import java.io.IOException;
import java.io.InputStream;

public class FaceTrackingActivity extends AppCompatActivity {
  private static final String TAG = FaceTrackingActivity.class.getSimpleName();

  private static final int RC_HANDLE_GMS = 9001;

  // permission request codes need to be < 256
  private static final int RC_HANDLE_CAMERA_PERM = 2;

  private CameraSource mCameraSource = null;
  private CameraSourcePreview mPreview;
  private GraphicOverlay mGraphicOverlay;

  private boolean mIsFrontFacing = true;

  private Bitmap[] mBitmaps = new Bitmap[2];

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_face_tracking);

    mPreview = (CameraSourcePreview) findViewById(R.id.preview);
    mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

    InputStream stream = getResources().openRawResource(R.raw.image_lion);
    Bitmap bitmap = BitmapFactory.decodeStream(stream);
    stream = getResources().openRawResource(R.raw.image_hat);
    Bitmap hat = BitmapFactory.decodeStream(stream);
    mBitmaps[0] = bitmap;
    mBitmaps[1] = hat;

    try {
      stream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Check for the camera permission before accessing the camera.  If the
    // permission is not granted yet, request permission.
    int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
    if (rc == PackageManager.PERMISSION_GRANTED) {
      createCameraSource();
    } else {
      requestCameraPermission();
    }
  }



  @Override
  protected void onResume() {
    super.onResume();

    startCameraSource();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mPreview.stop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mCameraSource != null) {
      mCameraSource.release();
    }
  }

  private void requestCameraPermission() {
    Log.w(TAG, "Camera permission is not granted. Requesting permission");

    final String[] permissions = new String[] { Manifest.permission.CAMERA };

    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
      ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
      return;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode != RC_HANDLE_CAMERA_PERM) {
      Log.d(TAG, "Got unexpected permission result: " + requestCode);
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      return;
    }

    if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, "Camera permission granted - initialize the camera source");
      // we have permission, so create the camerasource
      createCameraSource();
      return;
    }

    Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        finish();
      }
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Face Tracker sample")
        .setMessage(R.string.no_camera_permission)
        .setPositiveButton(R.string.ok, listener)
        .show();
  }

  @NonNull
  private FaceDetector createFaceDetector(Context context) {
    // For both front facing and rear facing modes, the detector is initialized to do landmark
    // detection (to find the eyes), classification (to determine if the eyes are open), and
    // tracking.
    //
    // Use of "fast mode" enables faster detection for frontward faces, at the expense of not
    // attempting to detect faces at more varied angles (e.g., faces in profile).  Therefore,
    // faces that are turned too far won't be detected under fast mode.
    //
    // For front facing mode only, the detector will use the "prominent face only" setting,
    // which is optimized for tracking a single relatively large face.  This setting allows the
    // detector to take some shortcuts to make tracking faster, at the expense of not being able
    // to track multiple faces.
    //
    // Setting the minimum face size not only controls how large faces must be in order to be
    // detected, it also affects performance.  Since it takes longer to scan for smaller faces,
    // we increase the minimum face size for the rear facing mode a little bit in order to make
    // tracking faster (at the expense of missing smaller faces).  But this optimization is less
    // important for the front facing case, because when "prominent face only" is enabled, the
    // detector stops scanning for faces after it has found the first (large) face.
    FaceDetector detector =
        new FaceDetector.Builder(context).setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .setTrackingEnabled(true)
            .setMode(FaceDetector.FAST_MODE)
            .setProminentFaceOnly(mIsFrontFacing)
            .setMinFaceSize(mIsFrontFacing ? 0.35f : 0.15f)
            .build();

    Detector.Processor<Face> processor;
    if (mIsFrontFacing) {
      // For front facing mode, a single tracker instance is used with an associated focusing
      // processor.  This configuration allows the face detector to take some shortcuts to
      // speed up detection, in that it can quit after finding a single face and can assume
      // that the nextIrisPosition face position is usually relatively close to the last seen
      // face position.
      Tracker<Face> tracker = new FaceTracker(mGraphicOverlay, mBitmaps);
      processor = new LargestFaceFocusingProcessor.Builder(detector, tracker).build();
    } else {
      // For rear facing mode, a factory is used to create per-face tracker instances.  A
      // tracker is created for each face and is maintained as long as the same face is
      // visible, enabling per-face state to be maintained over time.  This is used to store
      // the iris position and velocity for each face independently, simulating the motion of
      // the eyes of any number of faces over time.
      //
      // Both the front facing mode and the rear facing mode use the same tracker
      // implementation, avoiding the need for any additional code.  The only difference
      // between these cases is the choice of Processor: one that is specialized for tracking
      // a single face or one that can handle multiple faces.  Here, we use MultiProcessor,
      // which is a standard component of the mobile vision API for managing multiple items.
      MultiProcessor.Factory<Face> factory = new MultiProcessor.Factory<Face>() {
        @Override
        public Tracker<Face> create(Face face) {
          return new FaceTracker(mGraphicOverlay, mBitmaps);
        }
      };
      processor = new MultiProcessor.Builder<>(factory).build();
    }

    detector.setProcessor(processor);

    if (!detector.isOperational()) {
      // Note: The first time that an app using face API is installed on a device, GMS will
      // download a native library to the device in order to do detection.  Usually this
      // completes before the app is run for the first time.  But if that download has not yet
      // completed, then the above call will not detect any faces.
      //
      // isOperational() can be used to check if the required native library is currently
      // available.  The detector will automatically become operational once the library
      // download completes on device.
      Log.w(TAG, "Face detector dependencies are not yet available.");

      // Check for low storage.  If there is low storage, the native library will not be
      // downloaded, so detection will not become operational.
      IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
      boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

      if (hasLowStorage) {
        Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
        Log.w(TAG, getString(R.string.low_storage_error));
      }
    }
    return detector;
  }

  //==============================================================================================
  // Camera Source
  //==============================================================================================

  /**
   * Creates the face detector and the camera.
   */
  private void createCameraSource() {
    Context context = getApplicationContext();
    FaceDetector detector = createFaceDetector(context);

    int facing = CameraSource.CAMERA_FACING_FRONT;
    if (!mIsFrontFacing) {
      facing = CameraSource.CAMERA_FACING_BACK;
    }

    // The camera source is initialized to use either the front or rear facing camera.  We use a
    // relatively low resolution for the camera preview, since this is sufficient for this app
    // and the face detector will run faster at lower camera resolutions.
    //
    // However, note that there is a speed/accuracy trade-off with respect to choosing the
    // camera resolution.  The face detector will run faster with lower camera resolutions,
    // but may miss smaller faces, landmarks, or may not correctly detect eyes open/closed in
    // comparison to using higher camera resolutions.  If you have any of these issues, you may
    // want to increase the resolution.
    mCameraSource = new CameraSource.Builder(context, detector).setFacing(facing)
        .setRequestedPreviewSize(320, 240)
        .setRequestedFps(60.0f)
        .setAutoFocusEnabled(true)
        .build();
  }

  private void startCameraSource() {
    // check that the device has play services available.
    int code =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
    if (code != ConnectionResult.SUCCESS) {
      Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
      dlg.show();
    }

    if (mCameraSource != null) {
      try {
        mPreview.start(mCameraSource, mGraphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        mCameraSource.release();
        mCameraSource = null;
      }
    }
  }
}
