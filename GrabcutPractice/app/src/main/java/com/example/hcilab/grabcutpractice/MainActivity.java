package com.example.hcilab.grabcutpractice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2  {

    ImageView iv;   // will be deprecated
    Bitmap bitmap, bitmapResult;
    private CameraBridgeViewBase mOpenCvCameraView;
    Mat img, gray;
    int mWidth, mHeight;


    ProgressBar progressBar1;

    Scalar RED = new Scalar(0,0,255);
    Scalar PINK = new Scalar(230,130,255);
    Scalar BLUE = new Scalar(255,0,0);
    Scalar LIGHTBLUE = new Scalar(255,255,160);
    Scalar GREEN = new Scalar(0,255,0);

    int radius = 15;
    int thickness = -1;

    Mat image = new Mat();
    Mat image_canvas = new Mat();
    Mat mask = new Mat();
    Mat bgdModel= new Mat();
    Mat fgdModel= new Mat();
    Mat foreground= new Mat();
    Mat mFG= new Mat();
    Mat mBG= new Mat();
    Mat m255= new Mat();
    Rect rect;
    Mat fgdPxls= new Mat();
    Mat bgdPxls= new Mat();

    boolean initialized;
    boolean processing;

    boolean edit_fg = true;

    int iterCount;

    float mDips=1;
    float mMul=1;

    public static final String TAG = "Grabcut demo";
    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar1 = (ProgressBar) this.findViewById(R.id.progressBar1);
        mOpenCvCameraView = (CameraBridgeViewBase) this.findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setOnTouchListener(this);
        iv = (ImageView) this.findViewById(R.id.imageView);
        //iv.setOnTouchListener(this);
        Button btnGC  = (Button) this.findViewById(R.id.buttonGC);
        btnGC.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View arg0) {

                // save camera image here, and progress the object segmentation
                nextIteration();
                bitmapResult = getSaveImage();
                //iv.setImageBitmap(bitmapResult);

                progressBar1.setVisibility(View.GONE);
            }

        } );
        Button btnFG  = (Button) this.findViewById(R.id.buttonFG);
        btnFG.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View arg0) {
                edit_fg = true;
            }

        } );
        Button btnBG  = (Button) this.findViewById(R.id.buttonBG);
        btnBG.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View arg0) {
                edit_fg = false;
            }

        } );

