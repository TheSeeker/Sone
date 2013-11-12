/*
 * Sone - IdentityTest.java - Copyright © 2013 David Roden
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

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Sets.newHashSet;
import static net.pterodactylus.sone.freenet.wot.Identity.TO_CONTEXTS;
import static net.pterodactylus.sone.freenet.wot.Identity.TO_PROPERTIES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit test for {@link Identity}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class IdentityTest {

	@Test
	public void convertingAnIdentityIntoItsSones() {
		Identity identity = mock(Identity.class);
		when(identity.getContexts()).thenReturn(newHashSet("Test", "Test2"));
		Set<String> contexts = TO_CONTEXTS.apply(identity);
		assertThat(contexts, containsInAnyOrder("Test", "Test2"));
	}

	@Test
	public void convertingNullIntoAnEmptySet() {
		assertThat(TO_CONTEXTS.apply(null), empty());
	}

	@Test
	public void convertingAnIdentityIntoItsProperties() {
		Identity identity = mock(Identity.class);
		when(identity.getProperties()).thenReturn(of("KeyA", "ValueA", "KeyB", "ValueB"));
		Map<String, String> properties = TO_PROPERTIES.apply(identity);
		assertThat(properties.entrySet(), hasSize(2));
		assertThat(properties, hasEntry("KeyA", "ValueA"));
		assertThat(properties, hasEntry("KeyB", "ValueB"));
	}

	@Test
	public void convertingNullIntoAnEmptyMap() {
		assertThat(TO_PROPERTIES.apply(null).size(), is(0));
	}

}
