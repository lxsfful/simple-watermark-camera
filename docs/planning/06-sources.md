# 调研来源

调研日期：2026-06-13

## GitHub / 开源项目

- Android Camera Samples: https://github.com/android/camera-samples
- CameraX-Helper: https://github.com/Knightwood/CameraX-Helper
- arindamxd/android-camerax: https://github.com/arindamxd/android-camerax
- akexorcist/CameraX-Sample: https://github.com/akexorcist/CameraX-Sample
- Shah-Sahab/WatermarkCamera: https://github.com/Shah-Sahab/WatermarkCamera
- CoderMikeHe/MHWatermarkCamera: https://github.com/CoderMikeHe/MHWatermarkCamera
- Phimp.me Android: https://github.com/warpthatdot/phimpme-android
- cammingcai/AndroidCamera: https://github.com/cammingcai/AndroidCamera
- yunianvh/CameraDemo: https://github.com/yunianvh/CameraDemo

## 官方/技术参考方向

- Android CameraX 官方文档：CameraX、Preview、ImageCapture、PreviewView
- Android MediaStore / scoped storage
- AndroidX ExifInterface
- Android SensorManager orientation / rotation vector
- Android Location / Fused Location Provider

## 后续建议补充调研

当前 GitHub CLI 未登录，`gh search repos` / `gh search code` 无法执行。后续建议：

```bash
gh auth login
gh search repos "watermark camera android" --limit 50
gh search code "CameraX watermark" --limit 50
gh search code "ImageCapture MediaStore CameraX" --limit 50
gh search code "ExifInterface Canvas watermark Android" --limit 50
```

重点确认：

- 最新 CameraX + Compose 示例
- MediaStore 保存到 Pictures 子目录的最佳实践
- EXIF orientation 修正代码模式
- 低内存图片水印渲染策略
