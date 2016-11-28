package org.learn.android.myhue.event;

public class PropReportReceivedEvent extends DevListUpdateEvent {

	private String mMAC;
	private byte mEndPoint;
	public PropReportReceivedEvent(String mac, byte ep) {
		super();
		mMAC = mac;
		mEndPoint = ep;
	}
	
	public String getMAC() {
		return mMAC;
	}
	
	public byte getEndPoint() {
		return mEndPoint;
	}

}
