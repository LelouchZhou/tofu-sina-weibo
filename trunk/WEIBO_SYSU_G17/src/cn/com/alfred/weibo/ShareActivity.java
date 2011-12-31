package cn.com.alfred.weibo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.http.AccessToken;
import cn.com.alfred.weibo.util.InfoHelper;
import cn.com.alfred.weibo.widget.HighLightTextView;

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

	private Button btn_share;
	private Button btn_emtions;
	private ImageButton imgChooseBtn;
	private ImageView imageView;
	private TextView wordCounterTextView;
	private EditText contentEditText;
	private ProgressDialog dialog;
	private GridView gridView;
	private GridAdapter adapter;
	private String uploadImage = null;
	private List<Bitmap> emotioms = new ArrayList<Bitmap>();
	private List<String> emotioms_name = new ArrayList<String>();
	private boolean isInputMethodShow;
	private boolean isGridViewShow;
	private String lastString;

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

		isInputMethodShow = isGridViewShow = false;
		lastString = "";
		getEmotions();

		gridView = (GridView) findViewById(R.id.gridView);
		btn_share = (Button) findViewById(R.id.btn_share);
		btn_emtions = (Button) findViewById(R.id.btn_add_emotion);
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

		adapter = new GridAdapter();
		gridView.setAdapter(adapter);
		gridView.setVisibility(View.GONE);
		adapter.notifyDataSetChanged();
		gridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String text = contentEditText.getText().toString();
				if (TextUtils.isEmpty(text))
					text = "";

				text = text + emotioms_name.get(position);
				contentEditText.setText(text);
			}
		});

		btn_share.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (InfoHelper.checkNetWork(ShareActivity.this) && isChecked()) {
					dialog.show();
					new Thread(updateWeibo).start();
				}
			}
		});

		btn_emtions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (btn_emtions.getText().equals("输入文字")) {
					btn_emtions.setText("添加表情");

					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					inputMethodManager.toggleSoftInput(0,
							InputMethodManager.SHOW_FORCED);
					gridView.setVisibility(View.GONE);

					isInputMethodShow = true;
					isGridViewShow = false;

				} else {
					btn_emtions.setText("输入文字");

					InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					inputMethodManager.hideSoftInputFromWindow(
							contentEditText.getApplicationWindowToken(), 0);
					gridView.setVisibility(View.VISIBLE);

					isInputMethodShow = false;
					isGridViewShow = true;
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

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (lastString.equals(s.toString()))
					return;
				Log.d("onTextChanged", "s: " + s.toString());
				Log.d("onTextChanged", "lastString: " + lastString);
				lastString = new String(s.toString());
				textCountSet();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}

		});

		contentEditText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputMethodManager.toggleSoftInput(0,
						InputMethodManager.SHOW_FORCED);
				btn_emtions.setText("添加表情");
				isInputMethodShow = true;
				isGridViewShow = false;
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(
					contentEditText.getApplicationWindowToken(), 0);
			gridView.setVisibility(View.GONE);
			if (isInputMethodShow || isGridViewShow) {
				isInputMethodShow = isGridViewShow = false;
				return true;
			}
			new AlertDialog.Builder(ShareActivity.this)
					.setMessage("确认要退出发微博吗？")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									ShareActivity.this.finish();
								}
							})
					.setNegativeButton("取消",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
								}
							}).show();
			isInputMethodShow = isGridViewShow = false;
			return true;
		}
		return false;
	}

	private void getEmotions() {
		emotioms.clear();
		emotioms_name.clear();
		File file = new File(InfoHelper.getEmotionPath());
		File[] files = file.listFiles();
		Log.d(InfoHelper.TAG, "files size " + files.length);
		try {
			for (File tmp : files) {
				emotioms.add(BitmapFactory.decodeFile(tmp.getAbsolutePath()));
				emotioms_name.add(tmp.getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		if (TextUtils.isEmpty(textContent))
			textContent = "";
		int remainLength = 140 - textContent.length();
		if (remainLength <= 140) {
			wordCounterTextView.setTextColor(Color.BLACK);
		} else {
			wordCounterTextView.setTextColor(Color.RED);
		}
		HighLightTextView.setHighLightText(contentEditText, textContent);
		wordCounterTextView.setText(String.valueOf(remainLength));
		contentEditText.setSelection(contentEditText.length());
	}

	class GridAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return emotioms.size();
		}

		@Override
		public Bitmap getItem(int position) {
			return emotioms.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;
			if (convertView == null) {
				imageView = new ImageView(ShareActivity.this);
				imageView.setLayoutParams(new GridView.LayoutParams(45, 45));
				imageView.setAdjustViewBounds(false);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(8, 8, 8, 8);
			} else {
				imageView = (ImageView) convertView;
			}

			imageView.setImageBitmap(emotioms.get(position));
			return imageView;
		}

	}
}
