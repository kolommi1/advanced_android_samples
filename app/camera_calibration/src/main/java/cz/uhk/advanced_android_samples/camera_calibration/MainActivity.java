package cz.uhk.advanced_android_samples.camera_calibration;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.CharucoBoard;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Size;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
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

import static org.opencv.aruco.Aruco.DICT_4X4_50;
import static org.opencv.aruco.Aruco.getPredefinedDictionary;

public class MainActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG = "AAS_camera_calibration";

    private Mat cameraFrameGray;
    private CameraBridgeViewBase viewOpenCv;

    private Dictionary dictionary;
    private CharucoBoard charucoboard;
    private List<List<Mat>>  allCorners;
    private List<Mat> allIds;
    private List<Mat> allImgs;
    private Size cameraFrameSize;
    private Mat cameraMatrix, distCoeffs;
    private List<Mat>  rvecs , tvecs;
    private long firstTouchTime;
    private Mat ids;
    private List<Mat> corners, rejected;
    private AlertDialog ad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        viewOpenCv = findViewById(R.id.main_activity_surface_view);
        viewOpenCv.setVisibility(SurfaceView.VISIBLE);
        viewOpenCv.setCvCameraViewListener(this);
    }

    public boolean checkPermissions(){

        List<String> neededPermissions = new ArrayList<>();
        String[] permissions = new String [] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        boolean displayDialog= false;

        for (String permission : permissions) {
            // chybějící oprávnění
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Chybí práva: " + permission);
                // oprávnění požadováno poprvé nebo bylo odmítnuto se zaškrtnutou možností "Do not ask again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                    boolean firstTimeRequest;
                    SharedPreferences sp = getSharedPreferences("AAS_calibration_prefferences", Activity.MODE_PRIVATE);
                    firstTimeRequest = sp.getBoolean("firstTimeRequest" + permission, true);
                    // první požadavak
                    if (firstTimeRequest) {
                        neededPermissions.add(permission);
                        sp = getSharedPreferences("AAS_calibration_prefferences", Activity.MODE_PRIVATE);
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
                    ad = new AlertDialog.Builder(MainActivity.this).setMessage("Prosím manuálně poskytněte aplikaci požadovaná oprávnění: Kamera, Uložiště ").setPositiveButton("Přejít do nastavení",
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
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
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

    public void onCameraViewStarted(int width, int height) {
        // inicializace proměnných (při inicializaci v onCameraFrame vznikají úniky paměti)

        cameraFrameGray = new Mat(height, width, CvType.CV_8UC4);
        // slovník obsahující znaky pro detekci Aruco
        dictionary = getPredefinedDictionary(DICT_4X4_50);
        // kalibrační deska - nutno vytisknout
        charucoboard = CharucoBoard.create(5, 7, 0.03f, 0.022f, dictionary);

        allCorners = new ArrayList<>();
        allIds = new ArrayList<>();
        allImgs = new ArrayList<>();
        cameraFrameSize = new Size(width, height);
        cameraMatrix = new Mat();
        distCoeffs = new Mat();
        rvecs = new ArrayList<>();
        tvecs = new ArrayList<>();
        ids = new Mat();
        corners = new ArrayList<>();
        rejected = new ArrayList<>();
    }

    public void onCameraViewStopped() {
        cameraFrameGray.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // detekce značek Aruco/charuco probíhá v černobílém obraze
        cameraFrameGray = inputFrame.gray();
        DetectorParameters detectorParams = DetectorParameters.create();

        // detekce aruco značek
        // funkce naplní parametry "corners, ids, rejected" daty detekovaných aruco značek
        Aruco.detectMarkers(cameraFrameGray, dictionary, corners, ids, detectorParams, rejected);
        Aruco.refineDetectedMarkers(cameraFrameGray, charucoboard, corners, ids, rejected);

        rejected.clear();
        // zobrazení obrazu barevně
        return inputFrame.rgba();
    }

    public boolean onTouch(View v, MotionEvent e) {

        // dotyk obrazovky
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            firstTouchTime = e.getEventTime();
        }

        if (e.getAction() == MotionEvent.ACTION_UP) {
            // dotyk obrazovky byl delší než 1 vteřina
            if( e.getEventTime() - firstTouchTime > 1000){

                if(allIds.size() < 1) {
                    Toast.makeText(this, "Nedostatek snímků pro kalibraci", Toast.LENGTH_SHORT).show();
                    return true;
                }

                List< Mat > allCharucoCorners = new ArrayList<>();
                List< Mat > allCharucoIds = new ArrayList<>();

                for(int i = 0; i < allCorners.size(); i++) {
                    //
                    if(allCorners.get(i).size() != allIds.get(i).total()) {
                        Toast.makeText(this, "Nedostatek rohů pro kalibraci", Toast.LENGTH_SHORT).show();
                        allCorners.remove(i);
                        allIds.remove(i);
                        allImgs.remove(i);
                        return true;
                    }
                    // interpolace rohů charuco desky (naplní parametry "charucoCorners, charucoIds" )
                    Mat charucoCorners = new Mat(), charucoIds = new Mat();
                    Aruco.interpolateCornersCharuco(allCorners.get(i), allIds.get(i), allImgs.get(i), charucoboard,
                            charucoCorners, charucoIds);

                    if(charucoCorners.total() < 4) {
                        Toast.makeText(this, "Nedostatek rohů pro kalibraci", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    allCharucoCorners.add(charucoCorners);
                    allCharucoIds.add(charucoIds);
                }
                if(allCharucoCorners.size() < 4) {
                    Toast.makeText(this, "Nedostatek rohů pro kalibraci", Toast.LENGTH_SHORT).show();
                    return true;
                }

                try{
                    // calibrace kamery pomocí charuco desky (naplní parametry " cameraMatrix, distCoeffs, rvecs, tvecs")
                    int calibrationFlags = 0;
                    Aruco.calibrateCameraCharuco(allCharucoCorners, allCharucoIds, charucoboard, cameraFrameSize,
                            cameraMatrix, distCoeffs, rvecs, tvecs, calibrationFlags);
                }
                catch(CvException e1){
                    Log.e(TAG,e1.getMessage() );
                    return true;
                }
                // uložení parametrů kamery do souboru
                saveCameraParamsToFile(cameraMatrix, distCoeffs);
            }
            // normální dotyk obrazovky (kratší než 1 vteřinu)
            else{
                // pokud byli v aktuálním snímku kamery detekovány nějaké Aruco značky -> přidat snímek ke kalibraci
                if(ids.cols() > 0) {
                    Toast.makeText(this, "Snímek uložen", Toast.LENGTH_SHORT).show();
                    allCorners.add(corners);
                    allIds.add(ids);
                    allImgs.add(cameraFrameGray);
                    return true;
                }
                Toast.makeText(this, "Charuco deska nenalezena", Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }

    public void saveCameraParamsToFile(Mat cameraMatrix, Mat distCoeffs){

        try{
            // vytvoření souboru
            File calibFile = new File(Environment.getExternalStorageDirectory().toString(), "calibrationParams.txt");
            if(!calibFile.exists()) {
                boolean fileCreated = calibFile.createNewFile();
                if(!fileCreated) {
                    Log.e(TAG, "Došlo k chybě při vytváření souboru " + calibFile);
                }
            }

            // zapís matic do souboru (jeden řádek = jedna matice)
            FileOutputStream outStream = new FileOutputStream(calibFile);
            OutputStreamWriter streamWriter = new OutputStreamWriter(outStream);
            streamWriter.write(matrixToString(cameraMatrix));
            streamWriter.write("\n");
            streamWriter.write(matrixToString(distCoeffs));

            streamWriter.close();
            outStream.flush();
            outStream.close();
            Toast.makeText(this, "Parametry kamery uloženy do: " + calibFile.getCanonicalPath(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
        }
    }

    // převod dat z matice na text (jednotlivé hodnoty odděleny čárkou)
    public String matrixToString(Mat mat){
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < mat.rows();i++)
        {
            for(int j = 0; j < mat.cols();j++)
            {
                double[] values = mat.get(i, j);
                for (double value : values) {
                    builder.append(value);
                    builder.append(",");
                }

            }
        }
        return builder.toString();
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
}

