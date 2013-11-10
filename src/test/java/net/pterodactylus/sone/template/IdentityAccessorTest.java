/*
 * Sone - IdentityAccessorTest.java - Copyright © 2010–2013 David Roden
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

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.freenet.wot.DefaultOwnIdentity;
import net.pterodactylus.sone.freenet.wot.IdentityManager;
import net.pterodactylus.sone.freenet.wot.OwnIdentity;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link IdentityAccessor}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class IdentityAccessorTest {

	private final Mocks mocks = new Mocks();
	private final IdentityAccessor identityAccessor = new IdentityAccessor(mocks.core);

	@Before
	public void setup() {
		IdentityManager identityManager = mock(IdentityManager.class);
		when(mocks.core.getIdentityManager()).thenReturn(identityManager);
	}

	@Test
	public void uniqueNicknameWithoutIdIsFound() {
		when(mocks.core.getIdentityManager().getAllOwnIdentities()).thenReturn(newHashSet(
				(OwnIdentity) new DefaultOwnIdentity("Test", "Test", "R", "I")
		));
		OwnIdentity ownIdentity = new DefaultOwnIdentity("Unique", "Unique", "R", "I");
		String uniqueNickname = (String) identityAccessor.get(null, ownIdentity, "uniqueNickname");
		assertThat(uniqueNickname, is("Unique"));
	}

	@Test
	public void uniqueNicknameWithIdIsFound() {
		OwnIdentity ownIdentity = new DefaultOwnIdentity("Unique", "Unique", "R", "I");
		when(mocks.core.getIdentityManager().getAllOwnIdentities()).thenReturn(newHashSet(
				ownIdentity,
				new DefaultOwnIdentity("Unequivocal", "Unique", "R", "I")
		));
		String uniqueNickname = (String) identityAccessor.get(null, ownIdentity, "uniqueNickname");
		assertThat(uniqueNickname, is("Unique@Uni"));
	}

	@Test
	public void reflectionAccessorIsUsed() {
		OwnIdentity ownIdentity = new DefaultOwnIdentity("Unique", "Unique", "R", "I");
		String id = (String) identityAccessor.get(null, ownIdentity, "id");
		assertThat(id, is(ownIdentity.getId()));
	}

}
