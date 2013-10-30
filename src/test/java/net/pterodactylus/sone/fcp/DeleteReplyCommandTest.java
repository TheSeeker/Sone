/*
 * Sone - DeleteReplyCommandTest.java - Copyright © 2013 David Roden
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
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.support.SimpleFieldSet;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for {@link DeleteReplyCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DeleteReplyCommandTest {

	private final Mocks mocks = new Mocks();
	private final DeleteReplyCommand deleteReplyCommand = new DeleteReplyCommand(mocks.core);

	@Test
	public void verifyThatDeletingAReplyWorks() throws FcpException {
		Sone sone = mocks.mockSone("SoneId").local().create();
		PostReply postReply = mocks.mockPostReply(sone, "ReplyId");
		ArgumentCaptor<PostReply> postReplyCaptor = forClass(PostReply.class);
		doNothing().when(mocks.core).deleteReply(postReplyCaptor.capture());
		SimpleFieldSet deleteReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeleteReply")
				.put("Reply", "ReplyId")
				.get();
		Response response = deleteReplyCommand.execute(deleteReplyFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(postReplyCaptor.getValue(), is(postReply));
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("ReplyDeleted"));
	}

	@Test(expected = FcpException.class)
	public void verifyThatDeletingAReplyWithAnInvalidReplyIdCausesAnError() throws FcpException {
		SimpleFieldSet deleteReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeleteReply")
				.put("Reply", "ReplyId")
				.get();
		deleteReplyCommand.execute(deleteReplyFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void verifyThatDeletingAReplyWithoutReplyIdCausesAnError() throws FcpException {
		SimpleFieldSet deleteReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeleteReply")
				.get();
		deleteReplyCommand.execute(deleteReplyFieldSet, null, DIRECT);
	}

	@Test
	public void verifyThatDeletingAReplyFromANonLocalSoneCausesAnError() throws FcpException {
		Sone sone = mocks.mockSone("SoneId").create();
		mocks.mockPostReply(sone, "ReplyId");
		SimpleFieldSet deleteReplyFieldSet = new SimpleFieldSetBuilder()
				.put("Message", "DeleteReply")
				.put("Reply", "ReplyId")
				.get();
		Response response = deleteReplyCommand.execute(deleteReplyFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("Error"));
		assertThat(response.getReplyParameters().get("ErrorCode"), is("401"));
	}

}
