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

import static com.google.common.base.Optional.of;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.DAYS;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeSone;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeString;
import static net.pterodactylus.sone.fcp.Verifiers.verifyPostWithReplies;
import static net.pterodactylus.sone.fcp.Verifiers.verifyPosts;
import static net.pterodactylus.sone.fcp.Verifiers.verifyPostsWithReplies;
import static net.pterodactylus.sone.template.SoneAccessor.getNiceName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
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

	private final Mocks mocks = new Mocks();
	private final AbstractSoneCommand abstractSoneCommand = new AbstractSoneCommand(mocks.core) {
		@Override
		public Response execute(SimpleFieldSet parameters, Bucket data, AccessType accessType) throws FcpException {
			return null;
		}
	};

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
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
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
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
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
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
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
		Sone sone1 = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test1").withProfileName("Alpha", "A.", "First").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone2 = mocks.mockSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withName("Test2").withProfileName("Beta", "B.", "Second").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone3 = mocks.mockSone("-1Q6LhHvx91C1mSjOS3zznRSNUC4OxoHUbhIgBAyW1U").withName("Test3").withProfileName("Gamma", "C.", "Third").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		return asList(sone1, sone2, sone3);
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
		Sone sone1 = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test1").withProfileName("Alpha", "A.", "First").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone2 = mocks.mockSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withName("Test2").withProfileName("Beta", "B.", "Second").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone3 = mocks.mockSone("-1Q6LhHvx91C1mSjOS3zznRSNUC4OxoHUbhIgBAyW1U").withName("Test3").withProfileName("Gamma", "C.", "Third").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		PostReply postReply1 = mocks.mockPostReply(sone1, randomUUID().toString()).withTime(currentTimeMillis()).withText("Text 1").create();
		PostReply postReply2 = mocks.mockPostReply(sone2, randomUUID().toString()).withTime(currentTimeMillis()).withText("Text 2").create();
		PostReply postReply3 = mocks.mockPostReply(sone3, randomUUID().toString()).withTime(currentTimeMillis()).withText("Text 3").create();
		return asList(postReply1, postReply2, postReply3);
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
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Sone parsedSone = abstractSoneCommand.getMandatorySone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone, is(sone));
	}

	@Test(expected = FcpException.class)
	public void testParsingANonExistingMandatorySoneCausesAnError() throws FcpException {
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatorySone(soneFieldSet, "Sone");
	}

	@Test(expected = FcpException.class)
	public void testParsingAMandatorySoneFromANonExistingFieldCausesAnError() throws FcpException {
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatorySone(soneFieldSet, "RealSone");
	}

	@Test
	public void testParsingAMandatoryLocalSone() throws FcpException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").local().withName("Test").withProfileName("First", "M.", "Last").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Sone parsedSone = abstractSoneCommand.getMandatoryLocalSone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone, is(sone));
		assertThat(parsedSone.isLocal(), is(true));
	}

	@Test(expected = FcpException.class)
	public void testParsingANonLocalSoneAsMandatoryLocalSoneCausesAnError() throws FcpException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatoryLocalSone(soneFieldSet, "Sone");
	}

	@Test(expected = FcpException.class)
	public void testParsingAMandatoryLocalSoneFromANonExistingFieldCausesAnError() throws FcpException {
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		abstractSoneCommand.getMandatoryLocalSone(soneFieldSet, "RealSone");
	}

	@Test
	public void testParsingAnExistingOptionalSone() throws FcpException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Optional<Sone> parsedSone = abstractSoneCommand.getOptionalSone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone.isPresent(), is(true));
		assertThat(parsedSone.get(), is(sone));
	}

	@Test
	public void testParsingANonExistingOptionalSone() throws FcpException {
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Optional<Sone> parsedSone = abstractSoneCommand.getOptionalSone(soneFieldSet, "Sone");
		assertThat(parsedSone, notNullValue());
		assertThat(parsedSone.isPresent(), is(false));
	}

	@Test
	public void testParsingAnOptionalSoneFromANonExistingField() throws FcpException {
		SimpleFieldSet soneFieldSet = new SimpleFieldSetBuilder().put("Sone", "jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").get();
		Optional<Sone> sone = abstractSoneCommand.getOptionalSone(soneFieldSet, "RealSone");
		assertThat(sone, notNullValue());
		assertThat(sone.isPresent(), is(false));
	}

	@Test
	public void testParsingAPost() throws FcpException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post = mocks.mockPost(sone, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		SimpleFieldSet postFieldSet = new SimpleFieldSetBuilder().put("Post", post.getId()).get();
		Post parsedPost = abstractSoneCommand.getPost(postFieldSet, "Post");
		assertThat(parsedPost, notNullValue());
		assertThat(parsedPost, is(post));
	}

	@Test(expected = FcpException.class)
	public void testThatTryingToParseANonExistingPostCausesAnError() throws FcpException {
		SimpleFieldSet postFieldSet = new SimpleFieldSetBuilder().put("Post", "InvalidPostId").get();
		abstractSoneCommand.getPost(postFieldSet, "Post");
	}

	@Test(expected = FcpException.class)
	public void testThatTryingToParseAPostFromANonExistingFieldCausesAnError() throws FcpException {
		SimpleFieldSet postFieldSet = new SimpleFieldSetBuilder().get();
		abstractSoneCommand.getPost(postFieldSet, "Post");
	}

	@Test
	public void testParsingAReply() throws FcpException {
		Sone sone = mocks.mockSone(randomUUID().toString()).create();
		PostReply reply = mocks.mockPostReply(sone, randomUUID().toString()).create();
		SimpleFieldSet replyFieldSet = new SimpleFieldSetBuilder().put("Reply", reply.getId()).get();
		PostReply parsedReply = abstractSoneCommand.getReply(replyFieldSet, "Reply");
		assertThat(parsedReply, notNullValue());
		assertThat(parsedReply, is(reply));
	}

	@Test(expected = FcpException.class)
	public void testParsingANonExistingReply() throws FcpException {
		SimpleFieldSet replyFieldSet = new SimpleFieldSetBuilder().put("Reply", "InvalidReplyId").get();
		abstractSoneCommand.getReply(replyFieldSet, "Reply");
	}

	@Test(expected = FcpException.class)
	public void testParsingAReplyFromANonExistingField() throws FcpException {
		SimpleFieldSet replyFieldSet = new SimpleFieldSetBuilder().get();
		abstractSoneCommand.getReply(replyFieldSet, "Reply");
	}

	@Test
	public void testEncodingAPostWithoutRecipientAndReplies() throws FSParseException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post = mocks.mockPost(sone, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePost(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		verifyPost(postFieldSet, "Post.", post);
	}

	private void verifyPost(SimpleFieldSet postFieldSet, String prefix, Post post) throws FSParseException {
		assertThat(postFieldSet.get(prefix + "ID"), is(post.getId()));
		assertThat(postFieldSet.get(prefix + "Sone"), is(post.getSone().getId()));
		assertThat(postFieldSet.get(prefix + "Recipient"), is(post.getRecipientId().orNull()));
		assertThat(postFieldSet.getLong(prefix + "Time"), is(post.getTime()));
		assertThat(postFieldSet.get(prefix + "Text"), is(post.getText()));
	}

	@Test
	public void testEncodingAPostWithRecipientWithoutReplies() throws FSParseException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post = mocks.mockPost(sone, randomUUID().toString()).withRecipient("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePost(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		verifyPost(postFieldSet, "Post.", post);
	}

	@Test
	public void testEncodingAPostWithoutRecipientWithReplies() throws FSParseException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post = mocks.mockPost(sone, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		PostReply postReply = mocks.mockPostReply(sone, randomUUID().toString()).withTime(currentTimeMillis()).withText("Reply.").create();
		when(post.getReplies()).thenReturn(asList(postReply));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostWithReplies(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		verifyPostWithReplies(postFieldSet, "Post.", post);
	}

	@Test
	public void testEncodingAPostWithoutRecipientWithFutureReplies() throws FSParseException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post = mocks.mockPost(sone, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		PostReply postReply = mocks.mockPostReply(sone, randomUUID().toString()).withTime(currentTimeMillis() + DAYS.toMillis(1)).withText("Reply.").create();
		when(post.getReplies()).thenReturn(asList(postReply));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostWithReplies(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		verifyPostWithReplies(postFieldSet, "Post.", post);
	}

	@Test
	public void testEncodingAPostWithRecipientAndReplies() throws FSParseException {
		Sone sone = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test").withProfileName("First", "M.", "Last").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post = mocks.mockPost(sone, randomUUID().toString()).withRecipient("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		PostReply postReply = mocks.mockPostReply(sone, randomUUID().toString()).withTime(currentTimeMillis()).withText("Reply.").create();
		when(post.getReplies()).thenReturn(asList(postReply));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostWithReplies(post, "Post.");
		assertThat(postFieldSet, notNullValue());
		verifyPostWithReplies(postFieldSet, "Post.", post);
	}

	@Test
	public void testEncodingPostsWithoutRecipientAndReplies() throws FSParseException {
		Sone sone1 = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test1").withProfileName("Alpha", "A.", "First").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone2 = mocks.mockSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withName("Test2").withProfileName("Beta", "B.", "Second").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post1 = mocks.mockPost(sone1, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		Post post2 = mocks.mockPost(sone2, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some other Text.").create();
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePosts(asList(post1, post2), "Posts.");
		assertThat(postFieldSet, notNullValue());
		verifyPosts(postFieldSet, "Posts.", asList(post1, post2));
	}

	@Test
	public void testEncodingPostsWithRecipientWithoutReplies() throws FSParseException {
		Sone sone1 = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test1").withProfileName("Alpha", "A.", "First").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone2 = mocks.mockSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withName("Test2").withProfileName("Beta", "B.", "Second").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post1 = mocks.mockPost(sone1, randomUUID().toString()).withRecipient("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		Post post2 = mocks.mockPost(sone2, randomUUID().toString()).withRecipient("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some other Text.").create();
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePosts(asList(post1, post2), "Posts.");
		assertThat(postFieldSet, notNullValue());
		verifyPosts(postFieldSet, "Posts.", asList(post1, post2));
	}

	@Test
	public void testEncodingPostsWithoutRecipientWithReplies() throws FSParseException {
		Sone sone1 = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test1").withProfileName("Alpha", "A.", "First").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone2 = mocks.mockSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withName("Test2").withProfileName("Beta", "B.", "Second").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post1 = mocks.mockPost(sone1, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		Post post2 = mocks.mockPost(sone2, randomUUID().toString()).withRecipient(null).withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some other Text.").create();
		PostReply postReply1 = mocks.mockPostReply(sone2, randomUUID().toString()).withTime(currentTimeMillis()).withText("Reply from 2 to 1").create();
		PostReply postReply2 = mocks.mockPostReply(sone1, randomUUID().toString()).withTime(currentTimeMillis()).withText("Reply from 1 to 2").create();
		when(post1.getReplies()).thenReturn(asList(postReply1));
		when(post2.getReplies()).thenReturn(asList(postReply2));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostsWithReplies(asList(post1, post2), "Posts.");
		assertThat(postFieldSet, notNullValue());
		verifyPostsWithReplies(postFieldSet, "Posts.", asList(post1, post2));
	}

	@Test
	public void testEncodingPostsWithRecipientAndReplies() throws FSParseException {
		Sone sone1 = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test1").withProfileName("Alpha", "A.", "First").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone2 = mocks.mockSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withName("Test2").withProfileName("Beta", "B.", "Second").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post1 = mocks.mockPost(sone1, randomUUID().toString()).withRecipient("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		Post post2 = mocks.mockPost(sone2, randomUUID().toString()).withRecipient("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some other Text.").create();
		PostReply postReply1 = mocks.mockPostReply(sone2, randomUUID().toString()).withTime(currentTimeMillis()).withText("Reply from 2 to 1").create();
		PostReply postReply2 = mocks.mockPostReply(sone1, randomUUID().toString()).withTime(currentTimeMillis()).withText("Reply from 1 to 2").create();
		when(post1.getReplies()).thenReturn(asList(postReply1));
		when(post2.getReplies()).thenReturn(asList(postReply2));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostsWithReplies(asList(post1, post2), "Posts.");
		assertThat(postFieldSet, notNullValue());
		verifyPostsWithReplies(postFieldSet, "Posts.", asList(post1, post2));
	}

	@Test
	public void testEncodingPostsWithRecipientAndFutureReplies() throws FSParseException {
		Sone sone1 = mocks.mockSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withName("Test1").withProfileName("Alpha", "A.", "First").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Sone sone2 = mocks.mockSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withName("Test2").withProfileName("Beta", "B.", "Second").addProfileField("Test1", "Value1").withTime((long) (Math.random() * Long.MAX_VALUE)).create();
		Post post1 = mocks.mockPost(sone1, randomUUID().toString()).withRecipient("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some Text.").create();
		Post post2 = mocks.mockPost(sone2, randomUUID().toString()).withRecipient("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E").withTime((long) (Math.random() * Long.MAX_VALUE)).withText("Some other Text.").create();
		PostReply postReply1 = mocks.mockPostReply(sone2, randomUUID().toString()).withTime(currentTimeMillis()).withText("Reply from 2 to 1").create();
		PostReply postReply2 = mocks.mockPostReply(sone1, randomUUID().toString()).withTime(currentTimeMillis() + DAYS.toMillis(1)).withText("Reply from 1 to 2").create();
		when(post1.getReplies()).thenReturn(asList(postReply1));
		when(post2.getReplies()).thenReturn(asList(postReply2));
		SimpleFieldSet postFieldSet = abstractSoneCommand.encodePostsWithReplies(asList(post1, post2), "Posts.");
		assertThat(postFieldSet, notNullValue());
		verifyPostsWithReplies(postFieldSet, "Posts.", asList(post1, post2));
	}

}
