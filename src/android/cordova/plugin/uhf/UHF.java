package cordova.plugin.uhf;

import android.content.SharedPreferences;
import android.util.Log;

import com.uhf.api.Util;
import com.zistone.uhf.ZstCallBackListen;
import com.zistone.uhf.ZstUHFApi;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Arrays;

import static android.content.Context.MODE_PRIVATE;

/**
 * 超高频（UHF）读写卡插件。
 * TODO: 目前只针对特定的芯片及卡标签做了处理，需要通用化
 */
public class UHF extends CordovaPlugin {

    private final String TAG = "ZstUHFApi";
    private ZstUHFApi mZstUHFApi;
    private SharedPreferences sp;

    private int gpio1_num = 81, gpio2_num = 113;
    private String SerialName = "/dev/ttyHSL1";

    //-----------------//

    private int length = 6;// 读取数据的长度

    //-----------------------//


    private byte curMixeValue;
    private byte[] MixerArrValue = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06};


    private byte curIfampValue;
    private byte[] IfampArrValue = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
    private int mThrd = 0;
    private MyZstUhfListen listener;
    private byte[] result; // 暂存接收到的数据，主要是因为不是每次接收的都是完整的数据。有时需要经过2次或3次才能接收到完整的数据。
    private boolean start = false; // 是否开始多次轮巡
    private int siteid;   //将site定义为全局变量

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        String model = android.os.Build.MODEL;
        if (model.contains("msm8953")) {
            gpio1_num = 66;
            gpio2_num = 98;
            SerialName = "/dev/ttyHSL0";
        }
        sp = cordova.getActivity().getSharedPreferences("UHF_SHRAE", MODE_PRIVATE);
        listener = new MyZstUhfListen();
        mZstUHFApi = ZstUHFApi.getInstance(listener);
        mZstUHFApi.setModelPower(true, gpio1_num, gpio2_num);
        openScanDevice();
    }


    private void onDataReceived(final byte[] buffer, final int size, CallbackContext callbackContext) {
        Log.d(TAG, "Recv_Buffer = " + Util.byte2hex(buffer, size));
        if (buffer[0] == (byte)0xBB) {
            result = Arrays.copyOf(buffer,size);
        }
        else if(result != null) {
            int resultSize = result.length;
            if (resultSize > 0 && result[resultSize-1] != (byte)0x7E) {
                result = Arrays.copyOf(result, resultSize + size);
                System.arraycopy(buffer, 0, result, resultSize, size);
            }
        }
        if (result[result.length-1] != (byte)0x7E) { // 可能还没有接收完
            Log.d(TAG, Util.toHex(result[result.length-1]));
            Log.d(TAG, "waiting for subsequent data");
            return;
        }
        byte type = result[1];
        if (type == 0x01) { // 响应帧
            Log.d(TAG, "reply frame");
            if (!Util.isValid(result, result.length)) {
                callbackContext.error("data error");
                return;
            }
            byte command = result[2];
            int parameterLength = Util.getBytesAsWord(result, 3);
            int offset = 5; // 前5个数据是结构数据 [Header, Type, Command, PL(MSB), PL(LSB)]，需要跳过
            byte[] content;
            JSONObject jsonObject;
            switch (command) {
                case (byte)0x03: // 获取读写器模块信息
                    int infoType = result[offset]; // 0 硬件版本，1 软件版本，2 制造商
                    offset++;
                    content = Arrays.copyOfRange(result, offset, offset + parameterLength);
                    String info = new String(content);
                    Log.d(TAG, "infoType:" + infoType + ", info: " + info);
                    callbackContext.success(content);
                    break;
                case (byte)0x22: // 单次轮询
                    callbackContext.success();
                    break;
                case (byte)0x27: // 多次轮询
                    callbackContext.success();
                    break;
                case (byte)0x28: // 停止轮询
                    callbackContext.success();
                    break;
                case (byte)0x0C: // 设置 Select 参数
                    callbackContext.success();
                    break;
                case (byte)0x12: // 设置 Select 模式
                    callbackContext.success();
                    break;
                case (byte)0x39: // 读标签数据存储区
                    jsonObject = new JSONObject();
                    String epc = this.getEPC(result, 8);
                    String data = this.getData(result, 20, siteid); // 8 + epc byte length
                    try {
                        jsonObject.put("epc", epc);
                        jsonObject.put("data", data);
                        callbackContext.success(jsonObject);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        callbackContext.error(e.getMessage());
                    }
                    break;
                case (byte)0x49: // 写标签数据存储区
                    callbackContext.success();
                    break;
                case (byte)0x82: // 锁定Lock标签数据存储区
                    callbackContext.success();
                    break;
                case (byte)0x07: // 设置工作地区
                    callbackContext.success();
                    break;
                case (byte)0xAB: // 设置工作信道
                    callbackContext.success();
                    break;
                case (byte)0xAA: // 获取工作信道
                    callbackContext.success(result[offset]);
                    break;
                case (byte)0xB7: // 获取发射功率
                    callbackContext.success(Util.getBytesAsWord(result, offset) / 100);
                    break;
                case (byte)0xB6: // 设置发射功率
                    callbackContext.success();
                    break;
                case (byte)0xF0: // 设置接收解调器参数
                    callbackContext.success();
                    break;
                case (byte)0xF1: // 获取接收解调器参数
                    jsonObject = new JSONObject();
                    curMixeValue = result[offset];
                    curIfampValue = result[offset + 1];
                    mThrd = Util.getBytesAsWord(result, offset + 2);
                    try {
                        jsonObject.put("mixer", MixerArrValue[curMixeValue]);
                        jsonObject.put("ifAmp", MixerArrValue[curIfampValue]);
                        jsonObject.put("thrd", mThrd);
                        callbackContext.success(jsonObject);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                        callbackContext.error(e.getMessage());
                    }
                    break;
                case (byte)0xFF: // 0xFF, 出错的时候
                    String error = "0xFF: " + Util.byte2hex(result, result.length);
                    Log.e(TAG, error);
                    if (!start) {
                        callbackContext.error(error);
                    }
                    else {
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, error);
                        pluginResult.setKeepCallback(true);
                        callbackContext.sendPluginResult(pluginResult);
                    }
                    break;
                default:
                    // 其它情况暂时不做处理
                    callbackContext.error("not implement");
            }
        } else if (type == 0x02){ // 0x2 是通知帧；只有单次轮询、多次轮询时才会有通知帧。
            Log.d(TAG, "notice frame, length:"+result.length);
            if (result.length % 24 != 0) { // 可能会有多个数据帧，每个帧是24个字节
                Log.e(TAG, "data length error");
                callbackContext.error("data length error");
            }
            int count = result.length / 24;
            JSONArray ja = new JSONArray();
            for (int i=0; i<count; i++) {
                byte[] frame = Arrays.copyOfRange(result, i*24, i*24+24);
                Log.d(TAG, Util.byte2hex(frame, frame.length));
                if (Util.isValid(frame, frame.length)) {
                    ja.put(this.getEPC(frame, 8));
                }
                else {
                    Log.e(TAG, "data format error");
                    // 忽略，跳过
                }
            }
            if (ja.length() < 1) {
                Log.e(TAG, "empty data");
                callbackContext.error("empty data");
            }
            else {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, ja);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        }
    }

    /**
     * 获取响应帧中的 Data 信息
     *
     * @param buffer 帧数据
     * @param offset 偏移位置
     * @return data 信息
     */
    private String getData(byte[] buffer, int offset, int site) {
        byte[] data = Arrays.copyOfRange(buffer, offset, buffer.length-2);
        int end = -1;
        for (int i=0; i<data.length; i++) {
            if (data[i] == 0) {
                end = i;
                break;
            }
        }
        //user区时转码
        if (site == 3){
            if (end < 0) {
                return "";
            }
            byte[] bs = Arrays.copyOfRange(data, 0, end);
            String str = new String(bs);
            return str;
        } else if (site == 2) {             //TID区转码
            end = 24;
            byte[] bs = Arrays.copyOfRange(data, 0, end);
            String str2 = byte2hex(bs, end);
            return str2;
        } else if (site == 1) {             //EPC区转码
            end = 12;
            byte[] bs = Arrays.copyOfRange(data, 0, end);
            String str2 = byte2hex(bs, end);
            return str2;
        } else {
            return "";
        }
    }

    /**
     * 获取通知数据帧中的 EPC 信息
     *
     * @param buffer 帧数据
     * @return epc 信息
     */
    private String getEPC(byte[] buffer, int offset) {
        int length = 12;
        byte[] epc = Arrays.copyOfRange(buffer, offset, offset+length);
        return Util.byte2hex(epc, epc.length);
    }

    public class MyZstUhfListen implements ZstCallBackListen {
        private CallbackContext callbackContext;

        public void setCallbackContext(CallbackContext callbackContext) {
            this.callbackContext = callbackContext;
        }

        @Override
        public void onUhfReceived(byte[] data, int len) {
            onDataReceived(data, len, callbackContext);
        }
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


    private int openScanDevice() {
        String path = sp.getString("DEVICE", SerialName);
        int baud_rate = Integer.decode(sp.getString("BAUDRATE", "115200"));
        int data_bits = Integer.decode(sp.getString("DATA", "8"));
        int stop_bits = Integer.decode(sp.getString("STOP", "1"));
        int flow = 0;
        int parity = 'N';
        String flow_ctrl = sp.getString("FLOW", "None");
        String parity_check = sp.getString("PARITY", "None");
        /* Check parameters */
        if ((path.length() == 0) || (baud_rate == -1)) {
            throw new InvalidParameterException();
        }
        if (flow_ctrl.equals("RTS/CTS"))
            flow = 1;
        else if (flow_ctrl.equals("XON/XOFF"))
            flow = 2;

        if (parity_check.equals("Odd"))
            parity = 'O';
        else if (parity_check.equals("Even"))
            parity = 'E';

        int retOpen = -1;
        if (mZstUHFApi != null) {
            retOpen = mZstUHFApi.opendevice(
                    new File(path), baud_rate, flow,
                    data_bits, stop_bits, parity, gpio1_num);
        }
        if (retOpen == android.serialport.SerialPortManager.RET_OPEN_SUCCESS ||
                retOpen == android.serialport.SerialPortManager.RET_DEVICE_OPENED) {
            Log.d(TAG, "UHF opened");
        } else if (retOpen == android.serialport.SerialPortManager.RET_NO_PRTMISSIONS) {
            Log.e(TAG, "No permission");
        } else if (retOpen == android.serialport.SerialPortManager.RET_ERROR_CONFIG) {
            Log.e(TAG, "error config");
        }
        return retOpen;
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        listener.setCallbackContext(callbackContext);
        if (action.equals("readCard")) {
            this.readCard(args, callbackContext);
            return true;
        } else if (action.equals("searchCard")) {
            this.searchCard(callbackContext);
            return true;
        } else if (action.equals("startSearchCard")) {
            this.startSearchCard(callbackContext);
            return true;
        } else if (action.equals("stopSearchCard")) {
            this.stopSearchCard(callbackContext);
            return true;
        } else if (action.equals("writeCard")) {
            this.writeCard(args, callbackContext);
            return true;
        } else if (action.equals("getPower")) {
            this.getPower(callbackContext);
            return true;
        } else if (action.equals("setPower")) {
            this.setPower(args, callbackContext);
            return true;
        } else if (action.equals("getParam")) {
            this.getParam(callbackContext);
            return true;
        } else if (action.equals("setParam")) {
            this.setParam(args, callbackContext);
            return true;
        }
        return false;
    }

    private void startSearchCard(CallbackContext callbackContext) {
        start = true;
        mZstUHFApi.startInventory(10000);
        PluginResult pluginResult = new  PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true); // Keep callback
        callbackContext.sendPluginResult(pluginResult);
    }

    private void stopSearchCard(CallbackContext callbackContext) {
        start = false;
        mZstUHFApi.stopInventory();
    }

    private void searchCard(CallbackContext callbackContext) {
        mZstUHFApi.singleInventory();
    }

    private void readCard(JSONArray message, CallbackContext callbackContext) throws JSONException {
        JSONObject obj = message.getJSONObject(0);
        int site = obj.getInt("site");
        this.siteid = site;
        int addr = obj.getInt("addr");
        if (site == 1) {
            addr = 2;
            length = 6;
        } else if (site == 3) {
            addr = 0;
            length = 32;
        } else if (site == 2) {
            addr = 0;
            length = 12;
        }
        mZstUHFApi.readCradTag(Util.hexStr2Str("00000000"), (byte) site, addr, length);
    }

    private void writeCard(JSONArray message, CallbackContext callbackContext) throws JSONException {
        JSONObject obj = message.getJSONObject(0);
        String _data = obj.getString("data");
        int site = obj.getInt("site");
        int addr = obj.getInt("addr");
        byte[] password = Util.hexStr2Str("00000000");
        byte[] data = _data.getBytes();

        if (site == 1) {
            addr = 2;
            if (length > 6 || data.length > 12) {
                callbackContext.error("data too long");
            } else {
                mZstUHFApi.writeCradTag(password, (byte) site, addr, 6, this.appendEOF(data, 12));
            }
        } else if (site == 3) {
            byte[] datas = this.appendEOF(data, 64);
            length = datas.length / 2;
            mZstUHFApi.writeCradTag(password, (byte) site, addr, length, datas);
        }
    }

    /**
     * 在信息后面补充 0 作为结束符
     * <p>
     * 因为每次写的数据长度不一样，如果以前写的数据比这次多，以前的数据并不能被完全覆盖。
     * 在读取数据时容易出现除了这次写的数据，还有以前的数据拼接在后面。
     * 为了避免这种情况，每次写数据时，如果没写满，就在后面补1到2个0，作为数据结束符。
     * 读取数据时读到第一个0的时候就认为是数据的结束，不再处理后面的数据，以避免读取不必要的数据。
     * </p>
     *
     * @param data 要处理的数据
     * @param maxsize 最多可写入的数据长度
     * @return 附加上结束符的数据
     */
    private byte[] appendEOF(byte[] data, int maxsize) {
        if (data.length == maxsize) {
            return data;
        }
        int distance = maxsize - data.length;
        int size = distance >= 2 ? 2 : 1;
        return Arrays.copyOf(data, data.length+size); // 补充0作为结束，避免读出未“覆盖”到的数据。
    }

    private void getPower(CallbackContext callbackContext) {
        mZstUHFApi.getTransmissionPower();
    }

    private void setPower(JSONArray message, CallbackContext callbackContext) throws JSONException {
        int power = message.getInt(0);
        if (power > 26) {
            power = 26;
        } else if (power < 15) {
            power = 15;
        }
        mZstUHFApi.setTransmissionPower(power * 100);
    }

    private void setParam(JSONArray message, CallbackContext callbackContext) throws JSONException {
        mThrd = message.getInt(0);
        mZstUHFApi.setModemsParam(MixerArrValue[curMixeValue], IfampArrValue[curIfampValue], mThrd);
    }

    private void getParam(CallbackContext callbackContext) {
        mZstUHFApi.getModemsParam();
    }

    @Override
    public void onDestroy(){
        if(mZstUHFApi != null) {
            mZstUHFApi.closeDevice();
        }
    }
}
