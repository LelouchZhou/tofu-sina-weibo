package cn.com.alfred.weibo.gif;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;
import cn.com.alfred.weibo.R;
import cn.com.alfred.weibo.util.MyView;

public class GIFView extends View implements Runnable {

	private Bitmap bmb;
	private GIFDecode decode;
	private int ind;
	private int gifCount;

	private int x, y;

	public GIFView(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
	}

	public GIFView(android.content.Context context) {
		super(context);
		decode = new GIFDecode();
		decode.read(this.getResources().openRawResource(R.drawable.ic_launcher));
		ind = 0;
		// decode.
		gifCount = decode.getFrameCount();
		bmb = decode.getFrame(0);
		Thread t = new Thread(this);
		t.start();
	}

	public GIFView(android.content.Context context, String filepath) {
		super(context);
		decode = new GIFDecode();
		if (filepath != null) {
			try {
				decode.read(new FileInputStream(filepath));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Toast.makeText(context, "获取图片失败", Toast.LENGTH_LONG).show();
				((Activity) context).finish();
			}
		}
		ind = 0;
		gifCount = decode.getFrameCount();
		bmb = decode.getFrame(0);
		Thread t = new Thread(this);
		t.start();
	}

	public GIFView(android.content.Context context, InputStream is) {
		super(context);
		decode = new GIFDecode();
		if (is != null) {
			decode.read(is);
		} else {
			Toast.makeText(context, "获取图片失败", Toast.LENGTH_LONG).show();
			((Activity) context).finish();
		}
		ind = 0;
		// decode.
		gifCount = decode.getFrameCount();
		bmb = decode.getFrame(0);
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	protected void onDraw(Canvas canvas) {

		setAdaptedBitmap();

		canvas.drawBitmap(bmb, x, y, new Paint());
		bmb = decode.next();
	}

	private void setAdaptedBitmap() {
		float scale_x = MyView.screenWidth / (float) bmb.getWidth();
		float scale_y = MyView.screenHeight / (float) bmb.getHeight();
		float scale = Math.min(scale_x, scale_y);
		Matrix m = new Matrix();
		m.setScale(scale, scale);
		try {
			bmb = Bitmap.createBitmap(bmb, 0, 0, bmb.getWidth(),
					bmb.getHeight(), m, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		x = (MyView.screenWidth - bmb.getWidth()) >> 1;
		y = (MyView.screenHeight - bmb.getHeight()) >> 1;
	}

	public void run() {
		while (true) {
			try {
				this.postInvalidate();
				Thread.sleep(100);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	public void setInputStream(InputStream in) {
		decode = new GIFDecode();
		decode.read(in);
		ind = 0;
		// decode.
		gifCount = decode.getFrameCount();
		bmb = decode.getFrame(0);
		Thread t = new Thread(this);
		t.start();
	}
}
