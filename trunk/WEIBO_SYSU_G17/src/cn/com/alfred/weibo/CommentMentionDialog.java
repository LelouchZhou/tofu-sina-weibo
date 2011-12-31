package cn.com.alfred.weibo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import cn.com.alfred.weibo.OAuth.OAuthConstant;
import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.basicModel.WeiboException;

public class CommentMentionDialog {

	private CheckBox checkBox;
	private EditText editText;

	public CommentMentionDialog(final Context context, final boolean isComment,
			final long weiboID, final String cid) {

		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog, null);

		new AlertDialog.Builder(context)
				.setView(layout)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							Weibo weibo = OAuthConstant.getInstance()
									.getWeibo();
							String text = editText.getText().toString();
							String msg = "";
							if (TextUtils.isEmpty(text)) {
								Toast.makeText(context, "说点什么吧",
										Toast.LENGTH_LONG).show();
								return;
							}
							if (text.length() > 140) {
								Toast.makeText(context, "要评论的内容太长了",
										Toast.LENGTH_LONG).show();
								return;
							}
							if (isComment) {
								weibo.updateComment(text, weiboID + "", cid);
								msg = "评论成功";
								if (checkBox.isChecked()) {
									weibo.updateStatus(text, weiboID);
									msg = "评论且转发成功";
								}
							} else {
								weibo.updateStatus(text, weiboID);
								msg = "转发成功";
								if (checkBox.isChecked()) {
									weibo.updateComment(text, weiboID + "", cid);
									msg = "评论且转发成功";
								}
							}
							Toast.makeText(context, msg, Toast.LENGTH_LONG)
									.show();
						} catch (WeiboException e) {
							e.printStackTrace();
							Toast.makeText(context, "评论或转发失败",
									Toast.LENGTH_LONG).show();
						}
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).setTitle(isComment ? "评论微博" : "转发微博").show();

		checkBox = (CheckBox) layout.findViewById(R.id.dialog_cb);
		editText = (EditText) layout.findViewById(R.id.dialog_et);
	}
}
