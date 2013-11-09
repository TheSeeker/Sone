/*
 * Sone - CssClassNameFilterTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.template;

import static org.hamcrest.Matchers.is;

import java.util.Collections;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

/**
 * Unit test for {@link CssClassNameFilter}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class CssClassNameFilterTest {

	private final CssClassNameFilter cssClassNameFilter = new CssClassNameFilter();

	@Test
	public void stringIsSanitized() {
		String filtered = (String) cssClassNameFilter.format(null, createString(), Collections.<String, Object>emptyMap());
		MatcherAssert.assertThat(filtered, is(filterString(createString())));
	}

	private static String filterString(String string) {
		StringBuffer filteredString = new StringBuffer(string.length());
		for (char c : string.toCharArray()) {
			if (isADigit(c) || isACapitalLetter(c) || isASmallLetter(c) || (c == '-') || (c == '_')) {
				filteredString.append(c);
			} else {
				filteredString.append('_');
			}
		}
		return filteredString.toString();
	}

	private static boolean isASmallLetter(char c) {
		return ((c >= 'a') && (c <= 'z'));
	}

	private static boolean isACapitalLetter(char c) {
		return ((c >= 'A') && (c <= 'Z'));
	}

	private static boolean isADigit(char c) {
		return ((c >= '0') && (c <= '9'));
	}

	private static String createString() {
		StringBuilder string = new StringBuilder();
		for (int i = 0; i < 256; i++) {
			string.append((char) i);
		}
		return string.toString();
	}

}
