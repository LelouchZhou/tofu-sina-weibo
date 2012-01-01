package cn.com.alfred.weibo.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import cn.com.alfred.weibo.R;

public class ImageRel {

	private static Bitmap[] bitmaps_avatar;
	private static Bitmap[] bitmaps_loading;

	public static byte[] readStream(InputStream inStream) throws Exception {
		byte[] buffer = new byte[1024];
		int len = -1;
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		while ((len = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
		}
		byte[] data = outStream.toByteArray();

		outStream.close();
		inStream.close();
		return data;
	}

	public static boolean saveMyBitmap(String filePath, Bitmap bitmap) {
		File f = new File(filePath);
		try {
			f.createNewFile();
			FileOutputStream fOut = null;
			fOut = new FileOutputStream(f);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
			fOut.flush();
			fOut.close();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * @return the bitmaps_avatar
	 */
	public static Bitmap[] getBitmaps_avatar(Context context) {
		if (bitmaps_avatar == null) {
			bitmaps_avatar = new Bitmap[1];
			bitmaps_avatar[0] = BitmapFactory.decodeResource(
					context.getResources(), R.drawable.avatar);
		}
		return bitmaps_avatar;
	}

	/**
	 * @return the bitmaps_loading
	 */
	public static Bitmap[] getBitmaps_loading(Context context) {
		if (bitmaps_loading == null) {
			bitmaps_loading = new Bitmap[1];
			bitmaps_loading[0] = BitmapFactory.decodeResource(
					context.getResources(), R.drawable.loading);
		}
		return bitmaps_loading;
	}

	/**
	 * @param bitmaps_avatar
	 *            the bitmaps_avatar to set
	 */
	public static void setBitmaps_avatar(Bitmap[] bitmaps_avatar) {
		ImageRel.bitmaps_avatar = bitmaps_avatar;
	}

	/**
	 * @param bitmaps_loading
	 *            the bitmaps_loading to set
	 */
	public static void setBitmaps_loading(Bitmap[] bitmaps_loading) {
		ImageRel.bitmaps_loading = bitmaps_loading;
	}

	/**
	 * 将文字写在图片上
	 * 
	 * @param text
	 * @return
	 */
	public static Bitmap createBitmapFromText(String text) {
		String output = toSBC(text);

		Bitmap bitmap;
		int height;
		int width;
		if (output.length() > 19) {
			width = 400;
			height = 23 * (text.length() / 19 + (text.length() % 19 != 0 ? 1 : 0)) + 6;
		} else {
			width = 20 + output.length() * 20;
			height = 30;
		}
		bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		Paint p = new Paint();
		p.setColor(Color.BLACK);
		p.setTextSize(20);

		for (int index = 0; index < output.length(); index += 19) {
			canvas.drawText(output.substring(index, (index + 19) < output
					.length() ? (index + 19) : output.length()), 10,
					23 * (index / 19 + 1), p);
		}

		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();

		return bitmap;
	}

	/**
	 * 半角变全角
	 * 
	 * @param input
	 * @return
	 */
	public static String toSBC(String input) {
		char[] c = input.toCharArray();
		for (int i = 0; i < c.length; i++) {
			if (c[i] == 32) {
				c[i] = (char) 12288;
				continue;
			}
			if (c[i] < 127 && c[i] > 32)
				c[i] = (char) (c[i] + 65248);
		}
		return new String(c);
	}
}