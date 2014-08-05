package love.amy.player.floatplay;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.widget.VideoView;

import java.lang.reflect.Field;

import love.amy.player.R;
import android.content.Context;
import android.net.Uri;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 悬浮窗口
 * 
 * <!-- 悬浮窗口 所需权限 -->
 * <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />  
 * 
 * <!-- Vitamio所需权限 -->
 * <uses-permission android:name="android.permission.WAKE_LOCK" />
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
 *
 * <!-- Vitamio初始化必需 -->
 * <activity
 * android:name="io.vov.vitamio.activity.InitActivity"
 * android:configChanges="orientation|screenSize|smallestScreenSize|keyboard|keyboardHidden|navigation"
 * android:launchMode="singleTop"
 * android:theme="@android:style/Theme.NoTitleBar"
 * android:windowSoftInputMode="stateAlwaysHidden" />
 * 
 */
public class FloatPlayer implements OnInfoListener, OnBufferingUpdateListener, OnCompletionListener, OnErrorListener, OnPreparedListener, OnTouchListener, OnScaleGestureListener, OnClickListener{

	@SuppressWarnings("unused")
	private static final String TAG = "FloatPlayer";
	
	public static final int VIDEO_SOURCE_TYPE_PATH = 0;
	public static final int VIDEO_SOURCE_TYPE_URI = 1;
	private static final int VIDEO_SOURCE_TYPE_NONE = -1;
	
	private FloatPlayService mFloatPlayService;
	
	// WindowManager
	private WindowManager mWM;
	private LayoutParams mWindowParams;
	
	// Dimension
	private Display mDisplay;
	private int mStatusBarHeight = 0;
	
	// View
	private View rootView;
	private VideoView mVideoView;
	private TextView mDownloadRate;
	private TextView mLoadRate;
	private ProgressBar mProgressBar;

	// Vitamio
	private String mVideoSource;
	private int mVideoSourceType = VIDEO_SOURCE_TYPE_NONE;

	// Touch
	private ScaleGestureDetector mScaleGestureDetector;
	private int rawX;
	private int rawY;
	private int x;
	private int y;
	
	// State
	private boolean isPlayerStopped;

	public FloatPlayer(FloatPlayService floatPlayService) {
		this.mFloatPlayService = floatPlayService;
		this.mStatusBarHeight = getStatusBarHeight();
		
		initWindowManager();
		initView();
	}
	
	@SuppressWarnings({ "deprecation", "unused" })
	private void initWindowManager() {
		mWM = (WindowManager) mFloatPlayService.getSystemService(Context.WINDOW_SERVICE);
		
		mWindowParams = new LayoutParams();
		mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
		mWindowParams.flags = 680;
		mWindowParams.type = LayoutParams.TYPE_SYSTEM_ALERT;

		mDisplay = mWM.getDefaultDisplay();
		int screenWidth = mDisplay.getWidth();
		int screenHeight = mDisplay.getHeight();
		
		mWindowParams.width = screenWidth;
		mWindowParams.height = screenWidth * 3 / 4;
		
		mWindowParams.x = 0;
		mWindowParams.y = mStatusBarHeight; 
	}
	
	private void initView() {
		LayoutInflater inflater = (LayoutInflater) mFloatPlayService.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		rootView = inflater.inflate(R.layout.activity_vitamio, null);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.vitamio_probar);
		mLoadRate = (TextView) rootView.findViewById(R.id.vitamio_load_rate);			// 加载进度
		mDownloadRate = (TextView) rootView.findViewById(R.id.vitamio_download_rate);	// 下载速度
		mVideoView = (VideoView) rootView.findViewById(R.id.vitamio_videoview);
		rootView.findViewById(R.id.vitamio_switch_to_full).setOnClickListener(this);
		rootView.findViewById(R.id.vitamio_close).setOnClickListener(this);
		
