//获取用户选择的工作目录和脚本文件
var result = selectScriptFile();
if (!result) {
    exit();
}
var path = result.path;
var workDir = result.workDir;

var window = floaty.window(
    <frame>
        <button id="action" text="开始运行" w="90" h="40" bg="#77ffffff"/>
    </frame>
);

window.exitOnClose();

var execution = null;

window.action.click(()=>{
    if(window.action.getText() == '开始运行'){
        execution = engines.execScriptFile(path);
        window.action.setText('停止运行');
    }else{
        if(execution){
            execution.getEngine().forceStop();
        }
        window.action.setText('开始运行');
    }
});

window.action.longClick(()=>{
   window.setAdjustEnabled(!window.isAdjustEnabled());
   return true;
});

setInterval(()=>{}, 1000);

/**
 * 选择工作目录和脚本文件
 * @returns {{path: string, workDir: string}|null} 脚本路径和工作目录
 */
function selectScriptFile() {
    var dirOptions = [
        "当前脚本目录",
        "脚本根目录",
        "自定义目录..."
    ];
    var i = dialogs.singleChoice("请选择脚本所在目录", dirOptions);
    if (i < 0) {
        return null;
    }
    
    var workDir;
    switch (i) {
        case 0:
            workDir = engines.myEngine().cwd();
            break;
        case 1:
            workDir = files.getSdcardPath() + "/脚本";
            break;
        case 2:
            workDir = dialogs.rawInput("请输入脚本目录路径", files.getSdcardPath());
            if (!workDir) {
                return null;
            }
            break;
    }
    
    if(!files.exists(workDir) || !files.isDir(workDir)){
        toast("目录不存在或不是目录: " + workDir);
        return null;
    }
    
    // 列出目录中的脚本文件
    var scriptFiles = files.listDir(workDir, function(name){
        return name.endsWith(".js");
    });
    
    if (scriptFiles.length === 0) {
        toast("目录中没有脚本文件: " + workDir);
        return null;
    }
    
    // 让用户选择脚本
    var j = dialogs.singleChoice("请选择要运行的脚本", scriptFiles);
    if (j < 0) {
        return null;
    }
    
    return {
        path: files.join(workDir, scriptFiles[j]),
        workDir: workDir
    };
}
