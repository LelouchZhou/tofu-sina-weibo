package cn.com.alfred.weibo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.User;
import cn.com.alfred.weibo.basicModel.UserWapper;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;
import cn.com.alfred.weibo.json.JSONException;
import cn.com.alfred.weibo.json.JSONObject;
import cn.com.alfred.weibo.listener.OnGetMoreListener;
import cn.com.alfred.weibo.listener.OnRefreshListener;
import cn.com.alfred.weibo.util.ImageRel;
import cn.com.alfred.weibo.util.InfoHelper;
import cn.com.alfred.weibo.widget.AsyncImageView;
import cn.com.alfred.weibo.widget.AutoGetMoreListView;
import cn.com.alfred.weibo.widget.WaitingView;

public class FriendsOrFollowsList extends Activity implements
		OnRefreshListener, OnGetMoreListener {

	static class ViewHolder {

		AsyncImageView asyncImageView;
		TextView info;
		Button btn;
	}

	private FriendsOrFollowsListAdapter adapter;
	private AutoGetMoreListView autoGetMoreListView;
	private List<User> list = new ArrayList<User>();
	private List<JSONObject> list_relations = new ArrayList<JSONObject>();
	private boolean flag; // true for follower, false for friends
	private String cid;
	private UserWapper curUserWapper;
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case InfoHelper.LOADING_DATA_FAILED:
					Toast.makeText(FriendsOrFollowsList.this, "获取信息失败",
							Toast.LENGTH_LONG).show();
					FriendsOrFollowsList.this.finish();
					break;
				case InfoHelper.LOADING_DATA_COMPLETED:
					adapter.notifyDataSetChanged();
					FriendsOrFollowsList.this
							.setContentView(autoGetMoreListView);
					Toast.makeText(FriendsOrFollowsList.this, "刷新完成",
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
		setContentView(new WaitingView(this));
		cid = null;
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			flag = bundle.getBoolean("flag");
			cid = bundle.getString("cid");
		}
		if (TextUtils.isEmpty(cid))
			cid = OAuthConstant.getInstance().getAccessToken().getUserId() + "";

		autoGetMoreListView = new AutoGetMoreListView(this);
		autoGetMoreListView.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		adapter = new FriendsOrFollowsListAdapter();
		autoGetMoreListView.setAdapter(adapter);
		autoGetMoreListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent intent = new Intent(FriendsOrFollowsList.this,
						UserInfo.class);
				if (position < list.size())
					intent.putExtra("cid", list.get(position - 1).getId() + "");
				startActivity(intent);
			}
		});
		autoGetMoreListView.setOnRefreshListener(this);
		autoGetMoreListView.setOnGetMoreListener(this);
		// setContentView(autoGetMoreListView);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					if (flag) {
						// list = weibo.getFollowersStatuses(cid);

						curUserWapper = weibo.getFollowersStatuses(cid, -1);
						list = curUserWapper.getUsers();
						NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
						notificationManager
								.cancel(MainActivity.UNREAD_FOLLOWER);
						weibo.resetCount(4);
					} else {
						// list = weibo.getFriendsStatuses(cid);
						curUserWapper = weibo.getFriendsStatuses(cid, -1);
						list = curUserWapper.getUsers();
					}
					getRelation(false, 0);
					handler.sendEmptyMessage(InfoHelper.LOADING_DATA_COMPLETED);
				} catch (WeiboException e) {
					e.printStackTrace();
					handler.sendEmptyMessage(InfoHelper.LOADING_DATA_FAILED);
				}
			}
		}).start();

	}

	protected void getRelation(boolean isGetMore, int beg) {
		if (!isGetMore)
			list_relations.clear();

		Weibo weibo = OAuthConstant.getInstance().getWeibo();
		// boolean followed, following;
		for (int index = beg; index < list.size(); index++) {
			User user = list.get(index);
			// String text = "";
			try {
				// String sex = (user.getGender().equals("m") ? "他" : "她");
				list_relations.add(weibo.showFriendships(OAuthConstant
						.getInstance().getAccessToken().getUserId()
						+ "", user.getId() + ""));
				// JSONObject source = object.getJSONObject("source");
				// JSONObject target = object.getJSONObject("target");
				// following = Boolean.valueOf(target.getString("followed_by"));
				// // 我是否关注他
				// followed = Boolean.valueOf(source.getString("followed_by"));
				// // 他是否关注我
				// if (following && followed) {
				// text = text + "\n" + "互相关注";
				// } else {
				// if (following) {
				// text = text + "\n" + "我已关注" + sex;
				// } else {
				// text = text + "\n" + "我未关注" + sex;
				// }
				// if (followed) {
				// text = text + "\n" + sex + "已关注我";
				// } else {
				// text = text + "\n" + sex + "未关注我";
				// }
				// }
			} catch (WeiboException e) {
				e.printStackTrace();
				// } catch (JSONException e) {
				// e.printStackTrace();
			}
		}
	}

	class FriendsOrFollowsListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public User getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final User user = list.get(position);
			final ViewHolder holder;
			if (convertView == null) {
				LinearLayout ll = new LinearLayout(FriendsOrFollowsList.this);
				ll.setOrientation(LinearLayout.HORIZONTAL);

				LinearLayout ll1 = new LinearLayout(FriendsOrFollowsList.this);
				ll1.setOrientation(LinearLayout.VERTICAL);
				ll1.setLayoutParams(new LayoutParams(130, LayoutParams.WRAP_CONTENT));
				AsyncImageView asyncImageView = new AsyncImageView(
						FriendsOrFollowsList.this);
				TextView info = new TextView(FriendsOrFollowsList.this);
				final Button btn = new Button(FriendsOrFollowsList.this);

				ll1.addView(asyncImageView);
				ll1.addView(btn);
				ll.addView(ll1);
				ll.addView(info);
				convertView = ll;

				holder = new ViewHolder();
				holder.asyncImageView = asyncImageView;
				holder.info = info;
				holder.btn = btn;
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.asyncImageView.setPadding(10, 10, 10, 10);
			holder.asyncImageView.setProgressBitmaps(ImageRel
					.getBitmaps_avatar(FriendsOrFollowsList.this));
			holder.asyncImageView.setUrl(user.getProfileImageURL().toString());
			String info_text = "昵称: "
					+ user.getScreenName()
					+ "\n性别: "
					+ (user.getGender().equals("m") ? "男" : "女")
					+ "\n所在地: "
					+ user.getLocation()
					+ "\n博客地址："
					+ ((user.getURL() != null) ? user.getURL().toString() : "无")
					+ "\n粉丝数: " + user.getFollowersCount() + "\n关注数: "
					+ user.getFriendsCount();
			// Weibo weibo = OAuthConstant.getInstance().getWeibo();

			final String sex = (user.getGender().equals("m") ? "他" : "她");
			boolean following = false;
			boolean followed = true;
			try {
				// JSONObject object = weibo.showFriendships(OAuthConstant
				// .getInstance().getAccessToken().getUserId()
				// + "", user.getId() + "");
				JSONObject object = list_relations.get(position);
				JSONObject source = object.getJSONObject("source");
				JSONObject target = object.getJSONObject("target");
				following = Boolean.valueOf(target.getString("followed_by")); // 我是否关注他
				followed = Boolean.valueOf(source.getString("followed_by")); // 他是否关注我
				if (following && followed) {
					info_text = info_text + "\n" + "互相关注";
				} else {
					if (following) {
						info_text = info_text + "\n" + "我已关注" + sex;
					} else {
						info_text = info_text + "\n" + "我未关注" + sex;
					}
					if (followed) {
						info_text = info_text + "\n" + sex + "已关注我";
					} else {
						info_text = info_text + "\n" + sex + "未关注我";
					}
				}
			}
			// catch (WeiboException e) {
			// e.printStackTrace();
			// }
			catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {

			}

			holder.info.setPadding(10, 5, 10, 10);
			holder.info.setText(info_text);
			holder.info.setTextColor(Color.BLACK);

			if (following)
				holder.btn.setText("取消关注");
			else
				holder.btn.setText("关注" + sex);
			holder.btn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					if (holder.btn.getText().equals("关注" + sex)) {
						try {
							// 官方要求要用post，函数createFriendship(id)是用get
							weibo.createFriendshipByUserid(user.getId() + "");
							holder.btn.setText("取消关注");
							Toast.makeText(FriendsOrFollowsList.this, "关注成功!",
									Toast.LENGTH_SHORT).show();
						} catch (WeiboException e) {
							e.printStackTrace();
							Toast.makeText(FriendsOrFollowsList.this, "关注失败!",
									Toast.LENGTH_SHORT).show();
						}
					} else {
						try {
							weibo.destroyFriendship(user.getId() + "");
							holder.btn.setText("关注" + sex);
							Toast.makeText(FriendsOrFollowsList.this,
									"取消关注成功!", Toast.LENGTH_SHORT).show();
						} catch (WeiboException e) {
							e.printStackTrace();
							Toast.makeText(FriendsOrFollowsList.this,
									"取消关注失败!", Toast.LENGTH_SHORT).show();
						}
					}

				}
			});
			holder.btn.setFocusable(false);

			return convertView;
		}
	}

	@Override
	public void onGetMore() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				if (curUserWapper.getNextCursor() != 0) {
					try {
						Weibo weibo = OAuthConstant.getInstance().getWeibo();
						List<User> tmp;
						if (flag) {

							curUserWapper = weibo.getFollowersStatuses(cid,
									curUserWapper.getNextCursor());
							tmp = curUserWapper.getUsers();
						} else {
							curUserWapper = weibo.getFriendsStatuses(cid,
									curUserWapper.getNextCursor());
							tmp = curUserWapper.getUsers();
						}
						list.addAll(tmp);
						getRelation(true, list.size() - tmp.size());
					} catch (WeiboException e) {
						e.printStackTrace();
					}
				}
				autoGetMoreListView.getMoreFinished();
			}
		}).start();
	}

	@Override
	public void onRefresh() {

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					List<User> tmp;
					if (flag)
						tmp = weibo.getFollowersStatuses(cid);
					else
						tmp = weibo.getFriendsStatuses(cid);
					list.clear();
					list = tmp;
					getRelation(false, 0);
				} catch (WeiboException e) {
					e.printStackTrace();
				}
				autoGetMoreListView.refreshFinished();
			}
		}).start();
	}
}
