/*
 * Sone - OwnIdentityEventTest.java - Copyright © 2013 David Roden
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;

import net.pterodactylus.sone.freenet.wot.Identities;
import net.pterodactylus.sone.freenet.wot.OwnIdentity;

import org.junit.Test;

/**
 * TODO
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class OwnIdentityEventTest {

	private final OwnIdentity ownIdentity = Identities.createOwnIdentity("O1", Collections.<String>emptyList(), Collections.<String, String>emptyMap());
	private final OwnIdentityEvent ownIdentityEvent = new TestOwnIdentityEvent(ownIdentity);

	@Test
	public void ownIdentityEventStoresAndReturnsOwnIdentity() {
		assertThat(ownIdentityEvent.ownIdentity(), is(ownIdentity));
	}

	@Test
	public void equalIdentityEventsHashTheSameHashCode() {
		OwnIdentityEvent ownIdentityEvent = new TestOwnIdentityEvent(ownIdentity);
		assertThat(ownIdentityEvent, is(this.ownIdentityEvent));
		assertThat(ownIdentityEvent.hashCode(), is(this.ownIdentityEvent.hashCode()));
	}

	@Test
	public void differentEventClassDoesNotMatch() {
		OwnIdentityEvent ownIdentityEvent = new OwnIdentityEvent(ownIdentity) {
		};
		assertThat(ownIdentityEvent, not(is(this.ownIdentityEvent)));
	}

	private static class TestOwnIdentityEvent extends OwnIdentityEvent {

		protected TestOwnIdentityEvent(OwnIdentity ownIdentity) {
			super(ownIdentity);
		}

	}

}
