package com.example.bluetooth;

import android.Manifest;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.IOException;
import java.util.List;

public class ClassificationActivity extends AppCompatActivity {
    private static final String MODEL_FILE = "yamnet.tflite";
    private static final float MINIMUM_DISPLAY_THRESHOLD = 0.3f;

    private AudioClassifier mAudioClassifier;
    private AudioRecord mAudioRecord;
    private long classificationInterval = 1000;  // 샘플링 주기: 0.5초
    private Handler mHandler;

    TextView mTvResult;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.classification_main);

        HandlerThread handlerThread = new HandlerThread("backgroundThread");
        handlerThread.start();
        mHandler = HandlerCompat.createAsync(handlerThread.getLooper());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, 4);
//                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 4);
        }
        startAudioClassification();
    }

    private void startAudioClassification() {
        mTvResult = (TextView)findViewById(R.id.clfResult);
        if(mAudioClassifier != null) return;
        try {
            AudioClassifier classifier = AudioClassifier.createFromFile(this, MODEL_FILE);
            TensorAudio audioTensor = classifier.createInputTensorAudio();

            AudioRecord record = classifier.createAudioRecord();
            record.startRecording();

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    audioTensor.load(record);  // 현재 사용중인 SDK 보다 높은 버전 필요할 수도 있음.
                    List<Classifications> output = classifier.classify(audioTensor);
                    List<Category> filterModelOutput = output.get(0).getCategories();
                    for(Category c : filterModelOutput) {
                        if(c.getScore() > MINIMUM_DISPLAY_THRESHOLD) {
                            String babyStatus = c.getLabel();
                            System.out.println("TensorAudio_java" + "label: " + c.getLabel() + " score: " + c.getScore());
                            // runOnUiThread 사용 안할 시 에러 발생 (android.view.ViewRootImpl$CalledFromWrongThreadException)
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    switch (babyStatus) {
                                        case "Speech":
                                            mTvResult.setText("SPEECH");
                                            break;
                                        case "Clapping":
                                            mTvResult.setText("CLAPPING");
                                            break;
                                        case "Finger snapping":
                                            mTvResult.setText("FINGER SNAPPING");
                                            break;
                                        case "Whistling":
                                            mTvResult.setText("WHISTLING");
                                            break;
                                        case "Typing": case "Computer keyboard":
                                            mTvResult.setText("TYPING");
                                            break;
                                        default:
                                            mTvResult.setText("LISTENING..");
                                            break;
                                    }
                                }
                            });

                        }
                    }
                    mHandler.postDelayed(this, classificationInterval);
                }
            };

            mHandler.post(run);
            mAudioClassifier = classifier;
            mAudioRecord = record;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void stopAudioClassification() {
        mHandler.removeCallbacksAndMessages(null);
        mAudioRecord.stop();
        mAudioRecord = null;
        mAudioClassifier = null;
    }

    @Override
    protected void onDestroy() {
        stopAudioClassification();
        super.onDestroy();
    }
}

