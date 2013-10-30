/*
 * Sone - CreatePostCommandTest.java - Copyright © 2013 David Roden
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
import static java.lang.System.currentTimeMillis;
import static net.pterodactylus.sone.database.PostBuilder.PostCreated;
import static net.pterodactylus.sone.fcp.Verifiers.verifyAnswer;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.support.SimpleFieldSet;

import com.google.common.base.Optional;
import org.junit.Test;

/**
 * Unit test for {@link CreatePostCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class CreatePostCommandTest {

	private final long now = currentTimeMillis();
	private final Mocks mocks = new Mocks();
	private final CreatePostCommand createPostCommand = new CreatePostCommand(mocks.core);

	@Test
	public void verifyThatCreatingAPostWorks() throws FcpException {
		Sone sone = mocks.mockSone("Sone").local().create();
		mocks.mockSone("OtherSone").local().create();
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(mocks.core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.put("Text", "Text of the post.")
				.put("Recipient", "OtherSone")
				.get();
		Response response = createPostCommand.execute(createPostFieldSet, null, DIRECT);
		verifyAnswer(response, "PostCreated");
		assertThat(response.getReplyParameters().get("Post"), is(capturingPostCreated.post.getId()));
		assertThat(capturingPostCreated.post.getSone(), is(sone));
		assertThat(capturingPostCreated.post.getRecipientId(), is(of("OtherSone")));
		assertThat(capturingPostCreated.post.getText(), is("Text of the post."));
		assertThat(capturingPostCreated.post.getTime(), allOf(greaterThanOrEqualTo(now), lessThanOrEqualTo(currentTimeMillis())));
	}

	@Test
	public void verifyThatCreatingAPostWithoutRecipientWorks() throws FcpException {
		Sone sone = mocks.mockSone("Sone").local().create();
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(mocks.core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.put("Text", "Text of the post.")
				.get();
		Response response = createPostCommand.execute(createPostFieldSet, null, DIRECT);
		verifyAnswer(response, "PostCreated");
		assertThat(response.getReplyParameters().get("Post"), is(capturingPostCreated.post.getId()));
		assertThat(capturingPostCreated.post.getSone(), is(sone));
		assertThat(capturingPostCreated.post.getRecipientId(), is(Optional.<String>absent()));
		assertThat(capturingPostCreated.post.getText(), is("Text of the post."));
		assertThat(capturingPostCreated.post.getTime(), allOf(greaterThanOrEqualTo(now), lessThanOrEqualTo(currentTimeMillis())));
	}

	@Test
	public void verifyThatCreatingAPostDirectedToTheSendingSoneCausesAnError() throws FcpException {
		mocks.mockSone("Sone").local().create();
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(mocks.core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.put("Recipient", "Sone")
				.put("Text", "Text of the post.")
				.get();
		Response response = createPostCommand.execute(createPostFieldSet, null, DIRECT);
		verifyAnswer(response, "Error");
		assertThat(capturingPostCreated.post, nullValue());
	}

	@Test(expected = FcpException.class)
	public void verifyThatCreatingAPostWithoutTextCausesAnError() throws FcpException {
		mocks.mockSone("Sone").create();
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(mocks.core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.get();
		createPostCommand.execute(createPostFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void verifyThatCreatingAPostWithoutSoneCausesAnError() throws FcpException {
		mocks.mockSone("Sone").local().create();
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(mocks.core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Text", "Text of the post.")
				.get();
		createPostCommand.execute(createPostFieldSet, null, DIRECT);
	}

	private static class CapturingPostCreated implements PostCreated {

		public Post post;

		@Override
		public void postCreated(Post post) {
			this.post = post;
		}

	}

}
