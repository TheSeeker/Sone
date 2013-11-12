/*
 * Sone - Matchers.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone;

import static java.util.regex.Pattern.compile;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matchers used throughout the tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Matchers {

	public static Matcher<String> matches(final String regex) {
		return new TypeSafeMatcher<String>() {
			@Override
			protected boolean matchesSafely(String item) {
				return compile(regex).matcher(item).find();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("matches: ").appendValue(regex);
			}
		};
	}

}
