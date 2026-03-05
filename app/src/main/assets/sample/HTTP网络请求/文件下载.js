//获取用户选择的工作目录
var workDir = getWorkDir();

var url = "http://www.autojs.org/assets/uploads/profile/3-profileavatar.png";
var res = http.get(url);
if(res.statusCode != 200){
    toast("请求失败");
    exit();
}
var path = workDir + "/download_" + new Date().getTime() + ".png";
files.writeBytes(path, res.body.bytes());
toast("下载成功: " + path);
app.viewFile(path);

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
    var i = dialogs.singleChoice("请选择保存目录", options);
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
            var customPath = dialogs.rawInput("请输入保存目录路径", files.getSdcardPath());
            if (!customPath) {
                exit();
            }
            return customPath;
    }
}