		rootView.setOnTouchListener(this);
		mScaleGestureDetector = new ScaleGestureDetector(mFloatPlayService, this);
		// 没有这个画面就会透明, 不清晰
		mVideoView.setZOrderOnTop(true);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.vitamio_close:
			stop();
			break;
		case R.id.vitamio_switch_to_full:
			// TODO 跳转到VideoPlayerActivity
			break;
		}
	}
	
	/**
	 * 状态栏高度
	 * @return  
	 */
	private int getStatusBarHeight() {  
	    int statusBarHeight = 0;  
	    try {  
	        Class<?> c = Class.forName("com.android.internal.R$dimen");  
	        Object obj = c.newInstance();  
	        Field field = c.getField("status_bar_height");  
	        int id = Integer.parseInt(field.get(obj).toString());  
	        statusBarHeight = mFloatPlayService.getResources().getDimensionPixelSize(id);  
	    } catch (Exception e) {  
	        e.printStackTrace();  
	    }  
	    return statusBarHeight; 
	}
	
	/**
	 * 矫正位置, 使得画面不出边界
	 * @param leftX
	 * @param leftY
	 */
	@SuppressWarnings("deprecation")
	private void setCorrectXY(int leftX, int leftY) {
		// 当前 View 尺寸
    	int width = mWindowParams.width;
    	int heigth = mWindowParams.height;
    	// 屏幕尺寸
    	int screenWidth = mDisplay.getWidth();
    	int screenHeigth = mDisplay.getHeight();// - getStatusBarHeight();	// 去掉标题栏后的高度
    	// 矫正，使不出边界
    	leftX = (leftX + width) > screenWidth ? (screenWidth - width) : leftX;
    	leftY = (leftY +heigth) > screenHeigth ? (screenHeigth - heigth) : leftY;	
    	
    	leftX = leftX < 0 ? 0 : leftX;
    	leftY = leftY < 0 ? 0 : leftY;
    	
    	mWindowParams.x = leftX;
    	mWindowParams.y = leftY;
	}
	
	/**
	 * 更新位置
	 */
	private void updatePosition() {
		// 新位置
    	int leftX = rawX - x;
    	int leftY = rawY - y;
    	
    	setCorrectXY(leftX, leftY);
    	
    	mWM.updateViewLayout(rootView, mWindowParams);
	}
	
	/**
	 * 重新调整窗口大小
	 * @param scaleFactor
	 */
	private void resizeWindow(float scaleFactor) {
		// 当前显示宽度
		int currentWidth = mWindowParams.width;

		// 缩放后的宽度
		int scaledWidth = (int) (scaleFactor * currentWidth);

		@SuppressWarnings("deprecation")
		int maxWidth = mDisplay.getWidth();

		if (scaledWidth > maxWidth) {
			scaledWidth = maxWidth;
		} else if (scaledWidth < 200) {
			scaledWidth = 200;
		}
		// 视频宽高比
		int scaledHeight = (int) (scaledWidth * 3 / 4);

		// 中心点不变
		int centerX = mWindowParams.x + mWindowParams.width / 2;
		int centerY = mWindowParams.y + mWindowParams.height / 2;

		// 计算左上角
		int leftX = centerX - scaledWidth / 2;
		int leftY = centerY - scaledHeight / 2;

		mWindowParams.width = scaledWidth;
		mWindowParams.height = scaledHeight;

		// 矫正，使之不出边界
		setCorrectXY(leftX, leftY);
		
		android.view.ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
		lp.width = scaledWidth;
		lp.height = scaledHeight;
		
		mWM.updateViewLayout(rootView, mWindowParams);
	}
	
	// ---------------------------------
	// Vitamio 回调
	// ---------------------------------
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.setPlaybackSpeed(1.0f);
		mp.setBufferSize(10 * 1024); // byte
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		mLoadRate.setText(percent + "%");
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
	    switch (what) {
	    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
	      if (mVideoView.isPlaying()) {
	        mVideoView.pause();
	        mProgressBar.setVisibility(View.VISIBLE);
	        mDownloadRate.setText("");
	        mLoadRate.setText("");
	        mDownloadRate.setVisibility(View.VISIBLE);
	        mLoadRate.setVisibility(View.VISIBLE);
	      }
	      break;
	    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
	      mVideoView.start();
	      mProgressBar.setVisibility(View.GONE);
	      mDownloadRate.setVisibility(View.GONE);
	      mLoadRate.setVisibility(View.GONE);
	      // 画面第一次上下边界会花屏 TODO
	      resizeWindow(1);
	      break;
	    case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
	      mDownloadRate.setText("" + extra + "kb/s" + "  ");
	      break;
	    }
		return true;
	}
	
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// 未测试
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// 未测试
		mp.release();
		stop();
	}

	// -------------------------------
	// 移动 
	// -------------------------------
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mScaleGestureDetector.onTouchEvent(event);
		if(event.getPointerCount() > 1) {
			return false;
		}
		// 以屏幕左上角为原点
    	rawX = (int) event.getRawX();
    	rawY = (int) (event.getRawY()) - getStatusBarHeight();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// 以 View 左上角为原点
			x = (int) event.getX();
			y = (int) event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			updatePosition();
			break;
		case MotionEvent.ACTION_UP:
			x = y = rawX = rawY = 0;
			break;
		}
		return false;
	}

	// --------------------------------------
	// 缩放 
	// --------------------------------------
	
	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		float scaleFactor = detector.getScaleFactor();
		resizeWindow(scaleFactor);
		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
	}
	
	// -------------------------------
	// 公有方法
	// -------------------------------
	
	/**
	 * 设置视频源
	 * @param videoSourceType 地址类型
	 * @param videoSource 视频地址
	 * @see FloatPlayer#VIDEO_SOURCE_TYPE_PATH
	 * @see FloatPlayer#VIDEO_SOURCE_TYPE_URI
	 * @see VideoView#setVideoURI(Uri)
	 * @see VideoView#setVideoPath(String)
	 */
	public void setVideoSource(int videoSourceType, String videoSource) {
		mVideoSource = videoSource;
		switch (videoSourceType) {
		case VIDEO_SOURCE_TYPE_PATH:
			mVideoSourceType = VIDEO_SOURCE_TYPE_PATH;
			break;
		case VIDEO_SOURCE_TYPE_URI:
			mVideoSourceType = VIDEO_SOURCE_TYPE_URI;
			break;
		default:
			throw new IllegalArgumentException("视频源类型错误");
		}
	}
	
	public String getVideoSource() {
		return mVideoSource;
	}
	public int getVideoSourceType() {
		return mVideoSourceType;
	}
	
	/**
	 * 开始播放
	 */
	public void start() {
		// 加载到窗口
		mWM.addView(rootView, mWindowParams);
				
		switch (mVideoSourceType) {
		case VIDEO_SOURCE_TYPE_PATH:
			mVideoView.setVideoPath(mVideoSource);
			break;
		case VIDEO_SOURCE_TYPE_URI:
			mVideoView.setVideoURI(Uri.parse(mVideoSource));
			break;
		default:
			throw new NullPointerException("还没设置视频地址");
		}
		
		mVideoView.setMediaController(null);
		mVideoView.requestFocus();

		// 监听
		mVideoView.setOnInfoListener(this);
		mVideoView.setOnBufferingUpdateListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnPreparedListener(this);
		
		isPlayerStopped = false;
	}
	
	/**
	 * 停止播放
	 */
	public void stop() {
		if(isPlayerStopped) {
			return;
		}
		isPlayerStopped = true;
		// 从窗口中删除
		mWM.removeView(rootView);
		
		mVideoView.stopPlayback();
		mVideoView = null;
		mFloatPlayService.stopSelf();
	}
	
	/**
	 * 暂停播放
	 */
	public void pause() {
		mVideoView.pause();
	}
	
	/**
	 * 恢复播放
	 */
	public void resume() {
		mVideoView.resume();
	}
}

