/*
 * Sone - TemporaryImageTest.java - Copyright © 2013 David Roden
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import org.junit.Test;

/**
 * Unit test for {@link TemporaryImage}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class TemporaryImageTest {

	private final String mimeType = "application/octet-stream";
	private final byte[] imageData = new byte[] { 72, 69, 108, 108, 79, 32, 119, 79, 82, 76, 68 };
	private final int width = 640;
	private final int height = 360;
	private final TemporaryImage temporaryImage = new TemporaryImage(mimeType, imageData, width, height);

	@Test
	public void verifyThatTheTemporaryImageContainsTheGivenParameters() {
		assertThat(temporaryImage.getId(), notNullValue());
		assertThat(temporaryImage.getId().length(), greaterThan(0));
		assertThat(temporaryImage.getMimeType(), is(mimeType));
		assertThat(temporaryImage.getImageData(), is(imageData));
		assertThat(temporaryImage.getWidth(), is(width));
		assertThat(temporaryImage.getHeight(), is(height));
	}

}
