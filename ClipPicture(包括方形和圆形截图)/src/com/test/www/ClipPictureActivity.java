package com.test.www;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;

public class ClipPictureActivity extends Activity implements OnClickListener {
	private static final String TAG = "ClipPictureActivity";

	private ImageViewTouch mImageViewTouch;
	private ClipView clipView;
	private Button btnOK;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		mImageViewTouch = (ImageViewTouch) findViewById(R.id.imageviewTouch);
		mImageViewTouch.setBackgroundColor(Color.BLACK);

		BitmapDrawable bd = (BitmapDrawable) getResources().getDrawable(
				R.drawable.fruit);
		mImageViewTouch.setImageBitmapResetBase(bd.getBitmap(), true);
		mImageViewTouch.setFocusableInTouchMode(true);

		clipView = (ClipView) findViewById(R.id.clipview);

		btnOK = (Button) findViewById(R.id.sure);
		btnOK.setOnClickListener(this);

		setupOnTouchListeners(mImageViewTouch);
	}

	GestureDetector mGestureDetector;
	ScaleGestureDetector mScaleGestureDetector;
	private boolean mOnScale = false;
	private boolean mOnPagerScoll = false;
	private boolean mPaused;

	private void setupOnTouchListeners(View rootView) {
		mGestureDetector = new GestureDetector(this, new MyGestureListener(),
				null, true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1) {
			mScaleGestureDetector = new ScaleGestureDetector(this,
					new MyOnScaleGestureListener());
		}

		OnTouchListener rootListener = new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// NOTE: gestureDetector may handle onScroll..
				if (!mOnScale) {
					if (!mOnPagerScoll) {
						try {
							Log.d(TAG, "onTouchEvent");
							mGestureDetector.onTouchEvent(event);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1) {
					if (!mOnPagerScoll) {
						Log.d(TAG, "onTouchEvent:scale");
						mScaleGestureDetector.onTouchEvent(event);
					}
				}

				ImageViewTouch imageView = mImageViewTouch;
				if (!mOnScale && imageView != null
						&& imageView.mBitmapDisplayed != null
						&& imageView.mBitmapDisplayed.getBitmap() != null) {
					Matrix m = imageView.getImageViewMatrix();
					RectF rect = new RectF(0, 0, imageView.mBitmapDisplayed
							.getBitmap().getWidth(), imageView.mBitmapDisplayed
							.getBitmap().getHeight());
					m.mapRect(rect);
					// Log.d(TAG, "rect.right: " + rect.right +
					// ", rect.left: "
					// + rect.left + ", imageView.getWidth(): "
					// + imageView.getWidth());
					// 图片超出屏幕范围后移�?
					if (!(rect.right > imageView.getWidth() + 0.1 && rect.left < -0.1)) {
						try {
							// rootView.onTouchEvent(event);
						} catch (Exception e) {
							// why?
							e.printStackTrace();
						}
					}
				}

				// We do not use the return value of
				// mGestureDetector.onTouchEvent because we will not receive
				// the "up" event if we return false for the "down" event.
				return true;
			}
		};

		rootView.setOnTouchListener(rootListener);
	}

	private class MyOnScaleGestureListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {

		float currentScale;
		float currentMiddleX;
		float currentMiddleY;

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {

			Log.d(TAG, "gesture onScaleEnd");

			final ImageViewTouch imageView = getCurrentImageView();

			Log.d(TAG, "currentScale: " + currentScale + ", maxZoom: "
					+ imageView.mMaxZoom);
			if (currentScale > imageView.mMaxZoom) {
				imageView
						.zoomToNoCenterWithAni(currentScale
								/ imageView.mMaxZoom, 1, currentMiddleX,
								currentMiddleY);
				currentScale = imageView.mMaxZoom;
				imageView.zoomToNoCenterValue(currentScale, currentMiddleX,
						currentMiddleY);
			} else if (currentScale < imageView.mMinZoom) {
				imageView.zoomToNoCenterWithAni(currentScale,
						imageView.mMinZoom, currentMiddleX, currentMiddleY);
				currentScale = imageView.mMinZoom;
				imageView.zoomToNoCenterValue(currentScale, currentMiddleX,
						currentMiddleY);
			} else {
				imageView.zoomToNoCenter(currentScale, currentMiddleX,
						currentMiddleY);
			}

			imageView.center(true, true);

			// NOTE: 延迟修正缩放后可能移动问�?
			imageView.postDelayed(new Runnable() {
				public void run() {
					mOnScale = false;
				}
			}, 300);
			// Log.d(TAG, "gesture onScaleEnd");
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			Log.d(TAG, "gesture onScaleStart");
			mOnScale = true;
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector, float mx, float my) {
			Log.d(TAG, "gesture onScale");
			ImageViewTouch imageView = getCurrentImageView();
			float ns = imageView.getScale() * detector.getScaleFactor();

			currentScale = ns;
			currentMiddleX = mx;
			currentMiddleY = my;

			if (detector.isInProgress()) {
				imageView.zoomToNoCenter(ns, mx, my);
			}
			return true;
		}
	}

	private class MyGestureListener extends
			GestureDetector.SimpleOnGestureListener {

		// 截图框边界
		int[] edge;

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			Log.e(TAG, "gesture onScroll");
			if (mOnScale) {
				return true;
			}
			if (mPaused) {
				return false;
			}

			Log.d(TAG, "on Scroll moving:" + distanceX + ":" + distanceY);
			ImageViewTouch imageView = getCurrentImageView();
			imageView.panBy(-distanceX, -distanceY);

			Log.d(TAG, "imageView gettop:");

			// 超出边界效果去掉这个
			// imageView.center(true, true);

			if (edge == null) {
				edge = clipView.getRect();
			}

			imageView.touchEdge(edge[0], edge[1], edge[2], edge[3]);

			return true;
		}

		@Override
		public boolean onUp(MotionEvent e) {
			// getCurrentImageView().center(true, true);
			return super.onUp(e);
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {

			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (mPaused) {
				return false;
			}
			ImageViewTouch imageView = getCurrentImageView();
			// Switch between the original scale and 3x scale.
			if (imageView.mBaseZoom < 1) {
				if (imageView.getScale() > 2F) {
					imageView.zoomTo(1f);
				} else {
					imageView.zoomToPoint(3f, e.getX(), e.getY());
				}
			} else {
				if (imageView.getScale() > (imageView.mMinZoom + imageView.mMaxZoom) / 2f) {
					imageView.zoomTo(imageView.mMinZoom);
				} else {
					imageView.zoomToPoint(imageView.mMaxZoom, e.getX(),
							e.getY());
				}
			}

			return true;
		}
	}

	private ImageViewTouch getCurrentImageView() {
		return mImageViewTouch;
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.sure:
			Bitmap fianBitmap = clipView.getBitmap(this);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			fianBitmap.compress(Bitmap.CompressFormat.PNG, 70, baos);// 这里需要用PNG,
																		// 否则图像会有黑色边框,
																		// 通过intent传输图片,有可能会因为图片过大而导致传输失败,
																		// 可以将图片先存入本地临时目录中,
																		// 再从临时目录去读取
			byte[] bitmapByte = baos.toByteArray();

			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), PreviewActivity.class);
			intent.putExtra("bitmap", bitmapByte);

			startActivity(intent);
			break;
		default:
			break;
		}
	}

}