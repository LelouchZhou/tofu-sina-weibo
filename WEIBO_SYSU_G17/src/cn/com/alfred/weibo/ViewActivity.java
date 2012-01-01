package cn.com.alfred.weibo;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.TextView;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Comment;
import cn.com.alfred.weibo.basicModel.Status;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;
import cn.com.alfred.weibo.http.AccessToken;
import cn.com.alfred.weibo.util.ImageRel;
import cn.com.alfred.weibo.util.InfoHelper;
import cn.com.alfred.weibo.util.MyView;
import cn.com.alfred.weibo.widget.AsyncImageView;
import cn.com.alfred.weibo.widget.HighLightTextView;

/**
 * 显示单条微博
 * 
 * @author alfredtofu
 * 
 */
public class ViewActivity extends Activity {

	static class ViewHolder {

		AsyncImageView asyncImageView;
		TextView tv_name;
		HighLightTextView tv_text;
	}

	private AsyncImageView weibo_avatar;
	private AsyncImageView weibo_pic;
	private TextView weibo_screenName;
	private TextView weibo_no_comment;
	private HighLightTextView weibo_text;
	private Button weibo_btn_retweet;
	private Button weibo_btn_comment;
	private FrameLayout ff;
	private LinearLayout ll;
	private SlidingDrawer slidingDrawer;
	private ListView listView;
	
	private Status status;

	private String url;

	private long weiboID;

