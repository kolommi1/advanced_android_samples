package cz.uhk.advanced_android_samples.room_visualization.room_visualization;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4;

import static org.opencv.aruco.Aruco.DICT_4X4_50;
import static org.opencv.aruco.Aruco.getPredefinedDictionary;
import static org.opencv.core.CvType.CV_32F;

public class MyCvCameraViewListener implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "AAS_rv_OpenCV";
    private RoomVisualizationMainActivity mainActivity;

    private Mat cameraFrameRGB;
    private Mat cameraFrameGray;
    private Dictionary dictionary;
    private Mat cameraMatrix, distCoefs;
    private DetectorParameters parameters;
    private List<Mat> detectedMarkerCorners, rejected;
    private Mat detectedMarkerIds;
    private Mat rotationVectors, translationVectors;
    private double[] tvecData;
    private double[] rvecData;
    private Mat rotation;
    private Mat4 invertMat;
    private Mat4 viewMatrix;
    private int markerId;

    MyCvCameraViewListener(RoomVisualizationMainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // inicializace na jednom místě (zabrání únikům paměti)

        // slovník pro detekci Aruco značek
        dictionary = getPredefinedDictionary(DICT_4X4_50);
        // načtení dat ze souboru vytvořeného při kalibraci kamery
        List<Mat> camData = readMatFromFile();
        if(camData.size()>1){
            cameraMatrix = camData.get(0);
            distCoefs = camData.get(1);
        }

        parameters = DetectorParameters.create();
        parameters.set_cornerRefinementMethod(0);
        parameters.set_cornerRefinementMaxIterations(15);
        cameraFrameRGB = new Mat(width,height, CvType.CV_8UC4);
        cameraFrameGray = new Mat(width,height,CvType.CV_8UC1);
        detectedMarkerIds = new Mat();
        detectedMarkerCorners = new ArrayList<>();
        rejected = new ArrayList<>();
        rotationVectors = new Mat();
        translationVectors = new Mat();
        tvecData = new double[3];
        rvecData = new double[9];
        rotation = new Mat();
        invertMat = new Mat4();
        viewMatrix = new Mat4();
        markerId = 0;
        camData.clear();
    }

    @Override
    public void onCameraViewStopped() {
        cameraFrameRGB.release();
        cameraFrameGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if(cameraFrameGray != null){
            cameraFrameGray.release();
        }

        if(cameraFrameRGB != null){
            cameraFrameRGB.release();
        }

        // detekce probíhá ve stupních šedi
        cameraFrameGray = inputFrame.gray();

        // vykreslení značek Aruco vyžaduje rgb obraz
        Imgproc.cvtColor(inputFrame.rgba(), cameraFrameRGB, Imgproc.COLOR_RGBA2RGB, 3);

        // detekce aruco značek
        Aruco.detectMarkers(cameraFrameGray, dictionary, detectedMarkerCorners, detectedMarkerIds, parameters, rejected);
        rejected.clear();

        // pokud byla detekována nějaká značka aruco
        if(detectedMarkerIds.cols() > 0) {
            // vykreslení značek
            Aruco.drawDetectedMarkers(cameraFrameRGB, detectedMarkerCorners, detectedMarkerIds, new Scalar(255,255,0));
            // nalezen soubor s parametry kamery
            if(distCoefs != null && cameraMatrix != null ){
                // určení polohy značek z parametrů kamery, šířka markerů při kalibraci 2,2 cm, větší markery 6,4cm
                Aruco.estimatePoseSingleMarkers(detectedMarkerCorners, 0.064f, cameraMatrix, distCoefs, rotationVectors, translationVectors);
                detectedMarkerCorners.clear();
                // vykreslení xyz OS pro každou Aruco značku (rvec, tvec obsahují pozici a rotaci všech značek)
                for(int i = 0; i< rotationVectors.rows(); i++){
                    // submat - získání rotace/pozice jednotlivé značky
                    Aruco.drawAxis(cameraFrameRGB, cameraMatrix, distCoefs,
                            rotationVectors.submat(new Rect(0,i,1,1)),
                            translationVectors.submat(new Rect(0,i,1,1)), 0.04f);
                }

                // nad prvním detekovaný markerem bude pomocí OpenGL ES vykreslena krychle
                Mat tempRvec = rotationVectors.submat(new Rect(0,0,1,1));
                Mat tempTvec = translationVectors.submat(new Rect(0,0,1,1));
                // získání dat z matice do pole pro lepší manipulaci
                tempTvec.get(0,0, tvecData);
                // výpočet rotační matice 3x3 z rotačního vektoru
                Calib3d.Rodrigues(tempRvec, rotation);
                rotation.get(0,0, rvecData);
                tempRvec.release();
                tempTvec.release();

                // naplnění matice 4x4 - rotační maticí 3x3
                // 4. sloupec naplněn pozičním vektorem
                for(int row=0; row<3; row++)
                {
                    for(int col=0; col<3; col++)
                    {
                        viewMatrix =  viewMatrix.withElement(row, col, rvecData[row*3+col]);
                    }
                    viewMatrix = viewMatrix.withElement(row, 3, tvecData[row]*10);
                }
                viewMatrix = viewMatrix.withElement(3, 3,  1.0f);

                invertMat= invertMat.withElement(0, 0, 1.0f);
                invertMat=invertMat.withElement(1, 1, -1.0f);
                invertMat= invertMat.withElement(2, 2, -1.0f);
                invertMat= invertMat.withElement(3, 3,1.0f);
                // inverze os y a z kvůli nekompatibilitě s OpenGL ES souřadnicovým systémem
                viewMatrix = invertMat.mul(viewMatrix);
                int[] temp = new int[detectedMarkerIds.cols()];
                detectedMarkerIds.get(0,0, temp);
                markerId = temp[0];
            }
            rotation.release();
            rotationVectors.release();
            translationVectors.release();
            detectedMarkerIds.release();
        }
        return cameraFrameRGB;
    }

    // OpenGL ES použivá matice jiném formátu
    public Mat4 getViewMatrix(){
        return viewMatrix.transpose();
    }

    public int getMarkerId(){
        return markerId;
    }

    private List<Mat> readMatFromFile(){
        List<Mat> result = new ArrayList<>();
        try {
            // matice kamery je ve formátu 3x3 float
            int camRows=3, camCols=3;
            Mat tempMatrix = new Mat(camRows,camCols, CV_32F);
            File file = new File(Environment.getExternalStorageDirectory()+"/"+"calibrationParams.txt");
            if(!file.exists()){
                Toast.makeText(mainActivity, "Nenalezen soubor \"calibrationParams.txt\". Pro detekci polohy a rotace Aruco značek spusťte nejdříve aplikaci kalibrace. ", Toast.LENGTH_LONG).show();
            }
            else{
                BufferedReader stream = new BufferedReader(new FileReader(file.getPath()));
                // čtení matice camery(cameraMatrix)
                String line = stream.readLine();
                String[] splitted = line.split(","); //
                double[] matrixData = new double[camRows*camCols];

                for (int i = 0; i < camRows; i++) {
                    for (int y = 0; y < camCols; y++) {
                        matrixData[i*3+y]  = Double.parseDouble(splitted[i*3+y]);
                    }
                }
                tempMatrix.put(0,0,matrixData);
                result.add(tempMatrix);
                // čtení distCoeffs
                line = stream.readLine();
                splitted = line.split(",");
                matrixData = new double[splitted.length];
                // distCoeffs je matice 1xN typu float
                tempMatrix = new Mat(1,splitted.length, CV_32F);

                for (int i = 0; i < splitted.length; i++) {
                    matrixData[i]  = Double.parseDouble(splitted[i]);
                }

                tempMatrix.put(0,0,matrixData);
                result.add(tempMatrix);
                tempMatrix.release();
                stream.close();
            }
        } catch (IOException e){
            Log.e(TAG, e.getMessage());
        }

        return result;
    }
}
