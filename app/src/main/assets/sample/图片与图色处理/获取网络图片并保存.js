// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架
//这个是Auto.js图标的地址
var url = "https://www.autojs.org/assets/uploads/profile/3-profileavatar.png";
var logo = images.load(url);
var savePath = files.getSdcardPath() + "/auto.js.png";
images.save(logo, savePath);
