package cn.com.alfred.weibo.OAuth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.com.alfred.weibo.MainActivity;
import cn.com.alfred.weibo.R;
import cn.com.alfred.weibo.basicModel.User;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;
import cn.com.alfred.weibo.db.DBAdapter;
import cn.com.alfred.weibo.http.AccessToken;
import cn.com.alfred.weibo.http.RequestToken;
import cn.com.alfred.weibo.widget.WebViewActivity;

/**
 * 显示曾通过验证的用户列表
 * 
 * @author alfredtofu
 * 
 */
public class OAuthUserList extends Activity {

	private static final int NORMAL_MODE = 1;
	private static final int DELETE_MODE = 2;

	private boolean[] isCheck;
	private ArrayList<AccessToken> users = new ArrayList<AccessToken>();
	private UserAdapter userAdapter;
	private Button btn_addUser;
	private Button btn_deleteUser;
	private boolean status; // true for normal mode, false for delete mode
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			super.handleMessage(msg);
			switch (msg.what) {
				case NORMAL_MODE:
					btn_deleteUser.setText("删除用户");
					btn_addUser.setText("添加用户");
					status = true;
					break;
				case DELETE_MODE:
					btn_deleteUser.setText("取消");
					btn_addUser.setText("确定");
					status = false;
					isCheck = new boolean[users.size()];
					Arrays.fill(isCheck, false);
					break;
			}
			users = DBAdapter.getInstance(OAuthUserList.this)
					.getAllUsersAccessToken();
			userAdapter.notifyDataSetChanged();
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.userlist);

		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);

		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// WeiboEmotion.downloadEmotions();
		// }
		// }).start();

		status = true;

		btn_addUser = (Button) findViewById(R.id.btn_addUser);
		btn_addUser.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (status) {
					OAuthConstant.initData();

					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					try {
						RequestToken requestToken = weibo
								.getOAuthRequestToken("tofuweibo://UserList");
						Uri uri = Uri.parse(requestToken.getAuthenticationURL()
								+ "&from=xweibo");
						OAuthConstant.getInstance().setRequestToken(
								requestToken);
						Intent intent = new Intent(OAuthUserList.this,
								WebViewActivity.class);
						intent.putExtra("url", uri.toString());
						OAuthUserList.this.startActivity(intent);
					} catch (WeiboException e) {
						e.printStackTrace();
					}
				} else {
					for (int i = 0; i < users.size(); i++) {
						if (isCheck[i])
							DBAdapter.getInstance(OAuthUserList.this)
									.deleteUserToken(users.get(i));
					}
					handler.sendEmptyMessage(NORMAL_MODE);
				}
			}
		});

		btn_deleteUser = (Button) findViewById(R.id.btn_deleteUser);
		btn_deleteUser.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (status) {
					handler.sendEmptyMessage(DELETE_MODE);
				} else {
					handler.sendEmptyMessage(NORMAL_MODE);
				}
			}
		});
		ListView listView = (ListView) findViewById(R.id.user_list);
		userAdapter = new UserAdapter(this);
		listView.setAdapter(userAdapter);
		listView.setDividerHeight(5);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (status) {
					OAuthConstant.getInstance().setAccessToken(
							users.get(position));
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						SharedPreferences sp = OAuthUserList.this
								.getSharedPreferences("tofuweibo",
										Context.MODE_PRIVATE);
						Editor editor = sp.edit();
						ObjectOutputStream oos = new ObjectOutputStream(baos);
						oos.writeObject(users.get(position));
						String s = new String(Base64.encodeBase64(baos
								.toByteArray()));
						editor.putString("accessToken", s);
						editor.commit();
					} catch (IOException e) {
						e.printStackTrace();
						Toast.makeText(OAuthUserList.this, "保存token失败",
								Toast.LENGTH_LONG).show();
					}

					startActivity(new Intent(OAuthUserList.this,
							MainActivity.class));
				} else {
					isCheck[position] = !isCheck[position];
					((ViewHolder) view.getTag()).checkBox
							.setChecked(isCheck[position]);
				}
			}

		});

	}

	@Override
	public void onResume() {
		super.onResume();
		handler.sendEmptyMessage(NORMAL_MODE);
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);
		Uri uri = intent.getData();
		try {
			// 可能机子内存低会导致此时获得的requestToken为null，因为其内存空间被释放了。
			RequestToken requestToken = OAuthConstant.getInstance()
					.getRequestToken();
			AccessToken accessToken = requestToken.getAccessToken(uri
					.getQueryParameter("oauth_verifier"));

			OAuthConstant.getInstance().setAccessToken(accessToken);
			Weibo weibo = OAuthConstant.getInstance().getWeibo();
			weibo.setToken(OAuthConstant.getInstance().getToken(),
					OAuthConstant.getInstance().getTokenSecret());
			User user = weibo.showUser(accessToken.getUserId() + "");
			accessToken.setScreenName(user.getScreenName());
			DBAdapter.getInstance(this).saveUserToken(accessToken);
			users = DBAdapter.getInstance(this).getAllUsersAccessToken();
			userAdapter.notifyDataSetChanged();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "添加失败", Toast.LENGTH_LONG).show();
		}
		this.onResume();
	}

	class UserAdapter extends BaseAdapter {

		Context context;

		public UserAdapter(Context context) {
			this.context = context;
		}

		@Override
		public int getCount() {
			return users.size();
		}

		@Override
		public AccessToken getItem(int position) {
			return users.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View view;
			ViewHolder holder;
			if (convertView != null) {
				holder = (ViewHolder) convertView.getTag();
				view = convertView;
			} else {
				LinearLayout linearLayout = new LinearLayout(context);
				linearLayout.setBackgroundResource(R.drawable.item_bg_triangle);
				linearLayout.setLayoutParams(new AbsListView.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
				linearLayout.setOrientation(LinearLayout.HORIZONTAL);
				linearLayout.setGravity(Gravity.CENTER_VERTICAL);

				CheckBox checkBox = new CheckBox(context);
				checkBox.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				checkBox.setGravity(Gravity.RIGHT);
				linearLayout.addView(checkBox);

				ImageView imageView = new ImageView(context);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						40, 40);
				lp.weight = 1;
				imageView.setImageResource(R.drawable.triangle);
				linearLayout.addView(imageView, lp);

				TextView tv = new TextView(context);
				tv.setText(users.get(position).getScreenName());
				tv.setTextSize(20);
				tv.setTextColor(Color.BLACK);
				tv.setLayoutParams(new LayoutParams(
						LinearLayout.LayoutParams.FILL_PARENT,
						LayoutParams.WRAP_CONTENT));
				tv.setGravity(Gravity.CENTER_VERTICAL);
				linearLayout.addView(tv);

				view = linearLayout;
				holder = new ViewHolder();
				holder.tv = tv;
				holder.checkBox = checkBox;
				view.setTag(holder);
			}

			holder.checkBox.setFocusable(false);
			holder.checkBox.setChecked(false);
			holder.checkBox
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							isCheck[position] = isChecked;
						}
					});
			if (status) {
				holder.checkBox.setVisibility(View.GONE);
			} else {
				holder.checkBox.setVisibility(View.VISIBLE);
			}

			holder.tv.setText(users.get(position).getScreenName());

			return view;
		}
	}

	static class ViewHolder {

		TextView tv;
		CheckBox checkBox;
	}
}