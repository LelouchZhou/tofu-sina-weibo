package cn.com.alfred.weibo.util;

import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ZoomControls;
import cn.com.alfred.weibo.R;
import cn.com.alfred.weibo.gif.GIFView;
import cn.com.alfred.weibo.widget.AsyncImageView;

/*
 * 利用多点触控来控制ImageView中图像的放大与缩小 手指控制图片移动
 */
public class MyView extends Activity implements OnMenuItemClickListener {

	private Button btn_cancel, btn_save;
	private ZoomControls zc;
	private MyImageView imageView;
	// public static Bitmap bitmap;

	// 初始时图片的上下左右位置
	public static int init_left, init_right, init_top, init_bottom;
	private boolean init_flag;

	// 适应屏幕的图片的大小
	public static float bitmap_width, bitmap_height;

	// 图片相对于屏幕的大小比例
	private float scale_x, scale_y;

	// 两点触屏后之间的长度
	public static float beforeLenght;
	public static float afterLenght;

	// 单点移动的前后坐标值
	public static float afterX, afterY;
	public static float beforeX, beforeY;

	// 缩放的标记，不知道为什么多点触控这里只是响应move跟up，所以加一个标记位
	public static boolean point_first;

	public static int screenWidth;
	public static int screenHeight;

	public static Bitmap myBitmap;

	private String saveFileName;

	// 触控标记，单点(1)、多点(2)还是第一次触碰屏幕(3)
	private int point_flag;

	private String url;

	private GIFView gifView;
	private LinearLayout myview_waitingView;

	private static final int GIF_DOWNLOAD_SUCCESS = 5;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case GIF_DOWNLOAD_SUCCESS:
					myview_waitingView.setVisibility(View.GONE);
					MyView.this.setContentView(gifView);
					Toast.makeText(MyView.this, "下载图片完成", Toast.LENGTH_LONG)
							.show();
					break;
				case AsyncImageView.DOWNLOAD_GIF:
					gifinit();
					break;

				case AsyncImageView.DOWNLOAD_COMPLETED:
					myview_waitingView.setVisibility(View.GONE);
					Toast.makeText(MyView.this, "下载图片完成", Toast.LENGTH_LONG)
							.show();
					init();
					break;

