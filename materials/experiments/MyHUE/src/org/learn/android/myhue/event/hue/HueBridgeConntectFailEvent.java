package org.learn.android.myhue.event.hue;

public class HueBridgeConntectFailEvent {

	public static final int SUCCESS = 0;
	public static final int ERR_NO_RESPONSE = 1;
	public static final int ERR_NO_BRIDGE_FOUND = 2;
	public static final int ERR_AUTHENTICATION_FAILED = 3;

	public int mErrCode = SUCCESS;
	public HueBridgeConntectFailEvent(int errCode) {
		mErrCode = errCode;
	}
	
	public int getErrorCode() {
		return mErrCode;
	}
}
