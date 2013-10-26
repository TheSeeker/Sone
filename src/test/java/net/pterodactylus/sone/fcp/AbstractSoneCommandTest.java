/*
 * Sone - AbstractSoneCommandTest.java - Copyright © 2013 David Roden
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

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeSone;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeString;
import static net.pterodactylus.sone.template.SoneAccessor.getNiceName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Profile;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;
import net.pterodactylus.sone.freenet.fcp.FcpException;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;

import com.google.common.base.Optional;
import org.junit.Test;
import org.mockito.Matchers;

/**
 * Unit test for {@link AbstractSoneCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractSoneCommandTest {

	private final Core core = mock(Core.class);
	private final Database database = mock(Database.class);
	private final AbstractSoneCommand abstractSoneCommand = new AbstractSoneCommand(core) {
		@Override
		public Response execute(SimpleFieldSet parameters, Bucket data, AccessType accessType) throws FcpException {
			return null;
		}
	};

	public AbstractSoneCommandTest() {
		when(core.getDatabase()).thenReturn(database);
	}

	@Test
	public void testStringEncoding() {
		String testString = prepareStringToBeEncoded();

		String encodedString = encodeString(testString);
		assertThat(encodedString, notNullValue());
		assertThat(encodedString.length(), is(testString.length() + 3));
	}

	private String prepareStringToBeEncoded() {
		StringBuilder testString = new StringBuilder();
		for (int i = 0; i < 4000; ++i) {
			testString.append((char) i);
		}
		return testString.toString();
	}

	@Test
	public void testEncodingASone() throws FSParseException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		SimpleFieldSet soneFieldSet = encodeSone(sone, "Prefix.", Optional.<Sone>absent());
		assertThat(soneFieldSet, notNullValue());
		assertThat(soneFieldSet.get("Prefix.Name"), is("test"));
		assertThat(soneFieldSet.get("Prefix.NiceName"), is("First M. Last"));
		assertThat(soneFieldSet.getLong("Prefix.LastUpdated"), is(sone.getTime()));
		assertThat(soneFieldSet.get("Prefix.Followed"), nullValue());
		assertThat(soneFieldSet.getInt("Prefix.Field.Count"), is(1));
		assertThat(soneFieldSet.get("Prefix.Field.0.Name"), is("Test1"));
		assertThat(soneFieldSet.get("Prefix.Field.0.Value"), is("Value1"));
	}

	@Test
	public void testEncodingAFollowedSone() throws FSParseException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Sone localSone = prepareLocalSoneThatFollowsEverybody();
		SimpleFieldSet soneFieldSet = encodeSone(sone, "Prefix.", of(localSone));
		assertThat(soneFieldSet, notNullValue());
		assertThat(soneFieldSet.get("Prefix.Name"), is("test"));
		assertThat(soneFieldSet.get("Prefix.NiceName"), is("First M. Last"));
		assertThat(soneFieldSet.getLong("Prefix.LastUpdated"), is(sone.getTime()));
		assertThat(soneFieldSet.get("Prefix.Followed"), is("true"));
		assertThat(soneFieldSet.getInt("Prefix.Field.Count"), is(1));
		assertThat(soneFieldSet.get("Prefix.Field.0.Name"), is("Test1"));
		assertThat(soneFieldSet.get("Prefix.Field.0.Value"), is("Value1"));
	}

	@Test
	public void testEncodingANotFollowedSone() throws FSParseException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Sone localSone = prepareLocalSoneThatFollowsNobody();
		SimpleFieldSet soneFieldSet = encodeSone(sone, "Prefix.", of(localSone));
		assertThat(soneFieldSet, notNullValue());
		assertThat(soneFieldSet.get("Prefix.Name"), is("test"));
		assertThat(soneFieldSet.get("Prefix.NiceName"), is("First M. Last"));
		assertThat(soneFieldSet.getLong("Prefix.LastUpdated"), is(sone.getTime()));
		assertThat(soneFieldSet.get("Prefix.Followed"), is("false"));
		assertThat(soneFieldSet.getInt("Prefix.Field.Count"), is(1));
		assertThat(soneFieldSet.get("Prefix.Field.0.Name"), is("Test1"));
		assertThat(soneFieldSet.get("Prefix.Field.0.Value"), is("Value1"));
	}

	private Sone prepareLocalSoneThatFollowsEverybody() {
		Sone sone = mock(Sone.class);
		when(sone.hasFriend(Matchers.<String>any())).thenReturn(true);
		return sone;
	}

	private Sone prepareLocalSoneThatFollowsNobody() {
		Sone sone = mock(Sone.class);
		when(sone.hasFriend(Matchers.<String>any())).thenReturn(false);
		return sone;
	}

	@Test
	public void testEncodingMultipleSones() throws FSParseException {
		List<Sone> sones = prepareMultipleSones();
		SimpleFieldSet sonesFieldSet = AbstractSoneCommand.encodeSones(sones, "Prefix.");
		assertThat(sonesFieldSet, notNullValue());
		assertThat(sonesFieldSet.getInt("Prefix.Count"), is(sones.size()));
		assertThat(sonesFieldSet.get("Prefix.0.ID"), is(sones.get(0).getId()));
		assertThat(sonesFieldSet.get("Prefix.0.Name"), is(sones.get(0).getName()));
		assertThat(sonesFieldSet.get("Prefix.0.NiceName"), is(getNiceName(sones.get(0))));
		assertThat(sonesFieldSet.getLong("Prefix.0.Time"), is(sones.get(0).getTime()));
		assertThat(sonesFieldSet.get("Prefix.1.ID"), is(sones.get(1).getId()));
		assertThat(sonesFieldSet.get("Prefix.1.Name"), is(sones.get(1).getName()));
		assertThat(sonesFieldSet.get("Prefix.1.NiceName"), is(getNiceName(sones.get(1))));
		assertThat(sonesFieldSet.getLong("Prefix.1.Time"), is(sones.get(1).getTime()));
		assertThat(sonesFieldSet.get("Prefix.2.ID"), is(sones.get(2).getId()));
		assertThat(sonesFieldSet.get("Prefix.2.Name"), is(sones.get(2).getName()));
		assertThat(sonesFieldSet.get("Prefix.2.NiceName"), is(getNiceName(sones.get(2))));
		assertThat(sonesFieldSet.getLong("Prefix.2.Time"), is(sones.get(2).getTime()));
	}

	private List<Sone> prepareMultipleSones() {
		Sone sone1 = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test1", "Alpha", "A.", "First", (long) (Math.random() * Long.MAX_VALUE));
		Sone sone2 = createSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg", "Test2", "Beta", "B.", "Second", (long) (Math.random() * Long.MAX_VALUE));
		Sone sone3 = createSone("-1Q6LhHvx91C1mSjOS3zznRSNUC4OxoHUbhIgBAyW1U", "Test3", "Gamma", "C.", "Third", (long) (Math.random() * Long.MAX_VALUE));
		return asList(sone1, sone2, sone3);
	}

	private Sone createSone(String id, String name, String firstName, String middleName, String lastName, long time) {
		Sone sone = mock(Sone.class);
		when(sone.getId()).thenReturn(id);
		when(sone.getName()).thenReturn(name);
		when(sone.getProfile()).thenReturn(prepareProfile(sone, firstName, middleName, lastName));
		when(sone.getTime()).thenReturn(time);
		return sone;
	}

	private Sone createLocalSone(String id, String name, String firstName, String middleName, String lastName, long time) {
		Sone sone = mock(Sone.class);
		when(sone.getId()).thenReturn(id);
		when(sone.getName()).thenReturn(name);
		when(sone.getProfile()).thenReturn(prepareProfile(sone, firstName, middleName, lastName));
		when(sone.getTime()).thenReturn(time);
		when(sone.isLocal()).thenReturn(true);
		return sone;
	}

	private Profile prepareProfile(Sone sone, String firstName, String middleName, String lastName) {
		Profile profile = new Profile(sone).modify().setFirstName(firstName).setMiddleName(middleName).setLastName(lastName).update();
		profile.setField(profile.addField("Test1"), "Value1");
		return profile;
	}

	@Test
	public void testEncodingReplies() throws FSParseException {
		List<PostReply> postReplies = preparePostReplies();
		SimpleFieldSet postRepliesFieldSet = AbstractSoneCommand.encodeReplies(postReplies, "Prefix.");
		assertThat(postRepliesFieldSet, notNullValue());
		assertThat(postRepliesFieldSet.getInt("Prefix.Replies.Count"), is(postReplies.size()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.0.ID"), is(postReplies.get(0).getId()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.0.Sone"), is(postReplies.get(0).getSone().getId()));
		assertThat(postRepliesFieldSet.getLong("Prefix.Replies.0.Time"), is(postReplies.get(0).getTime()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.0.Text"), is(postReplies.get(0).getText()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.1.ID"), is(postReplies.get(1).getId()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.1.Sone"), is(postReplies.get(1).getSone().getId()));
		assertThat(postRepliesFieldSet.getLong("Prefix.Replies.1.Time"), is(postReplies.get(1).getTime()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.1.Text"), is(postReplies.get(1).getText()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.2.ID"), is(postReplies.get(2).getId()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.2.Sone"), is(postReplies.get(2).getSone().getId()));
		assertThat(postRepliesFieldSet.getLong("Prefix.Replies.2.Time"), is(postReplies.get(2).getTime()));
		assertThat(postRepliesFieldSet.get("Prefix.Replies.2.Text"), is(postReplies.get(2).getText()));
	}

	private List<PostReply> preparePostReplies() {
		Sone sone1 = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test1", "Alpha", "A.", "First", (long) (Math.random() * Long.MAX_VALUE));
		Sone sone2 = createSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg", "Test2", "Beta", "B.", "Second", (long) (Math.random() * Long.MAX_VALUE));
		Sone sone3 = createSone("-1Q6LhHvx91C1mSjOS3zznRSNUC4OxoHUbhIgBAyW1U", "Test3", "Gamma", "C.", "Third", (long) (Math.random() * Long.MAX_VALUE));
		PostReply postReply1 = createPostReply(sone1, "Text 1");
		PostReply postReply2 = createPostReply(sone2, "Text 2");
		PostReply postReply3 = createPostReply(sone3, "Text 3");
		return asList(postReply1, postReply2, postReply3);
	}

	private PostReply createPostReply(Sone sone, String text) {
		PostReply postReply = mock(PostReply.class);
		when(postReply.getId()).thenReturn(randomUUID().toString());
		when(postReply.getSone()).thenReturn(sone);
		when(postReply.getTime()).thenReturn((long) (Math.random() * Long.MAX_VALUE));
		when(postReply.getText()).thenReturn(text);
		return postReply;
	}

	@Test
	public void testEncodingLikes() throws FSParseException {
		List<Sone> likes = prepareMultipleSones();
		SimpleFieldSet likesFieldSet = AbstractSoneCommand.encodeLikes(likes, "Prefix.");
		assertThat(likesFieldSet, notNullValue());
		assertThat(likesFieldSet.getInt("Prefix.Count"), is(likes.size()));
		assertThat(likesFieldSet.get("Prefix.0.ID"), is(likes.get(0).getId()));
		assertThat(likesFieldSet.get("Prefix.1.ID"), is(likes.get(1).getId()));
		assertThat(likesFieldSet.get("Prefix.2.ID"), is(likes.get(2).getId()));
	}

	@Test
	public void testParsingAMandatorySone() throws FcpException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		when(core.getSone(eq("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E"))).thenReturn(of(sone));
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Sone parsedSone = abstractSoneCommand.getMandatorySone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone, is(sone));
	}

	@Test(expected = FcpException.class)
	public void testParsingANonExistingMandatorySoneCausesAnError() throws FcpException {
		when(core.getSone(Matchers.<String>any())).thenReturn(Optional.<Sone>absent());
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatorySone(soneFieldSet, "Sone");
	}

	@Test(expected = FcpException.class)
	public void testParsingAMandatorySoneFromANonExistingFieldCausesAnError() throws FcpException {
		when(core.getSone(Matchers.<String>any())).thenReturn(Optional.<Sone>absent());
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatorySone(soneFieldSet, "RealSone");
	}

	@Test
	public void testParsingAMandatoryLocalSone() throws FcpException {
		Sone sone = createLocalSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		when(core.getSone(eq("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E"))).thenReturn(of(sone));
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Sone parsedSone = abstractSoneCommand.getMandatoryLocalSone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone, is(sone));
		assertThat(parsedSone.isLocal(), is(true));
	}

	@Test(expected = FcpException.class)
	public void testParsingANonLocalSoneAsMandatoryLocalSoneCausesAnError() throws FcpException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		when(core.getSone(eq("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E"))).thenReturn(of(sone));
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatoryLocalSone(soneFieldSet, "Sone");
	}

	@Test(expected = FcpException.class)
	public void testParsingAMandatoryLocalSoneFromANonExistingFieldCausesAnError() throws FcpException {
		when(core.getSone(Matchers.<String>any())).thenReturn(Optional.<Sone>absent());
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatoryLocalSone(soneFieldSet, "RealSone");
	}

	@Test
	public void testParsingAnExistingOptionalSone() throws FcpException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		when(core.getSone(eq("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E"))).thenReturn(of(sone));
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Optional<Sone> parsedSone = abstractSoneCommand.getOptionalSone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone.isPresent(), is(true));
		assertThat(parsedSone.get(), is(sone));
	}

	@Test
	public void testParsingANonExistingOptionalSone() throws FcpException {
		when(core.getSone(Matchers.<String>any())).thenReturn(Optional.<Sone>absent());
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Optional<Sone> parsedSone = abstractSoneCommand.getOptionalSone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone.isPresent(), is(false));
	}

	@Test(expected = FcpException.class)
	public void testParsingAnOptionalSoneFromANonExistingFieldCausesAnError() throws FcpException {
		when(core.getSone(Matchers.<String>any())).thenReturn(Optional.<Sone>absent());
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getOptionalSone(soneFieldSet, "RealSone");
	}

	@Test
	public void testParsingAPost() throws FcpException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Post post = createPost(sone, null, (long) (Math.random() * Long.MAX_VALUE), "Some Text.");
		when(database.getPost(eq(post.getId()))).thenReturn(of(post));
		SimpleFieldSet postFieldSet = new SimpleFieldSetBuilder().put("Post", post.getId()).get();
		Post parsedPost = abstractSoneCommand.getPost(postFieldSet, "Post");
		assertThat(parsedPost, notNullValue());
		assertThat(parsedPost, is(post));
	}

	private Post createPost(Sone sone, String recipient, long time, String text) {
		Post post = mock(Post.class);
		when(post.getId()).thenReturn(randomUUID().toString());
		when(post.getSone()).thenReturn(sone);
		when(post.getRecipientId()).thenReturn(fromNullable(recipient));
		when(post.getTime()).thenReturn(time);
		when(post.getText()).thenReturn(text);
		return post;
	}

	@Test(expected = FcpException.class)
	public void testThatTryingToParseANonExistingPostCausesAnError() throws FcpException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Post post = createPost(sone, null, (long) (Math.random() * Long.MAX_VALUE), "Some Text.");
		when(database.getPost(Matchers.<String>any())).thenReturn(Optional.<Post>absent());
		SimpleFieldSet postFieldSet = new SimpleFieldSetBuilder().put("Post", post.getId()).get();
		abstractSoneCommand.getPost(postFieldSet, "Post");
	}

	@Test(expected = FcpException.class)
	public void testThatTryingToParseAPostFromANonExistingFieldCausesAnError() throws FcpException {
		SimpleFieldSet postFieldSet = new SimpleFieldSetBuilder().get();
		abstractSoneCommand.getPost(postFieldSet, "Post");
	}

	@Test
	public void testParsingAReply() throws FcpException {
		PostReply reply = createPostReply();
		when(database.getPostReply(eq(reply.getId()))).thenReturn(of(reply));
		SimpleFieldSet replyFieldSet = new SimpleFieldSetBuilder().put("Reply", reply.getId()).get();
		PostReply parsedReply = abstractSoneCommand.getReply(replyFieldSet, "Reply");
		assertThat(parsedReply, notNullValue());
		assertThat(parsedReply, is(reply));
	}

	private PostReply createPostReply() {
		PostReply postReply = mock(PostReply.class);
		when(postReply.getId()).thenReturn(randomUUID().toString());
		return postReply;
	}

	@Test(expected = FcpException.class)
	public void testParsingANonExistingReply() throws FcpException {
		PostReply reply = createPostReply();
		when(database.getPostReply(Matchers.<String>any())).thenReturn(Optional.<PostReply>absent());
		SimpleFieldSet replyFieldSet = new SimpleFieldSetBuilder().put("Reply", reply.getId()).get();
		abstractSoneCommand.getReply(replyFieldSet, "Reply");
	}

	@Test(expected = FcpException.class)
	public void testParsingAReplyFromANonExistingField() throws FcpException {
		SimpleFieldSet replyFieldSet = new SimpleFieldSetBuilder().get();
		abstractSoneCommand.getReply(replyFieldSet, "Reply");
	}

	@Test
	public void testEncodingAPostWithoutRecipientAndReplies() throws FSParseException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Post post = createPost(sone, null, (long) (Math.random() * Long.MAX_VALUE), "Some Text.");
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePost(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		assertThat(postFieldSet.get("Post.ID"), is(post.getId()));
		assertThat(postFieldSet.get("Post.Sone"), is(sone.getId()));
		assertThat(postFieldSet.get("Post.Recipient"), nullValue());
		assertThat(postFieldSet.getLong("Post.Time"), is(post.getTime()));
		assertThat(postFieldSet.get("Post.Text"), is(post.getText()));
	}

	@Test
	public void testEncodingAPostWithRecipientWithoutReplies() throws FSParseException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Post post = createPost(sone, "KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg", (long) (Math.random() * Long.MAX_VALUE), "Some Text.");
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePost(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		assertThat(postFieldSet.get("Post.ID"), is(post.getId()));
		assertThat(postFieldSet.get("Post.Sone"), is(sone.getId()));
		assertThat(postFieldSet.get("Post.Recipient"), is("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg"));
		assertThat(postFieldSet.getLong("Post.Time"), is(post.getTime()));
		assertThat(postFieldSet.get("Post.Text"), is(post.getText()));
	}

	@Test
	public void testEncodingAPostWithoutRecipientWithReplies() throws FSParseException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Post post = createPost(sone, null, (long) (Math.random() * Long.MAX_VALUE), "Some Text.");
		PostReply postReply = createPostReply(sone, "Reply.");
		when(post.getReplies()).thenReturn(asList(postReply));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostWithReplies(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		assertThat(postFieldSet.get("Post.ID"), is(post.getId()));
		assertThat(postFieldSet.get("Post.Sone"), is(sone.getId()));
		assertThat(postFieldSet.get("Post.Recipient"), nullValue());
		assertThat(postFieldSet.getLong("Post.Time"), is(post.getTime()));
		assertThat(postFieldSet.get("Post.Text"), is(post.getText()));
		assertThat(postFieldSet.getInt("Post.Replies.Count"), is(1));
		assertThat(postFieldSet.get("Post.Replies.0.ID"), is(postReply.getId()));
		assertThat(postFieldSet.get("Post.Replies.0.Sone"), is(postReply.getSone().getId()));
		assertThat(postFieldSet.getLong("Post.Replies.0.Time"), is(postReply.getTime()));
		assertThat(postFieldSet.get("Post.Replies.0.Text"), is(postReply.getText()));
	}

	@Test
	public void testEncodingAPostWithRecipientAndReplies() throws FSParseException {
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
		Post post = createPost(sone, "KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg", (long) (Math.random() * Long.MAX_VALUE), "Some Text.");
		PostReply postReply = createPostReply(sone, "Reply.");
		when(post.getReplies()).thenReturn(asList(postReply));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostWithReplies(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		assertThat(postFieldSet.get("Post.ID"), is(post.getId()));
		assertThat(postFieldSet.get("Post.Sone"), is(sone.getId()));
		assertThat(postFieldSet.get("Post.Recipient"), is("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg"));
		assertThat(postFieldSet.getLong("Post.Time"), is(post.getTime()));
		assertThat(postFieldSet.get("Post.Text"), is(post.getText()));
		assertThat(postFieldSet.getInt("Post.Replies.Count"), is(1));
		assertThat(postFieldSet.get("Post.Replies.0.ID"), is(postReply.getId()));
		assertThat(postFieldSet.get("Post.Replies.0.Sone"), is(postReply.getSone().getId()));
		assertThat(postFieldSet.getLong("Post.Replies.0.Time"), is(postReply.getTime()));
		assertThat(postFieldSet.get("Post.Replies.0.Text"), is(postReply.getText()));
	}

}
