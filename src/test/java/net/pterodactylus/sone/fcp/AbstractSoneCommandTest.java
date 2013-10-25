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
import static java.util.Arrays.asList;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeSone;
import static net.pterodactylus.sone.fcp.AbstractSoneCommand.encodeString;
import static net.pterodactylus.sone.template.SoneAccessor.getNiceName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

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
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
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
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
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
		Sone sone = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "test", "First", "M.", "Last", (long) (Math.random() * Long.MAX_VALUE));
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

	@Test
	public void testEncodingMultipleSones() throws FSParseException {
		List<Sone> sones = prepareMultipleSones();
		SimpleFieldSet sonesFieldSet = AbstractSoneCommand.encodeSones(sones, "Prefix.");
		assertThat(sonesFieldSet, notNullValue());
		assertThat(sonesFieldSet.getInt("Prefix.Count"), is(sones.size()));
		assertThat(sonesFieldSet.get("Prefix.0.ID"), is(sones.get(0).getId()));
		assertThat(sonesFieldSet.get("Prefix.0.Name"), is(sones.get(0).getName()));
		assertThat(sonesFieldSet.get("Prefix.0.NiceName"), is(getNiceName(sones.get(0))));
		assertThat(sonesFieldSet.getLong("Prefix.0.Time"), is(sones.get(0).getTime()));
		assertThat(sonesFieldSet.get("Prefix.1.ID"), is(sones.get(1).getId()));
		assertThat(sonesFieldSet.get("Prefix.1.Name"), is(sones.get(1).getName()));
		assertThat(sonesFieldSet.get("Prefix.1.NiceName"), is(getNiceName(sones.get(1))));
		assertThat(sonesFieldSet.getLong("Prefix.1.Time"), is(sones.get(1).getTime()));
		assertThat(sonesFieldSet.get("Prefix.2.ID"), is(sones.get(2).getId()));
		assertThat(sonesFieldSet.get("Prefix.2.Name"), is(sones.get(2).getName()));
		assertThat(sonesFieldSet.get("Prefix.2.NiceName"), is(getNiceName(sones.get(2))));
		assertThat(sonesFieldSet.getLong("Prefix.2.Time"), is(sones.get(2).getTime()));
	}

	private List<Sone> prepareMultipleSones() {
		Sone sone1 = createSone("jXH8d-eFdm14R69WyaCgQoSjaY0jl-Ut6etlXjK0e6E", "Test1", "Alpha", "A.", "First", (long) (Math.random() * Long.MAX_VALUE));
		Sone sone2 = createSone("KpoohJSbZGltHHG-YsxKV8ojjS5gwScRv50kl3AkLXg", "Test2", "Beta", "B.", "Second", (long) (Math.random() * Long.MAX_VALUE));
		Sone sone3 = createSone("-1Q6LhHvx91C1mSjOS3zznRSNUC4OxoHUbhIgBAyW1U", "Test3", "Gamma", "C.", "Third", (long) (Math.random() * Long.MAX_VALUE));
		return asList(sone1, sone2, sone3);
	}

	private Sone createSone(String id, String name, String firstName, String middleName, String lastName, long time) {
		Sone sone = mock(Sone.class);
		when(sone.getId()).thenReturn(id);
		when(sone.getName()).thenReturn(name);
		when(sone.getProfile()).thenReturn(prepareProfile(sone, firstName, middleName, lastName));
		when(sone.getTime()).thenReturn(time);
		return sone;
	}

	private Profile prepareProfile(Sone sone, String firstName, String middleName, String lastName) {
		Profile profile = new Profile(sone).modify().setFirstName(firstName).setMiddleName(middleName).setLastName(lastName).update();
		profile.setField(profile.addField("Test1"), "Value1");
		return profile;
	}

}
