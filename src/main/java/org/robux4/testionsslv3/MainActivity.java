package org.robux4.testionsslv3;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.koushikdutta.async.http.AsyncSSLEngineConfigurator;
import com.koushikdutta.ion.Ion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;


public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Ion.getDefault(this).getConscryptMiddleware().enable(false);

		if (true) {
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
/*
					SSLParameters sslp = engine.getSSLParameters();
					try {
						//Field sslParameters = engine.getClass().getDeclaredField("sslParameters");
						Field useSni = sslp.getClass().getDeclaredField("useSni");
						Field peerHost = engine.getClass().getSuperclass().getDeclaredField("peerHost");
						Field peerPort = engine.getClass().getSuperclass().getDeclaredField("peerPort");

						peerHost.setAccessible(true);
						peerPort.setAccessible(true);
						//sslParameters.setAccessible(true);
						useSni.setAccessible(true);

						//Object sslp = sslParameters.get(engine);

						peerHost.set(engine, host);
						peerPort.set(engine, port);
						useSni.set(sslp, true);

						engine.setSSLParameters(sslp);
					} catch (Exception e) {
					}
*/
				}
			});
		}
/*
		Ion.getDefault(this).getHttpClient().getSSLSocketMiddleware().addEngineConfigurator(new AsyncSSLEngineConfigurator() {
			@Override
			public void configureEngine(SSLEngine engine, String host, int port) {
				try {
					Field sslParameters = engine.getClass().getDeclaredField("sslParameters");
					Field useSni = sslParameters.getType().getDeclaredField("useSni");
					Field peerHost = engine.getClass().getSuperclass().getDeclaredField("peerHost");
					Field peerPort = engine.getClass().getSuperclass().getDeclaredField("peerPort");

					peerHost.setAccessible(true);
					peerPort.setAccessible(true);
					sslParameters.setAccessible(true);
					useSni.setAccessible(true);

					Object sslp = sslParameters.get(engine);

					peerHost.set(engine, host);
					peerPort.set(engine, port);
					useSni.set(sslp, true);

				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
*/
		try {
			Class<?> providerInstaller = Class.forName("com.google.android.gms.security.ProviderInstaller");
			Method mInsertProvider = providerInstaller.getDeclaredMethod("installIfNeeded", Context.class);
			mInsertProvider.invoke(null, this);

		} catch (Throwable ignored) {
			try {
				Context gms = createPackageContext("com.google.android.gms", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
				Class clazz = gms.getClassLoader().loadClass("com.google.android.gms.common.security.ProviderInstallerImpl");
				Method mInsertProvider = clazz.getDeclaredMethod("insertProvider", Context.class);
				mInsertProvider.invoke(null, this);
			} catch (Throwable e) {
			}
		}

		Ion.with((ImageView) findViewById(R.id.imageView))
				.load("http://cdn2.vox-cdn.com/thumbor/KxtZNw37jKNfxdA0hX5edHvbTBE=/0x0:2039x1359/800x536/cdn0.vox-cdn.com/uploads/chorus_image/image/44254028/lg-g-watch.0.0.jpg");

		Ion.with((ImageView) findViewById(R.id.imageViewSSL))
				.load("https://cdn2.vox-cdn.com/thumbor/KxtZNw37jKNfxdA0hX5edHvbTBE=/0x0:2039x1359/800x536/cdn0.vox-cdn.com/uploads/chorus_image/image/44254028/lg-g-watch.0.0.jpg");
	}
}
