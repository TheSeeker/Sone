package net.pterodactylus.sone.web.ajax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.web.page.FreenetRequest;
import net.pterodactylus.util.web.Response;

import freenet.clients.http.ToadletContext;
import freenet.support.api.HTTPRequest;

import org.junit.Test;

/**
 * Unit test for {@link JsonPage}.
 *
 * @author <a href="mailto:">David Roden</a>
 */
public class JsonPageTest {

	private static final String PATH = "/test/path";
	private final Mocks mocks = new Mocks();
	private final FreenetRequest request = createRequest();
	private final Response response = createResponse();

	@Test
	public void defaultJsonPageRequiresFormPassword() {
		JsonPage jsonPage = createJsonPage(true, true);
		assertThat(jsonPage.needsFormPassword(), is(true));
	}

	@Test
	public void defaultJsonPageRequiresLogin() {
		JsonPage jsonPage = createJsonPage(true, true);
		assertThat(jsonPage.requiresLogin(), is(true));
	}

	@Test
	public void defaultJsonPageReturnsTheCorrectPath() {
		JsonPage jsonPage = createJsonPage(true, true);
		assertThat(jsonPage.getPath(), is(PATH));
	}

	@Test
	public void defaultJsonPageIsNotAPrefixPage() {
		JsonPage jsonPage = createJsonPage(true, true);
		assertThat(jsonPage.isPrefixPage(), is(false));
	}

	@Test
	public void defaultJsonPageIsNotLinkExcepted() {
		JsonPage jsonPage = createJsonPage(true, true);
		assertThat(jsonPage.isLinkExcepted(null), is(false));
	}

	@Test
	public void defaultJsonPageResultsIn403IfAccessedWithFullAccessIfRequired() throws IOException {
		JsonPage jsonPage = createJsonPage(true, true);
		configureFullAccess();
		jsonPage.handleRequest(request, response);
		verifyResponse(response, 403, "Forbidden");
	}

	private void verifyResponseIs500(Response response) {
		assertThat(response.getStatusCode(), is(500));
	}

	private void verifyResponse(Response response, int statusCode, String statusText) {
		assertThat(response.getStatusCode(), is(statusCode));
		assertThat(response.getStatusText(), is(statusText));
	}

	private Response createResponse() {
		OutputStream outputStream = new ByteArrayOutputStream();
		return new Response(outputStream);
	}

	private FreenetRequest createRequest() {
		ToadletContext toadletContext = mock(ToadletContext.class);
		when(toadletContext.isAllowedFullAccess()).thenReturn(false);
		FreenetRequest request = mock(FreenetRequest.class);
		when(request.getToadletContext()).thenReturn(toadletContext);
		return request;
	}

	private void configureFullAccess() {
		mocks.core.getPreferences().setRequireFullAccess(true);
	}

	@Test
	public void defaultJsonPageResultsIn403IfPasswordIsRequiredButNotGiven() throws IOException {
		JsonPage jsonPage = createJsonPage(true, true);
		configureRequiredPassword("password");
		configureRequestPassword(request, null);
		jsonPage.handleRequest(request, response);
		verifyResponse(response, 403, "Forbidden");
	}

	private void configureRequestPassword(FreenetRequest request, String password) {
		HTTPRequest httpRequest = mock(HTTPRequest.class);
		when(httpRequest.getParam("formPassword")).thenReturn(password);
		when(request.getHttpRequest()).thenReturn(httpRequest);
	}

	private void configureRequiredPassword(String password) {
		when(mocks.webInterface.getFormPassword()).thenReturn(password);
	}

	@Test
	public void defaultJsonPageResultsIn403IfPasswordIsRequiredButWrong() throws IOException {
		JsonPage jsonPage = createJsonPage(true, true);
		configureRequiredPassword("password");
		configureRequestPassword(request, "wrongpassword");
		jsonPage.handleRequest(request, response);
		verifyResponse(response, 403, "Forbidden");
	}

	@Test
	public void defaultJsonPageResultsIn403IfLoginIsRequiredAndSessionIsNull() throws IOException {
		JsonPage jsonPage = createJsonPage(true, true);
		configureRequiredPassword("password");
		configureRequestPassword(request, "password");
		jsonPage.handleRequest(request, response);
		verifyResponse(response, 403, "Forbidden");
	}

	@Test
	public void defaultJsonPageResultsIn200() throws IOException {
		JsonPage jsonPage = createJsonPage(true, true);
		configureSession();
		configureRequiredPassword("password");
		configureRequestPassword(request, "password");
		jsonPage.handleRequest(request, response);
		verifyResponse(response, 200, "OK");
	}

	private void configureSession() {
		Sone sone = mocks.mockSone("Sone").create();
		when(mocks.webInterface.getCurrentSone(any(ToadletContext.class), anyBoolean())).thenReturn(sone);
	}

	@Test
	public void exceptionDuringReturnObjectCreationResultsIn500() throws IOException {
		JsonPage jsonPage = createExceptionThrowingJsonPage();
		configureSession();
		configureRequiredPassword("password");
		configureRequestPassword(request, "password");
		jsonPage.handleRequest(request, response);
		verifyResponseIs500(response);
	}

	@Test
	public void jsonPageWithoutPasswordAndLoginResultsIn200() throws IOException {
		JsonPage jsonPage = createJsonPage(false, false);
		jsonPage.handleRequest(request, response);
		verifyResponse(response, 200, "OK");
	}

	private JsonPage createJsonPage(final boolean needsFormPassword, final boolean requiresLogin) {
		return new JsonPage(PATH, mocks.webInterface) {
			@Override
			protected JsonReturnObject createJsonObject(FreenetRequest request) {
				return createSuccessJsonObject();
			}

			@Override
			protected boolean needsFormPassword() {
				return needsFormPassword;
			}

			@Override
			protected boolean requiresLogin() {
				return requiresLogin;
			}
		};
	}

	private JsonPage createExceptionThrowingJsonPage() {
		return new JsonPage(PATH, mocks.webInterface) {
			@Override
			protected JsonReturnObject createJsonObject(FreenetRequest request) {
				throw new NullPointerException();
			}
		};
	}

}
