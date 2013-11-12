/*
 * Sone - TrustTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet.wot;

import static java.util.regex.Pattern.compile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

/**
 * Unit test for {@link Trust}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class TrustTest {

	@Test
	public void trustCanBeCreated() {
		Trust trust = new Trust(5, 17, 2);
		assertThat(trust.getExplicit(), is(5));
		assertThat(trust.getImplicit(), is(17));
		assertThat(trust.getDistance(), is(2));
	}

	@Test
	public void nullTrustCanBeCreated() {
		Trust trust = new Trust(null, null, null);
		assertThat(trust.getExplicit(), nullValue());
		assertThat(trust.getImplicit(), nullValue());
		assertThat(trust.getDistance(), nullValue());
	}

	@Test
	public void equalTrustsHaveTheSameHashCode() {
		Trust trust1 = new Trust(5, 17, 2);
		Trust trust2 = new Trust(5, 17, 2);
		assertThat(trust1, is(trust2));
		assertThat(trust1.hashCode(), is(trust2.hashCode()));
	}

	@Test
	public void nullDoesNotMatchTrust() {
		Trust trust = new Trust(5, 17, 2);
		assertThat(trust, not(is((Object) null)));
	}

	@Test
	public void toStringContainsTheThreeValues() {
		String trustString = new Trust(5, 17, 2).toString();
		assertThat(trustString, matches("\\b5\\b"));
		assertThat(trustString, matches("\\b17\\b"));
		assertThat(trustString, matches("\\b2\\b"));
	}

	private static Matcher<String> matches(final String regex) {
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
