package org.robux4.testionsslv3;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.koushikdutta.async.http.AsyncSSLEngineConfigurator;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLEngine;


public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Ion.getDefault(this).getConscryptMiddleware().enable(false);

		if (false) {
			// disable SSLv3 unless that's the only protocol the engine can handle
			Ion.getDefault(this).getHttpClient().getSSLSocketMiddleware().addEngineConfigurator(new AsyncSSLEngineConfigurator() {
				@Override
				public void configureEngine(SSLEngine engine, String host, int port) {
					String[] protocols = engine.getEnabledProtocols();
					if (protocols != null && protocols.length > 1) {
						List<String> enabledProtocols = new ArrayList<String>(Arrays.asList(protocols));
						if (enabledProtocols.remove("SSLv3")) {
							protocols = enabledProtocols.toArray(new String[enabledProtocols.size()]);
							engine.setEnabledProtocols(protocols);
						}
					}
				}
			});
		}

		Ion.with((ImageView) findViewById(R.id.imageView))
				.load("http://cdn2.vox-cdn.com/thumbor/KxtZNw37jKNfxdA0hX5edHvbTBE=/0x0:2039x1359/800x536/cdn0.vox-cdn.com/uploads/chorus_image/image/44254028/lg-g-watch.0.0.jpg");

		Ion.with((ImageView) findViewById(R.id.imageViewSSL))
				.load("https://cdn2.vox-cdn.com/thumbor/KxtZNw37jKNfxdA0hX5edHvbTBE=/0x0:2039x1359/800x536/cdn0.vox-cdn.com/uploads/chorus_image/image/44254028/lg-g-watch.0.0.jpg");
	}
}
