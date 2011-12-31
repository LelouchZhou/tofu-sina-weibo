package cn.com.alfred.weibo.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import cn.com.alfred.weibo.R;
import cn.com.alfred.weibo.widget.AsyncImageView;

class MyImageView extends AsyncImageView {

	public MyImageView(Context context) {
		super(context);
	}

	public MyImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.tofuxmlns);

		gifNeeded = a.getBoolean(R.styleable.tofuxmlns_gif, false);
	}

	// 用来设置ImageView的位置
	private void setLocation(int x, int y) {
		int left = this.getLeft() + x;
		int right = this.getRight() + x;
		int top = this.getTop() + y;
		int bottom = this.getBottom() + y;

		// 图片的各顶点均不能超过屏幕的中心
		float bm_left = left + (MyView.screenWidth - MyView.bitmap_width) / 2
				* (right - left) / MyView.screenWidth;
		float bm_right = right - (MyView.screenWidth - MyView.bitmap_width) / 2
				* (right - left) / MyView.screenWidth;
		float bm_top = top + (MyView.screenHeight - MyView.bitmap_height) / 2
				* (bottom - top) / MyView.screenHeight;
		float bm_bottom = bottom - (MyView.screenHeight - MyView.bitmap_height)
				/ 2 * (bottom - top) / MyView.screenHeight;
		if (bm_left > MyView.screenWidth / 2
				|| bm_right < MyView.screenWidth / 2) {
			left = this.getLeft();
			right = this.getRight();
		}
		if (bm_top > MyView.screenHeight / 2
				|| bm_bottom < MyView.screenHeight / 2) {
			top = this.getTop();
			bottom = this.getBottom();
		}
		this.setFrame(left, top, right, bottom);
	}

	/*
	 * 用来放大缩小ImageView
	 * 
	 * 因为图片是填充ImageView的，所以也就有放大缩小图片的效果
	 */
	protected boolean setScale(float temp) {
		int left = this.getLeft() - (int) (temp);
		int right = this.getRight() + (int) (temp);
		int top = this.getTop() - (int) (temp);
		int bottom = this.getBottom() + (int) (temp);

		// 当缩放后的大小比屏幕小，则自动缩放成适应屏幕大小的尺寸
		if (right - left < MyView.bitmap_width / 5
				|| bottom - top < MyView.bitmap_height / 5) {
			this.setFrame(MyView.init_left, MyView.init_top, MyView.init_right,
					MyView.init_bottom);
			return false;
		}

//		// 最大不能超过原始图像像素的25倍
//		if (right - left > bitmap.getWidth() * 5
//				|| bottom - top > bitmap.getHeight() * 5) {
//			return false;
//		}

		this.setFrame(left, top, right, bottom);
		return true;
	}

	/*
	 * 让图片跟随手指触屏的位置移动 beforeX、Y是用来保存前一位置的坐标 afterX、Y是用来保存当前位置的坐标
	 * 它们的差值就是ImageView各坐标的增加或减少值
	 */
	public void moveWithFinger(MotionEvent event) {

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				MyView.beforeX = event.getX();
				MyView.beforeY = event.getY();
				break;

			case MotionEvent.ACTION_MOVE:
				MyView.afterX = event.getX();
				MyView.afterY = event.getY();

				this.setLocation((int) (MyView.afterX - MyView.beforeX),
						(int) (MyView.afterY - MyView.beforeY));

				MyView.beforeX = MyView.afterX;
				MyView.beforeY = MyView.afterY;
				break;

			case MotionEvent.ACTION_UP:
				break;
		}
	}

	/*
	 * 通过多点触屏放大或缩小图像 beforeLenght用来保存前一时间两点之间的距离 afterLenght用来保存当前时间两点之间的距离
	 */
	public void scaleWithFinger(MotionEvent event) {
		float moveX = event.getX(1) - event.getX(0);
		float moveY = event.getY(1) - event.getY(0);

		switch (event.getAction()) {
			case MotionEvent.ACTION_POINTER_DOWN:
				MyView.beforeLenght = (float) Math.sqrt((moveX * moveX)
						+ (moveY * moveY));
				break;
			case MotionEvent.ACTION_MOVE:
				// 得到两个点之间的长度
				MyView.afterLenght = (float) Math.sqrt((moveX * moveX)
						+ (moveY * moveY));
				if (!MyView.point_first) {
					float scaled = MyView.afterLenght - MyView.beforeLenght;

					if (scaled == 0)
						break;

					// if (scaled > 0) {
					this.setScale(scaled);
					// } else {
					// this.setScale(scaled / 2, 1);
					// }
				}
				MyView.point_first = false;
				MyView.beforeLenght = MyView.afterLenght;
				break;
		}
	}

}