package org.learn.android.myhue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Base64;

public class HueSharedPreferences {
    private static final String HUE_SHARED_PREFERENCES_STORE = "HueSharedPrefs";
    private static final String LAST_CONNECTED_USERNAME      = "LastConnectedUsername";
    private static final String LAST_CONNECTED_IP            = "LastConnectedIP";
    private static final String LAST_CONNECTED_MAC			 = "LastConnectedMAC";
    private static final String LAST_AUTH_STATUS                    = "LastAuthStatus";
    private static HueSharedPreferences instance = null;
    private SharedPreferences mSharedPreferences = null;
    
    private Editor mSharedPreferencesEditor = null;
    
    
    public void create() {
      
    }
    
    public static HueSharedPreferences getInstance(Context ctx) {
        if (instance == null) {
            instance = new HueSharedPreferences(ctx);
        }
        return instance;
    }
    
    private HueSharedPreferences(Context appContext) {
        mSharedPreferences = appContext.getSharedPreferences(HUE_SHARED_PREFERENCES_STORE, 0); // 0 - for private mode
        mSharedPreferencesEditor = mSharedPreferences.edit();
    }
    
    
    public String getUsername() {
         String username = mSharedPreferences.getString(LAST_CONNECTED_USERNAME, "");
         if (username == null || username.isEmpty()){
        	 return null;
         }
    	 return username;
	}

	public boolean setUsername(String username) {
		if (username == null) {
			username = "";
		}
        mSharedPreferencesEditor.putString(LAST_CONNECTED_USERNAME, username);
        return (mSharedPreferencesEditor.commit());
	}
    
    public String getLastConnectedIPAddress() {
        return mSharedPreferences.getString(LAST_CONNECTED_IP, "");
   }

   public boolean setLastConnectedIPAddress(String ipAddress) {
       mSharedPreferencesEditor.putString(LAST_CONNECTED_IP, ipAddress);
       return (mSharedPreferencesEditor.commit());
   }
   
   public boolean setLastConnectedMAC(String mac) {
	   mSharedPreferencesEditor.putString(LAST_CONNECTED_MAC, mac);
	   return (mSharedPreferencesEditor.commit());
   }
   public String getLastConnectedMAC() {
	   return mSharedPreferences.getString(LAST_CONNECTED_MAC, "");
   }
   
   public boolean setLastAuthStatus(boolean b) {
       mSharedPreferencesEditor.putBoolean(LAST_AUTH_STATUS, b);
       return (mSharedPreferencesEditor.commit());
   }
   public boolean getLastAuthStatus() {
       return mSharedPreferences.getBoolean(LAST_AUTH_STATUS, false);
   }
   
   
   public static String Lights2String(List<HueLight> lights) throws IOException {
       // 实例化一个ByteArrayOutputStream对象，用来装载压缩后的字节文件。
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          // 然后将得到的字符数据装载到ObjectOutputStream
          ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                  byteArrayOutputStream);
          // writeObject 方法负责写入特定类的对象的状态，以便相应的 readObject 方法可以还原它
          objectOutputStream.writeObject(lights);
          // 最后，用Base64.encode将字节文件转换成Base64编码保存在String中
          String SceneListString = new String(Base64.encode(
                  byteArrayOutputStream.toByteArray(), Base64.DEFAULT));
          // 关闭objectOutputStream
          objectOutputStream.close();
          return SceneListString;
      }
      @SuppressWarnings("unchecked")
      public static List<HueLight> String2Lights(String str) 
              throws StreamCorruptedException, IOException,
              ClassNotFoundException {
          byte[] mobileBytes = Base64.decode(str.getBytes(),
                  Base64.DEFAULT);
          ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                  mobileBytes);
          ObjectInputStream objectInputStream = new ObjectInputStream(
                  byteArrayInputStream);
          List<HueLight> lights = (List<HueLight>) objectInputStream
                  .readObject();
          objectInputStream.close();
          return lights;
      }
}
