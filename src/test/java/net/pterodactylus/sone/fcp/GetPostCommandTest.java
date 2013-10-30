/*
 * Sone - GetPostCommandTest.java - Copyright © 2013 David Roden
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

import static com.google.common.base.Optional.of;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import com.google.common.base.Optional;
import org.junit.Test;

/**
 * Unit test for {@link GetPostCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetPostCommandTest {

	private final Mocks mocks = new Mocks();
	private final GetPostCommand getPostCommand = new GetPostCommand(mocks.core);

	@Test
	public void verifyThatGettingAPostWithoutRepliesAndRecipientWorks() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post = preparePostWithoutRecipient(sone);
		SimpleFieldSet getPostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPost")
				.put("Post", "PostId")
				.get();
		Response response = getPostCommand.execute(getPostFieldSet, null, DIRECT);
		verifyAnswer(response);
		verifyPost(post, response);
	}

	@Test
	public void verifyThatGettingAPostWithoutRepliesAndWithRecipientWorks() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Sone otherSone = mocks.mockSone("OtherSoneId").create();
		Post post = preparePostWithRecipient(sone, otherSone);
		SimpleFieldSet getPostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPost")
				.put("Post", "PostId")
				.get();
		Response response = getPostCommand.execute(getPostFieldSet, null, DIRECT);
		verifyAnswer(response);
		verifyPost(post, response);
	}

	@Test
	public void verifyThatGettingAPostWithRepliesWorks() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post = preparePostWithoutRecipient(sone);
		PostReply postReply1 = mocks.mockPostReply(sone, "Reply1").create();
		when(postReply1.getText()).thenReturn("Reply 1.");
		PostReply postReply2 = mocks.mockPostReply(sone, "Reply2").create();
		when(postReply2.getText()).thenReturn("Reply 2.");
		when(post.getReplies()).thenReturn(asList(postReply1, postReply2));
		SimpleFieldSet getPostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPost")
				.put("Post", "PostId")
				.put("IncludeReplies", "true")
				.get();
		Response response = getPostCommand.execute(getPostFieldSet, null, DIRECT);
		verifyAnswer(response);
		verifyPost(post, response);
		assertThat(response.getReplyParameters().getInt("Post.Replies.Count"), is(post.getReplies().size()));
		verifyReply(response.getReplyParameters(), "Post.Replies.0.", postReply1);
		verifyReply(response.getReplyParameters(), "Post.Replies.1.", postReply2);
	}

	@Test(expected = FcpException.class)
	public void verifyThatGettingANonExistingPostCausesAnError() throws FcpException {
		SimpleFieldSet getPostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPost")
				.put("Post", "PostId")
				.get();
		getPostCommand.execute(getPostFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void verifyThatAMissingPostIdCausesAnError() throws FcpException {
		SimpleFieldSet getPostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPost")
				.get();
		getPostCommand.execute(getPostFieldSet, null, DIRECT);
	}

	private Post preparePostWithoutRecipient(Sone sone) {
		Post post = preparePost(sone);
		when(post.getRecipientId()).thenReturn(Optional.<String>absent());
		when(post.getRecipient()).thenReturn(Optional.<Sone>absent());
		return post;
	}

	private Post preparePostWithRecipient(Sone sone, Sone otherSone) {
		Post post = preparePost(sone);
		String otherSoneId = otherSone.getId();
		when(post.getRecipientId()).thenReturn(of(otherSoneId));
		when(post.getRecipient()).thenReturn(of(otherSone));
		return post;
	}

	private Post preparePost(Sone sone) {
		Post post = mocks.mockPost(sone, "PostId").create();
		when(post.getText()).thenReturn("Text of the post.");
		when(post.getTime()).thenReturn(1000L);
		return post;
	}

	private void verifyAnswer(Response response) {
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("Post"));
	}

	private void verifyReply(SimpleFieldSet replyParameters, String prefix, PostReply postReply) throws FSParseException {
		assertThat(replyParameters.get(format("%sID", prefix)), is(postReply.getId()));
		assertThat(replyParameters.get(format("%sSone", prefix)), is(postReply.getSone().getId()));
		assertThat(replyParameters.getLong(format("%sTime", prefix)), is(postReply.getTime()));
		assertThat(replyParameters.get(format("%sText", prefix)), is(postReply.getText()));
	}

	private void verifyPost(Post post, Response response) throws FSParseException {
		assertThat(response.getReplyParameters().get("Post.ID"), is(post.getId()));
		assertThat(response.getReplyParameters().get("Post.Sone"), is(post.getSone().getId()));
		assertThat(response.getReplyParameters().get("Post.Recipient"), is(post.getRecipientId().orNull()));
		assertThat(response.getReplyParameters().getLong("Post.Time"), is(post.getTime()));
		assertThat(response.getReplyParameters().get("Post.Text"), is(post.getText()));
	}

}
