package cn.com.alfred.weibo.widget;

import cn.com.alfred.weibo.R;
import cn.com.alfred.weibo.R.drawable;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class WaitingView extends LinearLayout {

	public WaitingView(Context context) {
		super(context);
		this.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		this.setGravity(Gravity.CENTER);
		this.setOrientation(LinearLayout.HORIZONTAL);
		setBackgroundResource(R.drawable.bg);
		
		ProgressBar progressBars = new ProgressBar(context, null,
				android.R.attr.progressBarStyleSmall);
		progressBars.setPadding(0, 0, 10, 0);
		progressBars.setIndeterminate(true);
		progressBars.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.progresscolor));
		this.addView(progressBars);
		
		
		TextView tv = new TextView(context);
		tv.setText("加载中...");
		tv.setTextColor( Color.BLACK);
		this.addView(tv);
	}
}
