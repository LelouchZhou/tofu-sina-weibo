package cn.com.alfred.weibo;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Status;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboResponse;
import cn.com.alfred.weibo.widget.AutoGetMoreListView;

public class UserWeibo extends Activity {

	private WeiboAdapter adapter;
	private AutoGetMoreListView autoGetMoreListView;
	private List<WeiboResponse> list;
	private String cid;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);
		
		try {
			cid = null;
			Bundle bundle = getIntent().getExtras();
			if (bundle != null)
				cid = bundle.getString("cid");
			if (TextUtils.isEmpty(cid))
				cid = OAuthConstant.getInstance().getAccessToken().getUserId()
						+ "";

			Weibo weibo = OAuthConstant.getInstance().getWeibo();
			list = new ArrayList<WeiboResponse>();
			list.addAll(weibo.getUserTimeline(cid));

			autoGetMoreListView = new AutoGetMoreListView(this);
			autoGetMoreListView.setLayoutParams(new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			adapter = new WeiboAdapter(list, this, 3, autoGetMoreListView);
			adapter.setCid(cid);
			autoGetMoreListView.setAdapter(adapter);
			autoGetMoreListView
					.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent,
								View view, int position, long id) {
							Intent intent = new Intent(UserWeibo.this,
									ViewActivity.class);
							intent.putExtra("cid",
									((Status) list.get(position - 1)).getId());
							startActivity(intent);
						}
					});
			setContentView(autoGetMoreListView);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "获取信息失败", Toast.LENGTH_LONG).show();
			UserWeibo.this.finish();
		}

	}
}
