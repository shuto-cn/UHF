# 超高频（UHF） RFID 卡读写的 Cordova 插件

## 说明
**插件是用于内部项目的，只针对特定的芯片及卡片做了处理，并不具备通用性。**

## 安装
使用 Cordova
<pre>cordova plugin add https://github.com/shuto-cn/UHF</pre>

使用 Ionic
<pre>ionic cordova plugin add https://github.com/shuto-cn/UHF</pre>

## 示例程序
https://github.com/shuto-cn/uhf-demo

## 目前提供的功能
### 单次询卡 - 读取卡的 EPC
* 调用：
<pre>cordova.plugins.uhf.searchCard(successCallBack, errorCallback);</pre>
* 参数：
* 返回值：
EPC数组，因为可能读到多个。<pre>["30396062C3AE88C00021E2BC"]</pre>

### 开始多次询卡 - 读取卡的 EPC
* 调用：
<pre>cordova.plugins.uhf.startSearchCard(successCallBack, errorCallback);</pre>
* 参数：
* 返回值：
EPC数组，因为可能读到多个。<pre>["30396062C3AE88C00021E2BC"]</pre>
 
### 停止多次询卡
* 调用：
<pre>cordova.plugins.uhf.stopSearchCard(successCallBack, errorCallback);</pre>
* 参数：
* 返回值：
 
### 写卡
* 调用：
<pre>cordova.plugins.uhf.writeCard(message, successCallBack, errorCallback);</pre>
* 参数：
<pre>
{
  site: 3, // 写入区域
  addr: 0, // 起始地址偏移
  data: "内容" // 写入数据
}
</pre>
**TODO: 补充各信息的解释**

* 返回值：
EPC数组，因为可能读到多个。<pre>["30396062C3AE88C00021E2BC"]</pre>

### 读卡
* 调用：
<pre>cordova.plugins.uhf.readCard(message, successCallBack, errorCallback);</pre>
* 参数：
<pre>
{
  site: 3, // 写入区域
  addr: 0, // 起始地址偏移
}
</pre>

* 返回值：
<pre>{epc:"30396062C3AE88C00021E2BC",data:"内容"}</pre>

### 设置功率
* 调用：
<pre>cordova.plugins.uhf.setPower(power, successCallBack, errorCallback);</pre>
* 参数：
<pre>
目前支持的功率值为 15 至 27，超出范围会按最大或最小值处理。
</pre>
* 返回值：

### 获取功率
* 调用：
<pre>cordova.plugins.uhf.getPower(successCallBack, errorCallback);</pre>
* 参数：
* 返回值：
<pre>
当前设备的功率值
</pre>

### 设置解调阈值
* 调用：
<pre>cordova.plugins.uhf.setParam(thrd, successCallBack, errorCallback);</pre>
* 参数：
<pre>
信号解调阈值越小能解调的标签返回RSSI越低，但越不稳定，低于一定值完全不能解调；
相反阈值越大能解调的标签返回信号RSSI越大，距离越近，越稳定。432是推荐的最小值。
</pre>
* 返回值：
**TODO: 设置阈值等多个参数信息**

### 获取解调器参数
* 调用：
<pre>cordova.plugins.uhf.getParam(successCallBack, errorCallback);</pre>
* 参数：
* 返回值：
<pre>
{
  mixer: 3, // 混频器增益
  ifAmp: 6, // 中频放大器增益
  thrd: 432 // 解调阈值
}
</pre>
**TODO: 参数的解释**
