package com.walker.localasr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 16000;
    private static final int REQUEST_RECORD_AUDIO = 1001;
    private volatile boolean running = false;
    private Thread worker;
    private OnlineRecognizer recognizer;
    private OnlineStream stream;
    private TextView result, status;
    private Button start;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main);
        result = findViewById(R.id.result); status = findViewById(R.id.status); start = findViewById(R.id.start);
        start.setOnClickListener(v -> toggle()); start.setEnabled(false);
        new Thread(() -> { try { prepareModel(); runOnUiThread(() -> { status.setText("模型已就绪 · 中文 + English · 全程本地"); start.setEnabled(true); }); }
            catch (Exception e) { runOnUiThread(() -> status.setText("模型准备失败: " + e.getMessage())); } }, "model-prepare").start();
    }
    private File modelDir() { return new File(getFilesDir(), "model"); }
    private void copyAsset(String name) throws IOException { File out = new File(modelDir(), name); if (out.exists() && out.length() > 0) return; modelDir().mkdirs();
        try (InputStream in = getAssets().open("model/" + name); FileOutputStream fos = new FileOutputStream(out)) { byte[] buf = new byte[1024 * 1024]; int n; while ((n = in.read(buf)) != -1) fos.write(buf, 0, n); } }
    private void prepareModel() throws IOException { copyAsset("encoder-epoch-99-avg-1.int8.onnx"); copyAsset("decoder-epoch-99-avg-1.onnx"); copyAsset("joiner-epoch-99-avg-1.int8.onnx"); copyAsset("tokens.txt"); copyAsset("bpe.model"); }
    private boolean modelReady() { File d = modelDir(); return new File(d,"encoder-epoch-99-avg-1.int8.onnx").exists() && new File(d,"decoder-epoch-99-avg-1.onnx").exists() && new File(d,"joiner-epoch-99-avg-1.int8.onnx").exists() && new File(d,"tokens.txt").exists() && new File(d,"bpe.model").exists(); }
    private void toggle() { if (running) stopAsr(); else startAsr(); }
    private void startAsr() { if (!modelReady()) { status.setText("模型仍在准备，请稍候"); return; }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_RECORD_AUDIO); return; }
        File d=modelDir(); OnlineTransducerModelConfig t=OnlineTransducerModelConfig.builder().setEncoder(new File(d,"encoder-epoch-99-avg-1.int8.onnx").getAbsolutePath()).setDecoder(new File(d,"decoder-epoch-99-avg-1.onnx").getAbsolutePath()).setJoiner(new File(d,"joiner-epoch-99-avg-1.int8.onnx").getAbsolutePath()).build();
        OnlineModelConfig m=OnlineModelConfig.builder().setTransducer(t).setTokens(new File(d,"tokens.txt").getAbsolutePath()).setModelingUnit("bpe").setBpeVocab(new File(d,"bpe.model").getAbsolutePath()).setNumThreads(2).setDebug(false).build();
        OnlineRecognizerConfig c=OnlineRecognizerConfig.builder().setOnlineModelConfig(m).setDecodingMethod("greedy_search").build(); recognizer=new OnlineRecognizer(c); stream=recognizer.createStream(); running=true; start.setText("停止识别"); status.setText("● 本地实时识别 · 中文 + English · small INT8"); worker=new Thread(this::captureLoop,"local-asr"); worker.start(); }
    private void captureLoop() { int min=AudioRecord.getMinBufferSize(SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT); int bs=Math.max(1600,min/2); AudioRecord r=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,SAMPLE_RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bs*2); short[] p=new short[bs]; float[] s=new float[bs];
        try { r.startRecording(); while(running){ int n=r.read(p,0,p.length); if(n<=0)continue; for(int i=0;i<n;i++)s[i]=p[i]/32768.0f; stream.acceptWaveform(s,SAMPLE_RATE); while(recognizer.isReady(stream))recognizer.decode(stream); String text=recognizer.getResult(stream).getText(); runOnUiThread(()->result.setText(text)); if(recognizer.isEndpoint(stream))recognizer.reset(stream); } } finally { try{r.stop();}catch(Exception ignored){} r.release(); } }
    private void stopAsr(){ running=false; if(worker!=null){try{worker.join(500);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}} if(stream!=null){stream.release();stream=null;} if(recognizer!=null){recognizer.release();recognizer=null;} start.setText("开始识别"); status.setText("已停止 · 全程本地"); }
    @Override protected void onDestroy(){stopAsr();super.onDestroy();}
}
