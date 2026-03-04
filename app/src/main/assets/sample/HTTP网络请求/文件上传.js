//如果遇到SocketTimeout的异常，重新多运行几次脚本即可
// SAF说明: 使用 files.getSdcardPath() 获取存储路径，兼容 Android 11+ 的存储访问框架

console.show();
example1();
example2();
example3();
example4();
example5();

function example1(){
    var res = http.postMultipart("http://posttestserver.com/post.php", {
        "file": open(files.getSdcardPath() + "/1.txt")
    });
    log("例子1:");
    log(res.body.string());
}

function example2(){
    var res = http.postMultipart("http://posttestserver.com/post.php", {
        "file": ["1.txt", files.getSdcardPath() + "/1.txt"]
    });
    log("例子2:");
    log(res.body.string());
}

function example3(){
    var res = http.postMultipart("http://posttestserver.com/post.php", {
        "file": ["1.txt", "text/plain", files.getSdcardPath() + "/1.txt"]
    });
    log("例子3:");
    log(res.body.string());
}

function example4(){
    var res = http.postMultipart("http://posttestserver.com/post.php", {
        "file": open(files.getSdcardPath() + "/1.txt"),
        "aKey": "aValue"
    });
    log("例子4:");
    log(res.body.string());
}
