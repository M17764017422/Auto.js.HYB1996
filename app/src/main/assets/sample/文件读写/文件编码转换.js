//工作目录（当前脚本所在目录）
var workDir = engines.myEngine().cwd();

//以UTF-8编码打开文件1.txt
var f = open(workDir + "/1.txt", "r", "utf-8");
//读取文件所有内容
var text = f.read();
//关闭文件
f.close();
//以gbk编码打开文件2.txt
var out = open(workDir + "/2.txt", "w", "gbk");
//写入内容
out.write(text);
//关闭文件
out.close();