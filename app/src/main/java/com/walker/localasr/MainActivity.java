package com.walker.localasr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.k2fsa.sherpa.onnx.OnlineModelConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizer;
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 16000;
    private static final int REQUEST_RECORD_AUDIO = 1001;
    private volatile boolean running = false;
    private Thread worker;
    private OnlineRecognizer recognizer;
    private OnlineStream stream;
    private TextView result;
    private TextView status;
    private Button start;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = findViewById(R.id.result);
        status = findViewById(R.id.status);
        start = findViewById(R.id.start);
        start.setOnClickListener(v -> toggle());
    }

    private File modelDir() {
        return new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "model");
    }

    private boolean modelReady() {
        File d = modelDir();
        return new File(d, "encoder-epoch-99-avg-1.int8.onnx").exists()
            && new File(d, "decoder-epoch-99-avg-1.onnx").exists()
            && new File(d, "joiner-epoch-99-avg-1.int8.onnx").exists()
            && new File(d, "tokens.txt").exists();
    }

    private void toggle() {
        if (running) stopAsr(); else startAsr();
    }

    private void startAsr() {
        if (!modelReady()) {
            status.setText("模型未找到：请按 README 下载中英 INT8 模型到 app 专属 model 目录");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        File d = modelDir();
        OnlineTransducerModelConfig transducer = OnlineTransducerModelConfig.builder()
                .setEncoder(new File(d, "encoder-epoch-99-avg-1.int8.onnx").getAbsolutePath())
                .setDecoder(new File(d, "decoder-epoch-99-avg-1.onnx").getAbsolutePath())
                .setJoiner(new File(d, "joiner-epoch-99-avg-1.int8.onnx").getAbsolutePath())
                .build();
        OnlineModelConfig model = OnlineModelConfig.builder()
                .setTransducer(transducer)
                .setTokens(new File(d, "tokens.txt").getAbsolutePath())
                .setNumThreads(1)
                .setDebug(false)
                .build();
        OnlineRecognizerConfig config = OnlineRecognizerConfig.builder()
                .setOnlineModelConfig(model)
                .setDecodingMethod("greedy_search")
                .build();

        recognizer = new OnlineRecognizer(config);
        stream = recognizer.createStream();
        running = true;
        start.setText("停止识别");
        status.setText("● 正在本地实时识别 · 中文 + English");

        worker = new Thread(this::captureLoop, "local-asr");
        worker.start();
    }

    private void captureLoop() {
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSamples = Math.max(1600, minBuffer / 2);
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSamples * 2);
        short[] pcm = new short[bufferSamples];
        float[] samples = new float[bufferSamples];
        try {
            recorder.startRecording();
            while (running) {
                int n = recorder.read(pcm, 0, pcm.length);
                if (n <= 0) continue;
                for (int i = 0; i < n; i++) samples[i] = pcm[i] / 32768.0f;
                stream.acceptWaveform(samples, SAMPLE_RATE);
                while (recognizer.isReady(stream)) recognizer.decode(stream);
                final String text = recognizer.getResult(stream).getText();
                if (!text.isEmpty()) runOnUiThread(() -> result.setText(text));
                if (recognizer.isEndpoint(stream)) recognizer.reset(stream);
            }
        } finally {
            try { recorder.stop(); } catch (Exception ignored) {}
            recorder.release();
        }
    }

    private void stopAsr() {
        running = false;
        if (worker != null) {
            try { worker.join(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        if (stream != null) { stream.release(); stream = null; }
        if (recognizer != null) { recognizer.release(); recognizer = null; }
        start.setText("开始实时识别");
        status.setText("已停止 · 全程本地");
    }

    @Override protected void onDestroy() {
        stopAsr();
        super.onDestroy();
    }
}
