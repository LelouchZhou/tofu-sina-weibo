package cn.com.alfred.weibo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Comment;
import cn.com.alfred.weibo.basicModel.Status;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;
import cn.com.alfred.weibo.basicModel.WeiboResponse;
import cn.com.alfred.weibo.util.InfoHelper;
import cn.com.alfred.weibo.widget.AutoGetMoreListView;
import cn.com.alfred.weibo.widget.WaitingView;

/**
 * 显示评论(包括发出和收到)以及@我的列表
 * 
 * @author alfredtofu
 * 
 */
public class InfoActivity extends TabActivity implements TabContentFactory,
		OnTabChangeListener, OnMenuItemClickListener {

	private TabHost tabHost;
	private FrameLayout frameLayout;
	private AutoGetMoreListView autoGetMoreListView;
	private WeiboAdapter infoAdapter;
	private List<WeiboResponse> commentToMe = new ArrayList<WeiboResponse>();
	private List<WeiboResponse> commentByMe = new ArrayList<WeiboResponse>();
	private List<WeiboResponse> mentions = new ArrayList<WeiboResponse>();
	private boolean[] isCompleted = new boolean[3];
	private boolean[] isRunning = new boolean[3];
	private boolean isFirst;
	private int preIndex;
	private int[] preListIndex = new int[3];
	private WaitingView waitingView;
	private Runnable[] runnables = new Runnable[3];
	private static String[] texts = new String[] { "“我收到的评论”", "“我发出的评论”",
			"“获取“@我的微博”" };
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			isRunning[msg.arg1] = false;
			switch (msg.what) {
				case InfoHelper.LOADING_DATA_FAILED:
					Toast.makeText(InfoActivity.this,
							"刷新" + texts[msg.arg1] + "失败, 可通过menu键刷新",
							Toast.LENGTH_LONG).show();
					break;
				case InfoHelper.LOADING_DATA_COMPLETED:
					isCompleted[msg.arg1] = true;
					infoAdapter.notifyDataSetChanged();
					Log.d("tabHost.getCurrentTab(): " + tabHost.getCurrentTab(),
							"msg.arg1: " + msg.arg1);
					if (tabHost.getCurrentTab() == msg.arg1) {
						Log.d("setVisibility", "setVisibility");
						waitingView.setVisibility(View.GONE);
						autoGetMoreListView.setVisibility(View.VISIBLE);
						// autoGetMoreListView.setSelection(1);

						// 不知道为什么第一次启动这个TabActivity的时候，infoAdapter.notifyDataSetChanged()没作用，通过这种方式可以解决，但是并不好
						if (isFirst) {
							isFirst = false;
							tabHost.setCurrentTab((msg.arg1 + 1) % 3);
							tabHost.setCurrentTab(msg.arg1);
						}
					}
					Toast.makeText(InfoActivity.this,
							"刷新" + texts[msg.arg1] + "完成", Toast.LENGTH_LONG)
							.show();
					break;
			}

		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tabactivity);

		isFirst = true;

		Arrays.fill(isCompleted, false);
		Arrays.fill(isRunning, false);
		Arrays.fill(preListIndex, 1);

		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);

		frameLayout = (FrameLayout) findViewById(android.R.id.tabcontent);
		autoGetMoreListView = new AutoGetMoreListView(this);
		waitingView = new WaitingView(this);
		frameLayout.addView(autoGetMoreListView);
		frameLayout.addView(waitingView);
		autoGetMoreListView.setVisibility(View.GONE);
		waitingView.setVisibility(View.VISIBLE);

		infoAdapter = new WeiboAdapter(commentByMe, this, 0,
				autoGetMoreListView);
		autoGetMoreListView.setAdapter(infoAdapter);
		autoGetMoreListView.setOnItemClickListener(itemClickListener);
		autoGetMoreListView.setOnRefreshListener(infoAdapter);
		autoGetMoreListView.setOnGetMoreListener(infoAdapter);

		initThread();

		initTabHost();

		Bundle bundle = this.getIntent().getExtras();
		int type = 0;
		if (bundle != null) {
			type = bundle.getInt("type", 0);
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			if (type == 0)
				notificationManager.cancel(MainActivity.UNREAD_COMMENT);
			else if (type == 2)
				notificationManager.cancel(MainActivity.UNREAD_MENTION);
		}
		preIndex = type;
		tabHost.setCurrentTab(type);
		switch (type) {
			case 0:
				infoAdapter.setCurList(commentByMe);
				break;
			case 1:
				infoAdapter.setCurList(commentToMe);
				break;
			case 2:
				infoAdapter.setCurList(mentions);
				break;
		}
		infoAdapter.setType(type);
		infoAdapter.notifyDataSetChanged();
		changeTabStyle();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		for (int i = 1; i <= 3; i++)
			menu.add(1, i, i, "刷新" + texts[i - 1]).setOnMenuItemClickListener(
					this);
		menu.add(1, 4, 4, "刷新全部").setOnMenuItemClickListener(this);
		menu.add(1, 5, 5, "发微博").setIntent(
				new Intent(this, ShareActivity.class));
		return true;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if (item.getItemId() > 4)
			return false;
		if (item.getItemId() != 4) {
			if (!isRunning[item.getItemId() - 1]) {
				new Thread(runnables[item.getItemId() - 1]).start();
				isRunning[item.getItemId() - 1] = true;
			}
		} else {
			for (int i = 0; i < 3; i++) {
				if (!isRunning[i]) {
					new Thread(runnables[i]).start();
					isRunning[i] = true;
				}
			}
		}

		return true;
	}

	@Override
	public View createTabContent(String tag) {
		if (!isRunning[tabHost.getCurrentTab()]) {
			new Thread(runnables[tabHost.getCurrentTab()]).start();
			isRunning[tabHost.getCurrentTab()] = true;
		}
		return frameLayout;
	}

	@Override
	public void onTabChanged(String tabId) {
		if (isCompleted[tabHost.getCurrentTab()]) {
			waitingView.setVisibility(View.GONE);
			autoGetMoreListView.setVisibility(View.VISIBLE);
		} else {
			waitingView.setVisibility(View.VISIBLE);
			autoGetMoreListView.setVisibility(View.GONE);
		}

		if (tabId.equals("COMMENT_BY_ME"))
			infoAdapter.setCurList(commentByMe);
		else if (tabId.equals("COMMENT_TO_ME"))
			infoAdapter.setCurList(commentToMe);
		else
			infoAdapter.setCurList(mentions);
		infoAdapter.setType(InfoActivity.this.getTabHost().getCurrentTab());
		infoAdapter.notifyDataSetChanged();
	}

	private void initThread() {
		runnables[0] = new Runnable() {

			@Override
			public void run() {
				Message msg = new Message();
				try {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					commentToMe.clear();
					commentToMe.addAll(weibo.getCommentsToMe());
					msg.what = InfoHelper.LOADING_DATA_COMPLETED;
				} catch (WeiboException e) {
					e.printStackTrace();
					msg.what = InfoHelper.LOADING_DATA_FAILED;
				}
				msg.arg1 = 0;
				handler.sendMessage(msg);
			}
		};

		runnables[1] = new Runnable() {

			@Override
			public void run() {
				Message msg = new Message();
				try {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					commentByMe.clear();
					commentByMe.addAll(weibo.getCommentsByMe());
					msg.what = InfoHelper.LOADING_DATA_COMPLETED;
				} catch (WeiboException e) {
					e.printStackTrace();
					msg.what = InfoHelper.LOADING_DATA_FAILED;
				}
				msg.arg1 = 1;
				handler.sendMessage(msg);
			}
		};

		runnables[2] = new Runnable() {

			@Override
			public void run() {
				Message msg = new Message();
				try {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					mentions.clear();
					mentions.addAll(weibo.getMentions());
					msg.what = InfoHelper.LOADING_DATA_COMPLETED;
				} catch (WeiboException e) {
					e.printStackTrace();
					msg.what = InfoHelper.LOADING_DATA_FAILED;
				}
				msg.arg1 = 2;
				handler.sendMessage(msg);
			}
		};
	}

	private void initTabHost() {
		tabHost = this.getTabHost();

		TabSpec ts1 = tabHost.newTabSpec("COMMENT_TO_ME").setIndicator("收到的评论")
				.setContent(this);
		tabHost.addTab(ts1);

		TabSpec ts2 = tabHost.newTabSpec("COMMENT_BY_ME").setIndicator("发出的评论");
		ts2.setContent(this);
		tabHost.addTab(ts2);

		TabSpec ts3 = tabHost.newTabSpec("MENTION").setIndicator("@我")
				.setContent(this);
		tabHost.addTab(ts3);

		tabHost.setOnTabChangedListener(this);
	}

	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				final int position, long id) {
			new AlertDialog.Builder(InfoActivity.this).setItems(
					new CharSequence[] { "评论", "查看原微博", "查看个人资料" },
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 2) {
								Intent intent = new Intent(InfoActivity.this,
										UserInfo.class);
								switch (tabHost.getCurrentTab()) {
									case 0:
										if (position > commentToMe.size() - 1)
											return;

										intent.putExtra(
												"cid",
												((Comment) commentToMe
														.get(position - 1))
														.getStatus().getUser()
														.getId()
														+ "");

										break;

									case 1:
										if (position > commentByMe.size() - 1)
											return;

										intent.putExtra(
												"cid",
												((Comment) commentByMe
														.get(position - 1))
														.getStatus().getUser()
														.getId()
														+ "");

										break;

									case 2:
										if (position > mentions.size() - 1)
											return;

										intent.putExtra(
												"cid",
												((Status) mentions
														.get(position - 1))
														.getUser().getId()
														+ "");
										break;
								}
								startActivity(intent);
								return;
							}

							if (which == 0) {
								switch (tabHost.getCurrentTab()) {
									case 0:
										if (position > commentToMe.size() - 1)
											return;

										new CommentMentionDialog(
												InfoActivity.this, true,
												((Comment) commentToMe
														.get(position - 1))
														.getStatus().getId(),
												((Comment) commentToMe
														.get(position - 1))
														.getId()
														+ "");
										break;

									case 1:
										if (position > commentByMe.size() - 1)
											return;

										new CommentMentionDialog(
												InfoActivity.this, true,
												((Comment) commentByMe
														.get(position - 1))
														.getStatus().getId(),
												((Comment) commentByMe
														.get(position - 1))
														.getId()
														+ "");
										break;

									case 2:
										if (position > mentions.size() - 1)
											return;

										new CommentMentionDialog(
												InfoActivity.this, true,
												(((Status) mentions
														.get(position - 1))
														.getId()), null);
										break;
								}
								return;
							}

							Intent intent = new Intent(InfoActivity.this,
									ViewActivity.class);
							switch (tabHost.getCurrentTab()) {
								case 0:
									if (position > commentToMe.size() - 1)
										return;

									intent.putExtra("cid",
											((Comment) commentToMe
													.get(position - 1))
													.getStatus().getId());
									break;

								case 1:
									if (position > commentByMe.size() - 1)
										return;

									intent.putExtra("cid",
											((Comment) commentByMe
													.get(position - 1))
													.getStatus().getId());
									break;

								case 2:
									if (position > mentions.size() - 1)
										return;

									intent.putExtra("cid", ((Status) mentions
											.get(position - 1)).getId());
									break;
								default:
									return;
							}
							InfoActivity.this.startActivity(intent);
						}

					}).show();

		}
	};

	private void changeTabStyle() {
		TabWidget widget = tabHost.getTabWidget();
		for (int i = 0; i < 3; i++) {
			View view = widget.getChildAt(i);
			view.setBackgroundResource(R.drawable.widget_btn);
			final int index = i;
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (tabHost.getCurrentTab() == index) {
						autoGetMoreListView.setSelection(1);
						preListIndex[index] = 1;

					} else {

						preListIndex[preIndex] = autoGetMoreListView
								.getFirstVisiblePosition();
						tabHost.setCurrentTab(index);

						if (isCompleted[index]) {
							waitingView.setVisibility(View.GONE);
							autoGetMoreListView.setVisibility(View.VISIBLE);
						} else {
							waitingView.setVisibility(View.VISIBLE);
							autoGetMoreListView.setVisibility(View.GONE);
						}

						switch (index) {
							case 0:
								infoAdapter.setCurList(commentToMe);
								break;
							case 1:
								infoAdapter.setCurList(commentByMe);
								break;
							case 2:
								infoAdapter.setCurList(mentions);
								break;
						}
						infoAdapter.setType(index);
						infoAdapter.notifyDataSetChanged();

						if (preListIndex[index] < infoAdapter.getCount() + 2)
							autoGetMoreListView
									.setSelection(preListIndex[index]);
					}
					Log.d("preIndex: " + preIndex, "index: " + index);
					preIndex = index;
					for (int i = 0; i < 3; i++) {
						Log.d("preListIndex " + i, preListIndex[i] + "");
					}
				}
			});
		}
	}
}
