package com.face.service;

import com.face.model.FaceData;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Service
public class FaceValidationService {

    private static final String DATA_IMAGES_PATH = "/data/images/";
    private static final String DATA_MODELS_PATH = "/data/models/";
    private static final int MIN_PHOTOS = 5;
    private static final int MAX_PHOTOS = 8;
    private static final double SIMILARITY_THRESHOLD = 100.0; // Threshold to consider faces as different

    public Map<String, Object> validateAndTrainFaces(String usuarioId, MultipartFile[] fotos) throws IOException {
        List<String> fotosValidas = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        // Validate number of photos
        if (fotos == null || fotos.length < MIN_PHOTOS) {
            errores.add("Se requieren al menos " + MIN_PHOTOS + " fotos");
            return createResponse(fotosValidas, errores);
        }
        if (fotos.length > MAX_PHOTOS) {
            errores.add("Se permiten máximo " + MAX_PHOTOS + " fotos");
            return createResponse(fotosValidas, errores);
        }

        // Initialize face detector
        CascadeClassifier faceDetector = loadFaceDetector();
        if (faceDetector == null || faceDetector.isNull()) {
            errores.add("Error al cargar el detector de rostros");
            return createResponse(fotosValidas, errores);
        }

        // Process each photo
        List<FaceData> validFaces = new ArrayList<>();
        for (int i = 0; i < fotos.length; i++) {
            MultipartFile foto = fotos[i];
            String fileName = foto.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "foto_" + i + ".jpg";
            }

            try {
                // Read image
                byte[] imageBytes = foto.getBytes();
                Mat image = imdecode(new Mat(imageBytes), IMREAD_COLOR);
                
                if (image.empty()) {
                    errores.add("Error al leer la imagen: " + fileName);
                    continue;
                }

                // Detect faces
                Mat grayImage = new Mat();
                cvtColor(image, grayImage, COLOR_BGR2GRAY);
                equalizeHist(grayImage, grayImage);

                RectVector faces = new RectVector();
                faceDetector.detectMultiScale(grayImage, faces);

                // Validate single face
                if (faces.size() == 0) {
                    errores.add("No se detectó ningún rostro en: " + fileName);
                    continue;
                } else if (faces.size() > 1) {
                    errores.add("Se detectaron múltiples rostros en: " + fileName);
                    continue;
                }

                // Extract face region
                Rect faceRect = faces.get(0);
                Mat faceRegion = new Mat(grayImage, faceRect);

                // Store valid face
                validFaces.add(new FaceData(fileName, image, faceRegion));
                fotosValidas.add(fileName);

            } catch (Exception e) {
                errores.add("Error al procesar la imagen " + fileName + ": " + e.getMessage());
            }
        }

        // Validate that faces are distinct
        if (validFaces.size() >= 2) {
            List<FaceData> distinctFaces = validateDistinctFaces(validFaces, errores);
            validFaces = distinctFaces;
            fotosValidas.clear();
            for (FaceData face : validFaces) {
                fotosValidas.add(face.getFileName());
            }
        }

        // If we have valid faces, save them and train model
        if (!validFaces.isEmpty()) {
            try {
                saveImages(usuarioId, validFaces);
                trainModel(usuarioId, validFaces);
            } catch (Exception e) {
                errores.add("Error al guardar imágenes o entrenar modelo: " + e.getMessage());
            }
        } else {
            errores.add("No hay suficientes fotos válidas para entrenar el modelo");
        }

