package love.amy.player.floatplay;

import io.vov.vitamio.utils.Log;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

public class FloatPlayService extends Service{

	private static FloatPlayService instance;
	
	private FloatPlayer mFloatPlayer;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public static boolean isInitialized() {
		return instance != null;
	}
	
	public static FloatPlayService getInstance() {
		return instance;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		stop();
		mFloatPlayer = new FloatPlayer(this);
		
		String videoSource = null;
		int videoSourceType = -1;
		if(intent != null) {
			videoSource = intent.getStringExtra("VideoSource");
			videoSourceType = intent.getIntExtra("VideoSourceType", -1);
		}
		if(!TextUtils.isEmpty(videoSource) && (videoSourceType == 0 || videoSourceType == 1)) {
			mFloatPlayer.setVideoSource(videoSourceType, videoSource);
			mFloatPlayer.start();
			Log.i("", "启动了");
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void stop() {
		if(mFloatPlayer != null) {
			mFloatPlayer.stop();
		}
		mFloatPlayer = null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		instance = null;
		stop();
	}
}
