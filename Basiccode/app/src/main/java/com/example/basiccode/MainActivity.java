package com.example.basiccode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    PreviewView previewView;
    Button startButton;
    Button stopButton;
    Button recogButton;
    ImageView imageView;
    String TAG="MainActivity";
    ProcessCameraProvider processCameraProvider;
    int lensFacing = CameraSelector.LENS_FACING_BACK;
    ImageCapture imageCapture;

    FirebaseVisionImage visionImage;

    private static final int REQUEST_IMAGE_CODE=101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView=findViewById(R.id.previewView);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        recogButton=findViewById(R.id.RecogButton); //인식버튼
        imageView=findViewById(R.id.imageview);


        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);

        try{
            processCameraProvider=ProcessCameraProvider.getInstance(this).get();
        }
        catch (ExecutionException e){
            e.printStackTrace();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    bindPreview();
                    bindImageCapture();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCameraProvider.unbindAll();
            }
        });

        recogButton.setOnClickListener(new View.OnClickListener() {
            Bitmap bitmap;
            Bitmap rotatedBitmap;
            @Override
            public void onClick(View v) {
                imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this),
                        new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image){
                                @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
                                Image mediaImage=image.getImage();
                                bitmap = mediaImageToBitmap(mediaImage);
                                Log.d("MainActivity", Integer.toString(bitmap.getWidth())); //4128
                                Log.d("MainActivity", Integer.toString(bitmap.getHeight())); //3096
                                rotatedBitmap=rotateBitmap(bitmap,image.getImageInfo().getRotationDegrees());
                                imageView.setImageBitmap(rotatedBitmap);
                                super.onCaptureSuccess(image);

                                //OCR 부분
                                visionImage=FirebaseVisionImage.fromBitmap(rotatedBitmap);
                                FirebaseVisionTextRecognizer textRecognizer=FirebaseVision.getInstance().getOnDeviceTextRecognizer();

                                textRecognizer.processImage(visionImage)
                                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                            @Override
                                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                                processTextRecognitionResult(firebaseVisionText);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(MainActivity.this, "wrong", Toast.LENGTH_SHORT).show();
                                            }
                                        });

//                                //Korean script library
//                                TextRecognizer recognizer =
//                                        TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

                            }
                        });
            };
        });
    }
    private void processTextRecognitionResult(FirebaseVisionText text) {
        StringBuilder fullText = new StringBuilder();
        List<FirebaseVisionText.TextBlock> blocks = text.getTextBlocks();
        if (blocks.size() == 0) {
            Log.d("TAG", "No text found");
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    fullText.append(elements.get(k).getText());
                    fullText.append(" ");
                }
            }
        }

        //Set text to display
        //detectedText.setText(fullText.toString());
        showDialog_OCR(fullText.toString());
    }
    void bindPreview(){
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        CameraSelector cameraSelector=new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) //디폴트 표준 비율
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        processCameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }
    void bindImageCapture() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        imageCapture = new ImageCapture.Builder()
                .build();
        //bindToLifecycle은 Camera객체를 반환
        processCameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
    }
    @Override
    protected void onPause() {
        super.onPause();
        processCameraProvider.unbindAll();
    }

    //사용자정의메소드
    public static Bitmap mediaImageToBitmap(Image mediaImage) {
        byte[] byteArray = mediaImageToByteArray(mediaImage);
        Bitmap bitmap = null;
        if (mediaImage.getFormat() == ImageFormat.JPEG) {
            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        }
        else if (mediaImage.getFormat() == ImageFormat.YUV_420_888) {
            YuvImage yuvImage = new YuvImage(byteArray, ImageFormat.NV21, mediaImage.getWidth(), mediaImage.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }

        return bitmap;
    }
    public static byte[] mediaImageToByteArray(Image mediaImage) {
        byte[] byteArray = null;
        if (mediaImage.getFormat() == ImageFormat.JPEG) {
            ByteBuffer buffer0 = mediaImage.getPlanes()[0].getBuffer();
            buffer0.rewind();
            int buffer0_size = buffer0.remaining();
            byteArray = new byte[buffer0_size];
            buffer0.get(byteArray, 0, buffer0_size);
        }
        else if (mediaImage.getFormat() == ImageFormat.YUV_420_888) {
            ByteBuffer buffer0 = mediaImage.getPlanes()[0].getBuffer();
            ByteBuffer buffer2 = mediaImage.getPlanes()[2].getBuffer();
            int buffer0_size = buffer0.remaining();
            int buffer2_size = buffer2.remaining();
            byteArray = new byte[buffer0_size + buffer2_size];
            buffer0.get(byteArray, 0, buffer0_size);
            buffer2.get(byteArray, buffer0_size, buffer2_size);
        }

        return byteArray;
    }
    public static Bitmap rotateBitmap(Bitmap bitmap, float degree){
        try{
            Matrix matrix = new Matrix();
            matrix.postRotate(degree);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            return rotatedBitmap;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //OCR 대화상자
    void showDialog_OCR(String Text){
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this)
                .setMessage("차량번호\n"+Text+"\n 시간\n "+CameraTime.Cameratime())
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog dialog=builder.create();
        dialog.show();
    }
}