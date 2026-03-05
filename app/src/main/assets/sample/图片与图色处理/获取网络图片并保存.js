//工作目录（当前脚本所在目录）
var workDir = engines.myEngine().cwd();
//这个是Auto.js图标的地址
var url = "https://www.autojs.org/assets/uploads/profile/3-profileavatar.png";
var logo = images.load(url);
//保存到工作目录
images.save(logo, workDir + "/auto.js.png");
