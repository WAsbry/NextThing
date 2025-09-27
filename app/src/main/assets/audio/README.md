# 预置音频资源

## 支持的音频格式
- MP3 (推荐)
- OGG
- WAV
- AAC

## 音频要求
- 时长：1-5秒
- 大小：建议不超过100KB
- 采样率：44100Hz 或 22050Hz
- 位深度：16bit

## 音频文件命名规范
```
[类别]_[名称].mp3
例如：light_ding.mp3, standard_beep.mp3, urgent_alarm.mp3
```

## 当前包含的音频资源

### 轻快提示音
- ding.mp3 - 叮当声
- chime.mp3 - 风铃声
- bell.mp3 - 铃声

### 标准提示音
- beep.mp3 - 哔哔声
- tone.mp3 - 音调
- click.mp3 - 点击声

### 紧急提示音
- alarm.mp3 - 警报声
- siren.mp3 - 警笛声
- horn.mp3 - 喇叭声

### 自然音效
- bird.mp3 - 鸟叫声
- water.mp3 - 水滴声
- wind.mp3 - 风声

## 添加新音频
1. 将音频文件放入此目录
2. 更新 PresetAudio.kt 枚举
3. 文件名需要与枚举中的 fileName 匹配