package cn.com.alfred.weibo.OAuth;

import cn.com.alfred.weibo.basicModel.Weibo;
import cn.com.alfred.weibo.http.AccessToken;
import cn.com.alfred.weibo.http.RequestToken;

/**
 * 一些OAuth认证常用的变量
 * 
 * @author alfredtofu
 *
 */
public class OAuthConstant {

	private static Weibo weibo = null;
	private static OAuthConstant instance = null;
	private RequestToken requestToken;
	private AccessToken accessToken;
	private String token;
	private String tokenSecret;

	private OAuthConstant() {
	};

	public static synchronized OAuthConstant getInstance() {
		if (instance == null)
			instance = new OAuthConstant();
		return instance;
	}

	public Weibo getWeibo() {
		if (weibo == null)
			weibo = new Weibo();
		return weibo;
	}

	public AccessToken getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(AccessToken accessToken) {
		this.accessToken = accessToken;
		this.token = accessToken.getToken();
		this.tokenSecret = accessToken.getTokenSecret();
		if (weibo == null)
			weibo = new Weibo();

		weibo.setToken(accessToken);
	}

	public RequestToken getRequestToken() {
		return requestToken;
	}

	public void setRequestToken(RequestToken requestToken) {
		this.requestToken = requestToken;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTokenSecret() {
		return tokenSecret;
	}

	public void setTokenSecret(String tokenSecret) {
		this.tokenSecret = tokenSecret;
	}

	public static void initData() {
		instance = null;
		weibo = null;
	}

}
