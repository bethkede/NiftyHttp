package com.nifty.http;

import com.nifty.http.error.AuthFailureError;
import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于HttpURLConnection 的网络连接
 */
public class HurlStack implements HttpStack {

	private static final String HEADER_CONTENT_TYPE = "Content-Type";

	//Https协议里的东西，暂时先不管
	private final SSLSocketFactory mSslSocketFactory;

	public HurlStack() {
		this(null);
	}

	public HurlStack(SSLSocketFactory sslSocketFactory) {
		mSslSocketFactory = sslSocketFactory;
	}

	@Override public HttpResponse performRequest(Request request, Map<String, String> additionalHeaders)
			throws IOException, AuthFailureError {

		String url = request.getUrl();

		HashMap<String, String> map = new HashMap<String, String>();
		map.putAll(request.getHeaders());
		map.putAll(additionalHeaders);

		URL parsedUrl = new URL(url);
		HttpURLConnection connection = openConnection(parsedUrl, request);
		for (String headerName : map.keySet()) {
			//Adds the given property to the request header
			connection.addRequestProperty(headerName, map.get(headerName));
		}
		//设置method和body
		setConnectionParametersForRequest(connection, request);

		//将返回封装成HttpResponse
		ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
		int responseCode = connection.getResponseCode();
		if (responseCode == -1) {
			// -1 is returned by getResponseCode() if the response code could not be retrieved.
			// Signal to the caller that something was wrong with the connection.
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}
		StatusLine responseStatus = new BasicStatusLine(protocolVersion,
				connection.getResponseCode(), connection.getResponseMessage());
		BasicHttpResponse response = new BasicHttpResponse(responseStatus);
		response.setEntity(entityFromConnection(connection));

		//解析response的header
		for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
			if (header.getKey() != null) {
				Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
				response.addHeader(h);
			}
		}
		return response;
	}

	private HttpURLConnection openConnection(URL url, Request request) throws IOException {
		HttpURLConnection connection = createConnection(url);
		//超时
		int timeoutMs = request.getTimeoutMs();
		connection.setConnectTimeout(timeoutMs);
		connection.setReadTimeout(timeoutMs);
		//TODO 待研究
		connection.setUseCaches(false);
		//读取
		connection.setDoInput(true);

		//TODO 待研究
		// use caller-provided custom SslSocketFactory, if any, for HTTPS
		if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
			((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
		}

		return connection;
	}

	protected HttpURLConnection createConnection(URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}

	private void setConnectionParametersForRequest(HttpURLConnection connection,
			Request request) throws IOException {
		switch (request.getMethod()) {
		case GET:
			// Not necessary to set the request method because connection defaults to GET but
			// being explicit here.
			connection.setRequestMethod("GET");
			break;
		case DELETE:
			connection.setRequestMethod("DELETE");
			break;
		case POST:
			connection.setRequestMethod("POST");
			addBodyIfExists(connection, request);
			break;
		case PUT:
			connection.setRequestMethod("PUT");
			addBodyIfExists(connection, request);
			break;
		case HEAD:
			connection.setRequestMethod("HEAD");
			break;
		case OPTIONS:
			connection.setRequestMethod("OPTIONS");
			break;
		case TRACE:
			connection.setRequestMethod("TRACE");
			break;
		case PATCH:
			connection.setRequestMethod("PATCH");
			addBodyIfExists(connection, request);
			break;
		default:
			throw new IllegalStateException("Unknown method type.");
		}
	}

	private static void addBodyIfExists(HttpURLConnection connection, Request request)
			throws IOException {
		//params
		byte[] body = request.getBody();
		if (body != null) {
			//输入
			connection.setDoOutput(true);
			//设置Content-Type  charset 用什么格式提交
			//application/x-www-form-urlencoded和multipart/form-data，默认为application/x-www-form-urlencoded。 当action为get时候，浏览器用x-www-form-urlencoded的编码方式把form数据转换成一个字串（name1=value1&name2=value2...），然后把这个字串append到url后面，用?分割，加载这个新的url。 当action为post时候，浏览器把form数据封装到http body中，然后发送到server。 如果没有type=file的控件，用默认的application/x-www-form-urlencoded就可以了。 但是如果有type=file的话，就要用到multipart/form-data了。浏览器会把整个表单以控件为单位分割，并为每个部分加上Content-Disposition(form-data或者file),Content-Type(默认为text/plain),name(控件name)等信息，并加上分割符(boundary)。
			connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			//写入body
			out.write(body);
			out.close();
		}
	}

	private static HttpEntity entityFromConnection(HttpURLConnection connection) {
		BasicHttpEntity entity = new BasicHttpEntity();
		InputStream inputStream;
		try {
			inputStream = connection.getInputStream();
		} catch (IOException ioe) {
			inputStream = connection.getErrorStream();
		}
		entity.setContent(inputStream);
		entity.setContentLength(connection.getContentLength());
		entity.setContentEncoding(connection.getContentEncoding());
		entity.setContentType(connection.getContentType());
		return entity;
	}
}
