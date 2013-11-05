/*
 * Sone - LikeReplyCommandTest.java - Copyright © 2013 David Roden
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
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import org.junit.Test;

/**
 * Unit test for {@link LikeReplyCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class LikeReplyCommandTest {

	private final Mocks mocks = new Mocks();
	private final LikeReplyCommand likeReplyCommand = new LikeReplyCommand(mocks.core);

	@Test
	public void theReplyWasLiked() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("Sone").local().create();
		PostReply postReply = mocks.mockPostReply(sone, "Reply").create();
		SimpleFieldSet likeReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikeReply")
				.put("Sone", "Sone")
				.put("Reply", "Reply")
				.get();
		Response response = likeReplyCommand.execute(likeReplyFieldSet, null, DIRECT);
		verifyAnswer(response, "ReplyLiked");
		verify(postReply).like(eq(sone));
		assertThat(response.getReplyParameters().getInt("LikeCount"), is(1));
	}

	@Test
	public void likingALikedReplyDoesNotIncreaseTheLikeCount() throws FcpException, FSParseException {
		Sone sone = mocks.mockSone("Sone").local().create();
		PostReply postReply = mocks.mockPostReply(sone, "Reply").create();
		postReply.like(sone);
		SimpleFieldSet likeReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikeReply")
				.put("Sone", "Sone")
				.put("Reply", "Reply")
				.get();
		Response response = likeReplyCommand.execute(likeReplyFieldSet, null, DIRECT);
		verifyAnswer(response, "ReplyLiked");
		verify(postReply, times(2)).like(eq(sone));
		assertThat(response.getReplyParameters().getInt("LikeCount"), is(1));
	}

	@Test(expected = FcpException.class)
	public void nonExistingSoneCausesAnError() throws FcpException, FSParseException {
		SimpleFieldSet likeReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikeReply")
				.put("Sone", "FalseSone")
				.put("Reply", "Reply")
				.get();
		likeReplyCommand.execute(likeReplyFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void missingSoneFieldCausesAnError() throws FcpException, FSParseException {
		SimpleFieldSet likeReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikeReply")
				.put("Reply", "Reply")
				.get();
		likeReplyCommand.execute(likeReplyFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void nonExistingReplyCausesAnError() throws FcpException, FSParseException {
		SimpleFieldSet likeReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikeReply")
				.put("Sone", "Sone")
				.put("Reply", "FalseReply")
				.get();
		likeReplyCommand.execute(likeReplyFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void missingReplyFieldCausesAnError() throws FcpException, FSParseException {
		SimpleFieldSet likeReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "LikeReply")
				.put("Sone", "Sone")
				.get();
		likeReplyCommand.execute(likeReplyFieldSet, null, DIRECT);
	}

}
