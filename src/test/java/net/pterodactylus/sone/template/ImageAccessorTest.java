/*
 * Sone - ImageAccessorTest.java - Copyright © 2013 David Roden
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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;

import org.junit.Test;

/**
 * Unit test for {@link ImageAccessor}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ImageAccessorTest {

	private final ImageAccessor imageAccessor = new ImageAccessor();

	@Test
	public void previousImageIsFound() {
		Image image = createImage("Image3");
		List<Image> images = createAlbumWithImages(asList(
				createImage("Image1"),
				createImage("Image2"),
				image
		));
		Image previousImage = (Image) imageAccessor.get(null, image, "previous");
		assertThat(previousImage, is(images.get(1)));
	}

	private List<Image> createAlbumWithImages(List<Image> images) {
		Album album = mock(Album.class);
		when(album.getImages()).thenReturn(images);
		for (Image image : images) {
			when(image.getAlbum()).thenReturn(album);
		}
		return images;
	}

	@Test
	public void noPreviousImageIsFound() {
		Image image = createImage("Image3");
		createAlbumWithImages(asList(
				image,
				createImage("Image1"),
				createImage("Image2")
		));
		Image previousImage = (Image) imageAccessor.get(null, image, "previous");
		assertThat(previousImage, nullValue());
	}

	@Test
	public void noNextImageIsFound() {
		Image image = createImage("Image3");
		List<Image> images = createAlbumWithImages(asList(
				createImage("Image1"),
				createImage("Image2"),
				image
		));
		Image previousImage = (Image) imageAccessor.get(null, image, "next");
		assertThat(previousImage, nullValue());
	}

	@Test
	public void nextImageIsFound() {
		Image image = createImage("Image3");
		List<Image> images = createAlbumWithImages(asList(
				image,
				createImage("Image1"),
				createImage("Image2")
		));
		Image previousImage = (Image) imageAccessor.get(null, image, "next");
		assertThat(previousImage, is(images.get(1)));
	}

	@Test
	public void reflectionAccessorIsUsed() {
		Image image = createImage("imageId");
		String id = (String) imageAccessor.get(null, image, "id");
		assertThat(id, is(image.getId()));
	}

	private static Image createImage(String id) {
		Image image = mock(Image.class);
		when(image.getId()).thenReturn(id);
		return image;
	}

}
