/*
 * Sone - CollectionAccessorTest.java - Copyright © 2013 David Roden
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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collection;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;

import org.junit.Test;

/**
 * Unit test for {@link CollectionAccessor}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class CollectionAccessorTest {

	private final Mocks mocks = new Mocks();
	private final CollectionAccessor collectionAccessor = new CollectionAccessor();

	@Test
	public void soneNamesAreReturnedSorted() {
		Sone sone1 = mocks.mockSone("Sone1").withName("Sone1").create();
		Sone sone2 = mocks.mockSone("Sone2").withName("Sone2").create();
		Sone sone3 = mocks.mockSone("Sone3").withName("Sone3").create();
		Collection<Sone> sones = asList(sone3, sone1, sone2);
		String soneNames = (String) collectionAccessor.get(null, sones, "soneNames");
		assertThat(soneNames, is("Sone1, Sone2, Sone3"));
	}

	@Test
	public void collectionsWithOtherObjectsUseOnlyTheSones() {
		Sone sone1 = mocks.mockSone("Sone1").withName("Sone1").create();
		Sone sone2 = mocks.mockSone("Sone2").withName("Sone2").create();
		Sone sone3 = mocks.mockSone("Sone3").withName("Sone3").create();
		Collection<Object> sones = asList(sone3, new Object(), sone1, sone2);
		String soneNames = (String) collectionAccessor.get(null, sones, "soneNames");
		assertThat(soneNames, is("Sone1, Sone2, Sone3"));
	}

	@Test
	public void reflectionAccessorIsUsed() {
		Collection<Object> objects = asList(new Object(), new Object());
		int size = (Integer) collectionAccessor.get(null, objects, "size");
		assertThat(size, is(objects.size()));
	}

	@Test
	public void nullCollectionReturnsNull() {
		assertThat(collectionAccessor.get(null, null, null), nullValue());
	}

}
