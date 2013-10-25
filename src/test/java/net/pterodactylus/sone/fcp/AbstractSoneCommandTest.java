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

import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

/**
 * Unit test for {@link AbstractSoneCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractSoneCommandTest {

	@Test
	public void testStringEncoding() {
		StringBuilder testString = new StringBuilder();
		for (int i = 0; i < 4000; ++i) {
			testString.append((char) i);
		}
		String encodedString = encodeString(testString.toString());
		assertThat(encodedString, notNullValue());
		assertThat(encodedString.length(), is(testString.length() + 3));
	}

}
