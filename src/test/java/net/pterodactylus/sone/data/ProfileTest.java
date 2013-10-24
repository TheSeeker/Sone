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

import static com.google.common.base.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.List;

import net.pterodactylus.sone.data.Profile.Field;

import com.google.common.base.Optional;
import org.junit.Test;

/**
 * Unit test for {@link Profile}.
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
	public void testGettingAFieldByName() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField");
		assertThat(testField.isPresent(), is(true));
	}

	@Test
	public void testGettingANonExistingFieldByName() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField2");
		assertThat(testField.isPresent(), is(false));
	}

	@Test
	public void testGettingAFieldById() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField");
		testField = profile.getFieldById(testField.get().getId());
		assertThat(testField.isPresent(), is(true));
	}

	@Test
	public void testGettingANonExistingFieldById() {
		Optional<Field> testField = profile.getFieldById("does not exist");
		assertThat(testField.isPresent(), is(false));
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
	public void testRenamingANonExistingField() {
		Field testField = profile.addField("TestField");
		profile.removeField(testField);
		profile.renameField(testField, "TestField2");
		Optional<Field> testField2 = profile.getFieldByName("TestField2");
		assertThat(testField2.isPresent(), is(false));
	}

	@Test
	public void testChangingTheValueOfAField() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField");
		profile.setField(testField.get(), "Test");
		testField = profile.getFieldByName("TestField");
		assertThat(testField.get().getValue(), is("Test"));
	}

	@Test
	public void testChangingTheValueOfANonExistingField() {
		Field testField = profile.addField("TestField");
		profile.removeField(testField);
		profile.setField(testField, "Test");
		Optional<Field> testField2 = profile.getFieldByName("TestField");
		assertThat(testField2.isPresent(), is(false));
	}

	@Test
	public void testDeletingAField() {
		profile.addField("TestField");
		Optional<Field> testField = profile.getFieldByName("TestField");
		profile.removeField(testField.get());
		testField = profile.getFieldByName("TestField");
		assertThat(testField.isPresent(), is(false));
	}

	@Test
	public void testDeletingANonExistingField() {
		Field testField = profile.addField("TestField");
		profile.removeField(testField);
		profile.removeField(testField);
	}

	@Test
	public void testGettingFieldList() {
		Field firstField = profile.addField("First");
		Field secondField = profile.addField("Second");
		List<Field> fields = profile.getFields();
		assertThat(fields, contains(firstField, secondField));
	}

	@Test
	public void testMovingAFieldUp() {
		Field firstField = profile.addField("First");
		Field secondField = profile.addField("Second");
		profile.moveFieldUp(secondField);
		List<Field> fields = profile.getFields();
		assertThat(fields, contains(secondField, firstField));
	}

	@Test
	public void testMovingTheFirstFieldUp() {
		Field firstField = profile.addField("First");
		Field secondField = profile.addField("Second");
		profile.moveFieldUp(firstField);
		List<Field> fields = profile.getFields();
		assertThat(fields, contains(firstField, secondField));
	}

	@Test
	public void testMovingAFieldDown() {
		Field firstField = profile.addField("First");
		Field secondField = profile.addField("Second");
		profile.moveFieldDown(firstField);
		List<Field> fields = profile.getFields();
		assertThat(fields, contains(secondField, firstField));
	}

	@Test
	public void testMovingTheLastFieldDown() {
		Field firstField = profile.addField("First");
		Field secondField = profile.addField("Second");
		profile.moveFieldDown(secondField);
		List<Field> fields = profile.getFields();
		assertThat(fields, contains(firstField, secondField));
	}

	@Test
	public void testModifyingAProfile() {
		profile.modify().setFirstName("First").setMiddleName("M.").setLastName("Last").setBirthYear(2013).setBirthMonth(10).setBirthDay(24).update();
		assertThat(profile.getFirstName(), is("First"));
		assertThat(profile.getMiddleName(), is("M."));
		assertThat(profile.getLastName(), is("Last"));
		assertThat(profile.getBirthYear(), is(2013));
		assertThat(profile.getBirthMonth(), is(10));
		assertThat(profile.getBirthDay(), is(24));
	}

	@Test
	public void testSettingAnAvatar() {
		profile.setAvatar(of("avatar1"));
		assertThat(profile.getAvatar(), is("avatar1"));
	}

	@Test
	public void testSettingNoAvatar() {
		profile.setAvatar(Optional.<String>absent());
		assertThat(profile.getAvatar(), is((String) null));
	}

}