//		Bitmap bitmap;
        if (iv.getDrawable() instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) iv.getDrawable()).getBitmap();
        } else {
            Drawable d = iv.getDrawable();
            bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            d.draw(canvas);
        }
        //BitmapDrawable
        //Bitmap _bitmap = ((BitmapDrawable)iv.getDrawable()).getBitmap();
        Log.i(TAG, "_bitmap getWidth="+bitmap.getWidth() + ",_bitmap.getHeight()="+bitmap.getHeight());


        DisplayMetrics metrics = this.getResources()
                .getDisplayMetrics();

        int mWidth = metrics.widthPixels;
        int mHeight = metrics.heightPixels;
        mDips  = getResources().getDisplayMetrics().density;
        Log.i(TAG, "mWidth="+mWidth + ",mHeight="+mHeight + ",mDips="+mDips);

        // float mMul = (512 *100)/mWidth;
        Log.i(TAG, "mMul="+mMul);
        //bitmap = _bitmap;//BitmapFactory.decodeResource(getResources(), R.drawable.xxx);

        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        bitmapResult = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        //bitmap = getResizedBitmap(bitmap, 512);
        //bitmapResult= getResizedBitmap(bitmapResult, 512);
        //iv.setImageBitmap(bitmap);

        //bitmapResult
        //  img = new Mat();
        //  Utils.bitmapToMat(bitmap, img);
        //  mask = new Mat( img.size(), CvType.CV_8UC1);
        //  bitmap.getWidth();
        setImage(bitmap);


    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            //mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    /*
    private final Camera.PreviewCallback mCameraCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera c) {
            Log.d(TAG, "ON Preview frame");
            int height = 300, width = 400;
            img = new Mat(height, width, CvType.CV_8UC1);
            gray = new Mat(height, width, CvType.CV_8UC1);
            img.put(0, 0, data);

            Imgproc.cvtColor(img, gray, Imgproc.COLOR_YUV420sp2GRAY);
            String pixvalue = String.valueOf(gray.get(300, 400)[0]);
            String pixval1 = String.valueOf(gray.get(300, 400+width/2)[0]);
            Log.d(TAG, pixvalue);
            Log.d(TAG, pixval1);
            // to do the camera image split processing using "data"
        }
    };
    */

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        Log.i(TAG, "bitmapRatio="+bitmapRatio);
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "counter=0 :::  tl.x=" + event.getX() + "  , tl.y=" + event.getY());
        //	 maskLabel:tapPoint foreground:edit_fg];
        //mMul=(float) 1.5;
        Log.i(TAG, "counter*mDips :::  tl.x=" + event.getX()*mMul + "  , tl.y=" +event.getY()*mMul);

        Point tapPoint = new Point (event.getX()*mMul, event.getY()*mMul);
        maskLabel(tapPoint, edit_fg);
        bitmapResult = getImage();
        iv.setImageBitmap(bitmapResult);

        /*
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            //  toggle();
            // Intent cameraIntent = new Intent( android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
            // startActivityForResult(cameraIntent, 1337);
            int bufferSize = width * height * 3;
            byte[] mPreviewBuffer = null;

            // New preview buffer.
            mPreviewBuffer = new byte[bufferSize + 4096];

            // with buffer requires addbuffer.
            camera.addCallbackBuffer(mPreviewBuffer);
            camera.setPreviewCallbackWithBuffer(mCameraCallback);
            break;
        default:
            break;
        }
         */

        //Core.circle(mask, new Point(event.getX() ,event.getY() ), 15, new Scalar(Imgproc.GC_FGD), -1);
        return true;
    }

    private void reset()
    {
        log("reset");
        if( !mask.empty() )
            mask.setTo(Scalar.all(Imgproc.GC_BGD));


        bgdPxls = Mat.zeros(image.size(), CvType.CV_8UC1);
        fgdPxls = Mat.zeros(image.size(), CvType.CV_8UC1);

        mFG.create(image.size(),CvType.CV_8UC3);
        mFG.setTo(RED);
        mBG.create(image.size(),CvType.CV_8UC3);
        mBG.setTo(BLUE);

        m255.create( image.size(), CvType.CV_8UC1);
        m255.setTo( new Scalar( 255));


        int off_x = 1; //image.cols * 0.1;
        int off_y = 1; //image.rows * 0.1;

        rect = new Rect(off_x, off_y, image.cols() - 2 * off_x, image.rows() - 2 * off_y);

        setRectInMask();
        initialized = false;
        iterCount = 0;
    }

    private void setRectInMask()
    {
        log("setRectInMask");
        mask.setTo( new  Scalar(Imgproc.GC_BGD) );
        rect.x = Math.max(0, rect.x);
        rect.y = Math.max(0, rect.y);
        rect.width = Math.min(rect.width, image.cols()-rect.x);
        rect.height = Math.min(rect.height, image.rows()-rect.y);
        mask.setTo(new  Scalar(Imgproc.GC_PR_FGD) );

        log("setRectInMask rect.width"+rect.width);
        log("setRectInMask rect.height"+rect.height);
    }



    public void setImage(Bitmap bitmap)
    {
        log("setImage");
        Utils.bitmapToMat(bitmap, image);
        List<Mat> planes  = new ArrayList<Mat>(4);
        List<Mat> planesRGB  = new ArrayList<Mat>(4);

        Core.split(image, planes);
        planesRGB.add(planes.get(0)  );
        planesRGB.add(planes.get(1) );
        planesRGB.add(planes.get(2) );
        Core.merge(planesRGB, image);

        mask.create( image.size(),CvType.CV_8UC1);
        reset();
    }



    private void nextIteration()
    {
        progressBar1.setVisibility(View.VISIBLE);
        log("nextIteration");
        if (processing) {
            return;
        }
        processing = true;


        if (initialized) {
            Imgproc.grabCut(image, mask, rect, bgdModel, fgdModel, 1, Imgproc.GC_INIT_WITH_RECT);

        } else {
            Imgproc.grabCut(image, mask , rect, bgdModel, fgdModel, 1, Imgproc.GC_INIT_WITH_MASK);


            initialized = true;
        }
        iterCount++;

        bgdPxls.setTo(new Scalar(0));
        fgdPxls.setTo(new Scalar(0));

        processing = false;
    }

    public Bitmap getImage()
    {

        log("getImage");
        Mat result = new Mat();
        Mat binMask;

        if (initialized == false) {
            image.copyTo(result);
        } else {
            binMask =getBinMask(mask);
            image.copyTo(result, binMask);
        }


        // TODO: alpha blending with a mask
        mFG.copyTo(result, fgdPxls);
        mBG.copyTo(result, bgdPxls);
        Imgproc.rectangle( result, new Point( rect.x, rect.y ), new Point(rect.x + rect.width-1, rect.y + rect.height-1 ), GREEN, 2);


        Utils.matToBitmap(result, bitmapResult);
        return bitmapResult;
    }

    public Bitmap getSaveImage()
    {
        log("getSaveImage");
        Mat result = new Mat();
        Mat binMask = new Mat();

        if (initialized == false) {

            image.copyTo(result);

        } else {

            binMask =   getBinMask(mask);
            image.copyTo(result, binMask);

            // add alpha channel from mask
            Mat alpha =  new Mat();
            m255.copyTo( alpha, binMask );
            List<Mat> v  = new ArrayList<Mat>(2);

            v.add(result);
            v.add(alpha);
            Core.merge(v, result);
        }
        return  transparentBG(result);
  /*
    Utils.matToBitmap(dst, bitmapResult);

    return bitmapResult;
    */
    }


    public void maskLabel(Point point,boolean isForeground )
    {

        log("maskLabel");
        Point p = new Point(point.x, point.y);



        if (isForeground) {
            Imgproc.circle( fgdPxls, p, radius,new Scalar( 1), thickness );
            Imgproc.circle( bgdPxls, p, radius,new Scalar( 0), thickness );
            Imgproc.circle( mask, p, radius, new Scalar(Imgproc.GC_FGD ), thickness );
        } else {
            Imgproc.circle( bgdPxls, p, radius,new Scalar( 1), thickness );
            Imgproc.circle( fgdPxls, p, radius,new Scalar( 0), thickness );
            Imgproc.circle( mask, p, radius,new Scalar( Imgproc.GC_BGD), thickness );
        }



    }


    public static void log(String message)
    {

        String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

        Log.d(className + "." + methodName + "():" + lineNumber, message);

    }


    private Mat getBinMask( Mat comMask)
    {
        Mat binMask = new Mat();
        if( comMask.empty() || comMask.type() != CvType.CV_8UC1 )
            //  CV_Error( CV_StsBadArg, "comMask is empty or has incorrect type (not CV_8UC1)" );
            if( binMask.empty() || binMask.rows() != comMask.rows() || binMask.cols() != comMask.cols() )
                binMask.create( comMask.size(), CvType.CV_8UC1 );

        Mat src2 = new Mat( comMask.size(), CvType.CV_8UC1 , new Scalar(1) );
        Core.bitwise_and(comMask, src2, binMask) ;

        return binMask;
    }



    private Bitmap transparentBG ( Mat src)
    {

        Mat dst = new Mat(src.size(), CvType.CV_8UC4);  //(src.rows,src.cols,CV_8UC4);
        Mat tmp = new Mat();
        Mat thr = new Mat();

        Imgproc.cvtColor(src,tmp,Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(tmp,thr,100,255,Imgproc.THRESH_BINARY);
        List<MatOfPoint>  contours =  new ArrayList<MatOfPoint>();
        MatOfInt4  hierarchy  =  new MatOfInt4();


        int largest_contour_index=0;
        int largest_area=0;

        Mat alpha = new Mat(src.size(),CvType.CV_8UC1,new Scalar(0));

        Imgproc.findContours( tmp, contours, hierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE ); // Find the contours in the image
        for( int i = 0; i< contours.size(); i++ ) // iterate through each contour.
        {
            double a= Imgproc.contourArea( contours.get(i),false);  //  Find the area of contour
            if(a>largest_area){
                largest_area=(int) a;
                largest_contour_index=i;                //Store the index of largest contour
            }
        }

        Imgproc.drawContours( alpha,contours, largest_contour_index, new Scalar(255), Core.FILLED , 8, hierarchy , Integer.MAX_VALUE, new Point() );


        List<Mat> rgb  = new ArrayList<Mat>(3);
        List<Mat> rgba  = new ArrayList<Mat>(4);

        Core.split(src,rgb);
        rgba.add(rgb.get(0)  );
        rgba.add(rgb.get(1)  );
        rgba.add(rgb.get(2)  );
        rgba.add( alpha );

        Core.merge(rgba ,dst);

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, output);
        return output;

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        img = inputFrame.rgba();
        gray = inputFrame.gray();
        return null;
    }
/*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
*/

}