				default:
					Toast.makeText(MyView.this, "获取图片失败", Toast.LENGTH_LONG)
							.show();
					MyView.this.finish();
			}
		}
	};;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.myview);

		WindowManager manager = getWindowManager();
		screenWidth = manager.getDefaultDisplay().getWidth();
		screenHeight = manager.getDefaultDisplay().getHeight();

		Bundle bundle = getIntent().getExtras();
		if (bundle == null || !bundle.containsKey("url")) {
			Toast.makeText(this, "获取图片失败", Toast.LENGTH_LONG).show();
			finish();
		}

		url = bundle.getString("url");

		imageView = (MyImageView) findViewById(R.id.imageView);
		imageView.setHandler(handler);
		imageView.setUrl(url);
		btn_cancel = (Button) findViewById(R.id.btn_return);
		btn_save = (Button) findViewById(R.id.btn_save);
		zc = (ZoomControls) findViewById(R.id.zoomControls);
		myview_waitingView = (LinearLayout) findViewById(R.id.myview_waitingView);
		zc.setVisibility(View.GONE);
		btn_cancel.setVisibility(View.GONE);
		btn_save.setVisibility(View.GONE);
	}

	protected void gifinit() {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			HttpResponse response = client.execute(get);
			HttpEntity entity = response.getEntity();
			long length = entity.getContentLength();

			if (length <= 0)
				throw new Exception();

			InputStream is = entity.getContent();
			gifView = new GIFView(this, is);
			handler.sendEmptyMessage(GIF_DOWNLOAD_SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			handler.sendEmptyMessage(10000);
		}

	}

	private void init() {
		zc.setVisibility(View.VISIBLE);
		btn_cancel.setVisibility(View.VISIBLE);
		btn_save.setVisibility(View.VISIBLE);

		btn_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				MyView.this.finish();
			}
		});
		btn_save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				savaBitmap();
			}
		});

		zc.setIsZoomOutEnabled(false);
		zc.setOnZoomInClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (init_flag) {
					getSize();
				}
				imageView.setScale((imageView.getRight() - imageView.getLeft()) / 8);
				enableZC();
			}
		});
		zc.setOnZoomOutClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (init_flag) {
					getSize();
				}
				imageView.setScale((imageView.getLeft() - imageView.getRight()) / 8);
				enableZC();
			}
		});

		// 设置图片填充ImageView
		imageView.setScaleType(ScaleType.FIT_CENTER);
		point_first = true;
		point_flag = 3;

		scale_x = imageView.getBitmap().getWidth() / screenWidth;
		scale_y = imageView.getBitmap().getHeight() / screenHeight;
		float scale_ratio = Math.max(scale_x, scale_y);

		bitmap_width = imageView.getBitmap().getWidth() / scale_ratio;
		bitmap_height = imageView.getBitmap().getHeight() / scale_ratio;

		scale_x = bitmap_width / screenWidth;
		scale_y = bitmap_height / screenHeight;

		init_flag = true;
	}

	private void getSize() {
		init_left = imageView.getLeft();
		init_right = imageView.getRight();
		init_top = imageView.getTop();
		init_bottom = imageView.getBottom();
		init_flag = false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (init_flag) {
			getSize();
		}

		if (MotionEvent.ACTION_UP == event.getAction()) {
			point_first = true;
			point_flag = 3;
			return true;
		}
		if (point_flag == 3) {

			if (event.getPointerCount() == 2) {
				imageView.scaleWithFinger(event);
				point_flag = 2;
			} else if (event.getPointerCount() == 1) {
				imageView.moveWithFinger(event);
				point_flag = 1;
			}

		} else if (point_flag == 2) {
			if (MotionEvent.ACTION_POINTER_UP == event.getAction()) {
				point_first = true;
				return true;
			}
			if (event.getPointerCount() == 2) {
				imageView.scaleWithFinger(event);
			}

		} else {
			point_first = true;
			if (event.getPointerCount() == 2) {
				imageView.scaleWithFinger(event);
				point_flag = 2;
			} else {
				imageView.moveWithFinger(event);
			}
		}
		enableZC();
		imageView.invalidate();
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, 1, 1, "保存照片").setOnMenuItemClickListener(this);
		return true;
	}

	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case 1:
				savaBitmap();
				break;
			default:
				return false;
		}
		return true;
	}

	private void savaBitmap() {
		/*
		 * ps:由于考虑到要标十字架。。用第一种方法还要考虑缩放 所以直接用第二种
		 */

		/**
		 * 可以把整个imageView截下来，但是像素必须低于系统所能提供的最大DrawingCache值
		 * 可通过imageView.layout(0, 0, screenWidth, screenHeight)实现
		 */
		imageView.layout(0, 0, (int) screenWidth, (int) screenHeight);
		if (false == imageView.isDrawingCacheEnabled()) {
			imageView.setDrawingCacheEnabled(true);
		}
		myBitmap = imageView.getDrawingCache();

		/**
		 * 另一种，直接获取imageView所在的view，然后直接截图。
		 */
		// View v = imageView.getRootView();
		// if (false == v.isDrawingCacheEnabled()) {
		// v.setDrawingCacheEnabled(true);
		// }
		// myBitmap = v.getDrawingCache();
		//

		if (myBitmap == null) {
			Toast.makeText(MyView.this, "保存失败", Toast.LENGTH_LONG).show();
		} else {
			saveFileName = InfoHelper.getWeiboPath() + InfoHelper.getFileName()
					+ ".jpg";
			if (ImageRel.saveMyBitmap(saveFileName, myBitmap))
				Toast.makeText(MyView.this, "保存成功，保存位置是" + saveFileName,
						Toast.LENGTH_LONG).show();
			else
				Toast.makeText(MyView.this, "保存失败", Toast.LENGTH_LONG).show();
		}
	}

	private void enableZC() {
		int temp = (imageView.getRight() - imageView.getLeft()) / 5;
		int left = imageView.getLeft() - temp;
		int right = imageView.getRight() + temp;
		int top = imageView.getTop() - temp;
		int bottom = imageView.getBottom() + temp;
		zc.setIsZoomInEnabled(true);
		zc.setIsZoomOutEnabled(true);
		// if (right - left > imageView.getBitmap().getWidth() * 5
		// || bottom - top > imageView.getBitmap().getHeight() * 5)
		// zc.setIsZoomInEnabled(false);

		temp = 0;
		left = imageView.getLeft() - temp;
		right = imageView.getRight() + temp;
		top = imageView.getTop() - temp;
		bottom = imageView.getBottom() + temp;
		if (right - left < MyView.bitmap_width / 5
				|| bottom - top < MyView.bitmap_height / 5)
			zc.setIsZoomOutEnabled(false);
	}

}