/*
 * Sone - SonePartTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.text;

import static net.pterodactylus.sone.template.SoneAccessor.getNiceName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;

import org.junit.Test;

/**
 * Unit test for {@link SonePart}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SonePartTest {

	private final Mocks mocks = new Mocks();
	private final Sone sone = mocks.mockSone("Sone").withName("NiceSone").withProfileName("N.", "Ice", "Name").create();
	private final SonePart sonePart = new SonePart(sone);

	@Test
	public void sonePartCanStoreAndReturnSone() {
		assertThat(sonePart.getSone(), is(sone));
	}

	@Test
	public void sonePartTextIsTheNiceNameOfTheSone() {
		assertThat(sonePart.getText(), is(getNiceName(sone)));
	}

	@Test
	public void twoSonePartsAreEqualIfTheirSonesAreEqual() {
		SonePart secondSonePart = new SonePart(sone);
		assertThat(sonePart, is(secondSonePart));
	}

	@Test
	public void equalSonePartsHaveEqualHashCodes() {
		SonePart secondSonePart = new SonePart(sone);
		assertThat(sonePart.hashCode(), is(secondSonePart.hashCode()));
	}

	@Test
	public void nullIsNotEqualToASonePart() {
		assertThat(sonePart, not(is((Object) null)));
	}

}
