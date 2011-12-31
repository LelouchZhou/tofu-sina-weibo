package cn.com.alfred.weibo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base64;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.http.AccessToken;
import cn.com.alfred.weibo.util.InfoHelper;

/**
 * 发表微博
 * 
 * @author alfredtofu
 * 
 */
public class ShareActivity extends Activity {

	private static final int REQUEST_CODE_GETIMAGE_BYSDCARD = 0;
	private static final int REQUEST_CODE_GETIMAGE_BYCAMERA = 1;
	private static final int UPDATE_SUCCESS = 0;
	private static final int UPDATE_FAILED = 1;

	private Button button = null;
	private ImageButton imgChooseBtn = null;
	private ImageView imageView = null;
	private TextView wordCounterTextView = null;
	private EditText contentEditText = null;
	private ProgressDialog dialog = null;
	private String uploadImage = null;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case UPDATE_SUCCESS:
					Toast.makeText(ShareActivity.this, "发微博成功",
							Toast.LENGTH_LONG).show();
					dialog.dismiss();
					ShareActivity.this.finish();
					break;

				case UPDATE_FAILED:
					Toast.makeText(ShareActivity.this, "发微博失败",
							Toast.LENGTH_LONG).show();
					dialog.dismiss();
					break;

			}
		}
	};
	private Runnable updateWeibo = new Runnable() {

		public void run() {
			try {
				Weibo weibo = OAuthConstant.getInstance().getWeibo();
				if (weibo == null) {
					SharedPreferences sp = getSharedPreferences("tofuweibo",
							Context.MODE_PRIVATE);
					String s = sp.getString("accessToken", null);
					if (s != null) {
						byte[] bytes = Base64.decodeBase64(s.getBytes());
						ByteArrayInputStream bais = new ByteArrayInputStream(
								bytes);
						ObjectInputStream ois = new ObjectInputStream(bais);
						AccessToken accessToken = (AccessToken) ois
								.readObject();
						if (accessToken != null)
							OAuthConstant.getInstance().setAccessToken(
									accessToken);
					}
				}
				String msg = contentEditText.getText().toString();
				if (msg.getBytes().length != msg.length()) {
					msg = URLEncoder.encode(msg, "UTF-8");
				}

				if (TextUtils.isEmpty(uploadImage)) {
					weibo.updateStatus(msg);
				} else {
					File file = new File(uploadImage);
					weibo.uploadStatus(msg, file);
				}
				handler.sendEmptyMessage(UPDATE_SUCCESS);
			} catch (Exception e) {
				e.printStackTrace();
				handler.sendEmptyMessage(UPDATE_FAILED);
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.sharemain);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.shareheader);

		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);

		button = (Button) findViewById(R.id.Button01);
		imgChooseBtn = (ImageButton) findViewById(R.id.share_imagechoose);
		imageView = (ImageView) findViewById(R.id.share_image);
		wordCounterTextView = (TextView) findViewById(R.id.share_word_counter);
		contentEditText = (EditText) findViewById(R.id.share_content);

		Intent it = getIntent();
		if (it != null && it.getAction() != null
				&& it.getAction().equals(Intent.ACTION_SEND)) {
			Bundle extras = it.getExtras();
			if (extras.containsKey("android.intent.extra.STREAM")) {
				Uri thisUri = (Uri) extras.get("android.intent.extra.STREAM");
				String thePath = InfoHelper
						.getAbsolutePathFromNoStandardUri(thisUri);

				if (TextUtils.isEmpty(thePath)) {
					uploadImage = getAbsoluteImagePath(thisUri);
				} else {
					uploadImage = thePath;
				}
				contentEditText.setText("分享图片");
				wordCounterTextView.setText(String.valueOf(140 - "分享图片"
						.length()));
			}
		}

		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (InfoHelper.checkNetWork(ShareActivity.this) && isChecked()) {
					dialog.show();
					new Thread(updateWeibo).start();
				}
			}
		});

		imgChooseBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				CharSequence[] items = { "手机相册", "手机拍照", "清除照片" };
				imageChooseItem(items);
			}
		});

		contentEditText.addTextChangedListener(new TextWatcher() {

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				textCountSet();
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				textCountSet();
			}

			@Override
			public void afterTextChanged(Editable s) {
				textCountSet();
			}
		});

		if (!TextUtils.isEmpty(uploadImage)) {

			String imageName = uploadImage.substring(uploadImage
					.lastIndexOf(File.separator) + 1);

			Bitmap bitmap = loadImgThumbnail(imageName,
					MediaStore.Images.Thumbnails.MICRO_KIND);
			if (bitmap != null) {
				imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
				imageView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent intent = new Intent();
						intent.setAction(android.content.Intent.ACTION_VIEW);
						intent.setDataAndType(
								Uri.fromFile(new File(uploadImage)), "image/*");
						startActivity(intent);
					}
				});
			}
		}

		dialog = new ProgressDialog(ShareActivity.this);
		dialog.setMessage("分享中...");
		dialog.setIndeterminate(false);
		dialog.setCancelable(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_GETIMAGE_BYSDCARD) {
			if (resultCode != RESULT_OK) {
				return;
			}

			if (data == null)
				return;

			Uri thisUri = data.getData();
			String thePath = InfoHelper
					.getAbsolutePathFromNoStandardUri(thisUri);

			// 如果是标准Uri
			if (TextUtils.isEmpty(thePath)) {
				uploadImage = getAbsoluteImagePath(thisUri);
			} else {
				uploadImage = thePath;
			}

			String imageName = uploadImage.substring(uploadImage
					.lastIndexOf(File.separator) + 1);

			Bitmap bitmap = loadImgThumbnail(imageName,
					MediaStore.Images.Thumbnails.MICRO_KIND);
			if (bitmap != null) {
				imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
			}
		}
		// 拍摄图片
		else if (requestCode == REQUEST_CODE_GETIMAGE_BYCAMERA) {
			if (resultCode != RESULT_OK) {
				return;
			}

			super.onActivityResult(requestCode, resultCode, data);

			Bitmap bitmap = InfoHelper.getScaleBitmap(ShareActivity.this,
					uploadImage);

			if (bitmap != null) {
				imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
			}
		}

		imageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.fromFile(new File(uploadImage)),
						"image/*");
				startActivity(intent);
			}
		});
	}

	/**
	 * 获取图片缩略图 只有Android2.1以上版本支持
	 * 
	 * @param imageName
	 * @param kind
	 *            MediaStore.Images.Thumbnails.MICRO_KIND
	 * @return
	 */
	protected Bitmap loadImgThumbnail(String imageName, int kind) {
		Bitmap bitmap = null;

		String[] proj = { MediaStore.Images.Media._ID,
				MediaStore.Images.Media.DISPLAY_NAME };

		Cursor cursor = managedQuery(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
				MediaStore.Images.Media.DISPLAY_NAME + "='" + imageName + "'",
				null, null);

		if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
			ContentResolver crThumb = getContentResolver();
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 1;
			bitmap = MediaStore.Images.Thumbnails.getThumbnail(crThumb,
					cursor.getInt(0), kind, options);
		}
		return bitmap;
	}

	/**
	 * 通过uri获取文件的绝对路径
	 * 
	 * @param uri
	 * @return
	 */
	protected String getAbsoluteImagePath(Uri uri) {
		String imagePath = "";
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, proj, null, null, null);

		if (cursor != null) {
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				imagePath = cursor.getString(column_index);
			}
		}

		return imagePath;
	}

	/**
	 * 检查微博是否合法
	 * 
	 * @return
	 */
	private boolean isChecked() {
		if (TextUtils.isEmpty(contentEditText.getText().toString())) {
			Toast.makeText(ShareActivity.this, "说点什么吧", Toast.LENGTH_SHORT)
					.show();
			return false;
		} else if (contentEditText.getText().toString().length() > 140) {
			int currentLength = contentEditText.getText().toString().length();

			Toast.makeText(ShareActivity.this,
					"已超出" + (currentLength - 140) + "字", Toast.LENGTH_SHORT)
					.show();
			return false;
		}
		return true;
	}

	/**
	 * 图片选择功能的选择
	 * 
	 * @param items
	 */
	public void imageChooseItem(CharSequence[] items) {
		new AlertDialog.Builder(ShareActivity.this).setTitle("增加图片")
				.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int item) {
						// 手机选图
						if (item == 0) {
							Intent intent = new Intent(
									Intent.ACTION_GET_CONTENT);
							intent.setType("image/*");
							startActivityForResult(intent,
									REQUEST_CODE_GETIMAGE_BYSDCARD);
						}
						// 拍照
						else if (item == 1) {
							Intent intent = new Intent(
									"android.media.action.IMAGE_CAPTURE");
							String fileName = InfoHelper.getWeiboPath()
									+ InfoHelper.getFileName() + ".jpg";
							intent.putExtra(MediaStore.EXTRA_OUTPUT,
									Uri.fromFile(new File(fileName)));
							startActivityForResult(intent,
									REQUEST_CODE_GETIMAGE_BYCAMERA);
						} else if (item == 2) {
							uploadImage = null;
							imageView.setBackgroundDrawable(null);
						}
					}
				}).show();
	}

	/**
	 * 设置微博字数
	 */
	private void textCountSet() {
		String textContent = contentEditText.getText().toString();
		int remainLength = 140 - textContent.length();
		if (remainLength <= 140) {
			wordCounterTextView.setTextColor(Color.BLACK);
		} else {
			wordCounterTextView.setTextColor(Color.RED);
		}
		wordCounterTextView.setText(String.valueOf(remainLength));
	}

}
