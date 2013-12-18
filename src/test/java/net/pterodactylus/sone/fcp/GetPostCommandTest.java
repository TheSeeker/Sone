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

import static java.util.Arrays.asList;
import static net.pterodactylus.sone.Verifiers.verifyAnswer;
import static net.pterodactylus.sone.Verifiers.verifyPostWithReplies;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
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
		Post post = mocks.mockPost(sone, "PostId").withTime(1000L).withText("Text of the post.").create();
		SimpleFieldSet getPostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPost")
				.put("Post", "PostId")
				.get();
		Response response = getPostCommand.execute(getPostFieldSet, null, DIRECT);
		verifyAnswer(response, "Post");
		verifyPostWithReplies(response.getReplyParameters(), "Post.", post);
	}

	@Test
	public void verifyThatGettingAPostWithoutRepliesAndWithRecipientWorks() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Sone otherSone = mocks.mockSone("OtherSoneId").create();
		Post post = mocks.mockPost(sone, "PostId").withRecipient(otherSone.getId()).withTime(1000L).withText("Text of the post.").create();
		SimpleFieldSet getPostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPost")
				.put("Post", "PostId")
				.get();
		Response response = getPostCommand.execute(getPostFieldSet, null, DIRECT);
		verifyAnswer(response, "Post");
		verifyPostWithReplies(response.getReplyParameters(), "Post.", post);
	}

	@Test
	public void verifyThatGettingAPostWithRepliesWorks() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post = mocks.mockPost(sone, "PostId").withTime(1000L).withText("Text of the post.").create();
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
		verifyAnswer(response, "Post");
		verifyPostWithReplies(response.getReplyParameters(), "Post.", post);
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

}
