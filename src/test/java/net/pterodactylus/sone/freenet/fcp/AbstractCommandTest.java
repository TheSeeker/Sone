/*
 * Sone - AbstractCommandTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet.fcp;

import static net.pterodactylus.sone.freenet.fcp.AbstractCommand.getBoolean;
import static net.pterodactylus.sone.freenet.fcp.AbstractCommand.getInt;
import static net.pterodactylus.sone.freenet.fcp.AbstractCommand.getString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.pterodactylus.sone.freenet.SimpleFieldSetBuilder;

import freenet.support.SimpleFieldSet;

import org.junit.Test;

/**
 * Unit test for {@link AbstractCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractCommandTest {

	@Test(expected = FcpException.class)
	public void verifyThatAskingForANonExistingKeyCausesAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().get();
		getString(fieldSet, "NonExistingKey");
	}

	@Test
	public void verifyThatAskingForAnExistingKeyDoesNotCauseAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Key", "Value").get();
		String value = getString(fieldSet, "Key");
		assertThat(value, is("Value"));
	}

	@Test
	public void verifyThatAskingForAnExistingKeyWithDefaultReturnsTheKey() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Key", "Value").get();
		String value = getString(fieldSet, "Key", "DefaultValue");
		assertThat(value, is("Value"));
	}

	@Test
	public void verifyThatAskingForANonExistingKeyWithDefaultReturnsTheDefaultKey() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().get();
		String value = getString(fieldSet, "Key", "DefaultValue");
		assertThat(value, is("DefaultValue"));
	}

	@Test
	public void verifyThatAskingForAnExistingIntDoesNotCauseAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Int", "15").get();
		int value = getInt(fieldSet, "Int");
		assertThat(value, is(15));
	}

	@Test(expected = FcpException.class)
	public void verifyThatAskingForANonExistingIntDoesCauseAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("OtherInt", "15").get();
		getInt(fieldSet, "Int");
	}

	@Test(expected = FcpException.class)
	public void verifyThatAskingForAnInvalidIntDoesCauseAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Int", "foo").get();
		getInt(fieldSet, "Int");
	}

	@Test
	public void verifyThasAksingForAValidIntWithDefaultValueReturnsTheInt() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Int", 15).get();
		int value = getInt(fieldSet, "Int", 30);
		assertThat(value, is(15));
	}

	@Test
	public void verifyThasAksingForANonExistingIntWithDefaultValueReturnsTheDefaultValue() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Int", 15).get();
		int value = getInt(fieldSet, "OtherInt", 30);
		assertThat(value, is(30));
	}

	@Test
	public void verifyThasAksingForAnInvalidIntWithDefaultValueReturnsTheDefaultValue() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Int", "foo").get();
		int value = getInt(fieldSet, "Int", 30);
		assertThat(value, is(30));
	}

	@Test
	public void verifyThatAskingForAValidBooleanDoesNotCauseAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Boolean", "true").get();
		boolean value = getBoolean(fieldSet, "Boolean");
		assertThat(value, is(true));
	}

	@Test(expected = FcpException.class)
	public void verifyThatAskingForAnInvalidBooleanCausesAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Boolean", "foo").get();
		getBoolean(fieldSet, "Boolean");
	}

	@Test(expected = FcpException.class)
	public void verifyThatAskingForANonExistingBooleanCausesAnError() throws FcpException {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Boolean", "true").get();
		getBoolean(fieldSet, "OtherBoolean");
	}

	@Test
	public void verifyThatAskingForAValidBooleanWithDefaultValueReturnsTheBoolean() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Boolean", "true").get();
		boolean value = getBoolean(fieldSet, "Boolean", false);
		assertThat(value, is(true));
	}

	@Test
	public void verifyThatAskingForAnInvalidBooleanWithDefaultValueReturnsTheDefaultValue() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Boolean", "foo").get();
		boolean value = getBoolean(fieldSet, "Boolean", true);
		assertThat(value, is(true));
	}

	@Test
	public void verifyThatAskingForANonExistingBooleanWithDefaultValueReturnsTheDefaultValue() {
		SimpleFieldSet fieldSet = new SimpleFieldSetBuilder().put("Boolean", "foo").get();
		boolean value = getBoolean(fieldSet, "OtherBoolean", true);
		assertThat(value, is(true));
	}

}
