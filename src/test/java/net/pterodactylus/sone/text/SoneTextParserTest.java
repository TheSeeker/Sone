/*
 * Sone - SoneTextParserTest.java - Copyright © 2011–2013 David Roden
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

package net.pterodactylus.sone.text;

import static com.google.common.collect.ImmutableList.builder;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.impl.DefaultSone;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Test;

/**
 * JUnit test case for {@link SoneTextParser}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneTextParserTest {

	private final Mocks mocks = new Mocks();
	private final SoneTextParser soneTextParser = new SoneTextParser(mocks.database);

	private Matcher<Iterable<Part>> matches(final Matcher<? extends Part>... partsToMatch) {
		return new TypeSafeMatcher<Iterable<Part>>() {

			private Matcher<Iterable<? extends Part>> iterableMatcher;

			@Override
			protected boolean matchesSafely(Iterable<Part> parts) {
				iterableMatcher = new IsIterableContainingInOrder(asList(partsToMatch));
				return iterableMatcher.matches(collapseParts(expandParts(parts)));
			}

			private Iterable<Part> expandParts(Iterable<? extends Part> parts) {
				PartContainer partContainer = new PartContainer();
				for (Part part : parts) {
					partContainer.add(part);
				}
				return partContainer;
			}

			private Collection<Part> collapseParts(Iterable<? extends Part> parts) {
				ImmutableList.Builder<Part> collapsedPartsBuilder = builder();
				PlainTextPart lastPlainTextPart = null;
				for (Part part : parts) {
					if (part instanceof PlainTextPart) {
						if (lastPlainTextPart != null) {
							lastPlainTextPart = new PlainTextPart(lastPlainTextPart.getText() + ((PlainTextPart) part).getText());
						} else {
							lastPlainTextPart = (PlainTextPart) part;
						}
					} else {
						if (lastPlainTextPart != null) {
							collapsedPartsBuilder.add(lastPlainTextPart);
							lastPlainTextPart = null;
						}
						collapsedPartsBuilder.add(part);
					}
				}
				if (lastPlainTextPart != null) {
					collapsedPartsBuilder.add(lastPlainTextPart);
				}
				return collapsedPartsBuilder.build();
			}

			@Override
			protected void describeMismatchSafely(Iterable<Part> parts, Description mismatchDescription) {
				iterableMatcher.describeMismatch(collapseParts(parts), mismatchDescription);
			}

			@Override
			public void describeTo(Description description) {
				iterableMatcher.describeTo(description);
			}
		};
	}

	private Iterable<Part> parse(String text) throws IOException {
		return soneTextParser.parse(null, new StringReader(text));
	}

	private Iterable<Part> parse(SoneTextParserContext context, String text) throws IOException {
		return soneTextParser.parse(context, new StringReader(text));
	}

	@Test
	public void parsePlainText() throws IOException {
		assertThat(parse("Test."), matches(is(new PlainTextPart("Test."))));
	}

	@Test
	public void parsePlainTextWithEmptyLinesAtTheBeginningAndEnd() throws IOException {
		assertThat(parse("\nTest.\n\n"), matches(is(new PlainTextPart("Test."))));
	}

	@Test
	public void parsePlainTextAndCollapseMultipleEmptyLines() throws IOException {
		assertThat(parse("\nTest.\n\n\nTest."), matches(is(new PlainTextPart("Test.\n\nTest."))));
	}

	@Test
	public void parseSimpleKskLinks() throws IOException {
		assertThat(parse("KSK@gpl.txt"), matches(is(new FreenetLinkPart("KSK@gpl.txt", "gpl.txt", false))));
	}

	@Test
	public void parseEmbeddedLinks() throws IOException {
		assertThat(parse("Link is KSK@gpl.txt\u200b."), matches(
				is(new PlainTextPart("Link is ")),
				is(new FreenetLinkPart("KSK@gpl.txt", "gpl.txt", false)),
				is(new PlainTextPart("\u200b."))
		));
	}

	@Test
	public void parseEmbeddedLinksAndLineBreaks() throws IOException {
		assertThat(parse("Link is KSK@gpl.txt\nKSK@test.dat\n"), matches(
				is(new PlainTextPart("Link is ")),
				is(new FreenetLinkPart("KSK@gpl.txt", "gpl.txt", false)),
				is(new PlainTextPart("\n")),
				is(new FreenetLinkPart("KSK@test.dat", "test.dat", false))
		));
	}

	@Test
	public void parseEmptyLinesAndSoneLinks() throws IOException {
		Sone sone = mocks.mockSone("DAxKQzS48mtaQc7sUVHIgx3fnWZPQBz0EueBreUVWrU").create();
		assertThat(parse("Some text.\n\nLink to sone://DAxKQzS48mtaQc7sUVHIgx3fnWZPQBz0EueBreUVWrU and stuff."), matches(
				is(new PlainTextPart("Some text.\n\nLink to ")),
				is(new SonePart(sone)),
				is(new PlainTextPart(" and stuff."))
		));
	}

	@Test
	public void parseEmptyHttpLinks() throws IOException {
		assertThat(parse("Some text. Empty link: http:// – nice!"), matches(
				is(new PlainTextPart("Some text. Empty link: http:// – nice!"))
		));
	}

	@Test
	public void parseTrustedSoneLinks() throws IOException {
		Sone trustedSone = mocks.mockSone("DAxKQzS48mtaQc7sUVHIgx3fnWZPQBz0EueBreUVWrU").create();
		assertThat(parse(new SoneTextParserContext(trustedSone), "Get SSK@DAxKQzS48mtaQc7sUVHIgx3fnWZPQBz0EueBreUVWrU/file.txt\u200b!"), matches(
				is(new PlainTextPart("Get ")),
				is(new FreenetLinkPart("SSK@DAxKQzS48mtaQc7sUVHIgx3fnWZPQBz0EueBreUVWrU/file.txt", "file.txt", true)),
				is(new PlainTextPart("\u200b!"))
		));
	}

	@Test
	public void parseHttpLink() throws IOException {
		assertThat(parse("http://w3.org/foo.html"), matches(
				is(new LinkPart("http://w3.org/foo.html", "w3.org/foo.html", "w3.org/foo.html"))
		));
	}

	@Test
	public void twoNonEmptyLinesAreParsedCorrectly() throws IOException {
		assertThat(parse("First line.\nSecond line."), matches(
				is(new PlainTextPart("First line.\nSecond line."))
		));
	}

	@Test
	public void malformedChkLinkIsParsedAsText() throws IOException {
		assertThat(parse("CHK@key/gpl.txt"), matches(
				is(new PlainTextPart("CHK@key/gpl.txt"))
		));
	}

	@Test
	public void malformedUskLinkIsParsedAsText() throws IOException {
		assertThat(parse("USK@key/site/"), matches(
				is(new PlainTextPart("USK@key/site/"))
		));
	}

	@Test
	public void httpsLinksAreParsedCorrectly() throws IOException {
		assertThat(parse("https://site/file.txt"), matches(
				is(new LinkPart("https://site/file.txt", "site/file.txt"))
		));
	}

	@Test
	public void postLinksAreParsedCorrectly() throws IOException {
		Sone sone = mocks.mockSone("Sone").create();
		Post post = mocks.mockPost(sone, randomUUID().toString()).create();
		assertThat(parse("post://" + post.getId()), matches(
				is(new PostPart(post))
		));
	}

	@Test
	public void linkToNonExistingPostIsParsedAsPlainText() throws IOException {
		String postId = randomUUID().toString();
		assertThat(parse("post://" + postId), matches(
				is(new PlainTextPart("post://" + postId))
		));
	}

	@Test
	public void tooShortPostLinkIsParsedAsPlainText() throws IOException {
		assertThat(parse("post://post"), matches(
				is(new PlainTextPart("post://post"))
		));
	}

	@Test
	public void freenetPrefixBeforeKeysIsCutOff() throws IOException {
		assertThat(parse("freenet:KSK@gpl.txt"), matches(
				is(new FreenetLinkPart("KSK@gpl.txt", "gpl.txt", false))
		));
	}

	@Test
	public void linkToNonExistingSoneCreatesLinkToEmptyShell() throws IOException {
		assertThat(parse("sone://1234567890123456789012345678901234567890123"), matches(
				is(new SonePart(new DefaultSone(mocks.database, "1234567890123456789012345678901234567890123", false, null)))
		));
	}

	@Test
	public void linkToTooShortSoneIdIsParsedAsPlainText() throws IOException {
		assertThat(parse("sone://Sone"), matches(
				is(new PlainTextPart("sone://Sone"))
		));
	}

	@Test
	public void cutOffQueryFromTextOfFreenetLink() throws IOException {
		assertThat(parse("KSK@gpl.txt?max-size=17"), matches(
				is(new FreenetLinkPart("KSK@gpl.txt?max-size=17", "gpl.txt", false))
		));
	}

	@Test
	public void linkWithoutMetaInformationShowsShortenedRoutingKey() throws IOException {
		assertThat(parse("CHK@DAxKQzS48mtaQc7sUVHIgx3fnWZPQBz0EueBreUVWrU"), matches(
				is(new FreenetLinkPart("CHK@DAxKQzS48mtaQc7sUVHIgx3fnWZPQBz0EueBreUVWrU", "CHK@DAxKQ", false))
		));
	}

	@Test
	public void httpLinkGetsPartOfPathRemoved() throws IOException {
		assertThat(parse("http://server.com/path/foo/test.html"), matches(
				is(new LinkPart("http://server.com/path/foo/test.html", "server.com/…/test.html"))
		));
	}

	@Test
	public void httpLinkThatEndsInASlashGetsSlashRemoved() throws IOException {
		assertThat(parse("http://server.com/path/foo/"), matches(
				is(new LinkPart("http://server.com/path/foo/", "server.com/…"))
		));
	}

	@Test
	public void httpLinkGetsWwwRemoved() throws IOException {
		assertThat(parse("http://www.server.com/foo.html"), matches(
				is(new LinkPart("http://www.server.com/foo.html", "server.com/foo.html"))
		));
	}

	@Test
	public void httpLinkGetsQueryRemoved() throws IOException {
		assertThat(parse("http://server.com/foo.html?id=4"), matches(
				is(new LinkPart("http://server.com/foo.html?id=4", "server.com/foo.html"))
		));
	}

	@Test
	public void multipleLinksInOneLine() throws IOException {
		assertThat(parse("KSK@gpl.txt and http://server.com/"), matches(
				is(new FreenetLinkPart("KSK@gpl.txt", "gpl.txt", false)),
				is(new PlainTextPart(" and ")),
				is(new FreenetLinkPart("http://server.com/", "server.com", false))
		));
	}

}
