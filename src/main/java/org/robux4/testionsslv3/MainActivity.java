package org.robux4.testionsslv3;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClientMiddleware;
import com.koushikdutta.async.http.AsyncSSLEngineConfigurator;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.koushikdutta.ion.builder.Builders;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLEngine;


public class MainActivity extends Activity {

	private static final boolean FORCE_USE_SNI = true; // dual call fails when we for Conscrypt to use SNI
	private static final boolean DISABLE_SSLV3 = false;
	private static final boolean DEBUG_ION = false;
	private static final int DELAY_BETWEEN_CALLS = 200; // ms

	private final static String FEEDLY_DOMAIN = "sandbox.feedly.com";
	private final static String FEEDLY_OAUTH2_TOKEN = "ArxsOch7ImEiOiJGZWVkbHkgc2FuZGJveCBjbGllbnQiLCJlIjoxNDE5MjQwNzEwNDI3LCJpIjoiZTkxMTFlOTAtN2Y1Mi00MTNiLThiZTYtYzc1OTBjMWZjZGYyIiwicCI6NiwidCI6MSwidiI6InNhbmRib3giLCJ3IjoiMjAxNC4yOCIsIngiOiJzdGFuZGFyZCJ9:sandbox";
	private static final String LOG_TAG = "TOPHE";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (DEBUG_ION) {
			Ion.getDefault(this).configure().setLogging(LOG_TAG, Log.VERBOSE);
		}

		// display the Play Services version used
		try {
			PackageInfo pI = getPackageManager().getPackageInfo("com.google.android.gms", 0);
			if (pI != null) {
				((TextView) findViewById(R.id.playServices)).setText(String.format("Play Services : %s (%d)", pI.versionName, pI.versionCode));
			}
		} catch (PackageManager.NameNotFoundException ignored) {
		}

		Ion.getDefault(this).getConscryptMiddleware().enable(false);

		Ion.getDefault(this).getHttpClient().getSSLSocketMiddleware().addEngineConfigurator(new AsyncSSLEngineConfigurator() {
			@Override
			public void configureEngine(SSLEngine engine, AsyncHttpClientMiddleware.GetSocketData data, String host, int port) {
				if (DISABLE_SSLV3) {
					String[] protocols = engine.getEnabledProtocols();
					if (protocols != null && protocols.length > 1) {
						List<String> enabledProtocols = new ArrayList<String>(Arrays.asList(protocols));
						if (enabledProtocols.remove("SSLv3")) {
							protocols = enabledProtocols.toArray(new String[enabledProtocols.size()]);
							engine.setEnabledProtocols(protocols);
						}
					}
				}

				if (!engine.getClass().getCanonicalName().contains(".conscrypt.")) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							((TextView) findViewById(R.id.playServices)).setText("Play Services not used");
						}
					});
				}

				if (FORCE_USE_SNI) {
					// temporary fix for https://github.com/koush/ion/issues/428
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
					} catch (Exception e) {
						Log.i(LOG_TAG, "Failed to set the flags in " + engine, e);
					}
				}
			}
		});

		// install Conscrypt ourselves as 1.4.1 doesn't set it right
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

		new Thread() {
			@Override
			public void run() {
				Builders.Any.B profileJsonFuture = Ion.getDefault(MainActivity.this)
						.build(MainActivity.this)
						.load("https://" + FEEDLY_DOMAIN + "/v3/profile")
						.addHeader("Authorization", "Bearer " + FEEDLY_OAUTH2_TOKEN);

				Future<Response<JsonObject>> profileJsonJob = profileJsonFuture.asJsonObject().withResponse();

				try {
					Log.v(LOG_TAG, "read profile data");
					final Response<JsonObject> profileResponse = profileJsonJob.get();
					Log.d(LOG_TAG, "profile result="+profileResponse.getResult()+" ex="+profileResponse.getException());
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (profileResponse.getResult() != null) {
								((TextView) findViewById(R.id.profile)).setText(String.valueOf(profileResponse.getResult()));
							} else {
								((TextView) findViewById(R.id.profile)).setText(String.valueOf(profileResponse.getException()));
							}
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.start();

		new Thread() {
			@Override
			public void run() {
				try {
					sleep(DELAY_BETWEEN_CALLS);
				} catch (InterruptedException e) {
				}

				Builders.Any.B categoriesJsonFuture = Ion.getDefault(MainActivity.this)
						.build(MainActivity.this)
						.load("https://" + FEEDLY_DOMAIN + "/v3/categories")
						.addHeader("Authorization", "Bearer " + FEEDLY_OAUTH2_TOKEN);

				Future<Response<JsonArray>> categoriesJsonJob = categoriesJsonFuture.asJsonArray().withResponse();

				try {
					Log.v(LOG_TAG, "read categories data");
					final Response<JsonArray> categoriesResponse = categoriesJsonJob.get();
					Log.d(LOG_TAG, "categories result="+categoriesResponse.getResult()+" ex="+categoriesResponse.getException());
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (categoriesResponse.getResult() != null) {
								((TextView) findViewById(R.id.categories)).setText(String.valueOf(categoriesResponse.getResult()));
							} else {
								((TextView) findViewById(R.id.categories)).setText(String.valueOf(categoriesResponse.getException()));
							}
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
