// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架
var logPath = files.getSdcardPath() + "/log.txt";
console.setGlobalLogConfig({
    file: logPath
});
console.log(1);
console.log(2);
console.error(3);
app.viewFile(logPath);