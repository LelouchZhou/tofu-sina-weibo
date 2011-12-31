package cn.com.alfred.weibo.widget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.ImageView;
import cn.com.alfred.weibo.R;
import cn.com.alfred.weibo.http.ImageItem;

public class AsyncImageView extends ImageView {

	// 有必要的话改成把图片存放在sdcard
	private static HashMap<String, SoftReference<byte[]>> imageCache = new HashMap<String, SoftReference<byte[]>>();
	private Bitmap[] bitmaps = null;
	private byte[] data;
	public static final int DOWNLOAD_FAILED = 1;
	public static final int DOWNLOAD_COMPLETED = 2;
	public static final int DOWNLOAD_GIF = 3;

	protected Bitmap bitmap = null;
	private Handler handler = null;
	protected boolean gifNeeded = false;
	private boolean isGif = false;

	public AsyncImageView(Context context) {
		super(context);
		initImageView();
		gifNeeded = false;
	}

	public AsyncImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initImageView();
	}

	/**
	 * the function to set the image from the url
	 * 
	 * @return return true if the url is vaild
	 */
	public boolean setUrl(String url) {
		try {
			if (URLUtil.isHttpUrl(url)) {// 如果为网络地址。则连接url下载图片
				if (imageCache.containsKey(url)) {
					SoftReference<byte[]> cache = imageCache.get(url);
					byte[] data = cache.get();

					if (data != null) {
						return this.setImageBitmap(data);
					}
				}

				new AsyncViewTask().execute(url);

			} else {// 如果为本地数据，直接解析
				byte[] data = getBytes(new FileInputStream(new File(url)));
				return this.setImageBitmap(data);
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (handler != null) {
				handler.sendEmptyMessage(DOWNLOAD_FAILED);
			}
			return false;
		}
		return true;
	}

	private boolean setImageBitmap(byte[] bitmap_data) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = 1;
		while (true) {
			try {
				bitmap = BitmapFactory.decodeByteArray(bitmap_data, 0,
						bitmap_data.length, opts);
				AsyncImageView.this.setImageBitmap(bitmap);
				break;
			} catch (OutOfMemoryError oom) {
				oom.printStackTrace();
				opts.inSampleSize++;
				continue;
			} catch (Exception e) {
				e.printStackTrace();
				bitmap = null;
				if (handler != null) {
					handler.sendEmptyMessage(DOWNLOAD_FAILED);
				}
				return false;
			}
		}
		if (handler != null) {
			handler.sendEmptyMessage(DOWNLOAD_COMPLETED);
		}
		data = bitmap_data;
		return true;
	}

	/**
	 * this function is to set the bitmaps which are showed during the process
	 * of downloading image
	 * 
	 * @param bitmaps
	 *            the bitmaps to set
	 */
	public void setProgressBitmaps(Bitmap[] bitmaps) {
		this.bitmaps = bitmaps;
		this.setImageBitmap(bitmaps[0]);
	}

	private void initImageView() {
		isGif = false;
		if (bitmaps == null) {
			bitmaps = new Bitmap[1];
			bitmaps[0] = BitmapFactory.decodeResource(this.getResources(),
					R.drawable.loading);
		}
		this.setImageBitmap(bitmaps[0]);
	}

	class AsyncViewTask extends AsyncTask<String, Integer, byte[]> {

		@Override
		protected byte[] doInBackground(String... strings) {
			byte[] data;
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(strings[0]);
				HttpResponse response = client.execute(get);
				HttpEntity entity = response.getEntity();
				long length = entity.getContentLength();

				if (length <= 0)
					return null;

				InputStream is = entity.getContent();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				int ch = -1;
				int count = 0;
				publishProgress(0);
				boolean isFirst = true;
				while ((ch = is.read(buf)) != -1) {
					baos.write(buf, 0, ch);
					count += ch;
					publishProgress((int) ((count / (float) length) * (bitmaps.length - 1)));
					if (isFirst && gifNeeded && ch > 8) {
						if (ImageItem.isGIF(buf)) {
							handler.sendEmptyMessage(DOWNLOAD_GIF);
							isGif = true;
							return null;
						}
					}
					isFirst = false;
				}
				is.close();
				data = baos.toByteArray();
				return data;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			// return data;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			AsyncImageView.this.setImageBitmap(bitmaps[progress[0]]);
		}

		@Override
		protected void onPostExecute(byte[] result) {
			if (result != null) {
				if (AsyncImageView.this.setImageBitmap(result))
					imageCache.put("url", new SoftReference<byte[]>(result));
			} else {
				if (!isGif && handler != null) {
					handler.sendEmptyMessage(DOWNLOAD_FAILED);
				}
			}

		}
	}

	/**
	 * @param handler
	 *            the handler to set
	 */
	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	/**
	 * @return the bitmap
	 */
	public Bitmap getBitmap() {
		return bitmap;
	}

	/**
	 * @param bitmap
	 *            the bitmap to set
	 */
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
		this.setImageBitmap(bitmap);
	}

	/**
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}

	public static byte[] getBytes(InputStream is) throws Exception {
		byte[] data = null;

		Collection<byte[]> chunks = new ArrayList<byte[]>();
		byte[] buffer = new byte[1024 * 1000];
		int read = -1;
		int size = 0;

		while ((read = is.read(buffer)) != -1) {
			if (read > 0) {
				byte[] chunk = new byte[read];
				System.arraycopy(buffer, 0, chunk, 0, read);
				chunks.add(chunk);
				size += chunk.length;
			}
		}

		if (size > 0) {
			ByteArrayOutputStream bos = null;
			try {
				bos = new ByteArrayOutputStream(size);
				for (Iterator<byte[]> itr = chunks.iterator(); itr.hasNext();) {
					byte[] chunk = (byte[]) itr.next();
					bos.write(chunk);
				}
				data = bos.toByteArray();
			} finally {
				if (bos != null) {
					bos.close();
				}
			}
		}
		return data;
	}
}
