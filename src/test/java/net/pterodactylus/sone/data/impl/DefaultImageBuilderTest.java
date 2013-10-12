/*
 * Sone - ImageBuilderImplTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.data.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.ImageBuilder;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * TODO
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultImageBuilderTest {

	private static final String ID = "12345";
	private static final long CREATION_TIME = 1234;
	private static final String KEY = "key";
	private static final int WIDTH = 640;
	private static final int HEIGHT = 270;

	private final Sone sone = mock(Sone.class);
	private final Album album = mock(Album.class);
	private final ImageBuilder imageBuilder = new DefaultImageBuilder(sone, album);

	@Test
	public void testImageCreationWithAllExplicitParameters() {
		Image image = imageBuilder.withId(ID).created(CREATION_TIME).at(KEY).sized(WIDTH, HEIGHT).build();
		assertThat(image, CoreMatchers.notNullValue());
		assertThat(image.getId(), is(ID));
		assertThat(image.getSone(), is(sone));
		assertThat(image.getCreationTime(), is(CREATION_TIME));
		assertThat(image.getKey(), is(KEY));
		assertThat(image.getWidth(), is(WIDTH));
		assertThat(image.getHeight(), is(HEIGHT));
	}

	@Test
	public void testImageCreationWithRandomId() {
		Image image = imageBuilder.randomId().created(CREATION_TIME).at(KEY).sized(WIDTH, HEIGHT).build();
		assertThat(image, CoreMatchers.notNullValue());
		assertThat(image.getId(), notNullValue());
		assertThat(image.getSone(), is(sone));
		assertThat(image.getCreationTime(), is(CREATION_TIME));
		assertThat(image.getKey(), is(KEY));
		assertThat(image.getWidth(), is(WIDTH));
		assertThat(image.getHeight(), is(HEIGHT));
	}

	@Test
	public void testImageCreationWithCurrentTime() {
		Image image = imageBuilder.withId(ID).createdNow().at(KEY).sized(WIDTH, HEIGHT).build();
		assertThat(image, CoreMatchers.notNullValue());
		assertThat(image.getId(), is(ID));
		assertThat(image.getSone(), is(sone));
		assertThat(image.getCreationTime() > 0, is(true));
		assertThat(image.getKey(), is(KEY));
		assertThat(image.getWidth(), is(WIDTH));
		assertThat(image.getHeight(), is(HEIGHT));
	}

	@Test(expected = IllegalStateException.class)
	public void testThatImageCreationWithoutAnIdFails() {
		imageBuilder.created(CREATION_TIME).at(KEY).sized(WIDTH, HEIGHT).build();
	}

	@Test(expected = IllegalStateException.class)
	public void testThatImageCreationWithoutATimeFails() {
		imageBuilder.withId(ID).at(KEY).sized(WIDTH, HEIGHT).build();
	}

	@Test(expected = IllegalStateException.class)
	public void testThatImageCreationWithoutASizeFails() {
		imageBuilder.withId(ID).createdNow().at(KEY).build();
	}

	@Test(expected = IllegalStateException.class)
	public void testThatImageCreationWithoutInvalidWidthFails() {
		imageBuilder.withId(ID).createdNow().at(KEY).sized(0, 1).build();
	}

	@Test(expected = IllegalStateException.class)
	public void testThatImageCreationWithoutInvalidHeightFails() {
		imageBuilder.withId(ID).createdNow().at(KEY).sized(1, 0).build();
	}

}
