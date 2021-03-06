package com.funtl.itoken.common.utils.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * ?????? HttpClient ?????????????????????http/https?????????
 *
 * @author Lusifer
 * @version V1.0.0
 * @date 2017/9/27 21:31
 * @name HttpAsyncClientUtil
 */
public class HttpAsyncClientUtil {
	private static final Logger logger = LoggerFactory.getLogger(HttpAsyncClientUtil.class);

	/**
	 * ??????????????????????????????
	 *
	 * @param keyStorePath ???????????????
	 * @param keyStorepass ???????????????
	 * @return
	 */
	public static SSLContext custom(String keyStorePath, String keyStorepass) {
		SSLContext sc = null;
		FileInputStream instream = null;
		KeyStore trustStore = null;
		try {
			trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			instream = new FileInputStream(new File(keyStorePath));
			trustStore.load(instream, keyStorepass.toCharArray());
			// ???????????????CA???????????????????????????
			sc = SSLContexts.custom().loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
			e.printStackTrace();
		} finally {
			try {
				instream.close();
			} catch (IOException e) {
			}
		}
		return sc;
	}

	/**
	 * ????????????
	 *
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("SSLv3");

		// ????????????X509TrustManager?????????????????????????????????????????????????????????
		X509TrustManager trustManager = new X509TrustManager() {
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		sc.init(null, new TrustManager[]{trustManager}, null);
		return sc;
	}

	/**
	 * ????????????
	 *
	 * @param hostOrIP
	 * @param port
	 */
	public static HttpAsyncClientBuilder proxy(String hostOrIP, int port) {
		// ??????????????????????????????????????????????????????
		HttpHost proxy = new HttpHost(hostOrIP, port, "http");
		DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
		return HttpAsyncClients.custom().setRoutePlanner(routePlanner);
	}

	/**
	 * ????????????
	 *
	 * @param url      ????????????
	 * @param map      ????????????
	 * @param encoding ??????
	 * @param handler  ???????????????
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public static void send(String url, Map<String, String> map, final String encoding, final AsyncHandler handler) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException {

		// ???????????????????????????https??????
		SSLContext sslcontext = createIgnoreVerifySSL();

		// ????????????http???https???????????????socket?????????????????????
		Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder.<SchemeIOSessionStrategy>create().register("http", NoopIOSessionStrategy.INSTANCE).register("https", new SSLIOSessionStrategy(sslcontext)).build();
		// ??????io??????
		IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setIoThreadCount(Runtime.getRuntime().availableProcessors()).build();
		// ?????????????????????
		ConnectingIOReactor ioReactor;
		ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
		PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor, null, sessionStrategyRegistry, null);

		// ??????????????????httpclient???????????????????????????
		//				final CloseableHttpAsyncClient client = proxy("127.0.0.1", 8087).setConnectionManager(connManager).build();

		// ????????????
		final CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();

		// ??????post??????????????????
		HttpPost httpPost = new HttpPost(url);

		// ????????????
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		if (map != null) {
			for (Entry<String, String> entry : map.entrySet()) {
				nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
		}
		// ??????????????????????????????
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, encoding));

		logger.debug("???????????????{}", url);
		logger.debug("???????????????{}", nvps.toString());

		// ??????header??????
		// ??????????????????Content-type?????????User-Agent???
		httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
		httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		// Start the client
		client.start();
		// ????????????????????????????????????????????????
		client.execute(httpPost, new FutureCallback<HttpResponse>() {

			@Override
			public void failed(Exception ex) {
				handler.failed(ex);
				close(client);
			}

			@Override
			public void completed(HttpResponse resp) {
				String body = "";
				// ????????????EntityUtils.toString()?????????????????????????????????????????????????????????????????????
				try {
					HttpEntity entity = resp.getEntity();
					if (entity != null) {
						final InputStream in = entity.getContent();
						try {
							final StringBuilder sb = new StringBuilder();
							final char[] tmp = new char[1024];
							final Reader reader = new InputStreamReader(in, encoding);
							int l;
							while ((l = reader.read(tmp)) != -1) {
								sb.append(tmp, 0, l);
							}
							body = sb.toString();
						} finally {
							in.close();
							EntityUtils.consume(entity);
						}
					}
				} catch (ParseException | IOException e) {
					e.printStackTrace();
				}
				handler.completed(body);
				close(client);
			}

			@Override
			public void cancelled() {
				handler.cancelled();
				close(client);
			}
		});
	}

	/**
	 * ??????client??????
	 *
	 * @param client
	 */
	private static void close(CloseableHttpAsyncClient client) {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class AsyncHandler implements IHandler {
		@Override
		public Object failed(Exception e) {
			logger.error("{} --?????????-- {} -- {}", Thread.currentThread().getName(), e.getClass().getName(), e.getMessage());
			return null;
		}

		@Override
		public Object completed(String respBody) {
			logger.debug("{} --????????????-- {}", Thread.currentThread().getName(), respBody);
			return null;
		}

		@Override
		public Object cancelled() {
			logger.debug("{} --?????????", Thread.currentThread().getName());
			return null;
		}
	}

	/**
	 * ??????????????????
	 *
	 * @author Lusifer
	 * @version V1.0.0
	 * @date 2017/9/27 21:34
	 * @name HttpAsyncClientUtil
	 */
	public interface IHandler {

		/**
		 * ?????????????????????????????????
		 *
		 * @return
		 */
		Object failed(Exception e);

		/**
		 * ?????????????????????????????????
		 *
		 * @return
		 */
		Object completed(String respBody);

		/**
		 * ?????????????????????????????????
		 *
		 * @return
		 */
		Object cancelled();
	}

	public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, ClientProtocolException, IOException {
		AsyncHandler handler = new AsyncHandler();
		String url = "http://php.weather.sina.com.cn/iframe/index/w_cl.php";
		Map<String, String> map = new HashMap<String, String>();
		map.put("code", "js");
		map.put("day", "0");
		map.put("city", "??????");
		map.put("dfc", "1");
		map.put("charset", "utf-8");
		send(url, map, "utf-8", handler);

		System.out.println("-----------------------------------");

		map.put("city", "??????");
		send(url, map, "utf-8", handler);

		System.out.println("-----------------------------------");

	}
}
