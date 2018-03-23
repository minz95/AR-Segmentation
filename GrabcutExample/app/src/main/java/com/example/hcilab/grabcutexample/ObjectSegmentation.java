package com.example.hcilab.grabcutexample;

import android.graphics.Bitmap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Created by HCILAB on 2018-03-23.
 */

public class ObjectSegmentation {
    Mat img;
    Mat bgdModel;
    Mat fgdModel;
    Point tl, br;
    Scalar color = new Scalar(255, 0, 0, 255);

    Bitmap bitmap;

    ObjectSegmentation(Bitmap bmp, Point l, Point r) {
        bitmap = bmp;
    }

    public void Segmentation(Mat img, Mat back) {
        Mat mask = new Mat();
        Mat src = new Mat(1, 1, CvType.CV_8U, new Scalar(3.0));
        Rect rect = new Rect(tl, br);
        Imgproc.grabCut(img, mask, rect, bgdModel, fgdModel, 1, 0);
        Core.compare(mask, src, mask, Core.CMP_EQ);

        Mat foreground = new Mat(img.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));

    }


}
