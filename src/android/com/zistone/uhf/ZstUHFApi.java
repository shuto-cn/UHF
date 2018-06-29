package com.zistone.uhf;

import java.io.File;
import java.io.OutputStream;

import com.uhf.api.Util;
import com.zistone.gpio.Gpio;

import android.content.Context;
import android.serialport.SerialPortManager;
import android.util.Log;
import android.widget.Toast;

public class ZstUHFApi extends SerialPortManager{
	private final String TAG = "ZstUHFApi";
	private int mGpio_num;
	private Gpio mGpio = Gpio.getInstance();
	private byte CMD_HEAD_BB = (byte)0xBB;
	private byte CMD_SINGLE_INVENTORY = (byte)0x22;//单次轮询
	private byte CMD_START_INVENTORY = (byte)0x27;//开始多次轮询
	private byte CMD_STOP_INVENTORY = (byte)0x28;//停止多次轮询
	private byte CMD_SET_TRANMISSIONPOWER = (byte)0xb6;//设置发射功率
	private byte CMD_GET_TRANMISSIONPOWER = (byte)0xb7;//获取发射功率
	private byte CMD_SET_WORK_AREA = (byte)0x07;//设置工作地区
	private byte CMD_SET_WORK_CHANNEL = (byte)0xab;//设置工作信道
	private byte CMD_GET_WORK_CHANNEL = (byte)0xaa;//获取工作信道
	private byte CMD_SET_MOODEMS_PARAM = (byte)0xf0;//设置接收解调器参数
	private byte CMD_GET_MOODEMS_PARAM = (byte)0xf1;//获取接收解调器参数
	private byte CMD_WRITE_CARD_TAG = (byte)0x49;//写标签
	private byte CMD_READ_CARD_TAG = (byte)0x39;//读标签
	private byte CMD_SET_SELECT_TAG = (byte)0x0C;//设置Select参数

	private static ZstUHFApi mZstUHFApi = null;

	private Context mContext;
	private ZstCallBackListen mCallback;

	private ZstUHFApi(ZstCallBackListen callback){
		mCallback = callback;
	}

	public void setListener(ZstCallBackListen listener) {
		this.mCallback = listener;
	}

	public static ZstUHFApi getInstance(ZstCallBackListen callback){
		if(mZstUHFApi == null){
			mZstUHFApi = new ZstUHFApi(callback);
		}
		return mZstUHFApi;
	}

	/**
	 * Open the device.
	 * @param device- device
	 * @param baudrate-Baud rate
	 * @param flow_ctrl-Flow control
	 * @param databits-Data bits
	 * @param stopbits-Stop bit
	 * @param parity-parity
	 * @param gpio-gpio number
	 * @return 1-opened 0-open success 1-No permissions 2-Parameter error 3-unknown error
	 * */
	public int opendevice(File device, int baudrate, int flow_ctrl, int databits, int stopbits, int parity, int gpio){
		mGpio_num = gpio;
		int ret =  openSerialPort(device, baudrate, flow_ctrl, databits, stopbits, parity);
		startReadSerialPort();
		return ret;
	}

	/**
	 * Close the device.
	 * */
	public void closeDevice(){
		stopReadSerialPort();
		closeSerialPort();
	}


	@Override
	protected void onDataReceived(byte[] buffer, int size) {
		if(mCallback != null){
			mCallback.onUhfReceived(buffer, size);
		}
	}

	private byte getCheckSum(byte[] data, int start){
		int mSum = 0x00;
		if(data == null || start >= data.length)
			return 0x00;
		for(int i=start; i<data.length; i++){
			mSum += data[i];
		}
		return (byte)(mSum&0xff);
	}

