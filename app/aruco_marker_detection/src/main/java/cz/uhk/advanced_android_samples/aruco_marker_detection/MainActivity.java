package cz.uhk.advanced_android_samples.aruco_marker_detection;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4;

public class MainActivity extends Activity implements OnTouchListener {
    private static final String TAG = "AAS_markerDetection";

    private CameraBridgeViewBase viewOpenCv;
    private MyCvCameraViewListener openCVCameraListener;
    private AlertDialog ad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // nastavení OpenCV view
        viewOpenCv = findViewById(R.id.opencv_surface_view);
        viewOpenCv.setVisibility(SurfaceView.VISIBLE);
        openCVCameraListener = new MyCvCameraViewListener(this);
        viewOpenCv.setCvCameraViewListener(openCVCameraListener);
    }

    public boolean checkPermissions(){

        List<String> neededPermissions = new ArrayList<>();
        String[] permissions = new String [] {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        boolean displayDialog= false;

        for (String permission : permissions) {
            // chybějící oprávnění
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Chybí práva: " + permission);
                // oprávnění požadováno poprvé nebo bylo odmítnuto se zaškrtnutou možností "Do not ask again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                    boolean firstTimeRequest;
                    SharedPreferences sp = getSharedPreferences("AAS_marker_detection_prefferences", Activity.MODE_PRIVATE);
                    firstTimeRequest = sp.getBoolean("firstTimeRequest" + permission, true);
                    // první požadavak
                    if (firstTimeRequest) {
                        neededPermissions.add(permission);
                        sp = getSharedPreferences("AAS_marker_detection_prefferences", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean("firstTimeRequest" + permission, false);
                        editor.apply();
                    }
                    // požadavek odmítnut s možností "Do no ask again"
                    else {
                        // zobrazit dialog pro manuální poskytnutí oprávnění
                        displayDialog = true;
                    }
                }
                // požadavek byl dříve odmítnut
                else {
                    neededPermissions.add(permission);
                }

                // vyslání požadavku na všechna odmítnutá práva
                if (!neededPermissions.isEmpty()) {
                    String[] perms = new String[neededPermissions.size()];
                    for (int j = 0; j < neededPermissions.size(); j++) {
                        perms[j] = neededPermissions.get(j);
                    }
                    ActivityCompat.requestPermissions(MainActivity.this, perms, 1000);
                }

                // zobrazení dialogu odkazující do nastavení aplikace
                if (displayDialog) {
                    ad = new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Prosím manuálně poskytněte aplikaci požadovaná oprávnění: Kamera, Uložiště ")
                            .setPositiveButton("Přejít do nastavení",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            intent.setData(uri);
                                            startActivity(intent);
                                        }

                                    }).create();
                    ad.show();
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == 1000) {
            // požadavek poskytnutí oprávnění byl odmítnut
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // Ukončení aplikace
                finish();
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (viewOpenCv != null)
            viewOpenCv.disableView();
        if(ad !=null)
            ad.dismiss();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(checkPermissions()){
            if (!OpenCVLoader.initDebug()) {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
            } else {
                baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (viewOpenCv != null)
            viewOpenCv.disableView();
        if(ad !=null)
            ad.dismiss();
    }

    public boolean onTouch(View v, MotionEvent event) {
        Toast.makeText(this, "Vzdálenost značky: " + String.format("%.2f", getDistance()) + "cm", Toast.LENGTH_SHORT).show();
        return false;
    }

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "Knihovna OpenCV úspěšně načtena");
                    viewOpenCv.enableView();
                    viewOpenCv.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    // funkce získá pohledovou matici OpenCV kamery
    public double getDistance(){
        Mat4 vMat = openCVCameraListener.getViewMatrix();

        // vzdálenost bodu od počátku [0,0,0]
        return Math.sqrt(Math.pow(vMat.get(3,0), 2)
                + Math.pow(vMat.get(3,1), 2)
                + Math.pow(vMat.get(3,2), 2))*10;
    }

}
