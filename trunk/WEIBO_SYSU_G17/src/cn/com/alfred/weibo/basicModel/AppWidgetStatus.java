package cn.com.alfred.weibo.basicModel;

public class AppWidgetStatus {

	String id;
	String screenName;
	String timelineText;
	/**
	 * @param id
	 * @param screenName
	 * @param timelineText
	 */
	public AppWidgetStatus(String id, String screenName, String timelineText) {
		super();
		this.id = id;
		this.screenName = screenName;
		this.timelineText = timelineText;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @return the screenName
	 */
	public String getScreenName() {
		return screenName;
	}
	
	/**
	 * @return the timelineText
	 */
	public String getTimelineText() {
		return timelineText;
	}
	
	
}
