package cn.com.alfred.weibo.db;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import cn.com.alfred.weibo.basicModel.AppWidgetStatus;
import cn.com.alfred.weibo.basicModel.Status;
import cn.com.alfred.weibo.basicModel.User;
import cn.com.alfred.weibo.basicModel.WeiboResponse;
import cn.com.alfred.weibo.http.AccessToken;
import cn.com.alfred.weibo.widget.AppWidgetAnimationService;

/**
 * 
 * @author alfredtofu
 * 
 */
public class DBAdapter {

	public static final String DATABASE_NAME = "weibo.db";
	public static final int DATABASE_VERSION = 1;
	public static final String[] TABLE_NAME = new String[] { "User_Token",
			"User_Info", "Friend_TimeLine" };
	public static final String[] TABLE_CREATE_SQL = new String[] {
			"CREATE TABLE User_Token (userID long primary key," + "token text,"
					+ "tokenSecret text," + "screenName text);",
			"CREATE TABLE User_Info (userID long primary key," + "name text,"
					+ "screenName text," + "location text,"
					+ "description text," + "url text);",
			"CREATE TABLE Friend_TimeLine (_id text, screenName text, "
					+ "timelineText text);" };

	public static boolean hasModify = false;

	private Context context;
	private MyDBHelper dbHelper;
	private SQLiteDatabase db = null;
	private static DBAdapter dbAdapter = null;

	public static synchronized DBAdapter getInstance(Context context) {
		if (dbAdapter == null)
			dbAdapter = new DBAdapter(context);
		return dbAdapter;
	}

	private DBAdapter(Context context) {
		this.context = context;
		dbHelper = new MyDBHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
		// dbHelper.onUpgrade(dbHelper.getWritableDatabase(), 1, 1);
	}

	/**
	 * 保存指定的accessToken
	 * 
	 * @param accessToken
	 */
	public void saveUserToken(AccessToken accessToken) {
		db = dbHelper.getWritableDatabase();
		try {
			db.execSQL(
					"insert into User_Token values(?, ?, ?, ?);",
					new String[] { accessToken.getUserId() + "",
							accessToken.getToken(),
							accessToken.getTokenSecret(),
							accessToken.getScreenName() });
		} catch (Exception e) {
			this.updateUserToken(accessToken);
		}
		db.close();
	}

	/**
	 * 更新指定的accessToken
	 * 
	 * @param accessToken
	 */
	public void updateUserToken(AccessToken accessToken) {
		if (db != null || db.isOpen())
			db.close();
		db = dbHelper.getWritableDatabase();
		db.execSQL(
				"update User_Token set token = ?, tokenSecret = ?, screenName = ? where userID = ?;",
				new String[] { accessToken.getToken(),
						accessToken.getTokenSecret(),
						accessToken.getScreenName(),
						accessToken.getUserId() + "" });
		db.close();
	}

	/**
	 * 保存用户资料
	 * 
	 * @param user
	 */
	public void saveUser(User user) {
		db = dbHelper.getWritableDatabase();
		db.execSQL(
				"insert into User_Token values(?, ?, ?, ?, ?, ?);",
				new String[] { user.getId() + "", user.getName(),
						user.getScreenName(), user.getLocation(),
						user.getDescription(), user.getURL().toString() });
		db.close();
	}

	/**
	 * 删除指定的accessToken
	 * 
	 * @param accessToken
	 */
	public void deleteUserToken(AccessToken accessToken) {
		db = dbHelper.getWritableDatabase();
		try {
			db.execSQL("delete from User_Token where userID = ?",
					new String[] { accessToken.getUserId() + "" });
		} catch (Exception e) {
			e.printStackTrace();
		}
		db.close();
	}

	/**
	 * 保存当前用户前20条friendsTimeline
	 * 
	 * @param friendsTimeline
	 */
	public void saveFriendTimeline(List<WeiboResponse> friendsTimeline) {
		db = dbHelper.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME[2]);
		db.execSQL(TABLE_CREATE_SQL[2]);
		for (int i = 0; i < 20 && i < friendsTimeline.size(); i++) {
			Status status = (Status) friendsTimeline.get(i);
			String id = status.getId() + "";
			String screenName = status.getUser().getScreenName();
			String timelineText = status.getText();

			db.execSQL("insert into " + TABLE_NAME[2] + " values(?, ?, ?)",
					new String[] { id, screenName, timelineText });
		}
		db.close();
	}

	/**
	 * 获得数据库中所有的friendsTimeline
	 * 
	 * @return
	 */
	public List<AppWidgetStatus> getFriendTimeline() {
		db = dbHelper.getReadableDatabase();
		List<AppWidgetStatus> friendTimeline = new ArrayList<AppWidgetStatus>();
		Cursor cursor = db.rawQuery("select * from " + TABLE_NAME[2],
				new String[] {});
		AppWidgetAnimationService.friendTimelines.clear();
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			String id = cursor.getString(0);
			String screenName = cursor.getString(1);
			String timelineText = cursor.getString(2);
			friendTimeline
					.add(new AppWidgetStatus(id, screenName, timelineText));
		}
		cursor.close();
		db.close();
		return friendTimeline;
	}

	/**
	 * 获得所有在此机登录过的用户的accessToken
	 * 
	 * @return
	 */
	public ArrayList<AccessToken> getAllUsersAccessToken() {
		ArrayList<AccessToken> accessTokens = new ArrayList<AccessToken>();
		db = dbHelper.getReadableDatabase();
		Cursor cursor = db
				.rawQuery("select * from User_Token", new String[] {});
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			long userId = Long.valueOf(cursor.getString(0));
			String token = cursor.getString(1);
			String tokenSecret = cursor.getString(2);
			String screenName = cursor.getString(3);
			AccessToken accessToken = new AccessToken(token, tokenSecret,
					screenName, userId);
			accessTokens.add(accessToken);
		}
		cursor.close();
		db.close();
		return accessTokens;
	}

	private class MyDBHelper extends SQLiteOpenHelper {

		public MyDBHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			for (int i = 0; i < TABLE_CREATE_SQL.length; i++) {
				db.execSQL(TABLE_CREATE_SQL[i]);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			for (int i = 0; i < TABLE_NAME.length; i++) {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME[i]);
			}
			this.onCreate(db);
		}

	}

}
