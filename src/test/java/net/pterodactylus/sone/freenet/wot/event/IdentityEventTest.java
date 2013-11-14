/*
 * Sone - IdentityEventTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet.wot.event;

import static net.pterodactylus.sone.freenet.wot.Identities.createIdentity;
import static net.pterodactylus.sone.freenet.wot.Identities.createOwnIdentity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;

import net.pterodactylus.sone.freenet.wot.Identity;
import net.pterodactylus.sone.freenet.wot.OwnIdentity;

import org.junit.Test;

/**
 * TODO
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class IdentityEventTest {

	private final OwnIdentity ownIdentity = createOwnIdentity("O1", Collections.<String>emptyList(), Collections.<String, String>emptyMap());
	private final Identity identity = createIdentity("I1", Collections.<String>emptyList(), Collections.<String, String>emptyMap());
	private final IdentityEvent identityEvent = new TestIdentityEvent(ownIdentity, identity);

	@Test
	public void identityEventStoresAndReturnsIdentities() {
		assertThat(identityEvent.ownIdentity(), is(ownIdentity));
		assertThat(identityEvent.identity(), is(identity));
	}

	@Test
	public void equalIdentityEventsHaveTheSameHashCode() {
		IdentityEvent identityEvent = new TestIdentityEvent(ownIdentity, identity);
		assertThat(identityEvent, is(this.identityEvent));
		assertThat(identityEvent.hashCode(), is(this.identityEvent.hashCode()));
	}

	@Test
	public void differentEventClassDoesNotMatch() {
		IdentityEvent identityEvent = new IdentityEvent(ownIdentity, identity) {
		};
		assertThat(identityEvent, not(is(this.identityEvent)));
	}

	private static class TestIdentityEvent extends IdentityEvent {

		protected TestIdentityEvent(OwnIdentity ownIdentity, Identity identity) {
			super(ownIdentity, identity);
		}

	}

}
