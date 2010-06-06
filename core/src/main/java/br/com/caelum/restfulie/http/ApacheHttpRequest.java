package br.com.caelum.restfulie.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import br.com.caelum.restfulie.Response;
import br.com.caelum.restfulie.RestClient;
import br.com.caelum.restfulie.Restfulie;
import br.com.caelum.restfulie.RestfulieException;

public class ApacheHttpRequest implements Request {

	private String verb = "GET";

	private final Map<String, String> headers = new HashMap<String, String>();

	private final URI uri;

	private final RestClient client;

	public ApacheHttpRequest(URI uri, RestClient client) {
		this.uri = uri;
		this.client = client;
	}

	private Response sendPayload(Object payload, String verb) {
		try {
			HttpURLConnection connection = prepareConnectionWithHeaders();
			if (!headers.containsKey("Content-type")) {
				throw new RestfulieException(
						"You should set a content type prior to sending some payload.");
			}
			connection.setDoOutput(true);
			connection.setRequestMethod(verb);
			OutputStream output = connection.getOutputStream();
			Writer writer = new OutputStreamWriter(output);
			client.getMediaTypes().forContentType(headers.get("Content-type")).marshal(payload, writer);
			DefaultResponse response = responseFor(connection,
					new IdentityContentProcessor());
			if (response.getCode() == 201) {
				return client.at(response.getHeader("Location").get(0)).get();
			}
			return response;
		} catch (IOException e) {
			throw new RestfulieException("Unable to execute " + uri, e);
		} catch (URISyntaxException e) {
			throw new RestfulieException("Unable to execute " + uri, e);
		}
	}

	private HttpURLConnection prepareConnectionWithHeaders()
			throws IOException, MalformedURLException {
		HttpURLConnection connection = (HttpURLConnection) uri.toURL()
				.openConnection();
		for (String header : headers.keySet()) {
			connection.addRequestProperty(header, headers.get(header));
		}
		return connection;
	}

	public Response access() {
		try {
			HttpURLConnection connection = prepareConnectionWithHeaders();
			connection.setDoOutput(false);
			connection.setRequestMethod(verb);
			DefaultResponse response = responseFor(connection,
					new HttpURLConnectionContentProcessor(connection));
			return response;
		} catch (IOException e) {
			throw new RestfulieException("Unable to execute " + uri, e);
		}
	}


	private DefaultResponse responseFor(HttpURLConnection connection,
			ContentProcessor processor) throws IOException {
		return new DefaultResponse(connection, client.getMediaTypes(), processor);
	}

	@Override
	public Request with(String key, String value) {
		headers.put(key, value);
		return this;
	}

	@Override
	public Request using(String verb) {
		this.verb = verb;
		return this;
	}

	public Request accept(String type) {
		return with("Accept", type);
	}

	public Response get() {
		return retrieve("GET");
	}

	private Response retrieve(String verb) {
		return using(verb).access();
	}

	@Override
	public Request as(String contentType) {
		return with("Content-type", contentType);
	}

	@Override
	public Response delete() {
		return retrieve("DELETE");
	}

	@Override
	public Response head() {
		return retrieve("HEAD");
	}

	@Override
	public Response options() {
		return retrieve("OPTIONS");
	}

	@Override
	public <T> Response patch(T object) {
		return sendPayload(object, "PATCH");
	}
	
	@Override
	public <T> Response post(T object) {
		return sendPayload(object, "POST");
	}

	@Override
	public <T> Response put(T object) {
		return sendPayload(object, "PUT");
	}


}
