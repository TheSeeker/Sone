/*
 * © 2013 xplosion interactive
 */

package net.pterodactylus.sone.web.ajax;

import static net.pterodactylus.sone.Verifiers.verifyJsonError;
import static net.pterodactylus.sone.Verifiers.verifySuccessfulJsonResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.web.WebInterface;
import net.pterodactylus.sone.web.page.FreenetRequest;

import org.junit.Test;

/**
 * Tests for {@link BookmarkAjaxPage}.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class BookmarkAjaxPageTest {

	private final Mocks mocks = new Mocks();
	private final Core core = mocks.core;
	private final WebInterface webInterface = mocks.webInterface;
	private final BookmarkAjaxPage bookmarkAjaxPage = new BookmarkAjaxPage(webInterface);

	@Test
	public void bookmarkingDoesNotRequireLogin() {
		assertThat(bookmarkAjaxPage.requiresLogin(), is(false));
	}

	@Test
	public void testBookmarkingExistingPost() throws URISyntaxException {
		JsonReturnObject jsonReturnObject = performRequest(bookmarkAjaxPage, "abc");
		verifySuccessfulJsonResponse(jsonReturnObject);
		verify(core, times(1)).bookmarkPost(eq("abc"));
	}

	@Test
	public void testBookmarkingMissingPost() throws URISyntaxException {
		JsonReturnObject jsonReturnObject = performRequest(bookmarkAjaxPage, null);
		verifyJsonError(jsonReturnObject, "invalid-post-id");
		verify(core, never()).bookmarkPost(anyString());
	}

	private JsonReturnObject performRequest(BookmarkAjaxPage bookmarkAjaxPage, String postId) throws URISyntaxException {
		FreenetRequest request = mocks.mockRequest("");
		when(request.getHttpRequest().getParam("post")).thenReturn(postId);
		when(request.getHttpRequest().getParam("post", null)).thenReturn(postId);
		return bookmarkAjaxPage.createJsonObject(request);
	}

}
