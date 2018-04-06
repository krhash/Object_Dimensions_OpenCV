package com.example.krush.objdimcal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProcessActivity extends AppCompatActivity {

    Mat orig, processed;
    double pixelsPerMetric=0;
    ImageView iv;
    class ProcessImage extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... params) {

            Imgproc.cvtColor(orig,processed,Imgproc.COLOR_RGB2GRAY);
            Imgproc.GaussianBlur(processed,processed,new Size(7,7),0,3);
            Imgproc.threshold(processed,processed, 120, 255, Imgproc.THRESH_BINARY);
            Imgproc.erode(processed, processed, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5)));
            Imgproc.dilate(processed, processed, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5)));
            Imgproc.Canny(processed,processed,60,80);

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(processed, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            MatOfPoint frameContour = new MatOfPoint();
            double maxArea = -1;
            for(int i=0; i < contours.size(); i++)
            {
                if(Imgproc.contourArea(contours.get(i)) > maxArea)
                {
                    frameContour = contours.get(i);
                    maxArea = Imgproc.contourArea(frameContour);
                }
                Imgproc.drawContours(orig,contours,-1,new Scalar(255,0,0),2);
            }

            if(maxArea != -1) {
                RotatedRect box = Imgproc.minAreaRect(new MatOfPoint2f(frameContour.toArray()));
                Point[] vertices = new Point[4];
                box.points(vertices);
                Point tltrMid = midpoint(vertices[0], vertices[1]);
                Point blbrMid = midpoint(vertices[2], vertices[3]);
                Point tlblMid = midpoint(vertices[0], vertices[3]);
                Point trbrMid = midpoint(vertices[1], vertices[2]);

                double width = euclideanDistance(tlblMid, trbrMid);
                double height = euclideanDistance(tltrMid, blbrMid);

                pixelsPerMetric = width / 6;

                width = width / pixelsPerMetric;      //width in inches
                height = height / pixelsPerMetric;     //height in inches


                Imgproc.putText(orig, String.valueOf(width), tltrMid, Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 0));
                Imgproc.putText(orig, String.valueOf(height), tlblMid, Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 0));
            }
            return "done";
        }

        @Override
        protected void onPostExecute(String s) {
            if(s == "done"){
                Toast.makeText(getBaseContext(),"Processed",Toast.LENGTH_SHORT);
                updateImageView();

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_acivity);
        iv = (ImageView)findViewById(R.id.imageView_procActivity);
        orig = Imgcodecs.imread("/storage/emulated/0/proc.jpg");
        processed = orig.clone();

        Bitmap bitmap = Bitmap.createBitmap(orig.cols(),orig.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(orig,bitmap);
        iv.setImageBitmap(bitmap);

        new ProcessImage().execute();
    }

    public Point midpoint(Point a, Point b){
        return new Point((a.x+b.x)*0.5,(a.y+b.y)*0.5);
    }

    public void updateImageView(){

        Bitmap bitmap = Bitmap.createBitmap(orig.cols(),orig.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(orig,bitmap);
        iv.setImageBitmap(bitmap);
    }

    public double euclideanDistance(Point a, Point b)
    {
        return Math.sqrt(((a.x-b.x)*(a.x-b.x)) + ((a.y-b.y)*(a.y-b.y)));
    }

}
