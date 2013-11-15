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

import static com.google.common.base.Objects.equal;
import static com.google.common.collect.Iterators.size;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import freenet.support.SimpleFieldSet;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
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

	public static <T> Matcher<Iterator<T>> contains(T... items) {
		return contains(asList(items));
	}

	public static <T> Matcher<Iterator<T>> contains(final Collection<T> items) {
		return new TypeSafeMatcher<Iterator<T>>() {
			@Override
			protected boolean matchesSafely(Iterator<T> iterator) {
				for (T item : items) {
					if (!iterator.hasNext()) {
						return false;
					}
					T nextItem = iterator.next();
					if (!Objects.equal(item, nextItem)) {
						return false;
					}
				}
				if (iterator.hasNext()) {
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("contains ").appendValue(items);
			}
		};
	}

	public static <T> Matcher<Iterator<T>> containsInAnyOrder(T... items) {
		return containsInAnyOrder(asList(items));
	}

	public static <T> Matcher<Iterator<T>> containsInAnyOrder(final Collection<T> items) {
		return new TypeSafeMatcher<Iterator<T>>() {
			private final List<T> remainingItems = Lists.newArrayList(items);
			@Override
			protected boolean matchesSafely(Iterator<T> iterator) {
				while (iterator.hasNext()) {
					T item = iterator.next();
					if (remainingItems.isEmpty()) {
						return false;
					}
					if (!remainingItems.remove(item)) {
						return false;
					}
				}
				if (!remainingItems.isEmpty()) {
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("contains ").appendValue(items);
			}
		};
	}

	public static Matcher<InputStream> delivers(final byte[] data) {
		return new TypeSafeMatcher<InputStream>() {
			byte[] readData = new byte[data.length];

			@Override
			protected boolean matchesSafely(InputStream inputStream) {
				int offset = 0;
				try {
					while (true) {
						int r = inputStream.read();
						if (r == -1) {
							return offset == data.length;
						}
						if (offset == data.length) {
							return false;
						}
						if (data[offset] != (readData[offset] = (byte) r)) {
							return false;
						}
						offset++;
					}
				} catch (IOException ioe1) {
					return false;
				}
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(data);
			}

			@Override
			protected void describeMismatchSafely(InputStream item, Description mismatchDescription) {
				mismatchDescription.appendValue(readData);
			}
		};
	}

	public static Matcher<SimpleFieldSet> matches(final SimpleFieldSet fieldSetToMatch) {
		return new TypeSafeMatcher<SimpleFieldSet>() {
			@Override
			protected boolean matchesSafely(SimpleFieldSet fieldSet) {
				if (size(fieldSet.keyIterator()) != size(fieldSetToMatch.keyIterator())) {
					return false;
				}
				for (Iterator<String> keys = fieldSetToMatch.keyIterator(); keys.hasNext(); ) {
					String key = keys.next();
					if (!equal(fieldSet.get(key), fieldSetToMatch.get(key))) {
						return false;
					}
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("is ").appendValue(fieldSetToMatch);
			}
		};
	}

}
