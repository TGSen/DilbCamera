package com.example.dlibtest;

import android.graphics.Bitmap;
import android.graphics.Point;

import com.example.dlibtest.Dlib.VisionDetRet;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.opencv.calib3d.Calib3d.Rodrigues;
import static org.opencv.calib3d.Calib3d.decomposeProjectionMatrix;
import static org.opencv.core.Core.norm;
import static org.opencv.core.Core.transpose;
import static org.opencv.core.CvType.CV_64FC1;

public class PoseDetection {

    public  double[] estimatePose(VisionDetRet results, Bitmap image) {


        Point topOfNose = results.getFaceLandmarks().get(33); //top of nose bridge
        Point leftEye = results.getFaceLandmarks().get(39);
        Point rightEye = results.getFaceLandmarks().get(42);

        Point noseTip = results.getFaceLandmarks().get(33);
        Point chin = results.getFaceLandmarks().get(8);
        Point eye_r_r = results.getFaceLandmarks().get(45);
        Point eye_l_l = results.getFaceLandmarks().get(36);
        Point mouth_l = results.getFaceLandmarks().get(48);
        Point mouth_r = results.getFaceLandmarks().get(54);
        Point ear_r = results.getFaceLandmarks().get(0);
        Point ear_l = results.getFaceLandmarks().get(16);
        Point jaw_l = results.getFaceLandmarks().get(5);
        Point jaw_r = results.getFaceLandmarks().get(11);

        MatOfPoint2f image_points = new MatOfPoint2f();
        List<org.opencv.core.Point> points2d = new ArrayList<org.opencv.core.Point>();
        points2d.add(new org.opencv.core.Point(topOfNose.x, topOfNose.y));
        points2d.add(new org.opencv.core.Point(chin.x, chin.y));
        points2d.add(new org.opencv.core.Point(eye_r_r.x, eye_r_r.y));
        points2d.add(new org.opencv.core.Point(eye_l_l.x, eye_l_l.y));
        points2d.add(new org.opencv.core.Point(mouth_r.x, mouth_r.y));
        points2d.add(new org.opencv.core.Point(mouth_l.x, mouth_l.y));
        //  points2d.add(new org.opencv.core.Point(jaw_r.x, jaw_r.y));
        //  points2d.add(new org.opencv.core.Point(jaw_l.x, jaw_l.y));
        //  points2d.add(new org.opencv.core.Point(ear_l.x, ear_l.y));
        //  points2d.add(new org.opencv.core.Point(ear_r.x, ear_r.y));

        image_points.fromList(points2d);

        // Create key 3d points with estimated depth values
        List<Point3> p3 = new ArrayList<Point3>();
        MatOfPoint3f model_points = new MatOfPoint3f();

        p3.add(new Point3(0, 0, 0)); // tip of nose
        p3.add(new Point3(0, -330, -65)); // chin
        p3.add(new Point3(-225, 170, -135)); //left corner left eye
        p3.add(new Point3(225, 170, -135)); //right corner right eye
        p3.add(new Point3(-150, -150, -125)); //left corner mouth
        p3.add(new Point3(150, -150, -125)); //right corner of the mouth
        //  p3.add(new Point3(-225, -170, -135)); //jaw left corner of the mouth
        //  p3.add(new Point3(225, -170, -135)); //jaw right corner of the mouth
        //  p3.add(new Point3(-325, 170, -150)); //left ear
       //   p3.add(new Point3(325, 170, -150)); //right ear

        model_points.fromList(p3);

        Mat imgMatrix = new Mat();
        Utils.bitmapToMat(image, imgMatrix);
        Point centre = new Point(imgMatrix.cols() / 2, imgMatrix.rows() / 2);

        double focal_length = imgMatrix.cols();

        //生成3*3矩阵
        Mat intrinsics = Mat.eye(3, 3, CvType.CV_64F);
        intrinsics.put(0, 0, focal_length);
        intrinsics.put(1, 1, focal_length);
        intrinsics.put(0, 2, imgMatrix.cols() /2);
        intrinsics.put(1, 2, imgMatrix.rows() / 2);

        //cam_matrix = new MatOfDouble(3, 3, CvType.CV_64FC1, mTest );
        MatOfDouble dist_coeffs =   new MatOfDouble();

        //MatOfDouble dist_coeffs = new MatOfDouble(4, 1);

        Mat rotation_vector = new Mat();
        Mat translation_vector = new Mat();

        //solve for pose
        Calib3d.solvePnP(model_points, image_points, intrinsics,
                dist_coeffs, rotation_vector, translation_vector);


        Mat rotCamerMatrix1=new Mat(3,3,CvType.CV_64F);
        Rodrigues(rotation_vector,rotCamerMatrix1);

        Mat eulerAngles=getEulerAngles(rotCamerMatrix1);
        double[] rotateEulerAngles=new double[]{eulerAngles.get(0,0)[0],eulerAngles.get(0,1)[0],eulerAngles.get(0,2)[0]};

/*
        Mat headPose=new Mat(4,4,CvType.CV_64FC1);
        headPose.put(0,0,rotCamerMatrix1.get(0,0)[0]); headPose.put(0,1,rotCamerMatrix1.get(0,1)[0]);headPose.put(0,2,rotCamerMatrix1.get(0,2)[0]);headPose.put(0,3,translation_vector.get(0,0)[0]);
        headPose.put(1,0,rotCamerMatrix1.get(1,0)[0]); headPose.put(1,1,rotCamerMatrix1.get(1,1)[0]);headPose.put(1,2,rotCamerMatrix1.get(1,2)[0]);headPose.put(1,3,translation_vector.get(0,1)[0]);
        headPose.put(2,0,rotCamerMatrix1.get(2,0)[0]); headPose.put(2,1,rotCamerMatrix1.get(2,1)[0]);headPose.put(2,2,rotCamerMatrix1.get(2,2)[0]);headPose.put(2,0,translation_vector.get(0,2)[0]);
        headPose.put(3,0,0);headPose.put(3,1,0);headPose.put(3,2,0);headPose.put(3,3,1);*/
        double []ur=rotationMatrixToEulerAngles(rotCamerMatrix1);


        return new double[] {rotateEulerAngles[0], rotateEulerAngles[1], rotateEulerAngles[2],
                translation_vector.get(0,0)[0],translation_vector.get(1,0)[0],translation_vector.get(2,0)[0]};
    }


