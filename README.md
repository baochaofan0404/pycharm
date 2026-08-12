# Local ASR

中文 + English 本地实时语音识别 Android 原型，目标设备：OPPO K11。

## 当前方案

- Android：sherpa-onnx
- 模型：`sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16`
- 推理：INT8 encoder + INT8 joiner + FP32 decoder
- 语言：中文 / English / 中英混说
- 音频：16 kHz / mono / PCM16
- 推理线程：1
- 网络：识别阶段不需要网络

官方模型文档把这个模型明确标为 small bilingual zh-en。INT8 encoder 约 41 MB、FP32 decoder 约 14 MB、INT8 joiner 约 3.1 MB，另有约 240 KB 的 BPE vocabulary；模型总量远小于普通 bilingual Zipformer。模型还提供 64/96 chunk 配置，chunk 越大通常 RTF 越低。

## 模型下载

```powershell
curl.exe -L -o model.tar.bz2 https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16.tar.bz2
```

解压后需要：

```text
model/
  encoder-epoch-99-avg-1.int8.onnx
  decoder-epoch-99-avg-1.onnx
  joiner-epoch-99-avg-1.int8.onnx
  tokens.txt
  bpe.model
```

Android App 当前从自己的 app 专属目录读取模型：

```text
Android/data/com.walker.localasr/files/Download/model/
```

开发测试时可以用 ADB 推入：

```powershell
adb shell mkdir -p /sdcard/Android/data/com.walker.localasr/files/Download/model
adb push model/. /sdcard/Android/data/com.walker.localasr/files/Download/model/
```

## 当前功能

- 麦克风实时识别
- 中文 + English
- 本地 CPU 推理
- INT8 模型
- 增量结果显示
- endpoint 后自动 reset

## 下一阶段

1. 自动下载并校验模型
2. VAD
3. 悬浮窗字幕
4. 视频/系统音频捕获研究
5. 字幕稳定器：减少实时结果闪烁
6. 中英混合专用文本后处理
7. K11 真机 benchmark：RTF / CPU / RAM / 温度 / 功耗
8. Windows 离线高精度转写器

## 上游依据

sherpa-onnx 已提供 Android Java demo、OnlineRecognizer API，以及 streaming Zipformer bilingual zh-en 示例。本项目把它收敛成面向中英实时字幕的轻量 Android App。
