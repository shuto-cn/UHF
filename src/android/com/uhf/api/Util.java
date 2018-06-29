package com.uhf.api;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

public class Util {
	public static SoundPool sp;
	public static Map<Integer, Integer> suondMap;
	public static Context context;
	private static int audioCurrentVolume;
	private static final String TAG = "ZstUHFApi";

	//初始化声音池
	public static void initSoundPool(Context context, int soundid) {
		Util.context = context;
		sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 1);
		suondMap = new HashMap<Integer, Integer>();
		suondMap.put(1, sp.load(context, soundid, 1));
		AudioManager am = (AudioManager) Util.context.getSystemService(Util.context.AUDIO_SERVICE);
		//返回当前AlarmManager最大音量
		float audioMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		//返回当前AudioManager对象的音量值
		audioCurrentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		float volumnRatio = audioCurrentVolume / audioMaxVolume;
	}

	//播放声音池声音
	public static void play(int sound) {
		sp.play(
				suondMap.get(sound), //播放的音乐Id
				audioCurrentVolume, //左声道音量
				audioCurrentVolume, //右声道音量
				1, //优先级，0为最低
				0, //循环次数，0无不循环，-1无永远循环
				1);//回放速度，值在0.5-2.0之间，1为正常速度
	}

	private final static char[] mChars = "0123456789ABCDEF".toCharArray();
	private final static String mHexStr = "0123456789ABCDEF";

	public static byte[] hexStr2Str(String hexStr) {
		if (hexStr == null) {
			return null;
		}
		hexStr = hexStr.toString().trim().replace(" ", "").toUpperCase(Locale.US);
		if (hexStr.length() % 2 != 0) {
			hexStr += 'F';
		}
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int iTmp = 0x00;
		for (int i = 0; i < bytes.length; i++) {
			iTmp = mHexStr.indexOf(hexs[2 * i]) << 4;
			iTmp |= mHexStr.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (iTmp & 0xFF);
		}
		return bytes;
	}

	public static String str2HexStr(String str) {
		StringBuilder sb = new StringBuilder("");
		byte[] bs = str.getBytes();
		int bit;
		for (int i = 0; i < bs.length; i++) {
			bit = (bs[i] & 0x0f0) >> 4;
			sb.append(mChars[bit]);
			bit = bs[i] & 0x0f;
			sb.append(mChars[bit]);
		}
		return sb.toString();
	}

	public static final String byte2hex(byte b[], int size) {
		if (b == null) {
			throw new IllegalArgumentException(
					"Argument b ( byte array ) is null! ");
		}
		String hs = "";
		String stmp = "";
		for (int n = 0; n < size; n++) {
			stmp = Integer.toHexString(b[n] & 0xff);
			if (stmp.length() == 1) {
				hs = hs + "0" + stmp;
			} else {
				hs = hs + stmp;
			}
		}
		return hs.toUpperCase();
	}
	// 0 失败 1 成功
	public static boolean checkSum(String data) {
		boolean ret = false;
		int allsum = 0;
		int hexret = 0;
		String[] _str = data.split(" ");

		if (_str.length >= 2) {
			for (int i = 1; i < _str.length - 2; i++) {
				allsum = allsum + toInt(_str[i]);
			}
			String cSum = Integer.toHexString(allsum % 256).toUpperCase();
			if (cSum.length() == 1)
				cSum = "0" + cSum;

			if (cSum.equals(_str[_str.length - 2])) {
				ret = true;
			}
		} else {
			ret = false;
		}
		return ret;
	}

	public static String hexString2Str(String hexStr) {

		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int n;
		for (int i = 0; i < bytes.length; i++) {
			n = mHexStr.indexOf(hexs[2 * i]) * 16;
			n += mHexStr.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (n & 0xff);
		}
		return new String(bytes);
	}

	public static String GetPower(String data) {
		String ret = "";
		String _hbyte = data.substring(15, 17);
		String _lbyte = data.substring(18, 20);
		int pow = toInt(_hbyte) * 256
				+ toInt(_lbyte);
		ret = String.valueOf(pow / 100);
		return ret;
	}

	public static byte[] charToByte(char c) {
		byte[] b = new byte[2];
		b[0] = (byte) ((c & 0xFF00) >> 8);
		b[1] = (byte) (c & 0xFF);
		return b;
	}

	public static char byteToChar(byte[] b) {
		char c = (char) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
		return c;
	}

	public static String toHex(byte b) {
		return ("" + mHexStr.charAt(0xf & b >> 4) + mHexStr.charAt(b & 0xf));
	}

	public static String toString(byte[] bytes, int len) {
		String ss = "123";
//		for (int i = 0; i < len; i++) {
//			ss+=toHex(bytes[i])+" ";
//		}
		return ss;
	}

	public static int toInt(String hex) {
		int ss = 0;
		if ((hex.charAt(0) - 'A') >= 0) {
			ss += (hex.charAt(0) - 'A' + 10) * 16;
		} else {
			ss += (hex.charAt(0) - '0') * 16;
		}
		if ((hex.charAt(1) - 'A') >= 0) {
			ss += hex.charAt(1) - 'A' + 10;
		} else {
			ss += hex.charAt(1) - '0';
		}
		return ss;
	}

	public static byte[] hexStr2String(String hexStr) {
		if (hexStr == null) {
			return null;
		}
		hexStr = hexStr.toString().trim().replace(" ", "").toUpperCase(Locale.US);
		while (true) {
			if (hexStr.length() >= 128) {
				break;
			}
			hexStr += "00";
		}
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int iTmp = 0x00;
		for (int i = 0; i < bytes.length; i++) {
			iTmp = mHexStr.indexOf(hexs[2 * i]) << 4;
			iTmp |= mHexStr.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (iTmp & 0xFF);
		}
		return bytes;
	}

	public static int toInt(byte b) {
		return (int) b & 0xFF;
	}

	/**
	 * 验证数据帧的格式
	 *
	 * @param buffer 数据帧
	 * @param size 数据量
	 * @return true 格式正确，否则返回 false
	 */
	public static boolean isValid(byte[] buffer, int size) {
		if (buffer.length < size) {
			Log.e(TAG, "error data size!");
			return false;
		}
		if (size <= 2) {
			Log.e(TAG, "error data size, too small!");
			return false;
		}
		if (buffer[0] != (byte)0xBB) {
			Log.e(TAG, "error data Header!");
			return false;
		}
		if (buffer[size-1] != (byte)0x7E) {
			Log.e(TAG, "error data End!");
			return false;
		}

		return checkSum(buffer, size);
	}

	/**
	 * 计算校验和(CheckSum)是否正确
	 *
	 * @param buffer 数据帧
	 * @param size 数据量
	 * @return true 校验正确，否则返回 false
	 */
	private static boolean checkSum(byte[] buffer, int size) {
		// [Header, Data0, Data1, ... CheckSum, End]
		int checkSumIdx = size-2;
		byte checkSum = buffer[checkSumIdx];
		byte checkSum_ = 0;
		for (int i = 1; i < checkSumIdx; i++) {
			checkSum_ += buffer[i];
		}
		return checkSum_ == checkSum;
	}

	/**
	 * 获取一个字（两个字节，前高位，后低位）的数据转为 int
	 *
	 * @param arr 需要获取数据的 byte 数组
	 * @param offset 开始获取的位置
	 * @return int 类型的数据
	 */
	public static int getBytesAsWord(byte[] arr, int offset) {
		return arr[offset] << 8 & 0xFF00 | arr[offset + 1] & 0xFF;
	}

	/**
	 * 字节数组转为16进制表示的字符串
	 *
	 * @param bytes 内容
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = mChars[v >>> 4];
			hexChars[j * 2 + 1] = mChars[v & 0x0F];
		}
		return new String(hexChars);
	}
}
