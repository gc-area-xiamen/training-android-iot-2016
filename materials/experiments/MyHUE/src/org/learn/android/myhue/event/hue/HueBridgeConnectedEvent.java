package org.learn.android.myhue.event.hue;

public class HueBridgeConnectedEvent {

	private String mIpString, mUserName;
	public HueBridgeConnectedEvent(String inIp, String username) {
		mIpString = inIp;
		mUserName = username;
	}
	
	public String getIp() {
		return mIpString;
	}

	public String getUsername() {
		return mUserName;
	}

}
