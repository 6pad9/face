package com.face.model;

import org.bytedeco.opencv.opencv_core.Mat;

public class FaceData {
    private String fileName;
    private Mat image;
    private Mat faceRegion;

    public FaceData(String fileName, Mat image, Mat faceRegion) {
        this.fileName = fileName;
        this.image = image;
        this.faceRegion = faceRegion;
    }

    public String getFileName() {
        return fileName;
    }

    public Mat getImage() {
        return image;
    }

    public Mat getFaceRegion() {
        return faceRegion;
    }
}
