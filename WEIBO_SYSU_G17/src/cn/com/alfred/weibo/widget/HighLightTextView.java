package cn.com.alfred.weibo.widget;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.widget.TextView;
import cn.com.alfred.weibo.util.InfoHelper;

public class HighLightTextView extends TextView {

	// 匹配@+人名
	public static final Pattern NAME_Pattern = Pattern.compile(
			"@[\\u4e00-\\u9fa5\\w\\-\\—]{2,30}", Pattern.CASE_INSENSITIVE);

	// 匹配话题#...#
	public static final Pattern TOPIC_PATTERN = Pattern
			.compile("#([^\\#|^\\@|.]+)#");

	// 匹配网址
	public final static Pattern URL_PATTERN = Pattern
			.compile("http://([\\w-]+\\.)+[\\w-]+(/[\\w-\\./?%&=]*)?");

	// 匹配表情[...]
	public static Pattern EMOTION_PATTERN = Pattern.compile(
			"\\[([\\u4E00-\\u9FA5\\uF900-\\uFA2D\\w]+)\\]",
			Pattern.CASE_INSENSITIVE);

	public HighLightTextView(Context context) {
		this(context, null);
	}

	public HighLightTextView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.textViewStyle);
	}

	public HighLightTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setText(String text) {
		setHighLightText(this, text);
//		SpannableStringBuilder style = new SpannableStringBuilder(text);
//
//		Matcher nameMatcher = NAME_Pattern.matcher(text);
//		while (nameMatcher.find()) {
//			style.setSpan(new ForegroundColorSpan(Color.BLUE),
//					nameMatcher.start(), nameMatcher.end(),
//					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		}
//
//		Matcher topicMatcher = TOPIC_PATTERN.matcher(text);
//		while (topicMatcher.find()) {
//			style.setSpan(new ForegroundColorSpan(Color.BLUE),
//					topicMatcher.start(), topicMatcher.end(),
//					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		}
//
//		Matcher urlMatcher = URL_PATTERN.matcher(text);
//		while (urlMatcher.find()) {
//			style.setSpan(new URLSpan(urlMatcher.group()), urlMatcher.start(),
//					urlMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		}
//
//		Matcher emotionMatcher = EMOTION_PATTERN.matcher(text);
//		while (emotionMatcher.find()) {
//			try {
//
//				Bitmap bitmap = BitmapFactory.decodeFile(InfoHelper
//						.getEmotionPath() + emotionMatcher.group());
//				if (bitmap == null)
//					throw new NullPointerException();
//
//				Drawable drawable = new BitmapDrawable(bitmap);
//
//				drawable.setBounds(0, 0, 30, 30);
//				ImageSpan span = new ImageSpan(drawable,
//						ImageSpan.ALIGN_BASELINE);
//				style.setSpan(span, emotionMatcher.start(),
//						emotionMatcher.end(),
//						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//			} catch (Exception e) {
//				e.printStackTrace();
//				style.setSpan(new ForegroundColorSpan(Color.BLUE),
//						emotionMatcher.start(), emotionMatcher.end(),
//						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//			}
//		}
//		super.setText(style);
	}
	
	/**
	 * 实现人名/话题/连接等高亮
	 * 
	 * @param text
	 */
	public static void setHighLightText(TextView textView, String text) {
		SpannableStringBuilder style = new SpannableStringBuilder(text);

		Matcher nameMatcher = NAME_Pattern.matcher(text);
		while (nameMatcher.find()) {
			style.setSpan(new ForegroundColorSpan(Color.BLUE),
					nameMatcher.start(), nameMatcher.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		Matcher topicMatcher = TOPIC_PATTERN.matcher(text);
		while (topicMatcher.find()) {
			style.setSpan(new ForegroundColorSpan(Color.BLUE),
					topicMatcher.start(), topicMatcher.end(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		Matcher urlMatcher = URL_PATTERN.matcher(text);
		while (urlMatcher.find()) {
			style.setSpan(new URLSpan(urlMatcher.group()), urlMatcher.start(),
					urlMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		Matcher emotionMatcher = EMOTION_PATTERN.matcher(text);
		while (emotionMatcher.find()) {
			try {

				Bitmap bitmap = BitmapFactory.decodeFile(InfoHelper
						.getEmotionPath() + emotionMatcher.group());
				if (bitmap == null)
					throw new NullPointerException();

				Drawable drawable = new BitmapDrawable(bitmap);

				drawable.setBounds(0, 0, 30, 30);
				ImageSpan span = new ImageSpan(drawable,
						ImageSpan.ALIGN_BASELINE);
				style.setSpan(span, emotionMatcher.start(),
						emotionMatcher.end(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} catch (Exception e) {
				e.printStackTrace();
				style.setSpan(new ForegroundColorSpan(Color.BLUE),
						emotionMatcher.start(), emotionMatcher.end(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		textView.setText(style);
	}
}
