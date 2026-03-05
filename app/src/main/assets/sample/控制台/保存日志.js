//工作目录（当前脚本所在目录）
var workDir = engines.myEngine().cwd();

console.setGlobalLogConfig({
    file: workDir + "/log.txt"
});
console.log(1);
console.log(2);
console.error(3);
app.viewFile(workDir + "/log.txt");