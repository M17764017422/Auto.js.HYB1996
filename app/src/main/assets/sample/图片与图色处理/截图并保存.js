// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架
if(!requestScreenCapture()){
    toast("请求截图失败");
    exit();
}
var img = captureScreen();
var savePath = files.getSdcardPath() + "/1.png";
images.saveImage(img, savePath);
