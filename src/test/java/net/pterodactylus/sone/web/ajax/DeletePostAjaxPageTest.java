package net.pterodactylus.sone.web.ajax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.web.page.FreenetRequest;

import org.junit.Test;

/**
 * Unit test for {@link DeletePostAjaxPage}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DeletePostAjaxPageTest {

	private Mocks mocks = new Mocks();
	private final DeletePostAjaxPage deletePostAjaxPage = new DeletePostAjaxPage(mocks.webInterface);
	private final FreenetRequest freenetRequest = mocks.mockRequest("deletePost.ajax");

	@Test
	public void canDeletePost() {
		Sone sender = mocks.mockSone("SoneId").local().create();
		Post post = mocks.mockPost(sender, "PostId").create();
		when(freenetRequest.getHttpRequest().getParam("post")).thenReturn("PostId");
		JsonReturnObject jsonReturnObject = deletePostAjaxPage.createJsonObject(freenetRequest);
		assertThat(jsonReturnObject.isSuccess(), is(true));
		verify(mocks.core).deletePost(post);
	}

	@Test
	public void canNotDeletePostWithInvalidId() {
		JsonReturnObject jsonReturnObject = deletePostAjaxPage.createJsonObject(freenetRequest);
		verifyFailure(jsonReturnObject, "invalid-post-id");
	}

	private void verifyFailure(JsonReturnObject jsonReturnObject, String errorId) {
		assertThat(jsonReturnObject.isSuccess(), is(false));
		assertThat(((JsonErrorReturnObject) jsonReturnObject).getError(), is(errorId));
	}

	@Test
	public void canNotDeletePostFromRemoteSone() {
		Sone sender = mocks.mockSone("SoneId").create();
		mocks.mockPost(sender, "PostId").create();
		when(freenetRequest.getHttpRequest().getParam("post")).thenReturn("PostId");
		JsonReturnObject jsonReturnObject = deletePostAjaxPage.createJsonObject(freenetRequest);
		verifyFailure(jsonReturnObject, "not-authorized");
	}

}
