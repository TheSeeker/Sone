package net.pterodactylus.sone.core;

import static com.google.common.base.Objects.equal;
import static java.lang.String.format;
import static java.util.logging.Level.OFF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.logging.Logger;

import net.pterodactylus.sone.core.SoneParser.DuplicateField;
import net.pterodactylus.sone.core.SoneParser.InvalidProtocolVersion;
import net.pterodactylus.sone.core.SoneParser.InvalidXml;
import net.pterodactylus.sone.core.SoneParser.MalformedXml;
import net.pterodactylus.sone.data.Client;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Profile.Field;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.SoneBuilder.SoneCreated;
import net.pterodactylus.sone.database.memory.MemoryDatabase;

import com.google.common.base.Optional;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

/**
 * Unit test for {@link SoneParser}.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class SoneParserTest {

	static {
		Logger.getLogger("").setLevel(OFF);
	}

	private final Core core = mock(Core.class);
	private final Database database = new MemoryDatabase(null);
	private final Sone originalSone = database.newSoneBuilder().by("test").using(new Client("TestClient", "1.0")).build(Optional.<SoneCreated>absent());
	private final SoneParser soneParser = new SoneParser();

	public SoneParserTest() {
		Optional<Image> image = mock(Optional.class);
		when(core.getImage(anyString())).thenReturn(image);
	}

	@Test(expected = InvalidXml.class)
	public void verifyThatAnInvalidXmlDocumentIsNotParsed() {
		soneParser.parseSone(database, originalSone, getXml("invalid-xml"));
	}

	@Test(expected = InvalidProtocolVersion.class)
	public void verifyThatANegativeProtocolVersionCausesAnError() {
		soneParser.parseSone(database, originalSone, getXml("negative-protocol-version"));
	}

	@Test(expected = InvalidProtocolVersion.class)
	public void verifyThatATooLargeProtocolVersionCausesAnError() {
		soneParser.parseSone(database, originalSone, getXml("too-large-protocol-version"));
	}

	@Test(expected = MalformedXml.class)
	public void verifyThatAMissingTimeCausesAnError() {
		soneParser.parseSone(database, originalSone, getXml("missing-time"));
	}

	@Test
	public void verifyThatAMissingClientCausesTheOriginalClientToBeUsed() {
		Sone sone = soneParser.parseSone(database, originalSone, getXml("missing-client"));
		assertThat(sone, notNullValue());
		assertThat(sone.getClient(), notNullValue());
		assertThat(sone.getClient(), is(originalSone.getClient()));
	}

	@Test
	public void verifyThatAnInvalidClientCausesTheOriginalClientToBeUsed() {
		Sone sone = soneParser.parseSone(database, originalSone, getXml("invalid-client"));
		assertThat(sone, notNullValue());
		assertThat(sone.getClient(), notNullValue());
		assertThat(sone.getClient(), is(originalSone.getClient()));
	}

	@Test(expected = MalformedXml.class)
	public void verifyThatAMissingProfileCausesAnError() {
		soneParser.parseSone(database, originalSone, getXml("missing-profile"));
	}

	@Test(expected = MalformedXml.class)
	public void verifyThatInvalidFieldsCauseAnError() {
		soneParser.parseSone(database, originalSone, getXml("invalid-field"));
	}

	@Test(expected = DuplicateField.class)
	public void verifyThatDuplicateFieldsCauseAnError() {
		soneParser.parseSone(database, originalSone, getXml("duplicate-field"));
	}

	@Test
	public void verifyThatMissingPostsDoNotCauseAnError() {
		soneParser.parseSone(database, originalSone, getXml("missing-posts"));
	}

	@Test(expected = MalformedXml.class)
	public void verifyThatInvalidPostsCauseAnError() {
		soneParser.parseSone(database, originalSone, getXml("invalid-posts"));
	}

	@Test
	public void verifyThatAnEmptyProfileIsParsedWithoutError() {
		Sone sone = soneParser.parseSone(database, originalSone, getXml("empty-profile"));
		assertThat(sone.getProfile().getFirstName(), nullValue());
		assertThat(sone.getProfile().getMiddleName(), nullValue());
		assertThat(sone.getProfile().getLastName(), nullValue());
		assertThat(sone.getProfile().getBirthYear(), nullValue());
		assertThat(sone.getProfile().getBirthMonth(), nullValue());
		assertThat(sone.getProfile().getBirthDay(), nullValue());
		assertThat(sone.getProfile().getAvatar(), nullValue());
		assertThat(sone.getProfile().getFields(), empty());
	}

	@Test
	public void verifyThatTheCreatedSoneMeetsAllExpectations() {
		Sone sone = soneParser.parseSone(database, originalSone, getXml("complete"));
		assertThat(sone, notNullValue());
		assertThat(sone.getTime(), is(1382419919000L));
		assertThat(sone.getClient(), notNullValue());
		assertThat(sone.getClient().getName(), is("Sone"));
		assertThat(sone.getClient().getVersion(), is("0.8.7"));
		assertThat(sone.getProfile(), notNullValue());
		assertThat(sone.getProfile().getFirstName(), is("First"));
		assertThat(sone.getProfile().getMiddleName(), is("M."));
		assertThat(sone.getProfile().getLastName(), is("Last"));
		assertThat(sone.getProfile().getBirthYear(), is(2013));
		assertThat(sone.getProfile().getBirthMonth(), is(10));
		assertThat(sone.getProfile().getBirthDay(), is(22));
		assertThat(sone.getProfile().getAvatar(), is("96431abe-3add-11e3-8a46-67047503bf6d"));
		assertThat(sone.getProfile().getFields(), contains(
				fieldMatcher("Field1", "Value1"),
				fieldMatcher("Field2", "Value2")
		));
		assertThat(sone.getPosts(), contains(
				postMatcher("d8c9586e-3adb-11e3-bb31-171fc040e645", "0rpD4gL8mszav2trndhIdKIxvKUCNAe2kjA3dLV8CVU", 1382420181000L, "Hello, User!"),
				postMatcher("bbb7ebf0-3adb-11e3-8a0b-630cd8f21cf3", null, 1382420140000L, "Hello, World!")
		));
		assertThat(sone.getReplies(), containsInAnyOrder(
				postReplyMatcher("f09fa448-3adb-11e3-a783-ab54a11aacc4", "bbb7ebf0-3adb-11e3-8a0b-630cd8f21cf3", 1382420224000L, "Talking to myself."),
				postReplyMatcher("0a376440-3adc-11e3-8f45-c7cc157436a5", "11ebe86e-3adc-11e3-b7b9-7f2c88018a33", 1382420271000L, "Talking to somebody I can't see.")
		));
		assertThat(sone.getLikedPostIds(), containsInAnyOrder(
				"bbb7ebf0-3adb-11e3-8a0b-630cd8f21cf3",
				"305d85e6-3adc-11e3-be45-8b53dd91f0af"
		));
		assertThat(sone.getLikedReplyIds(), containsInAnyOrder(
				"f09fa448-3adb-11e3-a783-ab54a11aacc4",
				"3ba28960-3adc-11e3-93c7-6713d170f44c"
		));
	}

	private Matcher<PostReply> postReplyMatcher(final String id, final String postId, final long time, final String text) {
		return new TypeSafeMatcher<PostReply>() {
			@Override
			protected boolean matchesSafely(PostReply postReply) {
				return postReply.getId().equals(id) && postReply.getPostId().equals(postId) && (postReply.getTime() == time) && postReply.getText().equals(text);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("PostReply(")
						.appendValue(id).appendText(", ")
						.appendValue(postId).appendText(", ")
						.appendValue(time).appendText(", ")
						.appendValue(text).appendText(")");
			}

			@Override
			protected void describeMismatchSafely(PostReply postReply, Description mismatchDescription) {
				mismatchDescription.appendText("PostReply(")
						.appendValue(postReply.getId()).appendText(", ")
						.appendValue(postReply.getPostId()).appendText(", ")
						.appendValue(postReply.getTime()).appendText(", ")
						.appendValue(postReply.getText()).appendText(")");
			}
		};
	}

	private Matcher<Post> postMatcher(final String id, final String recipient, final long time, final String text) {
		return new TypeSafeMatcher<Post>() {
			@Override
			protected boolean matchesSafely(Post post) {
				return post.getId().equals(id) && equal(post.getRecipientId().orNull(), recipient) && (post.getTime() == time) && post.getText().equals(text);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Post(")
						.appendValue(id).appendText(", ")
						.appendValue(recipient).appendText(", ")
						.appendValue(time).appendText(", ")
						.appendValue(text).appendText(")");
			}

			@Override
			protected void describeMismatchSafely(Post post, Description mismatchDescription) {
				mismatchDescription.appendText("Post(")
						.appendValue(post.getId()).appendText(", ")
						.appendValue(post.getRecipientId().orNull()).appendText(", ")
						.appendValue(post.getTime()).appendText(", ")
						.appendValue(post.getText()).appendText(")");
			}
		};
	}

	private Matcher<Field> fieldMatcher(final String name, final String value) {
		return new TypeSafeMatcher<Field>() {
			@Override
			protected boolean matchesSafely(Field field) {
				return field.getName().equals(name) && field.getValue().equals(value);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("field named ").appendValue(name).appendText(" with value ").appendValue(value);
			}

			@Override
			protected void describeMismatchSafely(Field field, Description mismatchDescription) {
				mismatchDescription.appendText("field named ").appendValue(field.getName()).appendText(" with value ").appendValue(field.getValue());
			}
		};
	}

	private InputStream getXml(String name) {
		return getClass().getResourceAsStream(format("/sone-parser/%s.xml", name));
	}

}