	private List<Comment> comments = new ArrayList<Comment>();
	private CommentAdapter adapter;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case InfoHelper.LOADING_DATA_FAILED:
					Toast.makeText(ViewActivity.this, "获取信息失败",
							Toast.LENGTH_LONG).show();
					ViewActivity.this.finish();
					break;
				case InfoHelper.LOADING_DATA_COMPLETED:
					adapter.notifyDataSetChanged();
					Toast.makeText(ViewActivity.this, "刷新完成", Toast.LENGTH_LONG)
							.show();
					ff.setVisibility(View.VISIBLE);
					ll.setVisibility(View.GONE);
					if (comments.size() == 0) {
						weibo_no_comment.setVisibility(View.VISIBLE);
						listView.setVisibility(View.GONE);
					} else {
						weibo_no_comment.setVisibility(View.GONE);
						listView.setVisibility(View.VISIBLE);
					}
					setData();
					break;
			}
		}

	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.weibo);

		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);

		Bundle bundle = getIntent().getExtras();
		if (bundle == null || !bundle.containsKey("cid")) {
			Toast.makeText(this, "获取微博信息失败", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		weiboID = bundle.getLong("cid");

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					if (weibo == null) {
						SharedPreferences sp = getSharedPreferences(
								"tofuweibo", Context.MODE_PRIVATE);
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
					status = weibo.showStatus(weiboID);
					comments = weibo.getComments(weiboID + "");
					handler.sendEmptyMessage(InfoHelper.LOADING_DATA_COMPLETED);
				} catch (Exception e) {
					e.printStackTrace();
					handler.sendEmptyMessage(InfoHelper.LOADING_DATA_FAILED);
					return;
				}
			}
		}).start();

		weibo_avatar = (AsyncImageView) findViewById(R.id.weibo_avatar);
		weibo_avatar.setProgressBitmaps(ImageRel.getBitmaps_avatar(this));
		weibo_pic = (AsyncImageView) findViewById(R.id.weibo_pic);
		weibo_pic.setProgressBitmaps(ImageRel.getBitmaps_loading(this));
		weibo_screenName = (TextView) findViewById(R.id.weibo_screenName);
		weibo_text = (HighLightTextView) findViewById(R.id.weibo_text);
		weibo_btn_comment = (Button) findViewById(R.id.weibo_btn_comment);
		weibo_btn_retweet = (Button) findViewById(R.id.weibo_btn_retweet);
		slidingDrawer = (SlidingDrawer) findViewById(R.id.slidingdrawer);
		weibo_no_comment = (TextView) findViewById(R.id.weibo_no_comment);
		ff = (FrameLayout) findViewById(R.id.weibo_ff);
		ll = (LinearLayout) findViewById(R.id.weibo_waitingView);

		listView = (ListView) findViewById(R.id.slide_listView);
		listView.setCacheColorHint(0);
		adapter = new CommentAdapter();
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				new CommentMentionDialog(ViewActivity.this, true, weiboID,
						comments.get(position).getId() + "");
			}
		});

		weibo_pic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ViewActivity.this, MyView.class);
				intent.putExtra("url", url);
				startActivity(intent);
			}
		});

		weibo_btn_comment.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new CommentMentionDialog(ViewActivity.this, true, weiboID, null);
			}
		});

		weibo_btn_retweet.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new CommentMentionDialog(ViewActivity.this, false, weiboID,
						null);
			}
		});

		slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {

			@Override
			public void onDrawerClosed() {
				Weibo weibo = OAuthConstant.getInstance().getWeibo();
				try {
					List<Comment> newComments = weibo.getComments(weiboID + "");
					comments = newComments;
					if (comments.size() == 0) {
						weibo_no_comment.setVisibility(View.VISIBLE);
						listView.setVisibility(View.GONE);
					} else {
						weibo_no_comment.setVisibility(View.GONE);
						listView.setVisibility(View.VISIBLE);
					}
					adapter.notifyDataSetChanged();
				} catch (WeiboException e) {
					e.printStackTrace();
					Toast.makeText(ViewActivity.this, "获取微博信息失败",
							Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	private void setData() {
		try {
			weibo_avatar.setUrl(status.getUser().getProfileImageURL()
					.toString());
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "获取微博信息失败", Toast.LENGTH_LONG).show();
		}
		weibo_screenName.setText(status.getUser().getScreenName());

		String text = status.getText();

		if (status.getRetweeted_status() != null) {
			text = text + "\n\n转自: @"
					+ status.getRetweeted_status().getUser().getScreenName()
					+ " : " + status.getRetweeted_status().getText();
			if (!TextUtils.isEmpty(status.getRetweeted_status()
					.getThumbnail_pic())) {
				weibo_pic.setVisibility(View.VISIBLE);
				weibo_pic.setUrl(status.getRetweeted_status()
						.getThumbnail_pic());
				weibo_pic.setVisibility(View.VISIBLE);
				url = status.getRetweeted_status().getOriginal_pic();
			}
		} else if (!TextUtils.isEmpty(status.getThumbnail_pic())) {
			weibo_pic.setUrl(status.getThumbnail_pic());
			weibo_pic.setVisibility(View.VISIBLE);
			url = status.getOriginal_pic();
		}
		weibo_text.setText(text);
	}

	class CommentAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return comments.size();
		}

		@Override
		public Comment getItem(int position) {
			return comments.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				LinearLayout view = new LinearLayout(ViewActivity.this);
				view.setOrientation(LinearLayout.HORIZONTAL);

				AsyncImageView asyncImageView = new AsyncImageView(
						ViewActivity.this);
				HighLightTextView tv_text = new HighLightTextView(
						ViewActivity.this);
				view.addView(asyncImageView);

				LinearLayout ll = new LinearLayout(ViewActivity.this);
				ll.setOrientation(LinearLayout.VERTICAL);

				TextView tv_name = new TextView(ViewActivity.this);
				ll.addView(tv_name);
				ll.addView(tv_text);

				view.addView(ll);

				convertView = view;
				holder = new ViewHolder();
				holder.asyncImageView = asyncImageView;
				holder.tv_text = tv_text;
				holder.tv_name = tv_name;
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.asyncImageView.setUrl(comments.get(position).getUser()
					.getProfileImageURL().toExternalForm());
			holder.asyncImageView.setProgressBitmaps(ImageRel
					.getBitmaps_avatar(ViewActivity.this));
			holder.asyncImageView.setPadding(10, 10, 10, 10);

			holder.tv_name.setTextColor(Color.BLUE);
			holder.tv_name.setText(comments.get(position).getUser()
					.getScreenName());
			holder.tv_name.setPadding(5, 10, 0, 0);

			holder.tv_text.setPadding(10, 10, 10, 10);
			holder.tv_text.setGravity(Gravity.CENTER_VERTICAL);
			holder.tv_text.setText(comments.get(position).getText());
			holder.tv_text.setTextColor(Color.BLACK);
			return convertView;
		}
	}
}
