package org.learn.android.myhue.view;

import org.learn.android.myhue.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

public class MyToggleButton extends View implements OnClickListener{
	private boolean currState;
	private Bitmap btmOpen;
	private Bitmap btmClose;
	private Paint mPaint;
	private int width, height;
	private OnClickToggleListener listener;
	

	/**
	 * 在代码里面创建对象的时候，使用此构造方法
	 */
	public MyToggleButton(Context context) {
		this(context, null);
	}
	
	/**
	 * 在布局文件中声名的view，创建时由系统自动调用。
	 * @param context	上下文对象
	 * @param attrs		属性集
	 */
	public MyToggleButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MyToggleButton);
		int count = ta.getIndexCount();
		
		for(int i=0; i<count; i++) {
			int itemId = ta.getIndex(i);		//获得某个属性的ID值
			switch(itemId) {
				case R.styleable.MyToggleButton_current_state: 
					currState = ta.getBoolean(itemId, false);
					break;
					
				case R.styleable.MyToggleButton_w: 
					width = (int) ta.getDimension(itemId, 0);
					break;
					
				case R.styleable.MyToggleButton_h: 
					height = (int) ta.getDimension(itemId, 0);
					break;
			}
		}
		
		ta.recycle();
		initWidget();
	}
	
	
	/**
	 * 绘制当前view的内容
	 */
	@Override
	protected void onDraw(Canvas canvas) {
//		super.onDraw(canvas);
		/*
		 * backgroundBitmap	要绘制的图片
		 * left	图片的左边届
		 * top	图片的上边届
		 * paint 绘制图片要使用的画笔
		 */
		if(currState) {
			Rect dst = new Rect();// 屏幕 >>目标矩形
			dst.left = 0;
			dst.top = 0;
			dst.right = width;
			dst.bottom = height;
			// 画出指定的位图，位图将自动--》缩放/自动转换，以填补目标矩形
			// 这个方法的意思就像 将一个位图按照需求重画一遍，画后的位图就是我们需要的了
			canvas.drawBitmap(btmOpen, null, dst, null);
			dst = null;
		}else {
			Rect dst = new Rect();// 屏幕 >>目标矩形
			dst.left = 0;
			dst.top = 0;
			dst.right = width;
			dst.bottom = height;
			// 画出指定的位图，位图将自动--》缩放/自动转换，以填补目标矩形
			// 这个方法的意思就像 将一个位图按照需求重画一遍，画后的位图就是我们需要的了
			canvas.drawBitmap(btmClose, null, dst, null);
			dst = null;
		}
		
	}
	
	private void initWidget() {
		//初始化图片
		if(btmOpen == null) {
			btmOpen = BitmapFactory.decodeResource(getResources(), R.drawable.bg_toggle_open);
		}
		
		if(btmClose == null) {
			btmClose = BitmapFactory.decodeResource(getResources(), R.drawable.bg_toggle_close);
		}
		

		//初始化 画笔
		mPaint = new Paint();
		mPaint.setAntiAlias(true);	// 打开抗矩齿
		
		setOnClickListener(this);
		invalidate();
	}

	@Override
	public void onClick(View v) {
		currState = !currState;
		if(listener != null) {
			listener.changeState(currState);
		}
		invalidate();
	}
	
	public interface OnClickToggleListener {
		public void changeState(boolean state);
	}
	
	public void setOnClickToggleListener(OnClickToggleListener listener) {
		this.listener = listener;
	}
	
	public void open() {
		currState = true;
		invalidate();
	}
	
	public void close() {
		currState = false;
		invalidate();
	}

	public boolean isOpen() {
		return currState;
	}
}

