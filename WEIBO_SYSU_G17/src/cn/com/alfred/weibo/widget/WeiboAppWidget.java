package cn.com.alfred.weibo.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class WeiboAppWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(
					context, WeiboAppWidget.class));
		}
		AppWidgetAnimationService.requestWidgetStart(appWidgetIds);

		Intent intent = new Intent(context, AppWidgetAnimationService.class)
				.setAction(AppWidgetAnimationService.ANIMATION_WIDGET_START);
		context.startService(intent);
	}

	@Override
	public void onDisabled(Context context) {
		Intent intent = new Intent(context, AppWidgetAnimationService.class)
				.setAction(AppWidgetAnimationService.ANIMATION_WIDGET_DELETED);
		context.startService(intent);
	}

	@Override
	public void onEnabled(Context context) {
	}

}