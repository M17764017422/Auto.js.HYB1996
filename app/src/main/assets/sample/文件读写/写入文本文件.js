//获取用户选择的工作目录
var workDir = getWorkDir();
//文件路径
var path = workDir + "/1.txt";
//要写入的文件内容
var text = "Hello, AutoJs";
//以写入模式打开文件
var file = open(path, "w");
//写入文件
file.write(text);
//关闭文件
file.close();
toast("文件已保存到: " + path);

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
            // 当前脚本所在目录
            return engines.myEngine().cwd();
        case 1:
            // 脚本根目录（用户设置的脚本目录）
            return files.getSdcardPath() + "/脚本";
        case 2:
            // 下载目录
            return files.getSdcardPath() + "/Download";
        case 3:
            // 用户自定义输入
            var customPath = dialogs.rawInput("请输入工作目录路径", files.getSdcardPath());
            if (!customPath) {
                exit();
            }
            return customPath;
    }
}