/*
 * Sone - AbstractSoneCommandTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.fcp;

import static com.google.common.base.Optional.of;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeSone;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.data.Profile;
import net.pterodactylus.sone.data.Sone;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import com.google.common.base.Optional;
import org.junit.Test;
import org.mockito.Matchers;

/**
 * Unit test for {@link AbstractSoneCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractSoneCommandTest {

	@Test
	public void testStringEncoding() {
		String testString = prepareStringToBeEncoded();

		String encodedString = encodeString(testString);
		assertThat(encodedString, notNullValue());
		assertThat(encodedString.length(), is(testString.length() + 3));
	}

	private String prepareStringToBeEncoded() {
		StringBuilder testString = new StringBuilder();
		for (int i = 0; i < 4000; ++i) {
			testString.append((char) i);
		}
		return testString.toString();
	}

	@Test
	public void testEncodingASone() throws FSParseException {
		Sone sone = prepareSoneToBeEncoded();
		SimpleFieldSet soneFieldSet = encodeSone(sone, "Prefix.", Optional.<Sone>absent());
		assertThat(soneFieldSet, notNullValue());
		assertThat(soneFieldSet.get("Prefix.Name"), is("test"));
		assertThat(soneFieldSet.get("Prefix.NiceName"), is("First M. Last"));
		assertThat(soneFieldSet.getLong("Prefix.LastUpdated"), is(sone.getTime()));
		assertThat(soneFieldSet.get("Prefix.Followed"), nullValue());
		assertThat(soneFieldSet.getInt("Prefix.Field.Count"), is(1));
		assertThat(soneFieldSet.get("Prefix.Field.0.Name"), is("Test1"));
		assertThat(soneFieldSet.get("Prefix.Field.0.Value"), is("Value1"));
	}

	@Test
	public void testEncodingAFollowedSone() throws FSParseException {
		Sone sone = prepareSoneToBeEncoded();
		Sone localSone = prepareLocalSoneThatFollowsEverybody();
		SimpleFieldSet soneFieldSet = encodeSone(sone, "Prefix.", of(localSone));
		assertThat(soneFieldSet, notNullValue());
		assertThat(soneFieldSet.get("Prefix.Name"), is("test"));
		assertThat(soneFieldSet.get("Prefix.NiceName"), is("First M. Last"));
		assertThat(soneFieldSet.getLong("Prefix.LastUpdated"), is(sone.getTime()));
		assertThat(soneFieldSet.get("Prefix.Followed"), is("true"));
		assertThat(soneFieldSet.getInt("Prefix.Field.Count"), is(1));
		assertThat(soneFieldSet.get("Prefix.Field.0.Name"), is("Test1"));
		assertThat(soneFieldSet.get("Prefix.Field.0.Value"), is("Value1"));
	}

	@Test
	public void testEncodingANotFollowedSone() throws FSParseException {
		Sone sone = prepareSoneToBeEncoded();
		Sone localSone = prepareLocalSoneThatFollowsNobody();
		SimpleFieldSet soneFieldSet = encodeSone(sone, "Prefix.", of(localSone));
		assertThat(soneFieldSet, notNullValue());
		assertThat(soneFieldSet.get("Prefix.Name"), is("test"));
		assertThat(soneFieldSet.get("Prefix.NiceName"), is("First M. Last"));
		assertThat(soneFieldSet.getLong("Prefix.LastUpdated"), is(sone.getTime()));
		assertThat(soneFieldSet.get("Prefix.Followed"), is("false"));
		assertThat(soneFieldSet.getInt("Prefix.Field.Count"), is(1));
		assertThat(soneFieldSet.get("Prefix.Field.0.Name"), is("Test1"));
		assertThat(soneFieldSet.get("Prefix.Field.0.Value"), is("Value1"));
	}

	private Sone prepareLocalSoneThatFollowsEverybody() {
		Sone sone = mock(Sone.class);
		when(sone.hasFriend(Matchers.<String>any())).thenReturn(true);
		return sone;
	}

	private Sone prepareLocalSoneThatFollowsNobody() {
		Sone sone = mock(Sone.class);
		when(sone.hasFriend(Matchers.<String>any())).thenReturn(false);
		return sone;
	}

	private Sone prepareSoneToBeEncoded() {
		long time = (long) (Math.random() * Long.MAX_VALUE);
		Sone sone = mock(Sone.class);
		when(sone.getName()).thenReturn("test");
		when(sone.getTime()).thenReturn(time);
		when(sone.getProfile()).thenReturn(prepareProfile(sone, "First", "M.", "Last"));
		return sone;
	}

	private Profile prepareProfile(Sone sone, String firstName, String middleName, String lastName) {
		Profile profile = new Profile(sone).modify().setFirstName(firstName).setMiddleName(middleName).setLastName(lastName).update();
		profile.setField(profile.addField("Test1"), "Value1");
		return profile;
	}

}
