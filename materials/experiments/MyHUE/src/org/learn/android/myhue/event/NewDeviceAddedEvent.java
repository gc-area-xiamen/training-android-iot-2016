package org.learn.android.myhue.event;

public class NewDeviceAddedEvent {

	private String mName;
	public NewDeviceAddedEvent(String name) {
		mName = name;
	}
	public String getName() {
		return mName;
	}

}