	private boolean sendData(byte head, byte command, int len, byte[] data){
		int Alen = 1+1+1+2+len+1+1;
		byte[] sendBuf = new byte[Alen];
		int i = 0;
		sendBuf[i++] = (byte)head;
		sendBuf[i++] = (byte)0x00;
		sendBuf[i++] = (byte)command;
		sendBuf[i++] = (byte) (len/256);
		sendBuf[i++] = (byte) (len%256);
		if(data != null && data.length >= len){
			for(int j=0; j<len; j++){
				sendBuf[i+j] = data[j];
			}
		}
		i+=len;
		sendBuf[i++] = getCheckSum(sendBuf, 1);
		sendBuf[i++] = (byte) 0x7E;
		Log.d(TAG,"Send_Data = "+ Util.byte2hex(sendBuf, sendBuf.length));
		try {
			if (mOutputStream != null) {
				mOutputStream.write(sendBuf);
				mOutputStream.flush();
			} else {
				Log.d(TAG,"mOutputStream == null");
				return false;
			}
		} catch (Exception e) {
			Log.d(TAG,"Exception e");
			return false;
		}
		return true;
	}

	/*
	 * 模块上电
	 * @param up: true 上电
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public void setModelPower(boolean up, int gpio1, int gpio2){
		String model= android.os.Build.MODEL;
		Log.d(TAG, "model = "+ model);
		Log.d(TAG, "up = "+ up);
		Log.d(TAG, "gpio1 = "+ gpio1);
		Log.d(TAG, "gpio2 = "+ gpio2);

		if(model.contains("msm8953")){
			if(up){
				if(gpio1 < 999)
					mGpio.set_gpio(1,gpio1);
				if(gpio2 < 999)
					mGpio.set_gpio(0,gpio2);

				if(gpio1 < 999){
					mGpio.set_gpio(0,gpio1);
					mGpio.set_gpio(1,gpio1);
					mGpio.set_gpio(0,gpio1);
					mGpio.set_gpio(1,gpio1);
					mGpio.set_gpio(0,gpio1);
					mGpio.set_gpio(1,gpio1);
				}
			}else{
				if(gpio1 < 999)
					mGpio.set_gpio(0,gpio1);
				if(gpio2 < 999)
					mGpio.set_gpio(1,gpio2);
			}
		}else{
			if(up){
				if(gpio1 < 999)
					mGpio.set_gpio(0,gpio1);
				if(gpio2 < 999)
					mGpio.set_gpio(0,gpio2);
			}else{
				if(gpio1 < 999)
					mGpio.set_gpio(1,gpio1);
				if(gpio2 < 999)
					mGpio.set_gpio(1,gpio2);
			}
		}
	}

	/*
	 * 单次轮询
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean singleInventory(){
		int len = 0;
		byte[] data = null;
		return sendData(CMD_HEAD_BB, CMD_SINGLE_INVENTORY, len, data);
	}

	/*
	 * 开始多次轮询
	 * @param num 轮训次数
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean startInventory(int num){
		int len = 3;
		byte[] data = new byte[len];
		data[0] = 0x22;
		data[1] = (byte) (num/256);
		data[2] = (byte) (num%256);
		return sendData(CMD_HEAD_BB, CMD_START_INVENTORY, len, data);
	}

	/*
	 * 停止多次轮询
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean stopInventory(){
		return sendData(CMD_HEAD_BB, CMD_STOP_INVENTORY, 0, null);
	}

	/*
	 * 设置发射功率
	 * @param power 发射功率
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean setTransmissionPower(int power){
		int len = 2;
		byte[] data = new byte[len];
		data[0] = (byte) (power/256);
		data[1] = (byte) (power%256);
		return sendData(CMD_HEAD_BB, CMD_SET_TRANMISSIONPOWER, len, data);
	}

	/*
	 * 读取发射功率
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean getTransmissionPower(){
		return sendData(CMD_HEAD_BB, CMD_GET_TRANMISSIONPOWER, 0, null);
	}

	/*
	 * 设置工作地区
	 * @param area  中国900MHz	01
	 * 				中国800MHz	04
	 * 				美国	02
	 * 				欧洲	03
	 * 				韩国	06
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean setWorkArea(byte area){
		int len = 1;
		byte[] data = new byte[len];
		data[0] = area;
		return sendData(CMD_HEAD_BB, CMD_SET_WORK_AREA, len, data);
	}

	/*
	 * 设置工作信道
	 * @param channel  中国900MHz	01
	 * 				中国800MHz	04
	 * 				美国	02
	 * 				欧洲	03
	 * 				韩国	06
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean setWorkChannel(byte channel){
		int len = 1;
		byte[] data = new byte[len];
		data[0] = channel;
		return sendData(CMD_HEAD_BB, CMD_SET_WORK_CHANNEL, len, data);
	}

	/*
	 * 获取工作信道
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean getWorkChannel(){
		return sendData(CMD_HEAD_BB, CMD_GET_WORK_CHANNEL, 0, null);
	}

	/*
	 * 获取接收解调器参数
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean getModemsParam(){
		return sendData(CMD_HEAD_BB, CMD_GET_MOODEMS_PARAM, 0, null);
	}

	/*
	 * 设置接收解调器参数
	 * @param 	Mixer_G 混频器增益
	 * 			IF_G	中频放大器增益
	 * 			Thrd	信号解调阈值
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean setModemsParam(byte Mixer_G, byte IF_G, int Thrd){
		int len = 4;
		byte[] data = new byte[len];
		data[0] = Mixer_G;
		data[1] = IF_G;
		data[2] = (byte) (Thrd/256);
		data[3] = (byte) (Thrd%256);
		return sendData(CMD_HEAD_BB, CMD_SET_MOODEMS_PARAM, len, data);
	}

	/*
	 * 读标签
	 * @param 	password 	密码
	 * 			membank 	标签数据存储区
	 * 			addr		读标签数据区地址偏移
	 * 			tag_len			读标签数据区地址长度
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean readCradTag(byte[] password, byte membank, int addr, int tag_len){
		int len = 9, i;
		byte[] data = new byte[len];
		if(password == null || password.length != 4)
			return false;
		for(i = 0; i < 4; i++){
			data[i] = password[i];
		}
		data[i++] = membank;
		data[i++] = (byte) (addr/256);
		data[i++] = (byte) (addr%256);
		data[i++] = (byte) (tag_len/256);
		data[i++] = (byte) (tag_len%256);
		return sendData(CMD_HEAD_BB, CMD_READ_CARD_TAG, len, data);
	}

	/*
	 * 写标签
	 * @param 	password 	密码
	 * 			membank 	标签数据存储区
	 * 			addr		读标签数据区地址偏移
	 * 			tag_len		读标签数据区地址长度
	 * 			tag_data	写入数据
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean writeCradTag(byte[] password, byte membank, int addr, int tag_len, byte[] tag_data) {
		int len = 9 + tag_data.length;
		byte[] data = new byte[len];
		if (password != null && password.length == 4) {
			int i;
			for(i = 0; i < 4; ++i) {
				data[i] = password[i];
			}

			data[i++] = membank;
			data[i++] = (byte)(addr / 256);
			data[i++] = (byte)(addr % 256);
			data[i++] = (byte)(tag_len / 256);
			data[i++] = (byte)(tag_len % 256);
			Log.d("ZstUHFApi", "i = " + i);

			for(int j = 0; j < tag_data.length; ++j) {
				Log.d("ZstUHFApi", "j = " + j);
				Log.d("ZstUHFApi", "tag_data[i] = " + tag_data[j]);
				data[i + j] = tag_data[j];
			}

			return this.sendData(this.CMD_HEAD_BB, this.CMD_WRITE_CARD_TAG, len, data);
		} else {
			return false;
		}
	}

	/*
	 * 设置Select参数
	 * @param 	SelParam
	 * 			Ptr
	 * 			Truncate
	 * 			Mask
	 * @return true 成功<br/>
	 * 		   false 失败
	 * */
	public boolean setSelect(int SelParam, byte[] Ptr, int Truncate, byte[] Mask){
		int len = 1+4+1+1+Mask.length, i = 0;
		if(Mask == null)
			return false;
		byte[] data = new byte[len];
		data[i++] = (byte) SelParam;
		data[i++] = Ptr[0];
		data[i++] = Ptr[1];
		data[i++] = Ptr[2];
		data[i++] = Ptr[3];
		data[i++] = (byte) (Mask.length*8);
		data[i++] = (byte) Truncate;
		for(int j=0; j<Mask.length;j++){
			data[i++] = Mask[j];
		}
		return sendData(CMD_HEAD_BB, CMD_SET_SELECT_TAG, len, data);
	}
}
