package cn.com.alfred.weibo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.User;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;
import cn.com.alfred.weibo.util.ImageRel;
import cn.com.alfred.weibo.util.InfoHelper;
import cn.com.alfred.weibo.widget.AsyncImageView;

public class UserInfo extends Activity implements OnClickListener {

	private TextView tv_failed;
	private TextView tv_name;
	private TextView tv_url;
	private TextView tv_decsription;
	private TextView tv_location;
	private LinearLayout ll;
	private LinearLayout userinfo_waitingView;
	private AsyncImageView userinfo_pic;
	private String cid;
	private User user;
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case InfoHelper.LOADING_DATA_FAILED:
					Toast.makeText(UserInfo.this, "获取信息失败", Toast.LENGTH_LONG)
							.show();
					tv_failed.setVisibility(View.VISIBLE);
					tv_failed.setText("获取用户资料失败");
					userinfo_waitingView.setVisibility(View.GONE);
					UserInfo.this.finish();
					break;

				case InfoHelper.LOADING_DATA_COMPLETED:
					ll.setVisibility(View.VISIBLE);
					userinfo_waitingView.setVisibility(View.GONE);
					tv_name.setText(user.getScreenName());
					tv_decsription
							.setText("介绍: "
									+ (TextUtils.isEmpty(user.getDescription()) ? "无" : user
											.getDescription()));
					tv_url.setText("博客: "
							+ (user.getURL() != null ? user.getURL().toString() : "无"));
					tv_location.setText(user.getLocation());
					userinfo_pic.setUrl(user.getProfileImageURL().toString());
					btn_follows.setText("粉丝数 " + user.getFollowersCount());
					btn_friends.setText("关注数 " + user.getFriendsCount());
					btn_weibo.setText("微博数 " + user.getStatusesCount());
					Toast.makeText(UserInfo.this, "获取用户资料完成", Toast.LENGTH_LONG)
							.show();
					break;
			}
		}
	};
	private Button btn_follows;
	private Button btn_friends;
	private Button btn_weibo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.userinfo);

		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);

		cid = null;
		Bundle bundle = getIntent().getExtras();
		if (bundle != null)
			cid = bundle.getString("cid");
		if (TextUtils.isEmpty(cid))
			cid = OAuthConstant.getInstance().getAccessToken().getUserId() + "";
		userinfo_pic = (AsyncImageView) findViewById(R.id.userinfo_pic);
		userinfo_pic.setProgressBitmaps(ImageRel.getBitmaps_avatar(this));
		ll = (LinearLayout) findViewById(R.id.userinfo_ll);
		tv_name = (TextView) findViewById(R.id.userinfo_tv_name);
		tv_decsription = (TextView) findViewById(R.id.userinfo_tv_description);
		tv_url = (TextView) findViewById(R.id.userinfo_tv_url);
		tv_location = (TextView) findViewById(R.id.userinfo_tv_location);
		userinfo_waitingView = (LinearLayout) findViewById(R.id.userinfo_waitingView);
		tv_failed = (TextView) findViewById(R.id.userinfo_failed);

		btn_follows = (Button) findViewById(R.id.userinfo_btn_follows);
		btn_friends = (Button) findViewById(R.id.userinfo_btn_friends);
		btn_weibo = (Button) findViewById(R.id.userinfo_btn_weibo);
		findViewById(R.id.userinfo_btn_refresh).setOnClickListener(this);

		btn_follows.setOnClickListener(this);
		btn_friends.setOnClickListener(this);
		btn_weibo.setOnClickListener(this);

		new Thread(new Runnable() {

			@Override
			public void run() {
				refreshUserInfo();
			}
		}).start();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.userinfo_btn_follows:
				Intent intent1 = new Intent(this, FriendsOrFollowsList.class);
				intent1.putExtra("flag", true);
				intent1.putExtra("cid", cid);
				startActivity(intent1);
				break;

			case R.id.userinfo_btn_friends:
				Intent intent2 = new Intent(this, FriendsOrFollowsList.class);
				intent2.putExtra("flag", false);
				intent2.putExtra("cid", cid);
				startActivity(intent2);
				break;

			case R.id.userinfo_btn_weibo:
				Intent intent3 = new Intent(this, UserWeibo.class);
				intent3.putExtra("cid", cid);
				startActivity(intent3);
				break;

			case R.id.userinfo_btn_refresh:
				refreshUserInfo();
				break;
		}
	}

	protected void refreshUserInfo() {
		try {
			Weibo weibo = OAuthConstant.getInstance().getWeibo();
			user = weibo.showUser(cid);
			handler.sendEmptyMessage(InfoHelper.LOADING_DATA_COMPLETED);
		} catch (WeiboException e) {
			e.printStackTrace();
			handler.sendEmptyMessage(InfoHelper.LOADING_DATA_FAILED);
		}
	}
}
