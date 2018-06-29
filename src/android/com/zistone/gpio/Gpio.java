package com.zistone.gpio;

public class Gpio {

	private static Gpio mGpioSet = new Gpio();
	
	private Gpio(){ }

	public static Gpio getInstance(){
		return mGpioSet; 
	}
	public native void set_gpio(int state,int pin_num);
	public native void set_gpio_pluse(int pin_num,int cn,int delay);
	static {
		System.loadLibrary("gpio");
	}
	
}
