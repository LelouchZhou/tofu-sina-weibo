package cn.com.alfred.weibo.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import cn.com.alfred.weibo.R;

/**
 * 自己实现WebView，供OAuth认证时打开Url
 */
public class WebViewActivity extends Activity {

	private WebView webView;

	public static WebViewActivity webInstance = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.web);
		setTitle("授权认证");

		webInstance = this;
		webView = (WebView) findViewById(R.id.web);
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSaveFormData(true);
		webSettings.setSavePassword(true);
		webSettings.setSupportZoom(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

		webView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				webView.requestFocus();
				return false;
			}
		});

		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.containsKey("url")) {
			webView.loadUrl(bundle.getString("url"));
			webView.setWebChromeClient(new WebChromeClient() {

				public void onProgressChanged(WebView view, int progress) {
					setTitle("加载中..." + progress + "%");
					setProgress(progress * 100);

					if (progress == 100)
						setTitle(R.string.app_name);
				}
			});
		}
	}

	/**
	 * 监听BACK键
	 * 
	 * @param keyCode
	 * @param event
	 * @return
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}