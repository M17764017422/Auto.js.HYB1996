// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架
var superMario = images.read("./super_mario.jpg");
var block = images.read("./block.png");
var points = images.matchTemplate(superMario, block, {
    threshold: 0.8
}).points;

toastLog(points);

var canvas = new Canvas(superMario);
var paint = new Paint();
paint.setColor(colors.parseColor("#2196F3"));
points.forEach(point => {
    canvas.drawRect(point.x, point.y, point.x + block.width, point.y + block.height, paint);
});
var image = canvas.toImage();
var savePath = files.getSdcardPath() + "/tmp.png";
images.save(image, savePath);

app.viewFile(savePath);

superMario.recycle();
block.recycle();
image.recycle();