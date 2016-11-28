package org.learn.android.myhue;

import java.io.Serializable;

import android.util.Log;

public class HueBridge implements Serializable {
    
    private static final long serialVersionUID = 2L;
	private static final String TAG = "HueBridge";
//	private static final long serialVersionUID = -3L;

	public static final short APPID_DEV_HUE_LIGHT = (short)1;
	public static final short APPID_DEV_HUE_GROUP = (short)2;

	private String mIP;
	public HueBridge(String inAddr, int endpoint) {
	    mIP = inAddr;
	}

	public boolean isOnline() {
		return HueManager.getInstance().isOnline();
	}
	
	public void setOnline(boolean is) {
		Log.e(TAG, "HueBridge online controlled by HueManager.");
	}
	private String mID;
	private String mUsername, mPassword;
	private short mIdxInExtTbl;
	private short mPort;
	private boolean mbAuthed;
	public String getId() {
		return mID;
	}
	public String getIp() {
		return mIP;
	}
	public void setIp(String ip) {
		mIP = ip;
	}
	public void setUsername(String s) {
	    mUsername = s;
	}
	public String getUsername() {
		return mUsername;
	}
	public String getPassword() {
		return mPassword;
	}
	public void setPassword(String password) {
		mPassword = password;
	}
	public void setPort(int port) {
		mPort = (short)port;
	}
	public short getPort() {
		return mPort;
	}
	public short getIdxInExtTbl() {
		return mIdxInExtTbl;
	}
	public boolean isAuthed() {
	    return mbAuthed;
	}
	public static HueBridge loadHueBridge(String id, String ip, String user, boolean authed) {
        HueBridge hb = new HueBridge(id, 0);
        hb.mID = id;
        hb.mIP = ip;
        hb.mUsername = user;
        hb.mbAuthed = authed;
        return hb;
    }
}
