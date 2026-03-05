//获取用户选择的工作目录
var workDir = getWorkDir();
//文件路径
var path = workDir + "/1.txt";
//打开文件
var file = open(path);
//读取文件的所有内容
var text = file.read();
//打印到控制台
print(text);
//关闭文件
file.close();
console.show();

/**
 * 获取用户选择的工作目录
 * @returns {string} 工作目录路径
 */
function getWorkDir() {
    var options = [
        "当前脚本目录",
        "脚本根目录",
        "下载目录",
        "自定义路径..."
    ];
    var i = dialogs.singleChoice("请选择工作目录", options);
    if (i < 0) {
        exit();
    }
    switch (i) {
        case 0:
            return engines.myEngine().cwd();
        case 1:
            return files.getSdcardPath() + "/脚本";
        case 2:
            return files.getSdcardPath() + "/Download";
        case 3:
            var customPath = dialogs.rawInput("请输入工作目录路径", files.getSdcardPath());
            if (!customPath) {
                exit();
            }
            return customPath;
    }
}