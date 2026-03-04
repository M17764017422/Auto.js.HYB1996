// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架
var url = "http://www.autojs.org/assets/uploads/profile/3-profileavatar.png";
var res = http.get(url);
if(res.statusCode != 200){
    toast("请求失败");
}
var savePath = files.getSdcardPath() + "/1.png";
files.writeBytes(savePath, res.body.bytes());
toast("下载成功");
app.viewFile(savePath);
