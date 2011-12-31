package cn.com.alfred.weibo.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.RemoteViews;
import android.widget.Toast;
import cn.com.alfred.weibo.R;
import cn.com.alfred.weibo.ViewActivity;
import cn.com.alfred.weibo.basicModel.AppWidgetStatus;
import cn.com.alfred.weibo.db.DBAdapter;

public class AppWidgetAnimationService extends Service {

	public static List<AppWidgetStatus> friendTimelines = new ArrayList<AppWidgetStatus>();

	private static List<Integer> mqWidgetIdList = new ArrayList<Integer>();

	public static final String ANIMATION_WIDGET_START = "WidgetAnimationService.ANIMATION_WIDGET_START";
	public static final String ANIMATION_WIDGET_DELETED = "WidgetAnimationService.ANIMATION_WIDGET_DELETED";
	public static final String ANIMATION_WIDGET_UPDATE = "WidgetAnimationService.ANIMATION_WIDGET_UPDATE";
	public static final String ANIMATION_WIDGET_SHOW = "WidgetAnimationService.ANIMATION_WIDGET_SHOW";

	private int layoutIdx = 0;
	private Handler serviceHandler;
	private static final int HANDLER_MSG_SHOW_ANIMATION = 1;

	private static int count = 0;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		friendTimelines = DBAdapter.getInstance(this).getFriendTimeline();

		try {
			serviceHandler = new Handler() {

				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					switch (msg.what) {
						case HANDLER_MSG_SHOW_ANIMATION:
							updateWidget(ANIMATION_WIDGET_UPDATE);
							break;
					}
				}
			};
			PendingIntent pending_next = PendingIntent
					.getService(
							this,
							0,
							new Intent(this, AppWidgetAnimationService.class)
									.setAction(AppWidgetAnimationService.ANIMATION_WIDGET_SHOW),
							PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			alarmManager.setRepeating(AlarmManager.RTC, 0, 6 * 1000,
					pending_next);

		} catch (Exception ex) {
			Toast.makeText(getApplicationContext(),
					"启动AgendaWidget失败! [" + ex.getMessage() + "]",
					Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		String s = intent.getAction();
		updateWidget(s);
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
	}

	public static void requestWidgetStart(int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			if (!mqWidgetIdList.contains(appWidgetId))
				mqWidgetIdList.add(appWidgetId);
		}
	}

	synchronized private void updateWidget(String action) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		if (ANIMATION_WIDGET_START.equals(action)) {
			makeAnimationWidgetViewSlide(appWidgetManager);
		} else if (ANIMATION_WIDGET_DELETED.equals(action)) {
			mqWidgetIdList.clear();
			this.stopSelf();
		} else if (ANIMATION_WIDGET_UPDATE.equals(action)) {
			makeAnimationWidgetViewSlide(appWidgetManager);
		} else if (ANIMATION_WIDGET_SHOW.equals(action)) {
			serviceHandler.removeMessages(HANDLER_MSG_SHOW_ANIMATION);
			serviceHandler.sendEmptyMessage(HANDLER_MSG_SHOW_ANIMATION);
		}
	}

	private RemoteViews buildWidgetUpdate() {

		RemoteViews RViews;

		if (layoutIdx == 0) {
			RViews = new RemoteViews(this.getPackageName(), R.layout.layout_a);
			layoutIdx = 1;
		} else {
			RViews = new RemoteViews(this.getPackageName(), R.layout.layout_b);
			layoutIdx = 0;
		}

		PendingIntent pending_next = PendingIntent
				.getService(
						this,
						0,
						new Intent(this, AppWidgetAnimationService.class)
								.setAction(AppWidgetAnimationService.ANIMATION_WIDGET_SHOW),
						PendingIntent.FLAG_UPDATE_CURRENT);

		RViews.setOnClickPendingIntent(R.id.btn_down, pending_next);

		if (friendTimelines.size() != 0) {
			Intent intent = new Intent(this, ViewActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra(
					"cid",
					Long.valueOf(friendTimelines.get(
							(count + 1) % friendTimelines.size()).getId()));
			PendingIntent pending_list = PendingIntent.getActivity(this, 0,
					intent, PendingIntent.FLAG_UPDATE_CURRENT);
			RViews.setOnClickPendingIntent(R.id.FrameLayout, pending_list);
		}

		return RViews;
	}

	synchronized private void makeAnimationWidgetViewSlide(
			AppWidgetManager appWidgetManager) {
		RemoteViews updateViews = buildWidgetUpdate();
		Bitmap in_bmp, out_bmp;

		out_bmp = makeBitmap(count);
		count++;
		in_bmp = makeBitmap(count);

		updateViews.setImageViewBitmap(R.id.Move_InImage, in_bmp);
		updateViews.setImageViewBitmap(R.id.Move_OutImage, out_bmp);

		for (int appWidgetId : mqWidgetIdList) {
			appWidgetManager.updateAppWidget(appWidgetId, updateViews);
		}
	}

	private Bitmap makeBitmap(int idx) {
		Bitmap bmp;

		bmp = Bitmap.createBitmap(270, 60, Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);
		Paint brush = new Paint(Paint.ANTI_ALIAS_FLAG);
		brush.setStyle(Paint.Style.FILL_AND_STROKE);
		brush.setTextSize(15);
		brush.setColor((idx % 2) == 0 ? Color.RED : Color.BLUE);
		if (friendTimelines.size() == 0)
			canvas.drawText("没有微博，请登录刷新", 80, 40, brush);
		else {
			canvas.drawText(friendTimelines.get(count % friendTimelines.size())
					.getScreenName(), 50, 12, brush);
			String text = friendTimelines.get(count % friendTimelines.size())
					.getTimelineText();
			canvas.drawText(
					text.substring(0, 12 < text.length() ? 12 : text.length()),
					50, 30, brush);
			if (12 < text.length()) {
				text = text.substring(12);
				canvas.drawText(text, 50, 48, brush);
			}
		}
		canvas.drawBitmap(
				Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
						this.getResources(), R.drawable.loading), 30, 30, true),
				15, 15, null);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();

		return bmp;
	}
}
