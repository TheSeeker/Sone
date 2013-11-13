/*
 * Sone - SimpleFieldSetBuilderTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet;

import static net.pterodactylus.sone.Matchers.contains;
import static net.pterodactylus.sone.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import org.junit.Test;

/**
 * Unit test for {@link SimpleFieldSetBuilder}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SimpleFieldSetBuilderTest {

	private final SimpleFieldSetBuilder simpleFieldSetBuilder = new SimpleFieldSetBuilder();

	@Test
	public void creatingASimpleFieldSetWithAStringValue() {
		SimpleFieldSet fieldSet = simpleFieldSetBuilder.put("Key", "Value").get();
		assertThat(fieldSet.keyIterator(), contains("Key"));
		assertThat(fieldSet.get("Key"), is("Value"));
	}

	@Test
	public void creatingASimpleFieldSetWithAnIntValue() throws FSParseException {
		SimpleFieldSet fieldSet = simpleFieldSetBuilder.put("Key", 17).get();
		assertThat(fieldSet.keyIterator(), contains("Key"));
		assertThat(fieldSet.getInt("Key"), is(17));
	}

	@Test
	public void creatingASimpleFieldSetWithALongValue() throws FSParseException {
		SimpleFieldSet fieldSet = simpleFieldSetBuilder.put("Key", 17L).get();
		assertThat(fieldSet.keyIterator(), contains("Key"));
		assertThat(fieldSet.getLong("Key"), is(17L));
	}

	@Test
	public void creatingASimpleFieldSetWithAnotherFieldSet() throws FSParseException {
		SimpleFieldSet originalFieldSet = new SimpleFieldSet(true);
		originalFieldSet.putSingle("StringKey", "Value");
		originalFieldSet.put("IntKey", 17);
		originalFieldSet.put("LongKey", 17L);
		SimpleFieldSet fieldSet = simpleFieldSetBuilder.put(originalFieldSet).get();
		assertThat(fieldSet.keyIterator(), containsInAnyOrder("IntKey", "StringKey", "LongKey"));
		assertThat(fieldSet.get("StringKey"), is("Value"));
		assertThat(fieldSet.getInt("IntKey"), is(17));
		assertThat(fieldSet.getLong("LongKey"), is(17L));
	}

}
