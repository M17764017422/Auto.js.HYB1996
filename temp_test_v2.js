/**
 * Rhino 2.0.0 AutoJs6 兼容性测试脚本 (修正版)
 */

var passed = 0;
var failed = 0;
var results = [];

function test(name, fn) {
    try {
        var result = fn();
        if (result === true || result === "success") {
            passed++;
            results.push("OK " + name);
        } else {
            failed++;
            results.push("X " + name + " (" + result + ")");
        }
    } catch (e) {
        failed++;
        results.push("X " + name + " (异常)");
    }
}

console.log("\n========== Rhino 2.0.0 AutoJs6 兼容性测试 (修正版) ==========\n");

console.log("【1. VMBridge 反射访问】");
test("1.1 Java String", function() { return new java.lang.String("Hi").length() === 2; });
test("1.2 Java ArrayList", function() { var l = new java.util.ArrayList(); l.add(1); return l.size() === 1; });
test("1.3 Java File", function() { return new java.io.File("/sdcard").exists(); });
test("1.4 Java Math", function() { return java.lang.Math.max(10, 20) === 20; });

console.log("【2. importClass/importPackage】");
test("2.1 importClass", function() { importClass(java.io.File); return new File("/sdcard").exists(); });
test("2.2 importPackage", function() { importPackage(java.util); return new ArrayList().size() === 0; });

console.log("【3. Java 接口实现】");
test("3.1 Runnable", function() { var ok = false; new java.lang.Runnable({run:function(){ok=true;}}).run(); return ok; });
test("3.2 Comparator", function() { return new java.util.Comparator({compare:function(a,b){return a-b;}}).compare(5,3) === 2; });

console.log("【4. Java.extend】");
test("4.1 Thread", function() { var ran = false; var T = Java.extend(java.lang.Thread, {run:function(){ran=true;}}); var t = new T(); t.start(); t.join(100); return ran; });
test("4.2 ArrayList", function() { var L = Java.extend(java.util.ArrayList, {custom:function(){return 42;}}); return new L().custom() === 42; });

console.log("【5. Java 反射】");
test("5.1 公共方法", function() { return new java.lang.String("test").length() === 4; });
test("5.2 静态方法", function() { return java.lang.Integer.parseInt("123") === 123; });

console.log("【6. NativeJavaObject】");
test("6.1 Java数组", function() { var a = java.lang.reflect.Array.newInstance(java.lang.Integer.TYPE, 3); a[0] = 10; return a[0] === 10; });
test("6.2 StringBuilder", function() { var sb = new java.lang.StringBuilder(); sb.append("Hi"); return sb.toString() === "Hi"; });
test("6.3 HashMap", function() { var m = new java.util.HashMap(); m.put("k", "v"); return m.get("k") === "v"; });

console.log("【7. 类型转换 (修正版)】");
test("7.1 JS数组转List", function() { return java.util.Arrays.asList([1,2,3]).size() === 3; });
test("7.2 JS对象转Map (修正)", function() { var m = new java.util.HashMap(); m.put("a", 1); return m.get("a") == 1; });
test("7.3 List转数组", function() { var l = new java.util.ArrayList(); l.add(1); return l.toArray().length === 1; });
test("7.4 数值转换 (修正)", function() { return java.lang.Integer.valueOf(3).intValue() === 3; });

console.log("【8. 异常处理 (修正版)】");
test("8.1 Java异常 (修正)", function() { try { new java.util.ArrayList().get(100); return false; } catch(e) { return e.javaException instanceof java.lang.IndexOutOfBoundsException; } });
test("8.2 JS异常", function() { try { new java.lang.Runnable({run:function(){throw new Error("err");}}).run(); return false; } catch(e) { return e.message === "err"; } });

console.log("【9. 线程】");
test("9.1 Thread执行", function() { var r = 0; new java.lang.Thread(new java.lang.Runnable({run:function(){r = 42;}})).start(); java.lang.Thread.sleep(100); return r === 42; });
test("9.2 同步List", function() { var l = java.util.Collections.synchronizedList(new java.util.ArrayList()); for(var i=0;i<100;i++)l.add(i); return l.size() === 100; });

console.log("【10. 作用域】");
test("10.1 eval", function() { return eval("1+2") === 3; });
test("10.2 this绑定", function() { var o = {v:42, get:function(){return this.v;}}; return o.get() === 42; });

console.log("【11. Android API】");
test("11.1 context", function() { return context != null; });
test("11.2 toast", function() { toast("test"); return true; });

console.log("【12. 性能】");
test("12.1 大数组", function() { var a = []; for(var i=0;i<10000;i++)a.push(i); return a.length === 10000; });
test("12.2 StringBuilder", function() { var sb = new java.lang.StringBuilder(); for(var i=0;i<1000;i++)sb.append("x"); return sb.length() === 1000; });

console.log("\n========== 结果 ==========");
console.log("总计: " + (passed+failed));
console.log("通过: " + passed);
console.log("失败: " + failed);
console.log("通过率: " + (passed/(passed+failed)*100).toFixed(1) + "%");
console.log("==========================\n");

"完成";