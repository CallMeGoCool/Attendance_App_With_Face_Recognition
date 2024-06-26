package com.example.attendanceapp;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FacialRecognition {
    private StudentAdapter adapter;
    private List<StudentItem> studentItems;

    private static final String IMAGES_FOLDER_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/ClassTrack";
    private Map<String, Mat> encodedFaces;
    private Context context;
    private CascadeClassifier faceDetector;

    public FacialRecognition(Context context, String className, String subjectName,StudentAdapter adapter, List<StudentItem> studentItems) {
        this.context = context;
        this.adapter=adapter;
        this.studentItems=studentItems;
        System.loadLibrary("opencv_java4");
        encodedFaces = new HashMap<>();
        loadEncodingImages(className, subjectName);
        faceDetector = loadCascadeClassifier();
    }

    private CascadeClassifier loadCascadeClassifier() {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            CascadeClassifier cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (cascadeClassifier.empty()) {
                Log.e("FacialRecognition", "Failed to load cascade classifier");
                return null;
            } else {
                Log.i("FacialRecognition", "Cascade classifier loaded successfully");
                return cascadeClassifier;
            }
        } catch (Exception e) {
            Log.e("FacialRecognition", "Error loading cascade classifier", e);
            return null;
        }
    }

    private void loadEncodingImages(String className, String subjectName) {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        String specificFolderPath = IMAGES_FOLDER_NAME + "/" + className + "/" + subjectName;
        File folder = new File(specificFolderPath);
        File[] studentDirs = folder.listFiles();
        if (studentDirs != null) {
            for (File studentDir : studentDirs) {
                if (studentDir.isDirectory()) {
                    File[] files = studentDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                executor.submit(() -> {
                                    String fileName = file.getName();
                                    String personName = fileName.substring(0, fileName.lastIndexOf('.'));
                                    Mat encodedFace = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                                    if (encodedFace.empty()) {
                                        Log.e("FacialRecognition", "Failed to read the image: " + file.getAbsolutePath());
                                    } else {
                                        // Apply histogram equalization
                                        Imgproc.equalizeHist(encodedFace, encodedFace);
                                        encodedFaces.put(personName, encodedFace);
                                    }
                                });
                            }
                        }
                    } else {
                        Log.e("FacialRecognition", "Failed to list files in the directory: " + studentDir.getAbsolutePath());
                    }
                }
            }
        } else {
            Log.e("FacialRecognition", "Failed to list files in the directory: " + specificFolderPath);
        }
        executor.shutdown();
    }

    public void compareImageWithStoredImages(String imagePath) {
        ExecutorService outerExecutor = Executors.newFixedThreadPool(1);
        ExecutorService innerExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        outerExecutor.submit(() -> {
            try {
                Mat image = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);
                if (image.empty()) {
                    Log.e("FacialRecognition", "Failed to open the image.");
                    return;
                }


                // Resize the image to 33% of its original size
                Mat resizedImage = new Mat();
                Size newSize = new Size(image.cols() * 0.50, image.rows() * 0.50);
                Imgproc.resize(image, resizedImage, newSize);


                // Apply histogram equalization
                Imgproc.equalizeHist(image, image);

                MatOfRect faceDetections = new MatOfRect();
                faceDetector.detectMultiScale(image, faceDetections);

                final String[] bestMatchPerson = new String[1];
                final double[] bestMatchValue = new double[1];

                for (Rect rect : faceDetections.toArray()) {
                    innerExecutor.submit(() -> {
                        Mat face = new Mat(image, rect);
                        for (Map.Entry<String, Mat> entry : encodedFaces.entrySet()) {
                            String personName = entry.getKey();
                            Mat encodedFace = entry.getValue();

                            // Resize the encodedFace if it's larger than the face
                            if (encodedFace.rows() > face.rows() || encodedFace.cols() > face.cols()) {
                                Imgproc.resize(encodedFace, encodedFace, face.size());
                            }

                            Mat result = new Mat();
                            Imgproc.matchTemplate(face, encodedFace, result, Imgproc.TM_CCOEFF_NORMED);
                            Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result);
                            if (minMaxLocResult.maxVal > bestMatchValue[0]) {
                                bestMatchValue[0] = minMaxLocResult.maxVal;
                                bestMatchPerson[0] = personName;
                            }
                        }
                    });
                }

                innerExecutor.shutdown();
                try {
                    // Wait for all inner tasks to finish
                    if (!innerExecutor.awaitTermination(90, TimeUnit.SECONDS)) {
                        innerExecutor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    innerExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }

                if (bestMatchPerson[0] != null && bestMatchValue[0] > 0.7) { // Adjust the threshold as needed
                    Log.i("FacialRecognition", "Hello, best match found with " + bestMatchPerson[0]);
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Hello, best match found with " + bestMatchPerson[0], Toast.LENGTH_SHORT).show();

                        // Extract the roll number from the bestMatchPerson[0] string
                        String bestMatchPersonRoll = bestMatchPerson[0].substring(0, bestMatchPerson[0].indexOf("(")).trim();
                        int roll = Integer.parseInt(bestMatchPersonRoll);

                        // Find the StudentItem with the matching roll number
                        for (int i = 0; i < studentItems.size(); i++) {
                            StudentItem item = studentItems.get(i);
                            if (item.getRoll() == roll) {
                                // Change the status and notify the adapter
                                changeStatus(i);
                                break;
                            }
                        }
                    });
                }else {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "No match found", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e("FacialRecognition", "Error occurred while comparing image with stored images", e);
            }
        });
        outerExecutor.shutdown();
        try {
            // Wait for all outer tasks to finish
            if (!outerExecutor.awaitTermination(90, TimeUnit.SECONDS)) {
                outerExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            outerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void compareVideoWithStoredImages(String videoPath) {
        ExecutorService outerExecutor = Executors.newFixedThreadPool(1);
        ExecutorService innerExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        outerExecutor.submit(() -> {
            try {
                VideoCapture videoCapture = new VideoCapture(videoPath);
                if (!videoCapture.isOpened()) {
                    Log.e("FacialRecognition", "Failed to open the video.");
                    return;
                }

                Mat frame = new Mat();
                final String[] bestMatchPerson = new String[1];
                final double[] bestMatchValue = new double[1];

                while (videoCapture.read(frame)) {
                    Mat grayFrame = new Mat();
                    Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);


                    // Resize the frame to 33% of its original size
                    Mat resizedFrame = new Mat();
                    Size newSize = new Size(grayFrame.cols() * 0.50, grayFrame.rows() * 0.50);
                    Imgproc.resize(grayFrame, resizedFrame, newSize);


                    // Apply histogram equalization
                    Imgproc.equalizeHist(grayFrame, grayFrame);

                    MatOfRect faceDetections = new MatOfRect();
                    faceDetector.detectMultiScale(grayFrame, faceDetections);

                    for (Rect rect : faceDetections.toArray()) {
                        innerExecutor.submit(() -> {
                            Mat face = new Mat(grayFrame, rect);
                            for (Map.Entry<String, Mat> entry : encodedFaces.entrySet()) {
                                String personName = entry.getKey();
                                Mat encodedFace = entry.getValue();

                                // Resize the encodedFace if it's larger than the face
                                if (encodedFace.rows() > face.rows() && encodedFace.cols() > face.cols()) {
                                    Imgproc.resize(encodedFace, encodedFace, face.size());
                                }

                                Mat result = new Mat();
                                Imgproc.matchTemplate(face, encodedFace, result, Imgproc.TM_CCOEFF_NORMED);
                                Core.MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result);
                                if (minMaxLocResult.maxVal > bestMatchValue[0]) {
                                    bestMatchValue[0] = minMaxLocResult.maxVal;
                                    bestMatchPerson[0] = personName;
                                }
                            }
                        });
                    }
                }

                videoCapture.release();

                innerExecutor.shutdown();
                try {
                    // Wait for all inner tasks to finish
                    if (!innerExecutor.awaitTermination(90, TimeUnit.SECONDS)) {
                        innerExecutor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    innerExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }

                if (bestMatchPerson[0] != null && bestMatchValue[0] > 0.7) { // Adjust the threshold as needed
                    Log.i("FacialRecognition", "Hello, best match found with " + bestMatchPerson[0]);
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Hello, best match found with " + bestMatchPerson[0], Toast.LENGTH_SHORT).show();

                        // Extract the roll number from the bestMatchPerson[0] string
                        String bestMatchPersonRoll = bestMatchPerson[0].substring(0, bestMatchPerson[0].indexOf("(")).trim();
                        int roll = Integer.parseInt(bestMatchPersonRoll);

                        // Find the StudentItem with the matching roll number
                        for (int i = 0; i < studentItems.size(); i++) {
                            StudentItem item = studentItems.get(i);
                            if (item.getRoll() == roll) {
                                // Change the status and notify the adapter
                                changeStatus(i);
                                break;
                            }
                        }
                    });
                }else {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "No match found", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e("FacialRecognition", "Error occurred while comparing video with stored images", e);
            }
        });
        outerExecutor.shutdown();
        try {
            // Wait for all outer tasks to finish
            if (!outerExecutor.awaitTermination(90, TimeUnit.SECONDS)) {
                outerExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            outerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    private void changeStatus(int position) {
        if (position >= 0 && position < studentItems.size()) {
            studentItems.get(position).setStatus("P");
            studentItems.get(position).setChanged(true);
            adapter.notifyItemChanged(position);
        } else {
            Log.e("FacialRecognition", "Invalid position: " + position);
        }
    }
}
