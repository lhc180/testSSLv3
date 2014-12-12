package org.robux4.testionsslv3;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncSSLEngineConfigurator;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.koushikdutta.ion.builder.Builders;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLEngine;


public class MainActivity extends Activity {

	private final static String FEEDLY_OAUTH2_TOKEN = "At2PvX57ImkiOiJiZmRiNDVkMi01MmRiLTRkMWUtOWRhOS01OGM5YWVjYzJjMzUiLCJ1IjoiMTAxMjk2MjUyNDE1MzQ5MDM1NzgwIiwicCI6NiwiYSI6IkZlZWRseSBzYW5kYm94IGNsaWVudCIsInQiOjEzOTc3MTg4NzkwNzJ9:palabre";
	private static final String LOG_TAG = "TOPHE";

	private static final int DELAY_BETWEEN_CALLS = 200; // ms

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Ion.getDefault(this).configure().setLogging(LOG_TAG, Log.VERBOSE);

		// display the Play Services version used
		try {
			PackageInfo pI = getPackageManager().getPackageInfo("com.google.android.gms", 0);
			if (pI != null) {
				((TextView) findViewById(R.id.playServices)).setText(String.format("Play Services : %s (%d)", pI.versionName, pI.versionCode));
			}
		} catch (PackageManager.NameNotFoundException ignored) {
		}

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

		//Ion.getDefault(this).getConscryptMiddleware().enable(true);

		Ion.getDefault(this).getHttpClient().getSSLSocketMiddleware().addEngineConfigurator(new AsyncSSLEngineConfigurator() {
			private static final boolean USE_SNI = false;
			//private static final boolean REMOVE_SSLV3 = false;

			/**
			 * true  / true  : never finished
			 * true  / false : finished
			 * false / true  : finished
			 * false / false : finished
			 */

			@Override
			public void configureEngine(SSLEngine engine, String host, int port) {
				if (!engine.getClass().getCanonicalName().contains(".conscrypt.")) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							((TextView) findViewById(R.id.playServices)).setText("Play Services not used");
						}
					});
				}

				if (USE_SNI) {
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
/*
				if (REMOVE_SSLV3) {
					String[] protocols = engine.getEnabledProtocols();
					if (protocols != null && protocols.length > 1) {
						List<String> enabledProtocols = new ArrayList<String>(Arrays.asList(protocols));
						if (enabledProtocols.remove("SSLv3")) {
							protocols = enabledProtocols.toArray(new String[enabledProtocols.size()]);
							engine.setEnabledProtocols(protocols);
						}
					}
				}
*/
			}
		});
/*
		if (false)
		new Thread() {
			@Override
			public void run() {
				JsonObject params = new JsonObject();

				params.addProperty("code", "AiSmoJx7ImkiOiI5YTY1YjA3OS1mYjE5LTQ5N2EtODExOS05ZWZjNDk3ZmIzMDAiLCJ1IjoiMTA1MzUwMjc5NDQwMDU1NzgwOTM2IiwiYSI6IlBhbGFicmUiLCJwIjo2LCJ0IjoxNDE4MzgyODIzMjM0fQ");
				params.addProperty("client_id", "palabre");
				params.addProperty("client_secret", "FE01H48LRK62325VQVGYOZ24YFZL");
				params.addProperty("redirect_uri", "palabre://feedlyauth");
				params.addProperty("grant_type", "authorization_code");

				Builders.Any.F authTokenFuture = Ion.getDefault(MainActivity.this)
						.build(MainActivity.this)
						.load("https://feedly.com/v3/auth/token")
						.addHeader("X-Requested-With", "com.levelup.palabre")
						.addHeader("Content-Length", "0")
						.addHeader("User-Agent", "com.levelup.palabre/56 Ion-1.4.1+AndroidAsync-1.4.1")
						//.addHeader("Authorization", "Bearer " + FEEDLY_OAUTH2_TOKEN)
						.setJsonObjectBody(params);


				Future<Response<JsonObject>> authTokenJob = authTokenFuture.asJsonObject().withResponse();

				try {
					Log.v(LOG_TAG, "read profile data");
					final Response<JsonObject> profileResponse = authTokenJob.get();
					Log.d(LOG_TAG, "auth token result="+profileResponse.getResult()+" ex="+profileResponse.getException());
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
*/
		new Thread() {
			@Override
			public void run() {
				Builders.Any.B profileJsonFuture = Ion.getDefault(MainActivity.this)
						.build(MainActivity.this)
						.load("https://feedly.com/v3/profile")
						.addHeader("X-Requested-With", "com.levelup.palabre")
						.addHeader("Content-Length", "0")
						.addHeader("User-Agent", "com.levelup.palabre/56 Ion-1.4.1+AndroidAsync-1.4.1")
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
						.load("https://feedly.com/v3/categories")
						.addHeader("X-Requested-With", "com.levelup.palabre")
						.addHeader("Content-Length", "0")
						.addHeader("User-Agent", "com.levelup.palabre/56 Ion-1.4.1+AndroidAsync-1.4.1")
						.addHeader("Authorization", "Bearer " + FEEDLY_OAUTH2_TOKEN);

				Future<Response<JsonObject>> categoriesJsonJob = categoriesJsonFuture.asJsonObject().withResponse();

				try {
					Log.v(LOG_TAG, "read categories data");
					final Response<JsonObject> categoriesResponse = categoriesJsonJob.get();
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
