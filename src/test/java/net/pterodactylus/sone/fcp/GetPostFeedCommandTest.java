/*
 * Sone - GetPostFeedCommandTest.java - Copyright © 2013 David Roden
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

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.DAYS;
import static net.pterodactylus.sone.fcp.Verifiers.verifyPost;
import static net.pterodactylus.sone.fcp.Verifiers.verifyPostReplies;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link  GetPostFeedCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetPostFeedCommandTest {

	private final Mocks mocks = new Mocks();
	private final GetPostFeedCommand getPostFeedCommand = new GetPostFeedCommand(mocks.core);
	private Sone remoteSone;
	private Sone friendSone;
	private Sone localSone;
	private Post localPost;
	private PostReply friendReplyToLocalPost;
	private PostReply remoteReplyToLocalPost;
	private Post friendPost;
	private Post remotePost;

	@Before
	public void setup() {
		remoteSone = mocks.mockSone("RSone").create();
		friendSone = mocks.mockSone("FSone").create();
		localSone = mocks.mockSone("LSone").local().withFriends(asList(friendSone.getId(), "NonExistingSone")).create();
		localPost = mocks.mockPost(localSone, "LPost").withTime(daysBefore(11)).withText("My post.").create();
		friendReplyToLocalPost = mocks.mockPostReply(friendSone, "FReply").inReplyTo(localPost).withTime(daysBefore(9)).withText("No.").create();
		remoteReplyToLocalPost = mocks.mockPostReply(remoteSone, "RReply").inReplyTo(localPost).withTime(daysBefore(7)).withText("Yes.").create();
		mocks.mockPostReply(remoteSone, "FutureReply1").inReplyTo(localPost).withTime(daysBefore(-1)).withText("Future!").create();
		friendPost = mocks.mockPost(friendSone, "FPost").withTime(daysBefore(12)).withText("The friend's post.").create();
		remotePost = mocks.mockPost(remoteSone, "RPost").withTime(daysBefore(13)).withText("Hello, LSone!").withRecipient(localSone.getId()).create();
	}

	@Test
	public void getFeedForLocalSone() throws FcpException, FSParseException {
		SimpleFieldSet getPostFeedFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPostFeed")
				.put("Sone", "LSone")
				.get();
		Response response = getPostFeedCommand.execute(getPostFeedFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostFeed"));
		assertThat(response.getReplyParameters().getInt("Posts.Count"), is(3));
		verifyPost(response.getReplyParameters(), "Posts.0.", localPost);
		verifyPostReplies(response.getReplyParameters(), "Posts.0.Replies.", asList(friendReplyToLocalPost, remoteReplyToLocalPost));
		verifyPost(response.getReplyParameters(), "Posts.1.", friendPost);
		verifyPost(response.getReplyParameters(), "Posts.2.", remotePost);
	}

	@Test
	public void getFeedForLocalSoneSkippingTheFirstPost() throws FcpException, FSParseException {
		SimpleFieldSet getPostFeedFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPostFeed")
				.put("Sone", "LSone")
				.put("StartPost", "1")
				.get();
		Response response = getPostFeedCommand.execute(getPostFeedFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostFeed"));
		assertThat(response.getReplyParameters().getInt("Posts.Count"), is(2));
		verifyPost(response.getReplyParameters(), "Posts.0.", friendPost);
		verifyPost(response.getReplyParameters(), "Posts.1.", remotePost);
	}

	@Test
	public void getFeedForLocalSoneWithTwoPosts() throws FcpException, FSParseException {
		SimpleFieldSet getPostFeedFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPostFeed")
				.put("Sone", "LSone")
				.put("MaxPosts", "2")
				.get();
		Response response = getPostFeedCommand.execute(getPostFeedFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostFeed"));
		assertThat(response.getReplyParameters().getInt("Posts.Count"), is(2));
		verifyPost(response.getReplyParameters(), "Posts.0.", localPost);
		verifyPostReplies(response.getReplyParameters(), "Posts.0.Replies.", asList(friendReplyToLocalPost, remoteReplyToLocalPost));
		verifyPost(response.getReplyParameters(), "Posts.1.", friendPost);
	}

	@Test
	public void getFeedForLocalSoneWithOnePostSkippingTheFirst() throws FcpException, FSParseException {
		SimpleFieldSet getPostFeedFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPostFeed")
				.put("Sone", "LSone")
				.put("MaxPosts", "1")
				.put("StartPost", "1")
				.get();
		Response response = getPostFeedCommand.execute(getPostFeedFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostFeed"));
		assertThat(response.getReplyParameters().getInt("Posts.Count"), is(1));
		verifyPost(response.getReplyParameters(), "Posts.0.", friendPost);
	}

	@Test
	public void getFeedForLocalSoneSkippingMorePostsThanThereAre() throws FcpException, FSParseException {
		SimpleFieldSet getPostFeedFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPostFeed")
				.put("Sone", "LSone")
				.put("StartPost", "10")
				.get();
		Response response = getPostFeedCommand.execute(getPostFeedFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostFeed"));
		assertThat(response.getReplyParameters().getInt("Posts.Count"), is(0));
	}

	@Test(expected = FcpException.class)
	public void getFeedWithoutLocalSone() throws FcpException, FSParseException {
		SimpleFieldSet getPostFeedFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "GetPostFeed")
				.get();
		getPostFeedCommand.execute(getPostFeedFieldSet, null, DIRECT);
	}

	private long daysBefore(int days) {
		return currentTimeMillis() - DAYS.toMillis(days);
	}

}