    public Mat getEulerAngles(Mat rotCamerMatrix)
    {
        Mat eulerAngles=new Mat(1,3,CvType.CV_64F);
        Mat cameraMatrix=new Mat(3,3,CvType.CV_64F);
        Mat rotMatrix=new Mat(3,3,CvType.CV_64F);
        Mat transVect=new Mat(4,1,CvType.CV_64F);
        Mat rotMatrixX=new Mat(3,3,CvType.CV_64F);
        Mat rotMatrixY=new Mat(3,3,CvType.CV_64F);
        Mat rotMatrixZ=new Mat(3,3,CvType.CV_64F);


       double []num={rotCamerMatrix.get(0,0)[0],rotCamerMatrix.get(0,1)[0],rotCamerMatrix.get(0,2)[0],0,
                rotCamerMatrix.get(1,0)[0],rotCamerMatrix.get(1,1)[0],rotCamerMatrix.get(1,2)[0],0,
                rotCamerMatrix.get(2,0)[0],rotCamerMatrix.get(2,1)[0],rotCamerMatrix.get(2,2)[0],0};

        Mat projMatirx=new Mat(3,4,CvType.CV_64FC1);
        projMatirx.put(0,0,num[0]); projMatirx.put(0,1,num[1]); projMatirx.put(0,2,num[2]); projMatirx.put(0,3,num[3]);
        projMatirx.put(1,0,num[4]); projMatirx.put(1,1,num[5]); projMatirx.put(1,2,num[6]); projMatirx.put(1,3,num[7]);
        projMatirx.put(2,0,num[8]); projMatirx.put(2,1,num[9]); projMatirx.put(2,2,num[10]); projMatirx.put(2,3,num[11]);


        decomposeProjectionMatrix(projMatirx, cameraMatrix,rotMatrix,transVect,rotMatrixX,rotMatrixY,rotMatrixZ,eulerAngles);



        return eulerAngles;
    }

    // Checks if a matrix is a valid rotation matrix.
    public static Boolean isRotationMatrix(Mat R)
    {
        Mat Rt = new Mat();
        transpose(R, Rt);
        Mat shouldBeIdentity = R.mul(Rt);

        Mat I = Mat.eye(3,3, shouldBeIdentity.type());

        return  norm(I, shouldBeIdentity) < 1e-6;

    }

    // Calculations
    // es rotation matrix to euler angles
    // The result is the same as MATLAB except the order
    // of the euler angles ( x and z are swapped ).
    public static double[] rotationMatrixToEulerAngles(Mat R)
    {

        assert(isRotationMatrix(R));

        double sy = (float)Math.sqrt(((float)(R.get(0, 0)[0] * R.get(0,0)[0])) + (float)(R.get(1,0)[0] * R.get(1,0)[0]));

        Boolean singular = sy < 1e-6; // If

        float x, y, z;
        if (!singular)
        {
            x = (float)atan2(R.get(2,1)[0] , R.get(2,2)[0]);
            y = (float)atan2(-R.get(2,0)[0], sy);
            z = (float)atan2(R.get(1,0)[0], R.get(0,0)[0]);
        }
        else
        {
            x = (float) atan2(-R.get(1,2)[0], R.get(1,1)[0]);
            y = (float) atan2(-R.get(2,0)[0], sy);
            z = 0;
        }
        return new double[] { x, y, z };

    }

    // Calculates rotation matrix given euler angles.
    Mat eulerAnglesToRotationMatrix(float[] theta)
    {
        // Calculate rotation about x axis

        Mat R_x = new MatOfDouble(1, 0, 0,
                0, cos(theta[0]), -sin(theta[0]),
                0, sin(theta[0]), cos(theta[0])
        );

        Mat R_y = new MatOfDouble(
                cos(theta[1]),    0,      sin(theta[1]),
                0,               1,      0,
                -sin(theta[1]),   0,      cos(theta[1])
        );

        Mat R_z = new MatOfDouble(
                cos(theta[2]),    -sin(theta[2]),      0,
                sin(theta[2]),    cos(theta[2]),       0,
                0,               0,                  1
        );

        // Combined rotation matrix
        Mat R = R_z.mul(R_y).mul(R_x);

        return R;

    }


}
