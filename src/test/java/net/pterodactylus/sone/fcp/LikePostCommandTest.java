/*
 * Sone - LikePostCommandTest.java - Copyright © 2013 David Roden
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

import static net.pterodactylus.sone.fcp.Verifiers.verifyAnswer;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
 * Unit test for {@link LikePostCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class LikePostCommandTest {

	private final Mocks mocks = new Mocks();
	private final LikePostCommand likePostCommand = new LikePostCommand(mocks.core);

	@Test
	public void thePostWasLiked() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("LSone").local().create();
		Post post = mocks.mockPost(sone, "Post").create();
		SimpleFieldSet likePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikePost")
				.put("Sone", "LSone")
				.put("Post", "Post")
				.get();
		Response response = likePostCommand.execute(likePostFieldSet, null, DIRECT);
		verifyAnswer(response, "PostLiked");
		assertThat(response.getReplyParameters().getInt("LikeCount"), is(1));
		verify(post).like(eq(sone));
	}

	@Test(expected = FcpException.class)
	public void nonExistingLocalSoneCausesAnError() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("LSone").local().create();
		mocks.mockPost(sone, "Post").create();
		SimpleFieldSet likePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikePost")
				.put("Sone", "Sone")
				.put("Post", "Post")
				.get();
		likePostCommand.execute(likePostFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void missingLocalSoneFieldCausesAnError() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("LSone").local().create();
		mocks.mockPost(sone, "Post").create();
		SimpleFieldSet likePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikePost")
				.put("Post", "Post")
				.get();
		likePostCommand.execute(likePostFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void nonExistingPostCausesAnError() throws FcpException, FSParseException {
		mocks.mockSone("LSone").local().create();
		SimpleFieldSet likePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikePost")
				.put("Sone", "LSone")
				.put("Post", "Post")
				.get();
		likePostCommand.execute(likePostFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void missingPostFieldCausesAnError() throws FcpException, FSParseException {
		mocks.mockSone("LSone").local().create();
		SimpleFieldSet likePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikePost")
				.put("Sone", "LSone")
				.get();
		likePostCommand.execute(likePostFieldSet, null, DIRECT);
	}

	@Test
	public void multipleLikesDontCountMultipleTimes() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("LSone").local().create();
		Post post = mocks.mockPost(sone, "Post").create();
		post.like(sone);
		SimpleFieldSet likePostFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikePost")
				.put("Sone", "LSone")
				.put("Post", "Post")
				.get();
		Response response = likePostCommand.execute(likePostFieldSet, null, DIRECT);
		verifyAnswer(response, "PostLiked");
		assertThat(response.getReplyParameters().getInt("LikeCount"), is(1));
		verify(post, times(2)).like(eq(sone));
	}

}
