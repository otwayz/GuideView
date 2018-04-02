package com.otway.guidesample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by Otway on 2018/2/27.
 */

public class GuideView extends FrameLayout implements ViewTreeObserver.OnGlobalLayoutListener {

	private final String TAG = this.getClass().getSimpleName();
	private final static int INVALID_COLOR = Integer.MIN_VALUE;
	public static final int CIRCLE = 0x11;
	public static final int RECTANGLE = 0x12;

	private int mBgColor = Color.parseColor("#99000000");
	private View mTargetView;
	private int mShape = CIRCLE;
	private boolean mIsMeasured;

	private RectF mRect;
	private int mRadius = 4;

	private void setOutsideEnable(boolean outsideEnable) {
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

	private GuideView(Context context, Builder builder) {
		super(context);
		init(context, builder);
	}

	private GuideView(Context context) {
		super(context);
	}

	private GuideView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private GuideView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private GuideView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	private void resetState() {
		mTargetView = null;
		mIsMeasured = false;
		mRadius = 4;
		mRect = null;
		mShape = RECTANGLE;
		mBgColor = Color.parseColor("#99000000");
	}

	private void init(Context context, Builder builder) {
		this.mContext = context;
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		this.setLayoutParams(layoutParams);

		this.setClickable(true);
		this.setFocusable(true);

		if (builder != null) {
			if (builder.guideLayoutId != -1) {
				setCustomGuideView(builder.guideLayoutId);
			} else if (builder.guideLayout != null) {
				setCustomGuideView(builder.guideLayout);
			} else {
				return;
			}
			setTargetView(builder.targetView);
			if (builder.bgColor != INVALID_COLOR) {
				setBgColor(builder.bgColor);
			}
			setShape(builder.shapeType);
			setRadius(builder.roundRadius);
			setOutsideEnable(builder.outsideAble);
		}

		setLayerType(LAYER_TYPE_SOFTWARE, null);
		setWillNotDraw(false);
	}

	public void show() {
		if (mCustomGuideView == null) {
			return;
		}

		if (mContext instanceof Activity && !((Activity) mContext).isFinishing()) {
			ViewGroup decorView = (ViewGroup) Activity.class.cast(mContext).getWindow().getDecorView();
			decorView.addView(this);
		}

		if (mTargetView != null) {
			mTargetView.getViewTreeObserver().addOnGlobalLayoutListener(this);
		} else {
			mIsMeasured = true;
			mCustomGuideView.setVisibility(VISIBLE);
		}
	}

	public void hide() {
		if (mCustomGuideView == null) {
			return;
		}

		ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0f).setDuration(300);
		alphaAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (mTargetView != null) {
					mTargetView.getViewTreeObserver().removeOnGlobalLayoutListener(GuideView.this);
				}
				GuideView.this.removeAllViews();

				if (mContext instanceof Activity) {
					ViewGroup decorView = (ViewGroup) Activity.class.cast(mContext).getWindow().getDecorView();
					decorView.removeView(GuideView.this);
				}

				resetState();
			}
		});
		alphaAnimator.start();
	}

	@Override
	public void onGlobalLayout() {
		Log.d(TAG, "onGlobalLayout: GlobalLayoutListener callback");

		if (mTargetView != null) {

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
			// FIXME there has not set the left and right margin , you should add rule in custom view to place a suitable location to fit the target view
			mCustomGuideView.setVisibility(VISIBLE);
		}
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

		Log.d(TAG, "drawBackground: is drawing background mRect: " + mRect);
		// draw bitmap then draw it on screen
		Bitmap bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas temp = new Canvas(bitmap);

		Paint bgPaint = new Paint();
		bgPaint.setColor(mBgColor);

		// draw bg
		temp.drawRect(0, 0, temp.getWidth(), temp.getHeight(), bgPaint);

		if (mRect != null) {
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
		}

		// draw on screen
		canvas.drawBitmap(bitmap, 0, 0, bgPaint);
		bitmap.recycle();
	}

	private void setCustomGuideView(@NonNull View customGuideView) {
		this.mCustomGuideView = customGuideView;
		this.mCustomGuideView.setVisibility(GONE);
		this.addView(mCustomGuideView);
	}

	private void setCustomGuideView(@LayoutRes int layoutId) {
		View inflate = LayoutInflater.from(mContext).inflate(layoutId, null);
		setCustomGuideView(inflate);
	}

	private void setTargetView(View view) {
		this.mTargetView = view;
	}

	private void setShape(@Shape int shape) {
		this.mShape = shape;
	}

	private void setBgColor(@ColorInt int color) {
		this.mBgColor = color;
	}

	private void setRadius(int radius) {
		this.mRadius = radius;
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

		private View targetView;
		private View guideLayout;
		private int guideLayoutId = -1;
		private int bgColor = GuideView.INVALID_COLOR;
		private int shapeType = CIRCLE;
		private int roundRadius = 4;
		private boolean outsideAble;

		public Builder setTargetView(View target) {
			targetView = target;
			return this;
		}

		public Builder setCustomView(View layout) {
			guideLayout = layout;
			return this;
		}

		public Builder setCustomView(@LayoutRes int layoutId) {
			guideLayoutId = layoutId;
			return this;
		}

		public Builder setBgColor(@ColorInt int color) {
			bgColor = color;
			return this;
		}

		public Builder setShape(@Shape int shape) {
			shapeType = shape;
			return this;
		}

		public Builder setRadius(int radius) {
			roundRadius = radius;
			return this;
		}

		public Builder setOutsideEnable(boolean enable) {
			outsideAble = enable;
			return this;
		}

		public GuideView show(Context context) {
			GuideView guideView = new GuideView(context, this);
			guideView.show();
			return guideView;
		}

		public GuideView build(Context context) {
			return new GuideView(context, this);
		}
	}
}
