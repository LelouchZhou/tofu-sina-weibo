package cn.com.alfred.weibo.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cn.com.alfred.weibo.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

	public static boolean saveMyBitmap(String bitName, Bitmap mBitmap) {
		File f = new File(bitName);
		try {
			f.createNewFile();
			FileOutputStream fOut = null;
			fOut = new FileOutputStream(f);
			mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
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

}