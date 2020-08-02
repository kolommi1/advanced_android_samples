package cz.uhk.advanced_android_samples.fantom_cube;

import android.os.Environment;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.GridBoard;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cz.uhk.advanced_android_samples.utils_library.tools.Tools;
import cz.uhk.advanced_android_samples.utils_library.transforms.Mat4;

import static org.opencv.aruco.Aruco.DICT_4X4_100;
import static org.opencv.aruco.Aruco.getPredefinedDictionary;
import static org.opencv.core.CvType.CV_8UC4;

public class MyCvCameraViewListener implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "AAS_ov_OpenCV";
    private MainActivity mainActivity;
    private Mat cameraFrameRGB, cameraFrameGray;
    private Dictionary dictionary;
    private GridBoard [] boards;
    private Mat cameraMatrix, distCoefs;
    private DetectorParameters parameters;
    private List<Mat> detectedMarkerCorners, rejected;
    private Mat detectedMarkerIds;
    private Mat[] rotationVectors, translationVectors;
    private Mat rotation;
    private Mat4 invertMat;
    private Mat4 viewMatrix;
    private int highestIndex;

    MyCvCameraViewListener(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // inicializace na jednom místě (zabrání únikům paměti)

        // slovník pro detekci Aruco značek
        dictionary = getPredefinedDictionary(DICT_4X4_100);

        boards = new GridBoard[6];
        // Vytvoření Aruco desek 0, 9, 18, 27, 36, 45 pro každou stranu krychle
        boards[0] = GridBoard.create(3, 3, 0.022f, 0.004f, dictionary,0);
        boards[1] = GridBoard.create(3, 3, 0.022f, 0.004f, dictionary,9);
        boards[2] = GridBoard.create(3, 3, 0.022f, 0.004f, dictionary,18);
        boards[3] = GridBoard.create(3, 3, 0.022f, 0.004f, dictionary,27);
        boards[4] = GridBoard.create(3, 3, 0.022f, 0.004f, dictionary,36);
        boards[5] = GridBoard.create(3, 3, 0.022f, 0.004f, dictionary,45);

        // načtení dat ze souboru vytvořeného při kalibraci kamery
        List<Mat> camData = Tools.readMatFromFile(mainActivity);
        if(camData.size()>1){
            cameraMatrix = camData.get(0);
            distCoefs = camData.get(1);
        }

        parameters = DetectorParameters.create();
        cameraFrameRGB = new Mat(width,height, CV_8UC4);
        cameraFrameGray = new Mat(width,height,CvType.CV_8UC1);
        detectedMarkerIds = new Mat();
        detectedMarkerCorners = new ArrayList<>();
        rejected = new ArrayList<>();
        rotationVectors = new Mat[]{new Mat(), new Mat(), new Mat(), new Mat(), new Mat(), new Mat()};
        translationVectors = new Mat[]{new Mat(), new Mat(), new Mat(), new Mat(), new Mat(), new Mat()};
        camData.clear();
        rotation = new Mat();
        invertMat = new Mat4();
        viewMatrix = new Mat4();
       // printArucoBoard();
    }

    // funkce uloží obrázek obsahující Aruco desku pro účely tisku
    public void printArucoBoard(){

        Mat boardImage = new Mat();
        boards[0].draw( new Size(2100, 2970), boardImage, 10, 1 );

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File file = new File(path, "board1.png");

        if (Imgcodecs.imwrite(file.toString(), boardImage))
            Log.e(TAG, "Obrázek byl úspěšně uložen");
        else
            Log.e(TAG, "Při ukládání obrázku došlo k chybě");

        boardImage.release();
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
            highestIndex = -1;
            // nalezen soubor s parametry kamery
            if(distCoefs != null && cameraMatrix != null ){
                // určení polohy desek z parametrů kamery a detekovaných značek
                int[] detectedBoardMarkersArray = new int[6];
                int highestBoardMarkers = 0;
                highestIndex = 0;
                for (int i=0; i<6; i++){
                    detectedBoardMarkersArray[i] = Aruco.estimatePoseBoard(detectedMarkerCorners, detectedMarkerIds, boards[i], cameraMatrix, distCoefs, rotationVectors[i], translationVectors[i]);
                    if(detectedBoardMarkersArray[i]>highestBoardMarkers){
                        highestBoardMarkers = detectedBoardMarkersArray[i];
                        highestIndex = i;
                    }
                }

                // byl detekován alespoň jedna značka z desky
                if(highestBoardMarkers > 0) {
                    // vykreslení xyz OS pro každou Aruco desku (rvec, tvec obsahují pozici a rotaci všech desek)
                    Aruco.drawAxis(cameraFrameRGB, cameraMatrix, distCoefs, rotationVectors[highestIndex], translationVectors[highestIndex], 0.04f);

                    // nad první detekovanou deskou bude pomocí OpenGL ES vykreslena krychle
                    Mat tempRvec = rotationVectors[highestIndex];
                    Mat tempTvec = translationVectors[highestIndex];
                    // výpočet rotační matice 3x3 z rotačního vektoru
                    Calib3d.Rodrigues(tempRvec, rotation);

                    // získání dat z matice do pole pro lepší manipulaci
                    float[] tvecData = new float[3];
                    float[] rvecData = new float[9];
                    tempTvec.get(0,0, tvecData);
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
                    invertMat= invertMat.withElement(1, 1, -1.0f);
                    invertMat= invertMat.withElement(2, 2, -1.0f);
                    invertMat= invertMat.withElement(3, 3,1.0f);
                    // inverze os y a z kvůli nekompatibilitě s OpenGL ES souřadnicovým systémem
                    viewMatrix = invertMat.mul(viewMatrix);
                }
            }

            rotation.release();
            for(int i=0;i<6;i++){
                rotationVectors[i].release();
                translationVectors[i].release();
            }
        }
        else {
            highestIndex = -1;
        }
        detectedMarkerIds.release();
        detectedMarkerCorners.clear();
        return cameraFrameRGB;
    }

    public int getHighestIndex(){
        return highestIndex;
    }

    // OpenGL ES použivá matice v jiném formátu
    public Mat4 getViewMatrix(){
        return viewMatrix.transpose();
    }

}
