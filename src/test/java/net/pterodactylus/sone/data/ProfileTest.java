/*
 * Sone - FieldTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.pterodactylus.sone.data.Profile.Field;

import com.google.common.base.Optional;
import org.junit.Test;

/**
 * Unit test for {@link Field}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ProfileTest {

	final Profile profile = new Profile((Sone) null);

	@Test
	public void testAddingAField() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField");
		assertThat(testField.isPresent(), is(true));
		assertThat(testField.get().getName(), is("TestField"));
	}

	@Test
	public void testRenamingAField() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField");
		profile.renameField(testField.get(), "RenamedField");
		Optional<Field> renamedField = profile.getFieldByName("RenamedField");
		assertThat(testField.get().getId(), is(renamedField.get().getId()));
	}

	@Test
	public void testChangingTheValueOfAField() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField");
		profile.setField(testField.get(), "Test");
		testField = profile.getFieldByName("TestField");
		assertThat(testField.get().getValue(), is("Test"));
	}

}
