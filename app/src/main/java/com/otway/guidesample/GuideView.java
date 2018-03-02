package com.otway.guidesample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Otway on 2018/2/27.
 */

public class GuideView extends RelativeLayout implements ViewTreeObserver.OnGlobalLayoutListener {

	private final String TAG = this.getClass().getSimpleName();
	private static final String SHOW_GUIDE_PREFIX = "show_guide_on_view_";
	public static final int CIRCLE = 0x11;
	public static final int RECTANGLE = 0x12;

	private int mBgColor = Color.parseColor("#99000000");
	private boolean mFirstBlood;
	private View mTargetView;
	private int mShape = RECTANGLE;
	private boolean mIsMeasured;

	private RectF mRect;
	private int mRadius = 4;


	public void setOutsideEnable(boolean outsideEnable) {
		if (outsideEnable) {
			this.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hide();
				}
			});
		} else {
			this.setOnClickListener(null);
		}
	}


	@IntDef({CIRCLE, RECTANGLE})
	public @interface Shape {
	}

	private Context mContext;
	private View mCustomGuideView;

	private GuideView(Context context) {
		super(context);
		init(context);
	}

	private void resetState() {
		mTargetView = null;
		mIsMeasured = false;
		mRadius = 4;
		mRect = null;
		mFirstBlood = false;
		mShape = RECTANGLE;
		mBgColor = Color.parseColor("#99000000");
	}

	private void init(Context context) {
		this.mContext = context;
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		this.setLayoutParams(layoutParams);

		this.setClickable(true);
		this.setFocusable(true);

		setLayerType(LAYER_TYPE_SOFTWARE, null);

		setWillNotDraw(false);

		resetState();
	}

	public void show() {
		if (mCustomGuideView == null) {
			return;
		}

		if (mFirstBlood && hasShown()) {
			return;
		}

		if (mFirstBlood && !hasShown()) {
			saveState();
		}

		if (mTargetView != null) {
			mTargetView.getViewTreeObserver().addOnGlobalLayoutListener(this);
		}

		if (mContext instanceof Activity) {
			ViewGroup decorView = (ViewGroup) Activity.class.cast(mContext).getWindow().getDecorView();
			decorView.addView(this);
		}
	}

	public void hide() {
		if (mCustomGuideView == null) {
			return;
		}

		mTargetView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		this.removeAllViews();

		if (mContext instanceof Activity) {
			ViewGroup decorView = (ViewGroup) Activity.class.cast(mContext).getWindow().getDecorView();
			decorView.removeView(this);
		}

		resetState();
	}

	@Override
	public void onGlobalLayout() {
		Log.d(TAG, "onGlobalLayout: GlobalLayoutListener callback");

		if (mIsMeasured) {
			mTargetView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			return;
		}

		int width = mTargetView.getWidth();
		int height = mTargetView.getHeight();

		if (height > 0 && width > 0) {
			mIsMeasured = true;
		}

		if (!mIsMeasured) {
			return;
		}

		int[] location = new int[2];
		mTargetView.getLocationOnScreen(location);

		if (mRect == null) {
			mRect = new RectF();
		}

		mRect.left = location[0];
		mRect.top = location[1];
		mRect.right = location[0] + width;
		mRect.bottom = location[1] + height;


		int id = mTargetView.getId();
		View view = findViewById(id);
		if (view != null) {
			ViewGroup.MarginLayoutParams layoutParams = (MarginLayoutParams) view.getLayoutParams();
			layoutParams.width = width;
			layoutParams.height = height;
			layoutParams.topMargin = location[1];
		}
		// todo there has not set the left and right margin , you should add rule in custom view to place a suitable location to fit the target view
		mCustomGuideView.setVisibility(VISIBLE);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (!mIsMeasured) {
			return;
		}

		drawBackground(canvas);
	}

	private void drawBackground(Canvas canvas) {

		Log.d(TAG, "drawBackground: is drawing background");

		// draw bitmap then draw it on screen
		Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas temp = new Canvas(bitmap);

		Paint bgPaint = new Paint();
		bgPaint.setColor(mBgColor);

		// draw bg
		temp.drawRect(0, 0, temp.getWidth(), temp.getHeight(), bgPaint);

		// hollow out paint
		Paint opPaint = new Paint();
		PorterDuffXfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);// or SRC_OUT
		opPaint.setXfermode(xfermode);
		opPaint.setAntiAlias(true);

		switch (mShape) {
			case CIRCLE:
				temp.drawCircle(mRect.centerX(), mRect.centerY(), mRadius, opPaint);
				break;
			case RECTANGLE:
				temp.drawRoundRect(mRect, mRadius, mRadius, opPaint);
				break;
		}

		// draw on screen
		canvas.drawBitmap(bitmap, 0, 0, bgPaint);
		bitmap.recycle();
	}

	public void setCustomGuideView(View customGuideView) {
		this.mCustomGuideView = customGuideView;
		this.mCustomGuideView.setVisibility(GONE);
		this.addView(mCustomGuideView);
	}

	public void setCustomGuideView(@LayoutRes int layoutId) {
		View inflate = LayoutInflater.from(mContext).inflate(layoutId, null);
		setCustomGuideView(inflate);
	}

	public void setTargetView(View view) {
		this.mTargetView = view;
	}

	public void setShape(@Shape int shape) {
		this.mShape = shape;
	}

	public void setBgColor(@ColorInt int color) {
		this.mBgColor = color;
	}

	public void setRadius(int radius) {
		this.mRadius = radius;
	}

	private void saveState() {
		if (mTargetView != null) {
			mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putBoolean(SHOW_GUIDE_PREFIX + mTargetView.getId(), true).apply();
		}
	}

	private boolean hasShown() {
		if (mTargetView == null) {
			return true;
		}
		return mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean(SHOW_GUIDE_PREFIX + mTargetView.getId(), false);
	}

	public void showOnce() {
		mFirstBlood = true;
	}

	public void setText(int viewId, CharSequence text) {
		TextView textView = findViewByIdx(viewId);
		if (textView != null) {
			textView.setText(text);
		}
	}

	public void setOnClickListener(int viewId, OnClickListener listener) {
		View view = findViewById(viewId);
		if (view != null) {
			view.setOnClickListener(listener);
		}
	}

	private <T extends View> T findViewByIdx(int viewId) {
		if (mCustomGuideView == null || viewId < 0) {
			return null;
		}

		View view = mCustomGuideView.findViewById(viewId);

		if (view != null) {
			return (T) view;
		}

		return null;
	}

	public static class Builder {

		private GuideView guideView;

		private Builder(Context context) {
			guideView = new GuideView(context);
		}

		public static Builder newInstance(Context context) {
			return new Builder(context);
		}

		public Builder setTargetView(View target) {
			guideView.setTargetView(target);
			return this;
		}

		public Builder setCustomView(View guideLayout) {
			guideView.setCustomGuideView(guideLayout);
			return this;
		}

		public Builder setCustomView(@LayoutRes int layout) {
			guideView.setCustomGuideView(layout);
			return this;
		}

		public Builder setBgColor(@ColorInt int color) {
			guideView.setBgColor(color);
			return this;
		}

		public Builder setOnClickListener(@IdRes int viewId, OnClickListener listener) {
			guideView.setOnClickListener(viewId, listener);
			return this;
		}

		public Builder setText(@IdRes int viewId, CharSequence text) {
			guideView.setText(viewId, text);
			return this;
		}

		public Builder setShape(@Shape int shape) {
			guideView.setShape(shape);
			return this;
		}

		public Builder setRadius(int radius) {
			guideView.setRadius(radius);
			return this;
		}

		public Builder showOnce() {
			guideView.showOnce();
			return this;
		}

		public Builder setOutsideEnable(boolean enable) {
			guideView.setOutsideEnable(enable);
			return this;
		}


		public GuideView build() {
			return guideView;
		}

	}
}
