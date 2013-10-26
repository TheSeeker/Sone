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
import static net.pterodactylus.sone.data.Mocks.mockCore;
import static net.pterodactylus.sone.data.Mocks.mockDatabase;
import static net.pterodactylus.sone.data.Mocks.mockLocalSone;
import static net.pterodactylus.sone.database.PostBuilder.PostCreated;
import static net.pterodactylus.sone.freenet.fcp.Command.AccessType.DIRECT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.Database;
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
	private final Database database = mockDatabase();
	private final Core core = mockCore(database);
	private final CreatePostCommand createPostCommand = new CreatePostCommand(core);

	@Test
	public void verifyThatCreatingAPostWorks() throws FcpException {
		Sone sone = mockLocalSone(core, "Sone");
		mockLocalSone(core, "OtherSone");
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.put("Text", "Text of the post.")
				.put("Recipient", "OtherSone")
				.get();
		Response response = createPostCommand.execute(createPostFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(capturingPostCreated.post, notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostCreated"));
		assertThat(response.getReplyParameters().get("Post"), is(capturingPostCreated.post.getId()));
		assertThat(capturingPostCreated.post.getSone(), is(sone));
		assertThat(capturingPostCreated.post.getRecipientId(), is(of("OtherSone")));
		assertThat(capturingPostCreated.post.getText(), is("Text of the post."));
		assertThat(capturingPostCreated.post.getTime(), allOf(greaterThanOrEqualTo(now), lessThanOrEqualTo(currentTimeMillis())));
	}

	@Test
	public void verifyThatCreatingAPostWithoutRecipientWorks() throws FcpException {
		Sone sone = mockLocalSone(core, "Sone");
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.put("Text", "Text of the post.")
				.get();
		Response response = createPostCommand.execute(createPostFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(capturingPostCreated.post, notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("PostCreated"));
		assertThat(response.getReplyParameters().get("Post"), is(capturingPostCreated.post.getId()));
		assertThat(capturingPostCreated.post.getSone(), is(sone));
		assertThat(capturingPostCreated.post.getRecipientId(), is(Optional.<String>absent()));
		assertThat(capturingPostCreated.post.getText(), is("Text of the post."));
		assertThat(capturingPostCreated.post.getTime(), allOf(greaterThanOrEqualTo(now), lessThanOrEqualTo(currentTimeMillis())));
	}

	@Test
	public void verifyThatCreatingAPostDirectedToTheSendingSoneCausesAnError() throws FcpException {
		mockLocalSone(core, "Sone");
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.put("Recipient", "Sone")
				.put("Text", "Text of the post.")
				.get();
		Response response = createPostCommand.execute(createPostFieldSet, null, DIRECT);
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is("Error"));
		assertThat(capturingPostCreated.post, nullValue());
	}

	@Test(expected = FcpException.class)
	public void verifyThatCreatingAPostWithoutTextCausesAnError() throws FcpException {
		mockLocalSone(core, "Sone");
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

		SimpleFieldSet createPostFieldSet = new SimpleFieldSetBuilder()
				.put("Sone", "Sone")
				.get();
		createPostCommand.execute(createPostFieldSet, null, DIRECT);
	}

	@Test(expected = FcpException.class)
	public void verifyThatCreatingAPostWithoutSoneCausesAnError() throws FcpException {
		mockLocalSone(core, "Sone");
		CapturingPostCreated capturingPostCreated = new CapturingPostCreated();
		when(core.postCreated()).thenReturn(Optional.<PostCreated>of(capturingPostCreated));

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
