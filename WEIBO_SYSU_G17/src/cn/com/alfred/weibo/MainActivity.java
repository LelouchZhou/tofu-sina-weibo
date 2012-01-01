package cn.com.alfred.weibo;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.OAuth.OAuthUserList;
import cn.com.alfred.weibo.basicModel.Count;
import cn.com.alfred.weibo.basicModel.Status;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;
import cn.com.alfred.weibo.basicModel.WeiboResponse;
import cn.com.alfred.weibo.db.DBAdapter;
import cn.com.alfred.weibo.emotion.WeiboEmotion;
import cn.com.alfred.weibo.util.InfoHelper;
import cn.com.alfred.weibo.widget.AutoGetMoreListView;
import cn.com.alfred.weibo.widget.WaitingView;

/**
 * 显示用户自己以及其关注的人的微博列表/用户的个人资料
 * 
 * @author alfredtofu
 * 
 */
public class MainActivity extends TabActivity implements TabContentFactory,
		OnMenuItemClickListener {

	public static int UNREAD_COMMENT = 1000;
	public static int UNREAD_FOLLOWER = 2000;
	public static int UNREAD_MENTION = 3000;

	private TabHost tabHost;
	private WeiboAdapter msgAdapter;
	private List<WeiboResponse> friendsTimeline = new ArrayList<WeiboResponse>();
	private AutoGetMoreListView autoGetMoreListView;
	private int preIndex = 0;
	private FrameLayout ff;

	private WaitingView waitingView;
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case InfoHelper.LOADING_DATA_FAILED:
					Toast.makeText(MainActivity.this, "获取信息失败",
							Toast.LENGTH_LONG).show();
					MainActivity.this.finish();
					break;
				case InfoHelper.LOADING_DATA_COMPLETED:
					msgAdapter.notifyDataSetChanged();
					waitingView.setVisibility(View.GONE);
					autoGetMoreListView.setVisibility(View.VISIBLE);
					DBAdapter.getInstance(MainActivity.this)
							.saveFriendTimeline(friendsTimeline);
					autoGetMoreListView.setSelection(1);
					Toast.makeText(MainActivity.this, "刷新首页微博完成",
							Toast.LENGTH_LONG).show();
					break;
				case WeiboEmotion.EMOTIONS_DOWNLOAD_COMPLETED:
					Toast.makeText(MainActivity.this, "表情包下载或更新完毕",
							Toast.LENGTH_LONG).show();
					break;
				case WeiboEmotion.EMOTIONS_DOWNLOAD_FAILED:
					Toast.makeText(MainActivity.this, "表情包下载或更新失败",
							Toast.LENGTH_LONG).show();
					break;

			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);

		waitingView = new WaitingView(this);

		autoGetMoreListView = new AutoGetMoreListView(this);
		msgAdapter = new WeiboAdapter(friendsTimeline, this, 3,
				autoGetMoreListView);
		autoGetMoreListView.setAdapter(msgAdapter);
		autoGetMoreListView.setOnGetMoreListener(msgAdapter);
		autoGetMoreListView.setOnRefreshListener(msgAdapter);
		autoGetMoreListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(MainActivity.this,
						ViewActivity.class);
				intent.putExtra("cid",
						((Status) friendsTimeline.get(position - 1)).getId());
				startActivityForResult(intent, 1);
			}
		});

		initTab();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, 1, 1, "发微博").setIntent(
				new Intent(this, ShareActivity.class));
		Intent intent1 = new Intent(this, FriendsOrFollowsList.class);
		intent1.putExtra("flag", false);
		menu.add(1, 2, 2, "已关注的人").setIntent(intent1);
		Intent intent2 = new Intent(this, FriendsOrFollowsList.class);
		intent2.removeExtra("flag");
		intent2.putExtra("flag", true);
		menu.add(1, 3, 3, "粉丝").setIntent(intent2);
		menu.add(1, 4, 4, "下载表情包").setOnMenuItemClickListener(this);
		menu.add(1, 5, 5, "更新表情包").setOnMenuItemClickListener(this);
		return true;
	}

	private void initTab() {
//		setContentView(R.layout.tabactivity);
		tabHost = this.getTabHost();
		tabHost.setBackgroundResource(R.drawable.bg);
//		ff = (FrameLayout) findViewById(android.R.id.tabcontent);
		ff = new FrameLayout(this);

		TabSpec ts1 = tabHost.newTabSpec("HOME").setIndicator("首页");
		ts1.setContent(this);
		tabHost.addTab(ts1);

		TabSpec ts2 = tabHost.newTabSpec("MSG").setIndicator("信息")
				.setContent(this);
		tabHost.addTab(ts2);

		TabSpec ts3 = tabHost.newTabSpec("INFO").setIndicator("资料")
				.setContent(new Intent(this, UserInfo.class));
		// TabSpec ts3 = tabHost.newTabSpec("INFO").setIndicator("资料")
		// .setContent(this);
		tabHost.addTab(ts3);

		// tabHost.setOnTabChangedListener(new OnTabChangeListener() {
		//
		// @Override
		// public void onTabChanged(String tabId) {
		// if (tabId.equals("MSG")) {
		// startActivityForResult(new Intent(MainActivity.this,
		// InfoActivity.class), 1);
		// } else {
		// preIndex = tabHost.getCurrentTab();
		// }
		// }
		// });
		changeTabStyle();
	}

	public static void unReadNotify(Context context) {
		Weibo weibo = OAuthConstant.getInstance().getWeibo();
		Count unread_count;
		try {
			unread_count = weibo.getUnread();
			NotificationManager notificationManager = (NotificationManager) context
					.getSystemService(NOTIFICATION_SERVICE);
			if (unread_count.getComments() != 0) {
				Notification notification = new Notification(R.drawable.image,
						"你有" + unread_count.getComments() + "条未读评论.",
						System.currentTimeMillis());
				Intent intent = new Intent(context, InfoActivity.class);
				intent.putExtra("type", 0);
				PendingIntent contentIntent = PendingIntent.getActivity(
						context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				notification.setLatestEventInfo(context,
						"你有" + unread_count.getComments() + "条未读评论.", "",
						contentIntent);
				notification.defaults = Notification.DEFAULT_ALL;
				notificationManager.notify(UNREAD_COMMENT, notification);
			}
			if (unread_count.getMentions() != 0) {
				Notification notification = new Notification(R.drawable.image,
						"你有" + unread_count.getMentions() + "条未读@你的微薄.",
						System.currentTimeMillis());
				Intent intent = new Intent(context, InfoActivity.class);
				intent.putExtra("type", 2);
				PendingIntent contentIntent = PendingIntent.getActivity(
						context, UNREAD_MENTION, intent,
						PendingIntent.FLAG_UPDATE_CURRENT);
				notification.setLatestEventInfo(context,
						"你有" + unread_count.getMentions() + "条未读@你的微薄.", "",
						contentIntent);
				notification.defaults = Notification.DEFAULT_ALL;
				notificationManager.notify(UNREAD_MENTION, notification);
			}
			if (unread_count.getFollowers() != 0) {
				Notification notification = new Notification(R.drawable.image,
						"你有" + unread_count.getFollowers() + "新的关注者.",
						System.currentTimeMillis());
				Intent intent = new Intent(context, FriendsOrFollowsList.class);
				intent.putExtra("flag", true);
				PendingIntent contentIntent = PendingIntent.getActivity(
						context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				notification.setLatestEventInfo(context,
						"你有" + unread_count.getFollowers() + "新的关注者.", "",
						contentIntent);
				notification.defaults = Notification.DEFAULT_ALL;
				notificationManager.notify(UNREAD_FOLLOWER, notification);
			}

			if (unread_count.getDm() != 0) {
				Notification notification = new Notification(R.drawable.image,
						"你有" + unread_count.getDm() + "条未读的私信.",
						System.currentTimeMillis());
				Intent intent = new Intent(context, OAuthUserList.class);
				PendingIntent contentIntent = PendingIntent.getActivity(
						context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				notification.setLatestEventInfo(context,
						"你有" + unread_count.getDm() + "条未读的私信.", "",
						contentIntent);
				notification.defaults = Notification.DEFAULT_ALL;
				notificationManager.notify(UNREAD_FOLLOWER, notification);
			}
		} catch (WeiboException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		tabHost.setCurrentTab(preIndex);
	}

	@Override
	public View createTabContent(String tag) {
		if (tag.equals("HOME")) {
			ff.addView(waitingView);
			ff.addView(autoGetMoreListView);
			autoGetMoreListView.setVisibility(View.GONE);
			new Thread(new Runnable() {

				@Override
				public void run() {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					try {
						friendsTimeline.addAll(weibo.getFriendsTimeline());
						handler.sendEmptyMessage(InfoHelper.LOADING_DATA_COMPLETED);
					} catch (WeiboException e) {
						e.printStackTrace();
						handler.sendEmptyMessage(InfoHelper.LOADING_DATA_FAILED);
					}
					unReadNotify(MainActivity.this);
				}
			}).start();
		}
		return ff;
	}

	private void changeTabStyle() {
		TabWidget widget = tabHost.getTabWidget();
		for (int i = 0; i < 3; i++) {
			View view = widget.getChildAt(i);
			view.setBackgroundResource(R.drawable.widget_btn);
			final int index = i;
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (index == 1) {
						startActivityForResult(new Intent(MainActivity.this,
								InfoActivity.class), 1);
					} else {
						preIndex = tabHost.getCurrentTab();
						if (tabHost.getCurrentTab() == index) {
							autoGetMoreListView.setSelection(1);
						} else {
							tabHost.setCurrentTab(index);
							preIndex = tabHost.getCurrentTab();
						}
					}
				}
			});
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case 4:
				Toast.makeText(MainActivity.this, "开始下载表情包，请稍候...",
						Toast.LENGTH_LONG).show();
				new Thread(new Runnable() {

					@Override
					public void run() {
						WeiboEmotion.downloadEmotions(handler);
					}
				}).start();
				return true;
			case 5:
				Toast.makeText(MainActivity.this, "开始更新表情包，请稍候...",
						Toast.LENGTH_LONG).show();
				new Thread(new Runnable() {

					@Override
					public void run() {
						WeiboEmotion.updateEmotions(handler);
					}
				}).start();
				return true;
		}
		return false;
	}

}
