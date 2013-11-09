/*
 * Sone - PartContainerTest.java - Copyright © 2013 David Roden
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link PartContainer}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class PartContainerTest {

	private final PartContainer partContainer = new PartContainer();

	@Before
	public void setup() {
		partContainer.add(createPart("Test!"));
		partContainer.add(createPart(" More Test!"));
	}

	@Test
	public void partContainerCanReturnContainedParts() {
		assertThat(partContainer, contains(matchesPart("Test!"), matchesPart(" More Test!")));
	}

	@Test
	public void partContainerCanReturnSpecificParts() {
		assertThat(partContainer.getPart(0), matchesPart("Test!"));
		assertThat(partContainer.getPart(1), matchesPart(" More Test!"));
	}

	@Test
	public void partContainerKnowsTheNumberOfContainedParts() {
		assertThat(partContainer.size(), is(2));
	}

	@Test
	public void partContainerReturnsNestedPartsWhenIteratingOverIt() {
		addNestedPartContainer();
		assertThat(partContainer, contains(matchesPart("Test!"), matchesPart(" More Test!"), matchesPart(" (nested)")));
	}

	@Test(expected = NoSuchElementException.class)
	public void throwsExceptionWhenIteratingOverTheEndOfTheIterator() {
		Iterator<Part> parts = partContainer.iterator();
		parts.next();
		parts.next();
		parts.next();
	}

	@Test
	public void removingElementsFromTheIteratorDoesNotRemoveThemFromTheContainer() {
		Iterator<Part> parts = partContainer.iterator();
		parts.remove();
		assertThat(partContainer.size(), is(2));
	}

	@Test
	public void textOfContainedPartsIsConcatenated() {
		addNestedPartContainer();
		assertThat(partContainer.getText(), is("Test! More Test! (nested)"));
	}

	@Test
	public void partsCanBeRemoved() {
		partContainer.removePart(1);
		assertThat(partContainer, contains(matchesPart("Test!")));
	}

	private void addNestedPartContainer() {
		Part nestedPart = createPart(" (nested)");
		PartContainer nestedContainer = new PartContainer();
		nestedContainer.add(nestedPart);
		partContainer.add(nestedContainer);
	}

	private static Matcher<Part> matchesPart(final String text) {
		return new TypeSafeMatcher<Part>() {
			@Override
			protected boolean matchesSafely(Part part) {
				return part.getText().equals(text);
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(text);
			}
		};
	}

	private static Part createPart(final String text) {
		return new Part() {
			@Override
			public String getText() {
				return text;
			}

			@Override
			public boolean isPlainText() {
				return true;
			}

			@Override
			public boolean isFreenetLink() {
				return false;
			}
		};
	}

}
