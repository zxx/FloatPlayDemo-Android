package love.amy.player;

import io.vov.vitamio.LibsChecker;
import love.amy.player.floatplay.FloatPlayService;
import love.amy.player.floatplay.FloatPlayer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (!LibsChecker.checkVitamioLibs(this)) {
			return;
		}
	}

	public void play(View v) {
		Intent i = new Intent(this, FloatPlayService.class);
		i.putExtra("VideoSource", "rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp");
		i.putExtra("VideoSourceType", FloatPlayer.VIDEO_SOURCE_TYPE_URI);
//		i.putExtra("VideoSource", "/mnt/sdcard/bluetel/Cache/mzf.mp4");
//		i.putExtra("VideoSourceType", FloatPlayer.VIDEO_SOURCE_TYPE_PATH);
		startService(i);
	}

}
