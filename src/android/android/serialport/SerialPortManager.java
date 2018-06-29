/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android.serialport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.Log;

public abstract class SerialPortManager{
	private static final String TAG = "Zst->SerialPortManager";
	private Context mContext;
	private SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	protected InputStream mInputStream;
	private boolean isReadIng = false;
	private boolean isThreadRead = false;
	private ReadThread mReadThread = null;
	public static final int RET_DEVICE_OPENED = 1;
	public static final int RET_OPEN_SUCCESS = 0;
	public static final int RET_NO_PRTMISSIONS = -1;
	public static final int RET_ERROR_CONFIG = -2;
	public static final int RET_ERROR_UNKNOW = -3;
	public static final int max_size = 2048;

	private class ReadThread extends Thread {
		@Override
		public void run() {
			super.run();
			
			while(!isInterrupted() && isThreadRead) {
				int size;
				try {
					byte[] buffer = new byte[max_size];
					if (mInputStream == null){
						Log.e(TAG, "mInputStream == null");
						continue;
					}
					size = mInputStream.read(buffer);
					if (size > 0) {
						if(size > max_size)
							size = max_size;
						if(isReadIng){
							onDataReceived(buffer, size);
						}
					}
				} catch (IOException e) {
					Log.e(TAG, "IOException");
					e.printStackTrace();
					return;
				}
			}
		}
	}

	public SerialPortManager() {
	}

	protected abstract void onDataReceived(final byte[] buffer, final int size);
	
	protected int openSerialPort(File device, int baudrate, int flow_ctrl, int databits, int stopbits, int parity){
		Log.d(TAG, "openSerialPort");
		try {
			if(mSerialPort == null){
				mSerialPort = new SerialPort(device, baudrate, flow_ctrl, databits, stopbits, parity);
				if(mSerialPort != null){
					mOutputStream = mSerialPort.getOutputStream();
					mInputStream = mSerialPort.getInputStream();
					return RET_OPEN_SUCCESS;
				}
			}
			else return RET_DEVICE_OPENED;
		} catch (SecurityException e) {
			return RET_NO_PRTMISSIONS;
		} catch (IOException e) {
			return RET_ERROR_UNKNOW;
		} catch (InvalidParameterException e) {
			return RET_ERROR_CONFIG;
		}
		return RET_ERROR_UNKNOW;
	}
	
	
	protected void startReadSerialPort() {
		isReadIng = true;
		Log.d(TAG, "startReadSerialPort");
		if(mReadThread == null || isThreadRead == false){
			isThreadRead = true;
			mReadThread = new ReadThread();
			mReadThread.start();
		}
	}
	
	protected void stopReadSerialPort() {
		Log.d(TAG, "stopReadSerialPort");
		isReadIng = false;
	}
	
	protected void closeSerialPort() {
		Log.d(TAG, "closeSerialPort");
		isReadIng = false;
		if (mReadThread != null){
			isThreadRead = false;
			mReadThread.interrupt();
		}
		if(mSerialPort != null)
			mSerialPort.closeSerialPort();
		mSerialPort = null;
	}
}
