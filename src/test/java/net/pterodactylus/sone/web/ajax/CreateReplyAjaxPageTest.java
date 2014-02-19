package net.pterodactylus.sone.web.ajax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.web.page.FreenetRequest;

import freenet.support.api.HTTPRequest;

import org.junit.Test;

/**
 * Unit test for {@link CreateReplyAjaxPage}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class CreateReplyAjaxPageTest {

	private final Mocks mocks = new Mocks();
	private final CreateReplyAjaxPage createReplyAjaxPage = new CreateReplyAjaxPage(mocks.webInterface);
	private final FreenetRequest freenetRequest = mocks.mockRequest("createReply.ajax");

	@Test
	public void canCreateReplyWithAllOptionalValues() {
		Sone sender = mocks.mockSone("SenderId").local().create();
		Sone remote = mocks.mockSone("Remote").create();
		mocks.mockPost(remote, "PostId").create();
		HTTPRequest httpRequest = createHttpRequest(freenetRequest);
		setReplyText(httpRequest, "Reply Text");
		when(httpRequest.getParam("sender")).thenReturn("SenderId");
		JsonReturnObject jsonReturnObject = createReplyAjaxPage.createJsonObject(freenetRequest);
		verifySuccess(sender, jsonReturnObject);
	}

	private void verifySuccess(Sone sender, JsonReturnObject jsonReturnObject) {
		assertThat(jsonReturnObject.isSuccess(), is(true));
		assertThat(jsonReturnObject.get("reply").textValue(), notNullValue());
		assertThat(jsonReturnObject.get("sone").textValue(), is(sender.getId()));
	}

	private void setReplyText(HTTPRequest httpRequest, String replyText) {
		when(httpRequest.getParam("text")).thenReturn(replyText);
	}

	private HTTPRequest createHttpRequest(FreenetRequest freenetRequest) {
		HTTPRequest httpRequest = mock(HTTPRequest.class);
		when(httpRequest.getParam("post")).thenReturn("PostId");
		when(freenetRequest.getHttpRequest()).thenReturn(httpRequest);
		return httpRequest;
	}

	@Test
	public void canCreateReplyWithDefaultSender() {
		Sone sender = mocks.mockSone("SenderId").current().local().create();
		Sone remote = mocks.mockSone("Remote").create();
		mocks.mockPost(remote, "PostId").create();
		HTTPRequest httpRequest = createHttpRequest(freenetRequest);
		setReplyText(httpRequest, "Reply Text");
		JsonReturnObject jsonReturnObject = createReplyAjaxPage.createJsonObject(freenetRequest);
		verifySuccess(sender, jsonReturnObject);
	}

	@Test
	public void canNotCreateReplyWithoutSender() {
		Sone remote = mocks.mockSone("Remote").create();
		mocks.mockPost(remote, "PostId").create();
		HTTPRequest httpRequest = createHttpRequest(freenetRequest);
		setReplyText(httpRequest, "Reply Text");
		JsonReturnObject jsonReturnObject = createReplyAjaxPage.createJsonObject(freenetRequest);
		verifyFailure(jsonReturnObject, "invalid-sone-id");
	}

	private void verifyFailure(JsonReturnObject jsonReturnObject, String errorId) {
		assertThat(jsonReturnObject.isSuccess(), is(false));
		assertThat(((JsonErrorReturnObject) jsonReturnObject).getError(), is(errorId));
	}

	@Test
	public void canNotCreateReplyWithoutPost() {
		mocks.mockSone("SenderId").local().create();
		HTTPRequest httpRequest = createHttpRequest(freenetRequest);
		setReplyText(httpRequest, "Reply Text");
		when(httpRequest.getParam("sender")).thenReturn("SenderId");
		JsonReturnObject jsonReturnObject = createReplyAjaxPage.createJsonObject(freenetRequest);
		verifyFailure(jsonReturnObject, "invalid-post-id");
	}

	@Test(expected = IllegalStateException.class)
	public void canNotCreateReplyWithoutText() {
		mocks.mockSone("SenderId").local().create();
		Sone remote = mocks.mockSone("Remote").create();
		mocks.mockPost(remote, "PostId").create();
		HTTPRequest httpRequest = createHttpRequest(freenetRequest);
		setReplyText(httpRequest, "");
		when(httpRequest.getParam("sender")).thenReturn("SenderId");
		createReplyAjaxPage.createJsonObject(freenetRequest);
	}

}
