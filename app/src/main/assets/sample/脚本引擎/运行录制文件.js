//工作目录（当前脚本所在目录）
var workDir = engines.myEngine().cwd();
var scriptFiles = files.listDir(workDir, function(name){
    return name.endsWith(".auto");
});
var i = dialogs.singleChoice("请选择要运行的脚本", scriptFiles);
if(i < 0){
    exit();
}
var path = files.join(workDir, scriptFiles[i]);
engines.execAutoFile(path);