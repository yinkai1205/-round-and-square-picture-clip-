package com.test.www;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Window;

public class ClipView extends View {

	private int squareSize;

	private boolean isRoundCip = false;//需要圆形截图, 改为true

	public ClipView(Context context) {
		super(context);
	}

	public ClipView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ClipView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = this.getWidth();
		int height = this.getHeight();

		squareSize = (int) (width * 0.6);

		Paint paint = new Paint();
		paint.setColor(0xaa000000);

		if (isRoundCip) {
			// 圆形截图实现策略，原理其实就是画了一个非常厚的空心圆
			paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
			paint.setAntiAlias(true); // 消除锯齿

			paint.setStrokeWidth(squareSize);
			canvas.drawCircle(width / 2, height / 2, squareSize, paint);

			paint.setStrokeWidth(dip2px(getContext(), 2));
			paint.setColor(Color.WHITE);
			canvas.drawCircle(width / 2, height / 2, squareSize / 2, paint);
		} else {
			// 方形截图实现策略
			isRoundCip = false;
			// top
			canvas.drawRect(0, 0, width, (height - squareSize) / 2, paint);
			// left
			canvas.drawRect(0, (height - squareSize) / 2,
					(width - squareSize) / 2, (height + squareSize) / 2, paint);
			// right
			canvas.drawRect((width + squareSize) / 2,
					(height - squareSize) / 2, width,
					(height + squareSize) / 2, paint);
			// bottom
			canvas.drawRect(0, (height + squareSize) / 2, width, height, paint);

			paint.setStyle(Paint.Style.STROKE); // 绘制空心圆
			paint.setAntiAlias(true); // 消除锯齿
			paint.setStrokeWidth(dip2px(getContext(), 2));
			paint.setColor(Color.WHITE);

			int[] rect = getRect();
			canvas.drawRect(rect[1], rect[0], rect[3], rect[2], paint);
		}
	}

	public int[] getRect() {
		int[] rect = new int[4];
		int top = (getHeight() - squareSize) / 2;
		int left = (getWidth() - squareSize) / 2;
		int bottom = getHeight() - top;
		int right = getWidth() - left;

		rect[0] = top;
		rect[1] = left;
		rect[2] = bottom;
		rect[3] = right;

		return rect;
	}

	public Bitmap getBitmap(Activity activity) {
		int[] h = getBarHeight(activity);
		Bitmap screenShoot = takeScreenShot(activity);

		int[] i = getRect();

		int delta = 0;
		if (isRoundCip) {
			delta = dip2px(getContext(), 2);
		}

		Bitmap finalBitmap = Bitmap.createBitmap(screenShoot, i[1] + delta,
				i[0] + h[1] + h[0] + delta, squareSize - 2 * delta, squareSize
						- 2 * delta);

		if (isRoundCip) {
			return getCircularBitmap(finalBitmap);
		} else {
			return finalBitmap;
		}
	}

	private int[] getBarHeight(Activity activity) {
		Rect frame = new Rect();
		activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
		int statusBarHeight = frame.top;

		int contenttop = activity.getWindow()
				.findViewById(Window.ID_ANDROID_CONTENT).getTop();
		int titleBarHeight = contenttop - statusBarHeight;

		Log.v("ClipView", "statusBarHeight = " + statusBarHeight
				+ ", titleBarHeight = " + titleBarHeight);

		return new int[] { statusBarHeight, titleBarHeight };
	}

	private Bitmap takeScreenShot(Activity activity) {
		View view = activity.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		return view.getDrawingCache();
	}

	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	public static Bitmap getCircularBitmap(Bitmap bitmap) {
		Bitmap output;

		if (bitmap.getWidth() > bitmap.getHeight()) {
			output = Bitmap.createBitmap(bitmap.getHeight(),
					bitmap.getHeight(), Config.ARGB_8888);
		} else {
			output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(),
					Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		float r = 0;

		if (bitmap.getWidth() > bitmap.getHeight()) {
			r = bitmap.getHeight() / 2;
		} else {
			r = bitmap.getWidth() / 2;
		}

		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawCircle(r, r, r, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	public static Bitmap getCroppedBitmap(Bitmap bmp, int radius) {
		Bitmap sbmp;
		if (bmp.getWidth() != radius || bmp.getHeight() != radius)
			sbmp = Bitmap.createScaledBitmap(bmp, radius, radius, false);
		else
			sbmp = bmp;
		Bitmap output = Bitmap.createBitmap(sbmp.getWidth(), sbmp.getHeight(),
				Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xffa19774;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, sbmp.getWidth(), sbmp.getHeight());

		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(Color.parseColor("#BAB399"));
		canvas.drawCircle(sbmp.getWidth() / 2 + 0.7f,
				sbmp.getHeight() / 2 + 0.7f, sbmp.getWidth() / 2 + 0.1f, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(sbmp, rect, rect, paint);

		return output;
	}

	public static Bitmap get(Bitmap bitmapimg) {
		Bitmap output = Bitmap.createBitmap(bitmapimg.getWidth(),
				bitmapimg.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmapimg.getWidth(),
				bitmapimg.getHeight());

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawCircle(bitmapimg.getWidth() / 2, bitmapimg.getHeight() / 2,
				bitmapimg.getWidth() / 2, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmapimg, rect, rect, paint);
		return output;
	}
}
