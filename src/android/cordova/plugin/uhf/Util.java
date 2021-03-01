package cordova.plugin.uhf;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

/**
 * 工具类，主要用于实现各类字符串之间的转换
 */
public class Util {

	public static String str2HexStr(String origin) {
		byte[] bytes = origin.getBytes();
		String hex = bytesToHexString(bytes, bytes.length);
		return hex;
	}

	public static String hexStr2Str(String hex) {
		byte[] bb = hexStringToBytes(hex);
		String rr = new String(bb);
		return rr;
	}

	public static String bytes2Str(byte[] src) {
		String rr = bytesToHexString(src, src.length);
		String str = new String(hexStr2Str(rr));
		return str;
	}

	private static String bytesToHexString(byte[] src, int size) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || size <= 0) {
			return null;
		}
		for (int i = 0; i < size; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		String str = stringBuilder.toString();
		if (str.length() < 128) {
			str += "00";
		}
		return str;
	}

	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			// 以0x00为分界线，不读取后面的数据
			if ((byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1])) == 00) {
				break;
			}
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}
}