        return createResponse(fotosValidas, errores);
    }

    private CascadeClassifier loadFaceDetector() {
        try {
            // Try to load from OpenCV data directory
            String[] possiblePaths = {
                "/usr/share/opencv4/haarcascades/haarcascade_frontalface_default.xml",
                "/usr/local/share/opencv4/haarcascades/haarcascade_frontalface_default.xml",
                System.getProperty("user.home") + "/.javacv/haarcascade_frontalface_default.xml"
            };

            for (String path : possiblePaths) {
                File file = new File(path);
                if (file.exists()) {
                    return new CascadeClassifier(path);
                }
            }

            // If not found, extract from JavaCV resources
            return extractAndLoadCascade();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private CascadeClassifier extractAndLoadCascade() throws IOException {
        // Create a temporary directory for the cascade file
        Path cascadeDir = Paths.get(System.getProperty("user.home"), ".javacv");
        Files.createDirectories(cascadeDir);
        
        Path cascadePath = cascadeDir.resolve("haarcascade_frontalface_default.xml");
        
        if (!Files.exists(cascadePath)) {
            // Load from JavaCV bundled resources
            try (var is = getClass().getResourceAsStream("/org/bytedeco/javacv/haarcascade_frontalface_default.xml")) {
                if (is != null) {
                    Files.copy(is, cascadePath);
                } else {
                    // Use OpenCV's built-in data
                    String opencvData = org.bytedeco.opencv.global.opencv_objdetect.class
                        .getResource("haarcascade_frontalface_default.xml").getPath();
                    if (opencvData != null && new File(opencvData).exists()) {
                        return new CascadeClassifier(opencvData);
                    }
                }
            }
        }
        
        return new CascadeClassifier(cascadePath.toString());
    }

    private List<FaceData> validateDistinctFaces(List<FaceData> faces, List<String> errores) {
        List<FaceData> distinctFaces = new ArrayList<>();
        distinctFaces.add(faces.get(0)); // Add first face

        for (int i = 1; i < faces.size(); i++) {
            FaceData currentFace = faces.get(i);
            boolean isDifferent = true;

            for (FaceData existingFace : distinctFaces) {
                if (areFacesSimilar(currentFace.getFaceRegion(), existingFace.getFaceRegion())) {
                    errores.add("La foto " + currentFace.getFileName() + " es muy similar a " + existingFace.getFileName());
                    isDifferent = false;
                    break;
                }
            }

            if (isDifferent) {
                distinctFaces.add(currentFace);
            }
        }

        return distinctFaces;
    }

    private boolean areFacesSimilar(Mat face1, Mat face2) {
        try {
            // Resize both faces to same size for comparison
            Size size = new Size(100, 100);
            Mat resizedFace1 = new Mat();
            Mat resizedFace2 = new Mat();
            resize(face1, resizedFace1, size);
            resize(face2, resizedFace2, size);

            // Calculate structural similarity
            Mat diff = new Mat();
            absdiff(resizedFace1, resizedFace2, diff);
            
            Scalar meanDiff = mean(diff);
            double similarity = meanDiff.get(0);

            return similarity < SIMILARITY_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveImages(String usuarioId, List<FaceData> faces) throws IOException {
        Path userDir = Paths.get(DATA_IMAGES_PATH, usuarioId);
        Files.createDirectories(userDir);

        for (int i = 0; i < faces.size(); i++) {
            FaceData face = faces.get(i);
            String fileName = "face_" + i + ".jpg";
            Path imagePath = userDir.resolve(fileName);
            imwrite(imagePath.toString(), face.getImage());
        }
    }

    private void trainModel(String usuarioId, List<FaceData> faces) throws IOException {
        // Prepare training data
        MatVector images = new MatVector(faces.size());
        Mat labels = new Mat(faces.size(), 1, CV_32SC1);

        for (int i = 0; i < faces.size(); i++) {
            images.put(i, faces.get(i).getFaceRegion());
            // All faces belong to the same user (label 0)
            labels.ptr(i, 0).putInt(0);
        }

        // Create and train LBPH recognizer
        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        recognizer.train(images, labels);

        // Save model
        Path modelsDir = Paths.get(DATA_MODELS_PATH);
        Files.createDirectories(modelsDir);
        
        Path modelPath = modelsDir.resolve(usuarioId + "_model.yml");
        recognizer.save(modelPath.toString());
    }

    private Map<String, Object> createResponse(List<String> fotosValidas, List<String> errores) {
        Map<String, Object> response = new HashMap<>();
        response.put("fotosValidas", fotosValidas);
        response.put("errores", errores);
        return response;
    }
}
