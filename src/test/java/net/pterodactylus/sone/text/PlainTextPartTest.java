/*
 * Sone - PlainTextPartTest.java - Copyright © 2013 David Roden
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
 * Unit test for {@link PlainTextPart}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class PlainTextPartTest {

	private final PlainTextPart plainTextPart = new PlainTextPart("<plain-text>");

	@Test
	public void plainTextPartIsAPlainTextPart() {
		assertThat(plainTextPart.isPlainText(), is(true));
	}

	@Test
	public void plainTextPartIsNotAFreenetLink() {
		assertThat(plainTextPart.isFreenetLink(), is(false));
	}

	@Test
	public void plainTextPartCanStoreAndReturnText() {
		assertThat(plainTextPart.getText(), is("<plain-text>"));
	}

	@Test
	public void equalPartsHaveTheSameHashCode() {
		PlainTextPart secondPlainTextPart = new PlainTextPart(plainTextPart.getText());
		assertThat(secondPlainTextPart, is(plainTextPart));
		assertThat(secondPlainTextPart.hashCode(), is(plainTextPart.hashCode()));
	}

	@Test
	public void nullDoesNotMatchThePlainTextPart() {
		assertThat(plainTextPart, not(is((Object) null)));
	}

	@Test
	public void toStringContainsTheText() {
		assertThat(plainTextPart.toString(), containsString("<plain-text>"));
	}

}
