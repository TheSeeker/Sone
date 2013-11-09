/*
 * Sone - LinkPartTest.java - Copyright © 2013 David Roden
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link LinkPart}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class LinkPartTest {

	@Test
	public void linkPartIsNotAPlainTextPart() {
		LinkPart linkPart = new LinkPart("http://li.nk/link.html", "link.html");
		assertThat(linkPart.isPlainText(), is(false));
	}

	@Test
	public void linkPartIsNotAFreenetLink() {
		LinkPart linkPart = new LinkPart("http://li.nk/link.html", "link.html");
		assertThat(linkPart.isFreenetLink(), is(false));
	}

	@Test
	public void linkPartWithoutTitleGetsTextAsTitle() {
		LinkPart linkPart = new LinkPart("http://li.nk/link.html", "link.html");
		assertThat(linkPart.getLink(), is("http://li.nk/link.html"));
		assertThat(linkPart.getText(), is("link.html"));
		assertThat(linkPart.getTitle(), is("link.html"));
	}

	@Test
	public void linkPartWithDifferentTitleAndTextIsCreatedCorrectly() {
		LinkPart linkPart = new LinkPart("http://li.nk/link.html", "link.html", "the link");
		assertThat(linkPart.getLink(), is("http://li.nk/link.html"));
		assertThat(linkPart.getText(), is("link.html"));
		assertThat(linkPart.getTitle(), is("the link"));
	}

	@Test
	public void twoLinkPartsWithSameParametersAreEqual() {
		LinkPart linkPart1 = new LinkPart("http://li.nk/link.html", "link.html", "the link");
		LinkPart linkPart2 = new LinkPart("http://li.nk/link.html", "link.html", "the link");
		assertThat(linkPart1, is(linkPart2));
		assertThat(linkPart2, is(linkPart1));
		assertThat(linkPart1, hasHashCode(linkPart2.hashCode()));
	}

	@Test
	public void twoLinkPartsWithDifferentLinksAreNotEqual() {
		LinkPart linkPart1 = new LinkPart("http://li.nk/link.html", "link.html", "the link");
		LinkPart linkPart2 = new LinkPart("http://li.nk/link2.html", "link.html", "the link");
		assertThat(linkPart1, not(is(linkPart2)));
		assertThat(linkPart2, not(is(linkPart1)));
	}

	@Test
	public void twoLinkPartsWithDifferentTextsAreNotEqual() {
		LinkPart linkPart1 = new LinkPart("http://li.nk/link.html", "link.html", "the link");
		LinkPart linkPart2 = new LinkPart("http://li.nk/link.html", "link2.html", "the link");
		assertThat(linkPart1, not(is(linkPart2)));
		assertThat(linkPart2, not(is(linkPart1)));
	}

	@Test
	public void twoLinkPartsWithDifferentTitlesAreNotEqual() {
		LinkPart linkPart1 = new LinkPart("http://li.nk/link.html", "link.html", "the link");
		LinkPart linkPart2 = new LinkPart("http://li.nk/link.html", "link.html", "the link2");
		assertThat(linkPart1, not(is(linkPart2)));
		assertThat(linkPart2, not(is(linkPart1)));
	}

	@Test
	public void linkPartDoesNotEqualAnObject() {
		LinkPart linkPart1 = new LinkPart("http://li.nk/link.html", "link.html", "the link");
		assertThat(linkPart1, not(is(new Object())));
		assertThat(new Object(), not(is((Object) linkPart1)));
	}

	public class HasHashCode<T> extends BaseMatcher<T> {

		private final int hashCode;

		public HasHashCode(int hashCode) {
			this.hashCode = hashCode;
		}

		@Override
		public boolean matches(Object item) {
			return (item != null) && (item.hashCode() == hashCode);
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("has hash ").appendValue(hashCode);
		}

	}

	public <T> Matcher<T> hasHashCode(int hashCode) {
		return new HasHashCode<T>(hashCode);
	}

}
