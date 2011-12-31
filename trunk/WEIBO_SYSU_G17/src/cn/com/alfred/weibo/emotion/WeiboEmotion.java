package cn.com.alfred.weibo.emotion;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Emotion;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.util.InfoHelper;

public class WeiboEmotion {

	public static final int EMOTIONS_DOWNLOAD_COMPLETED = 10;
	public static final int EMOTIONS_DOWNLOAD_FAILED = 11;

	public static HashMap<String, String> emotions = new HashMap<String, String>();

	public static HashMap<String, String> loadEmotions() {
		emotions.clear();
		File file = new File(InfoHelper.getEmotionPath());
		File[] files = file.listFiles();
		try {
			for (File tmp : files) {
				emotions.put(tmp.getName(), tmp.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return emotions;
	}

	public static HashMap<String, String> downloadEmotions(Handler handler) {

		emotions = loadEmotions();

		Weibo weibo = OAuthConstant.getInstance().getWeibo();
		try {
			List<Emotion> list = weibo.getEmotions();
			for (Emotion tmp : list) {
				if (emotions.containsKey(tmp.getPhrase()))
					continue;

				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(tmp.getUrl());
				HttpResponse response = client.execute(get);
				HttpEntity entity = response.getEntity();
				long length = entity.getContentLength();
				if (length == 0)
					continue;

				InputStream is = entity.getContent();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				int ch = -1;
				while ((ch = is.read(buf)) != -1) {
					baos.write(buf, 0, ch);
				}
				is.close();

				File file = new File(InfoHelper.getEmotionPath()
						+ tmp.getPhrase());
				baos.writeTo(new FileOutputStream(file));
				emotions.put(file.getName(), file.getAbsolutePath());

			}
		} catch (Exception e) {
			e.printStackTrace();
			handler.sendEmptyMessage(InfoHelper.LOADING_DATA_FAILED);
			emotions.clear();
			return emotions;
		}
		handler.sendEmptyMessage(WeiboEmotion.EMOTIONS_DOWNLOAD_COMPLETED);
		return emotions;
	}

	public static HashMap<String, String> updateEmotions(Handler handler) {

		File file = new File(InfoHelper.getEmotionPath());
		File[] files = file.listFiles();
		try {
			for (File tmp : files) {
				tmp.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
			handler.sendEmptyMessage(InfoHelper.LOADING_DATA_FAILED);
			emotions.clear();
			return emotions;
		}
		handler.sendEmptyMessage(WeiboEmotion.EMOTIONS_DOWNLOAD_COMPLETED);
		return downloadEmotions(handler);
	}
}
