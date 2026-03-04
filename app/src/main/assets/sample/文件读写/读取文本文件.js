// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架
//文件路径
var path = files.getSdcardPath() + "/1.txt";
//打开文件
var file = open(path);
//读取文件的所有内容
var text = file.read();
//打印到控制台
print(text);
//关闭文件
file.close();
console.show();
