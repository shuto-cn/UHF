package com.zistone.uhf;

public interface ZstCallBackListen {
	/**
	 * callback
	 * @param data-data
	 * @param data len
	 * */
	public void onUhfReceived(byte[] data, int len);
}