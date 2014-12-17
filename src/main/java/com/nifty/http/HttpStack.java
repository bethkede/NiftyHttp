package com.nifty.http;

import com.nifty.http.error.AuthFailureError;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

/**
 * 执行Http的接口,可以扩展在2.3一下使用HttpClient
 */
public interface HttpStack {

	/**
	 * 执行Http 的 request请求
	 *
	 * @param request
	 * @param additionalHeaders 添加的headers
	 * @return
	 * @throws java.io.IOException
	 */
	public HttpResponse performRequest(Request request, Map<String, String> additionalHeaders)
			throws IOException, AuthFailureError;
}
