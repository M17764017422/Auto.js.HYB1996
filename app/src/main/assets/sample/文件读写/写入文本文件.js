// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架
//文件路径
var path = files.getSdcardPath() + "/1.txt";
//要写入的文件内容
var text = "Hello, AutoJs";
//以写入模式打开文件
var file = open(path, "w");
//写入文件
file.write(text);
//关闭文件
file.close();
