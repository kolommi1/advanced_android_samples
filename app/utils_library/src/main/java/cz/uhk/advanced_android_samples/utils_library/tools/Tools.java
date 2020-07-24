package cz.uhk.advanced_android_samples.utils_library.tools;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_32F;

public class Tools {

    private static final String TAG = "AAS_tools";

    public static List<Mat> readMatFromFile(Context mainActivity){
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
