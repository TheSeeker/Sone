package net.pterodactylus.sone.web.ajax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.web.page.FreenetRequest;

import freenet.clients.http.ToadletContext;
import freenet.support.api.HTTPRequest;

import org.junit.Test;

/**
 * Unit test for {@link CreatePostAjaxPage}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class CreatePostAjaxPageTest {

	private final Mocks mocks = new Mocks();
	private final CreatePostAjaxPage createPostAjaxPage = new CreatePostAjaxPage(mocks.webInterface);

	@Test
	public void missingSoneResultsInAnError() {
		JsonReturnObject jsonReturnObject = createPostAjaxPage.createJsonObject(mocks.mockRequest(""));
		verifyError(jsonReturnObject, "auth-required");
	}

	private void verifyError(JsonReturnObject jsonReturnObject, String error) {
		assertThat(jsonReturnObject.isSuccess(), is(false));
		assertThat(((JsonErrorReturnObject) jsonReturnObject).getError(), is(error));
	}

	@Test
	public void missingTextResultsInError() {
		configureLocalSone();
		FreenetRequest request = mocks.mockRequest("");
		JsonReturnObject jsonReturnObject = createPostAjaxPage.createJsonObject(request);
		verifyError(jsonReturnObject, "text-required");
	}

	@Test
	public void emptyTextResultsInError() {
		configureLocalSone();
	}

	private void configureText(FreenetRequest request, String text) {
		HTTPRequest httpRequest = request.getHttpRequest();
		when(httpRequest.getParam("text")).thenReturn(text);
	}

	private void configureLocalSone() {
		Sone sone = mocks.mockSone("Sone").local().create();
		when(mocks.webInterface.getCurrentSone(any(ToadletContext.class))).thenReturn(sone);
	}

	@Test
	public void postIsCreated() {
		configureLocalSone();
		FreenetRequest request = mocks.mockRequest("");
		configureText(request, "Test text.");
		JsonReturnObject jsonReturnObject = createPostAjaxPage.createJsonObject(request);
		assertThat(jsonReturnObject.isSuccess(), is(true));
		assertThat(mocks.database.getPost(jsonReturnObject.get("postId").asText()).isPresent(), is(true));
	}

	@Test
	public void postWithMissingRecipientIsCreatedWithoutRecipient() {
		configureLocalSone();
		FreenetRequest request = mocks.mockRequest("");
		configureText(request, "Test text.");
		when(request.getHttpRequest().getParam("recipient")).thenReturn("Recipient");
		JsonReturnObject jsonReturnObject = createPostAjaxPage.createJsonObject(request);
		assertThat(jsonReturnObject.isSuccess(), is(true));
		assertThat(mocks.database.getPost(jsonReturnObject.get("postId").asText()).isPresent(), is(true));
		assertThat(jsonReturnObject.get("recipient").textValue(), nullValue());
	}

	@Test
	public void postWithRecipientIsCreatedWithRecipient() {
		configureLocalSone();
		FreenetRequest request = mocks.mockRequest("");
		configureRecipient(request);
		configureText(request, "Test text.");
		JsonReturnObject jsonReturnObject = createPostAjaxPage.createJsonObject(request);
		assertThat(jsonReturnObject.isSuccess(), is(true));
		assertThat(mocks.database.getPost(jsonReturnObject.get("postId").asText()).isPresent(), is(true));
		assertThat(jsonReturnObject.get("recipient").textValue(), is("Recipient"));
	}

	private void configureRecipient(FreenetRequest request) {
		mocks.mockSone("Recipient").create();
		when(request.getHttpRequest().getParam("recipient")).thenReturn("Recipient");
	}

}
