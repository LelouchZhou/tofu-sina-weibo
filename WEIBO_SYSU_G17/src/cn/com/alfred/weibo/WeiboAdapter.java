package cn.com.alfred.weibo;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Comment;
import cn.com.alfred.weibo.basicModel.Paging;
import cn.com.alfred.weibo.basicModel.Status;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;
import cn.com.alfred.weibo.basicModel.WeiboResponse;
import cn.com.alfred.weibo.listener.OnGetMoreListener;
import cn.com.alfred.weibo.listener.OnRefreshListener;
import cn.com.alfred.weibo.util.ImageRel;
import cn.com.alfred.weibo.widget.AsyncImageView;
import cn.com.alfred.weibo.widget.AutoGetMoreListView;
import cn.com.alfred.weibo.widget.HighLightTextView;

/**
 * 用于显示评论/@我/微博等列表的adapter
 * 
 * @author alfredtofu
 * 
 */
public class WeiboAdapter extends BaseAdapter implements OnRefreshListener,
		OnGetMoreListener {

	public static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	private List<WeiboResponse> curList;
	private Context context;
	private AutoGetMoreListView autoGetMoreListView;
	private int type; // 0 for 收到的评论
						// 1 for 发出的评论
						// 2 for @我
						// 3 for 微博
	private String cid; // 代表当前查看的是指定人的微博

	static class ViewHolder {
		AsyncImageView wbicon;
		HighLightTextView wbtext;
		TextView wbtime;
		TextView wbuser;
		ImageView wbimage;
	}
	
	
	public WeiboAdapter(List<WeiboResponse> curList, Context context, int type,
			AutoGetMoreListView autoGetMoreListView) {
		this.curList = curList;
		this.context = context;
		this.type = type;
		this.autoGetMoreListView = autoGetMoreListView;
		this.autoGetMoreListView.setOnRefreshListener(this);
		this.autoGetMoreListView.setOnGetMoreListener(this);
		this.cid = null;

		System.setProperty("weibo4j.oauth.consumerKey", Weibo.CONSUMER_KEY);
		System.setProperty("weibo4j.oauth.consumerSecret",
				Weibo.CONSUMER_SECRET);
	}

	@Override
	public int getCount() {
		return curList.size();
	}

	@Override
	public WeiboResponse getItem(int position) {
		return curList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.timeline,
					null);
			AsyncImageView wbicon = (AsyncImageView) convertView
					.findViewById(R.id.wbicon);
			HighLightTextView wbtext = (HighLightTextView) convertView.findViewById(R.id.wbtext);
			TextView wbtime = (TextView) convertView.findViewById(R.id.wbtime);
			TextView wbuser = (TextView) convertView.findViewById(R.id.wbuser);
			ImageView wbimage = (ImageView) convertView.findViewById(R.id.wbimage);
			
			holder = new ViewHolder();
			holder.wbicon = wbicon;
			holder.wbimage = wbimage;
			holder.wbtext = wbtext;
			holder.wbtime = wbtime;
			holder.wbuser = wbuser;
			convertView.setTag(holder);
		}else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.wbimage.setVisibility(View.GONE);
		holder.wbicon.setProgressBitmaps(ImageRel.getBitmaps_avatar(context));
		String screenName = "";
		switch (type) {
			case 0:
			case 1:
				Comment comment = (Comment) curList.get(position);

				String commentText = comment.getText();
				if (comment.getReply_comment() != null) {
					commentText = commentText
							+ "\n\n回复@"
							+ comment.getReply_comment().getUser()
									.getScreenName() + " 的评论: "
							+ comment.getReply_comment().getText();

					if (comment.getStatus() != null)
						commentText = commentText + "\n\n原微博是: @"
								+ comment.getStatus().getUser().getScreenName()
								+ ": " + comment.getStatus().getText();

					if (comment.getStatus().getRetweeted_status() != null)
						commentText = commentText
								+ "\n转自: @"
								+ comment.getStatus().getRetweeted_status()
										.getUser().getScreenName()
								+ ": "
								+ comment.getStatus().getRetweeted_status()
										.getText();

				} else if (comment.getStatus() != null) {
					commentText = commentText + "\n\n评论@"
							+ comment.getStatus().getUser().getScreenName()
							+ " 的微博: " + comment.getStatus().getText();

					if (comment.getStatus().getRetweeted_status() != null)
						commentText = commentText
								+ "\n转自: @"
								+ comment.getStatus().getRetweeted_status()
										.getUser().getScreenName()
								+ ": "
								+ comment.getStatus().getRetweeted_status()
										.getText();
				}

				if (comment.getStatus() != null) {
					if (comment.getStatus().getRetweeted_status() != null) {
						if (!TextUtils.isEmpty(comment.getStatus()
								.getRetweeted_status().getThumbnail_pic()))
							holder.wbimage.setVisibility(View.VISIBLE);
					} else if (!TextUtils.isEmpty(comment.getStatus()
							.getThumbnail_pic())) {
						holder.wbimage.setVisibility(View.VISIBLE);
					}
				}

				holder.wbtext.setText(commentText);
				holder.wbicon.setUrl(comment.getUser().getProfileImageURL().toString());
				holder.wbtime.setText(sdf.format(comment.getCreatedAt()));
				screenName = comment.getUser().getScreenName();
				break;

			case 2:
			case 3:
				Status status = (Status) curList.get(position);

				holder.wbicon.setUrl(status.getUser().getProfileImageURL().toString());

				String text = status.getText();
				if (status.getRetweeted_status() != null)
					text = text
							+ "\n\n转自: @"
							+ status.getRetweeted_status().getUser()
									.getScreenName() + " :"
							+ status.getRetweeted_status().getText();
				holder.wbtext.setText(text);
				holder.wbtime.setText(sdf.format(status.getCreatedAt()));
				screenName = status.getUser().getScreenName();

				if (status.getRetweeted_status() != null) {
					if (!TextUtils.isEmpty(status.getRetweeted_status()
							.getThumbnail_pic()))
						holder.wbimage.setVisibility(View.VISIBLE);
				} else if (!TextUtils.isEmpty(status.getThumbnail_pic()))
					holder.wbimage.setVisibility(View.VISIBLE);
				break;
		}

		if (screenName.length() > 6) {
			screenName = screenName.substring(0, 6) + "...";
		}
		holder.wbuser.setText(screenName);
		return convertView;
	}

	public void setCurList(List<WeiboResponse> curList) {
		this.curList = curList;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public void onGetMore() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Paging paging = new Paging(2);
				if (curList.size() > 0) {
					Weibo weibo = OAuthConstant.getInstance().getWeibo();
					try {
						switch (type) {
							case 0:
								paging.setMaxId(((Comment) curList.get(curList
										.size() - 1)).getId());
								curList.addAll(weibo.getCommentsToMe(paging));
								break;
								
							case 1:
								paging.setMaxId(((Comment) curList.get(curList
										.size() - 1)).getId());
								curList.addAll(weibo.getCommentsByMe(paging));
								break;
								
							case 2:
								paging.setMaxId(((Status) curList.get(curList
										.size() - 1)).getId());
								curList.addAll(weibo.getMentions(paging));
								break;
								
							case 3:
								paging.setMaxId(((Status) curList.get(curList
										.size() - 1)).getId());
								if (TextUtils.isEmpty(cid))
									curList.addAll(weibo
											.getFriendsTimeline(paging));
								else
									curList.addAll(weibo.getUserTimeline(cid,
											paging));
								break;
						}
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
				Weibo weibo = OAuthConstant.getInstance().getWeibo();
				NotificationManager notificationManager = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);
				try {
					switch (type) {
						case 0:
							List<Comment> newCommentToMe = weibo
									.getCommentsToMe();
							curList.clear();
							curList.addAll(newCommentToMe);
							notificationManager
									.cancel(MainActivity.UNREAD_COMMENT);
							weibo.resetCount(1);
							break;
							
						case 1:
							List<Comment> newCommentByMe = weibo
									.getCommentsByMe();
							curList.clear();
							curList.addAll(newCommentByMe);
							break;
							
						case 2:
							List<Status> newMentions = weibo.getMentions();
							curList.clear();
							curList.addAll(newMentions);

							notificationManager
									.cancel(MainActivity.UNREAD_MENTION);
							weibo.resetCount(2);
							break;

						case 3:
							List<Status> newTimeline;
							if (TextUtils.isEmpty(cid))
								newTimeline = weibo.getFriendsTimeline();
							else
								newTimeline = weibo.getUserTimeline(cid);
							curList.clear();
							curList.addAll(newTimeline);
							break;
					}
				} catch (WeiboException e) {
					e.printStackTrace();
				}
				autoGetMoreListView.refreshFinished();
			}
		}).start();
	}

	/**
	 * @param cid
	 *            the cid to set
	 */
	public void setCid(String cid) {
		this.cid = cid;
	}
}