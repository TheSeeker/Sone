/*
 * Sone - GetPostsCommandTest.java - Copyright © 2013 David Roden
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
import static net.pterodactylus.sone.fcp.Verifiers.verifyAnswer;
import static net.pterodactylus.sone.fcp.Verifiers.verifyPostsWithReplies;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;

import java.util.Collections;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import org.junit.Test;

/**
 * Unit test for {@link GetPostsCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetPostsCommandTest {

	private final Mocks mocks = new Mocks();
	private final GetPostsCommand getPostsCommand = new GetPostsCommand(mocks.core);

	@Test
	public void multiplePostsAreReturnedCorrectly() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post1 = mocks.mockPost(sone, "Post1").withTime(1000L).withText("1").create();
		Post post2 = mocks.mockPost(sone, "Post2").withTime(2000L).withText("1").create();
		Post post3 = mocks.mockPost(sone, "Post3").withTime(3000L).withText("1").create();
		Sone otherSone = mocks.mockSone("OtherSone").create();
		mocks.mockPostReply(otherSone, "Reply1").inReplyTo(post1).withText("R").create();
		SimpleFieldSet getPostsFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPosts")
				.put("Sone", "SoneId")
				.get();
		Response response = getPostsCommand.execute(getPostsFieldSet, null, DIRECT);
		verifyAnswer(response, "Posts");
		verifyPostsWithReplies(response.getReplyParameters(), "Posts.", asList(post3, post2, post1));
	}

	@Test
	public void skippingMorePostsThanThereAreReturnsAnEmptyList() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post1 = mocks.mockPost(sone, "Post1").withTime(1000L).withText("1").create();
		Post post2 = mocks.mockPost(sone, "Post2").withTime(2000L).withText("1").create();
		Post post3 = mocks.mockPost(sone, "Post3").withTime(3000L).withText("1").create();
		Sone otherSone = mocks.mockSone("OtherSone").create();
		mocks.mockPostReply(otherSone, "Reply1").inReplyTo(post1).withText("R").create();
		SimpleFieldSet getPostsFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPosts")
				.put("Sone", "SoneId")
				.put("StartPost", "5")
				.get();
		Response response = getPostsCommand.execute(getPostsFieldSet, null, DIRECT);
		verifyAnswer(response, "Posts");
		verifyPostsWithReplies(response.getReplyParameters(), "Posts.", Collections.<Post>emptyList());
	}

	@Test
	public void getOnlyOnePostAndSkipTheFirstPost() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post1 = mocks.mockPost(sone, "Post1").withTime(1000L).withText("1").create();
		Post post2 = mocks.mockPost(sone, "Post2").withTime(2000L).withText("1").create();
		Post post3 = mocks.mockPost(sone, "Post3").withTime(3000L).withText("1").create();
		Sone otherSone = mocks.mockSone("OtherSone").create();
		mocks.mockPostReply(otherSone, "Reply1").inReplyTo(post1).withText("R").create();
		SimpleFieldSet getPostsFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPosts")
				.put("Sone", "SoneId")
				.put("MaxPosts", "1")
				.put("StartPost", "1")
				.get();
		Response response = getPostsCommand.execute(getPostsFieldSet, null, DIRECT);
		verifyAnswer(response, "Posts");
		verifyPostsWithReplies(response.getReplyParameters(), "Posts.", asList(post2));
	}

	@Test
	public void getOnlyTheFirstTwoPosts() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post1 = mocks.mockPost(sone, "Post1").withTime(1000L).withText("1").create();
		Post post2 = mocks.mockPost(sone, "Post2").withTime(2000L).withText("1").create();
		Post post3 = mocks.mockPost(sone, "Post3").withTime(3000L).withText("1").create();
		Sone otherSone = mocks.mockSone("OtherSone").create();
		mocks.mockPostReply(otherSone, "Reply1").inReplyTo(post1).withText("R").create();
		SimpleFieldSet getPostsFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPosts")
				.put("Sone", "SoneId")
				.put("MaxPosts", "2")
				.get();
		Response response = getPostsCommand.execute(getPostsFieldSet, null, DIRECT);
		verifyAnswer(response, "Posts");
		verifyPostsWithReplies(response.getReplyParameters(), "Posts.", asList(post3, post2));
	}

	@Test
	public void skipTheFirstPost() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("SoneId").create();
		Post post1 = mocks.mockPost(sone, "Post1").withTime(1000L).withText("1").create();
		Post post2 = mocks.mockPost(sone, "Post2").withTime(2000L).withText("1").create();
		Post post3 = mocks.mockPost(sone, "Post3").withTime(3000L).withText("1").create();
		Sone otherSone = mocks.mockSone("OtherSone").create();
		mocks.mockPostReply(otherSone, "Reply1").inReplyTo(post1).withText("R").create();
		SimpleFieldSet getPostsFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPosts")
				.put("Sone", "SoneId")
				.put("StartPost", "1")
				.get();
		Response response = getPostsCommand.execute(getPostsFieldSet, null, DIRECT);
		verifyAnswer(response, "Posts");
		verifyPostsWithReplies(response.getReplyParameters(), "Posts.", asList(post2, post1));
	}

	@Test(expected = FcpException.class)
	public void aMissingSoneCausesAnError() throws FcpException {
		SimpleFieldSet getPostsFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPosts")
				.get();
		getPostsCommand.execute(getPostsFieldSet, null, DIRECT);
	}

}
