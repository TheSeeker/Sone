/*
 * Sone - DeletePostCommandTest.java - Copyright © 2013 David Roden
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.pterodactylus.sone.fcp;

import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.doNothing;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.support.SimpleFieldSet;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for {@link DeletePostCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DeletePostCommandTest {

	private final Mocks mocks = new Mocks();
	private final DeletePostCommand deletePostCommand = new DeletePostCommand(mocks.core);

	@Test
	public void verifyThatDeletingAPostWorks() throws FcpException {
		Sone sone = mocks.mockSone("Sone").local().create();
		Post post = mocks.mockPost(sone, "PostId");
		ArgumentCaptor<Post> deletedPost = forClass(Post.class);
		doNothing().when(mocks.core).deletePost(deletedPost.capture());
		SimpleFieldSet deletePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeletePost")
				.put("Post", "PostId")
				.get();
		Response response = deletePostCommand.execute(deletePostFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostDeleted"));
		assertThat(deletedPost.getValue(), is(post));
	}

	@Test
	public void verifyThatDeletingAPostFromANonLocalSoneCausesAnError() throws FcpException {
		Sone sone = mocks.mockSone("Sone").create();
		Post post = mocks.mockPost(sone, "PostId");
		SimpleFieldSet deletePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeletePost")
				.put("Post", "PostId")
				.get();
		Response response = deletePostCommand.execute(deletePostFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("Error"));
		assertThat(response.getReplyParameters().get("ErrorCode"), is("401"));
	}

	@Test(expected = FcpException.class)
	public void verifyThatDeletingWithAMissingPostIdCausesAnError() throws FcpException {
		SimpleFieldSet deletePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeletePost")
				.get();
		deletePostCommand.execute(deletePostFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void verifyThatDeletingAPostWithAnInvalidPostIdCausesAnError() throws FcpException {
		Sone sone = mocks.mockSone("Sone").local().create();
		mocks.mockPost(sone, "PostId");
		SimpleFieldSet deletePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeletePost")
				.put("Post", "OtherPostId")
				.get();
		deletePostCommand.execute(deletePostFieldSet, null, DIRECT);
	}

}
