package org.learn.android.myhue;

import org.json.hue.JSONArray;
import org.json.hue.JSONException;
import org.json.hue.JSONObject;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import org.learn.android.myhue.utils.DialogUtils;
import org.learn.android.myhue.utils.WindowUtil;
import org.learn.android.myhue.view.MyToggleButton;
import org.learn.android.myhue.view.MyToggleButton.OnClickToggleListener;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class LightHueAct extends BaseActivity implements
		OnSeekBarChangeListener, OnClickListener {
	private static final int RANGE_VALUE = 20;
	private TextView tvTitle, tvName, tvRoom, tvDimmingPercent;
	private ImageView ivBack, ivFloatFrame, ivColorChooseWidget, ivColorPlate, ivLightType;
	private SeekBar sbBrightAdjust;
	private RelativeLayout rlContent;
	private int circleCenterX, circleCenterY, curColor, startX, startY,
			circleCenterX2, circleCenterY2;
	private float factorW = 0, factorH = 0;
	private Bitmap bmpFloatFrame, bmpColor;
	private LayoutParams lpForCircle;
	private LayoutParams lpForFloatFrame;
	private int ivColorPlateLeft, ivColorPlateTop;
	private int ivFloatFrameWidth, ivFloatFrameHeight;
	private int preColor;
	private HueLight mDevice;
	private int r, g, b, brightness;
	private int mRangeR, mRangeG, mRangeB;
	private MyToggleButton toggle;
	private Dialog mDlgProgress;
	private boolean mIsSimu=false;
//	private Handler mHandler = new Handler(new Callback() {
//
//		@Override
//		public boolean handleMessage(Message msg) {
//			return false;
//		}
//	});

	@Override
	protected int getContentViewId() {
		return R.layout.activity_light_hue;
	}

	@Override
	protected void findViews() {
		tvTitle = getView(R.id.tv_title);
		ivBack = getView(R.id.iv_back);
		sbBrightAdjust = getView(R.id.sb_brightness_adjust);
		ivFloatFrame = getView(R.id.iv_float_frame);
		ivColorChooseWidget = getView(R.id.iv_color_choose_widget);
		rlContent = getView(R.id.rl_content);
		ivColorPlate = getView(R.id.iv_color_plate);
		tvName = getView(R.id.tv_name);
		tvRoom = getView(R.id.tv_room);
		tvDimmingPercent = getView(R.id.tv_dimming_percent);
		toggle = getView(R.id.toggle);
		ivLightType = getView(R.id.iv_light);
	}

	private void initRGB() {
		rlContent.post(new Runnable() {

			@Override
			public void run() {

				BitmapDrawable bd = (BitmapDrawable) ivColorPlate.getDrawable();
				bmpColor = bd.getBitmap();

				Rect outRect = new Rect();
				ivColorPlate.getHitRect(outRect);

				ivColorPlateTop = ivColorPlate.getTop();
				ivColorPlateLeft = ivColorPlate.getLeft();

				ivFloatFrameWidth = ivFloatFrame.getWidth();
				ivFloatFrameHeight = ivFloatFrame.getHeight();

				factorW = bmpColor.getWidth() / (float) ivColorPlate.getWidth();
				factorH = bmpColor.getHeight() / (float) ivColorPlate.getHeight();

//				Log.d(TAG, "init ivColorPlateTop--->"+ivColorPlateTop);
//				Log.d(TAG, "init ivColorPlate--->"+ivColorPlate.getHeight());
//				Log.d(TAG, "init r--->"+r);
//				Log.d(TAG, "init g--->"+g);
//				Log.d(TAG, "init b--->"+b);
				// TODO 传入rgb值
				initPositionFromColorPlate(r, g, b);
				setColorWidgetLocation();
				setColorWidgetColor();
				
				// init it
                if (brightness == 0) {
                    setColorChooseWidgetVisibility(false);
                    toggle.close();
                } else {
                    setColorChooseWidgetVisibility(toggle.isOpen());
                }
			}
		});
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void initData() {
		mDevice = (HueLight) getIntent().getSerializableExtra("light");
		mIsSimu = (Boolean)getIntent().getSerializableExtra("isSimu");
		if (mDevice.getAppDevId() == HueBridge.APPID_DEV_HUE_GROUP) {
			ivLightType.setImageResource(R.drawable.ic_light_hue_group);
            tvTitle.setText("灯组");
            tvName.setText("Group " + mDevice.getEndPoint());
		} else {
		    tvTitle.setText("灯光");
		    tvName.setText("Light " + mDevice.getEndPoint());
		}

		HueBridge hb = mDevice.getBridge();
		if (hb == null) {
			Log.d(TAG, "No bridge");
			return;
		}
		Log.d(TAG, "hb ip " + hb.getIp());
		Log.d(TAG, " username " + hb.getUsername());
		String ip = hb.getIp();
		String user = hb.getUsername();
		String url_prefix = String.format("http://%s/api/%s", ip, user);
		HttpUtils http = new HttpUtils(3*1000); //HueManager.getHttpClient();
		String url = String.format("%s/%s/%d", url_prefix, (mDevice.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "lights" : "groups", mDevice.getEndPoint());
		Log.d(TAG, url);
		http.send(HttpMethod.GET, url, null, new RequestCallBack<String>() {

			@Override
			public void onFailure(HttpException arg0, String arg1) {
				// TODO： finish if fail
				Log.e(TAG, "init " + arg1);
				Toast.makeText(mContext, "Connect HUE light fail.", Toast.LENGTH_SHORT).show();
				if (!mIsSimu) finish();
			}

			@Override
			public void onSuccess(ResponseInfo<String> arg0) {
				Log.d(TAG, arg0.result);
				try {
					JSONObject jsonObject = new JSONObject(arg0.result);
					JSONObject state = jsonObject.getJSONObject((mDevice.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "state" : "action");
					brightness = state.getInt("bri");
					boolean isOn = state.getBoolean("on");
					Log.d(TAG, "Init bri " + brightness);
					Log.d(TAG, "Init on " + isOn);
					if (brightness == 0 || !isOn) {
						toggle.close();
					} else {
						toggle.open();
					}
					sbBrightAdjust.setProgress(brightness);

					JSONArray arr = state.getJSONArray("xy");
					float[] xy = new float[2];
					xy[0] = (float) arr.getDouble(0);
					xy[1] = (float) arr.getDouble(1);

					int color = PHUtilities.colorFromXY(xy, "");
					r = ((color & 0x00FF0000) >> 16);
					g = ((color & 0x0000FF00) >> 8);
					b = (color & 0x000000FF);
					Log.d(TAG, "X y " + xy[0] + " " + xy[1]);
					Log.d(TAG, "brightness " + brightness);

                    initRGB();
					if (mDlgProgress!=null) {
					    mDlgProgress.dismiss();
					    mDlgProgress = null;
					}
				} catch (JSONException e) {
					Log.d(TAG, "Json parse error.");
					e.printStackTrace();
					if (mDlgProgress!=null) {
                        mDlgProgress.dismiss();
                        mDlgProgress = null;
                    }
					Toast.makeText(mContext, "Connect HUE light fail.", Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		});
	}

	@Override
	protected void setListener() {
		sbBrightAdjust.setOnSeekBarChangeListener(this);
		ivBack.setOnClickListener(this);

		sbBrightAdjust.setProgress(brightness);

		toggle.setOnClickToggleListener(new OnClickToggleListener() {

			@Override
			public void changeState(boolean state) {
				setColorChooseWidgetVisibility(state);
				// PHLightState stateHueLight =
				// HueManager.getInstance().getLightStatus(mDevice.getEndPoint());
				if (!state) {
				    Log.d(TAG, "Change to off");
					if (!mIsSimu) HueManager.getInstance().setLightOn(mDevice, state);
					sbBrightAdjust.setProgress(0);
				} else {
				    Log.d(TAG, "Change to on, bri " + brightness);
				    if (brightness == 0) {
				        brightness = 181;
						if (!mIsSimu) HueManager.getInstance().setLightBrightness(mDevice, brightness);
				    } else {
						if (!mIsSimu) HueManager.getInstance().setLightOn(mDevice, state);
				    }
					sbBrightAdjust.setProgress(brightness);
				}
			}
		});

		ivFloatFrame.setOnTouchListener(new OnTouchListener() {
			int left;
			int top;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startX = (int) event.getRawX();
						startY = (int) event.getRawY();
						break;

					case MotionEvent.ACTION_MOVE:
						int newX = (int) event.getRawX();
						int newY = (int) event.getRawY();
						int dx = newX - startX;
						int dy = newY - startY;

						left = lpForCircle.leftMargin + dx;
						top = lpForCircle.topMargin + dy;

						// 考虑边界问题
						if (left < ivColorPlateLeft) {
							left = ivColorPlateLeft;
						}
						if (top < ivColorPlateTop) {
							top = ivColorPlateTop;
						}
						if (left > (WindowUtil.getScreenWidth(mContext) - ivColorPlateLeft - ivColorChooseWidget.getWidth())) {
							left = WindowUtil.getScreenWidth(mContext) - ivColorPlateLeft - ivColorChooseWidget.getWidth();
						}
						if (top > (rlContent.getHeight() - ivColorChooseWidget.getHeight())) {
							top = rlContent.getHeight() - ivColorChooseWidget.getHeight();
						}

						circleCenterX = left + ivColorChooseWidget.getWidth() / 2;
						circleCenterY = top + ivColorChooseWidget.getHeight() / 2;
						setColorWidgetLocation();
						setColorWidgetColor();

						// 重新初始化手指的开始结束位置。
						startX = (int) event.getRawX();
						startY = (int) event.getRawY();
						break;

					case MotionEvent.ACTION_UP:
						setColorWidgetColor();
						// saveInfo();
						// TODO 拖动颜色选择滑块后进行hue灯颜色的调整
//						Log.d(TAG, "r--------->" + r);
//						Log.d(TAG, "g--------->" + g);
//						Log.d(TAG, "b--------->" + b);
//						Log.d(TAG, "brightness--------->" + brightness);
						if (!mIsSimu) HueManager.getInstance().setLightRGB(mDevice, r, g, b);
						break;
				}
				return true;
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.iv_back:
				finish();
				break;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		tvDimmingPercent.setText(String.format("%d%%", progress * 100 / 254));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int status = seekBar.getProgress();
		brightness = status;
		Log.d(TAG, "Progress stop touch " + status);
		// TODO 停止拖动后调整灯光强度
		if (brightness == 0) {
			// TODO 关闭hue灯
			toggle.close();
		} else {
			toggle.open();
		}
		if (!mIsSimu) HueManager.getInstance().setLightBrightness(mDevice, brightness);
	}

	private void setColorWidgetLocation() {
		lpForCircle = new LayoutParams(ivColorChooseWidget.getWidth(), ivColorChooseWidget.getHeight());
		lpForCircle.leftMargin = circleCenterX - ivColorChooseWidget.getWidth() / 2;
		lpForCircle.topMargin = circleCenterY - ivColorChooseWidget.getHeight() / 2;
		ivColorChooseWidget.setLayoutParams(lpForCircle);

		lpForFloatFrame = new LayoutParams(ivFloatFrameWidth, ivFloatFrameHeight);
		lpForFloatFrame.leftMargin = circleCenterX - ivFloatFrame.getWidth() / 2;
		lpForFloatFrame.topMargin = circleCenterY - ivFloatFrame.getHeight() - ivColorChooseWidget.getHeight() / 2;
		ivFloatFrame.setLayoutParams(lpForFloatFrame);
	}

	private void setColorWidgetColor() {
		Bitmap bmp = ((BitmapDrawable) ivFloatFrame.getDrawable()).getBitmap();
		bmpFloatFrame = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
		Canvas canvas = new Canvas(bmpFloatFrame);
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(5);
		canvas.drawBitmap(bmp, new Matrix(), paint);

		int x = (int) ((circleCenterX - ivColorPlateLeft) * factorW);
		int y = (int) ((circleCenterY - ivColorPlateTop) * factorH);
		if (x < 0) {
			x = 0;
		} else if (x >= bmpColor.getWidth()) {
			x = bmpColor.getWidth() - 1;
		}

		if (y < 0) {
			y = 0;
		} else if (y >= bmpColor.getHeight()) {
			y = bmpColor.getHeight() - 1;
		}

		preColor = curColor;
		curColor = bmpColor.getPixel(x, y);
		r = Color.red(curColor);
		g = Color.green(curColor);
		b = Color.blue(curColor);

		int centerX = bmpFloatFrame.getWidth() / 2;
		int centerY = bmpFloatFrame.getHeight() / 2;

		// 以图片中心点开始扫描图片右上部分
		for (int i = centerX; i < bmpFloatFrame.getWidth(); i++) {
			for (int j = centerY; j > 0; j--) {
				if (bmpFloatFrame.getPixel(i, j) != Color.TRANSPARENT && bmpFloatFrame.getPixel(i, j) != preColor) {
					break;
				} else {
					bmpFloatFrame.setPixel(i, j, curColor);
				}
			}
		}

		// 以图片中心点开始扫描图片左上部分
		for (int i = centerX; i > 0; i--) {
			for (int j = centerY; j > 0; j--) {
				if (bmpFloatFrame.getPixel(i, j) != Color.TRANSPARENT && bmpFloatFrame.getPixel(i, j) != preColor) {
					break;
				} else {
					bmpFloatFrame.setPixel(i, j, curColor);
				}
			}
		}

		// 以图片中心点开始扫描图片右下部分
		for (int i = centerX; i < bmpFloatFrame.getWidth(); i++) {
			for (int j = centerY + 1; j < bmpFloatFrame.getHeight(); j++) {
				if (bmpFloatFrame.getPixel(i, j) != Color.TRANSPARENT && bmpFloatFrame.getPixel(i, j) != preColor) {
					break;
				} else {
					bmpFloatFrame.setPixel(i, j, curColor);
				}
			}
		}

		// 以图片中心点开始扫描图片左下部分
		for (int i = centerX; i > 0; i--) {
			for (int j = centerY + 1; j < bmpFloatFrame.getHeight(); j++) {
				if (bmpFloatFrame.getPixel(i, j) != Color.TRANSPARENT && bmpFloatFrame.getPixel(i, j) != preColor) {
					break;
				} else {
					bmpFloatFrame.setPixel(i, j, curColor);
				}
			}
		}

		ivFloatFrame.setImageBitmap(bmpFloatFrame);
	}

	/**
	 * 设置浮动的颜色选择框是否可见
	 * 
	 * @param flag
	 */
	private void setColorChooseWidgetVisibility(boolean flag) {
		if (flag) {
			ivFloatFrame.setVisibility(View.VISIBLE);
			ivColorChooseWidget.setVisibility(View.VISIBLE);
		} else {
			ivFloatFrame.setVisibility(View.GONE);
			ivColorChooseWidget.setVisibility(View.GONE);
		}
	}

	private void initPositionFromColorPlate(int r, int g, int b) {

		outer: for (int i = 0; i < bmpColor.getWidth(); i++) {
			for (int j = 0; j < bmpColor.getHeight(); j++) {
				int color = bmpColor.getPixel(i, j);
				int tempR = Color.red(color);
				int tempG = Color.green(color);
				int tempB = Color.blue(color);
				if (tempR == r && tempG == g && tempB == b) {
					circleCenterX = (int) (i / factorW + ivColorPlateLeft);
					circleCenterY = (int) (j / factorH + ivColorPlateTop);
					break outer;
				}

				int rangeR = Math.abs(tempR - r);
				int rangeG = Math.abs(tempG - g);
				int rangeB = Math.abs(tempB - b);
				if (rangeR <= RANGE_VALUE && rangeG <= RANGE_VALUE && rangeB <= RANGE_VALUE) {
					if (this.mRangeR != 0 && this.mRangeG != 0 && this.mRangeB != 0) {
						int total = rangeR + rangeG + rangeB;
						int total2 = this.mRangeR + this.mRangeG + this.mRangeB;

						if (total < total2) {
							
							circleCenterX2 = (int) (i / factorW + ivColorPlateLeft);
							circleCenterY2 = (int) (j / factorH + ivColorPlateTop);

							this.mRangeR = rangeR;
							this.mRangeG = rangeG;
							this.mRangeB = rangeB;
						}
					} else {
						circleCenterX2 = (int) (i / factorW + ivColorPlateLeft);
						circleCenterY2 = (int) (j / factorH + ivColorPlateTop);

						this.mRangeR = rangeR;
						this.mRangeG = rangeG;
						this.mRangeB = rangeB;
					}
				}
			}
		}

		if (circleCenterX == 0 && circleCenterY == 0) {
			if (circleCenterX2 == 0 && circleCenterY2 == 0) {
				circleCenterX = ivColorPlateLeft;
				circleCenterY = ivColorPlateTop;
			} else {
				circleCenterX = circleCenterX2;
				circleCenterY = circleCenterY2;
			}
		}
	}
}
