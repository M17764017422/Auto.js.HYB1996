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

setInterval(()=>{}, 1000);

var execution = null;

//记录按键被按下时的触摸坐标
var x = 0, y = 0;
//记录按键被按下时的悬浮窗位置
var windowX, windowY;
//记录按键被按下的时间以便判断长按等动作
var downTime;

window.action.setOnTouchListener(function(view, event){
    switch(event.getAction()){
        case event.ACTION_DOWN:
            x = event.getRawX();
            y = event.getRawY();
            windowX = window.getX();
            windowY = window.getY();
            downTime = new Date().getTime();
            return true;
        case event.ACTION_MOVE:
            //移动手指时调整悬浮窗位置
            window.setPosition(windowX + (event.getRawX() - x),
                windowY + (event.getRawY() - y));
            //如果按下的时间超过1.5秒判断为长按，退出脚本
            if(new Date().getTime() - downTime > 1500){
                exit();
            }
            return true;
        case event.ACTION_UP:
            //手指弹起时如果偏移很小则判断为点击
            if(Math.abs(event.getRawY() - y) < 5 && Math.abs(event.getRawX() - x) < 5){
                onClick();
            }
            return true;
    }
    return true;
});

function onClick(){
    if(window.action.getText() == '开始运行'){
        execution = engines.execScriptFile(path);
        window.action.setText('停止运行');
    }else{
        if(execution){
            execution.getEngine().forceStop();
        }
        window.action.setText('开始运行');
    }
}

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
