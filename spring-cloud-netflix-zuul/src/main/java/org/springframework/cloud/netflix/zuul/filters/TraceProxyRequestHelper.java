/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.zuul.filters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.context.RequestContext;

import org.springframework.boot.actuate.trace.http.HttpExchangeTracer;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.Include;
import org.springframework.util.MultiValueMap;

/**
 * @author Spencer Gibb
 */
public class TraceProxyRequestHelper extends ProxyRequestHelper {

	private HttpTraceRepository traces;

	@Deprecated
	// TODO Remove in 2.1.x
	public TraceProxyRequestHelper() {
	}

	public TraceProxyRequestHelper(ZuulProperties zuulProperties) {
		super(zuulProperties);
	}

	private final HttpExchangeTracer tracer = new HttpExchangeTracer(
			Include.defaultIncludes());

	public void setTraces(HttpTraceRepository traces) {
		this.traces = traces;
	}

	@Override
	public Map<String, Object> debug(String verb, String uri,
			MultiValueMap<String, String> headers, MultiValueMap<String, String> params,
			InputStream requestEntity) throws IOException {
		Map<String, Object> info = new LinkedHashMap<>();
		if (this.traces != null) {
			RequestContext context = RequestContext.getCurrentContext();
			info.put("method", verb);
			info.put("path", uri);
			info.put("query", getQueryString(params));
			info.put("remote", true);
			info.put("proxy", context.get("proxy"));
			Map<String, Object> trace = new LinkedHashMap<>();
			Map<String, Object> input = new LinkedHashMap<>();
			trace.put("request", input);
			info.put("headers", trace);
			debugHeaders(headers, input);
			HttpServletRequest request = context.getRequest();
			if (shouldDebugBody(context)) {
				// Prevent input stream from being read if it needs to go downstream
				if (requestEntity != null) {
					debugRequestEntity(info, request.getInputStream());
				}
			}
			HttpTrace httpTrace = tracer
					.receivedRequest(new ServletTraceableRequest(request));
			this.traces.add(httpTrace);
			return info;
		}
		return info;
	}

	void debugHeaders(MultiValueMap<String, String> headers, Map<String, Object> map) {
		for (Entry<String, List<String>> entry : headers.entrySet()) {
			Collection<String> collection = entry.getValue();
			Object value = collection;
			if (collection.size() < 2) {
				value = collection.isEmpty() ? "" : collection.iterator().next();
			}
			map.put(entry.getKey(), value);
		}
	}

	public void appendDebug(Map<String, Object> info, int status,
			MultiValueMap<String, String> headers) {
		if (this.traces != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> trace = (Map<String, Object>) info.get("headers");
			Map<String, Object> output = new LinkedHashMap<>();
			trace.put("response", output);
			debugHeaders(headers, output);
			output.put("status", "" + status);
		}
	}

	private void debugRequestEntity(Map<String, Object> info, InputStream inputStream)
			throws IOException {
		if (RequestContext.getCurrentContext().isChunkedRequestBody()) {
			info.put("body", "<chunked>");
			return;
		}
		char[] buffer = new char[4096];
		int count = new InputStreamReader(inputStream, Charset.forName("UTF-8"))
				.read(buffer, 0, buffer.length);
		if (count > 0) {
			String entity = new String(buffer).substring(0, count);
			info.put("body", entity.length() < 4096 ? entity : entity + "<truncated>");
		}
	}

}
