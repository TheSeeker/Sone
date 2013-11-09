/*
 * Sone - FreenetLinkPartTest.java - Copyright © 2013 David Roden
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

/**
 * Unit test for {@link FreenetLinkPart}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class FreenetLinkPartTest {

	@Test
	public void freenetLinkPartIsNotAPlainTextPart() {
		FreenetLinkPart freenetLinkPart = new FreenetLinkPart("link", "text", true);
		assertThat(freenetLinkPart.isPlainText(), is(false));
	}

	@Test
	public void freenetLinkPartIsAFreenetLink() {
		FreenetLinkPart freenetLinkPart = new FreenetLinkPart("link", "text", true);
		assertThat(freenetLinkPart.isFreenetLink(), is(true));
	}

	@Test
	public void trustedAttributeIsStoredAndReturnedWhenSet() {
		FreenetLinkPart freenetLinkPart = new FreenetLinkPart("link", "text", true);
		assertThat(freenetLinkPart.isTrusted(), is(true));
	}

	@Test
	public void trustedAttributeIsStoredAndReturnedWhenNotSet() {
		FreenetLinkPart freenetLinkPart = new FreenetLinkPart("link", "text", "title", false);
		assertThat(freenetLinkPart.isTrusted(), is(false));
	}

	@Test
	public void hashCodeMatchesForEqualParts() {
		FreenetLinkPart freenetLinkPart1 = new FreenetLinkPart("link", "text", "title", false);
		FreenetLinkPart freenetLinkPart2 = new FreenetLinkPart("link", "text", "title", false);
		assertThat(freenetLinkPart1, is(freenetLinkPart2));
		assertThat(freenetLinkPart1.hashCode(), is(freenetLinkPart2.hashCode()));
	}

	@Test
	public void nullDoesNotMatchAFreenetLinkPart() {
		FreenetLinkPart freenetLinkPart = new FreenetLinkPart("link", "text", true);
		assertThat(freenetLinkPart, not(is((Object) null)));
	}

	@Test
	public void toStringContainsLinkTextAndTitle() {
		FreenetLinkPart freenetLinkPart = new FreenetLinkPart("<some-link>", "<some-text>", "<some-title>", true);
		assertThat(freenetLinkPart.toString(), containsString("<some-link>"));
		assertThat(freenetLinkPart.toString(), containsString("<some-text>"));
		assertThat(freenetLinkPart.toString(), containsString("<some-title>"));
	}

}
