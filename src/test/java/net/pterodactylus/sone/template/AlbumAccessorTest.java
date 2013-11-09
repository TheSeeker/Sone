/*
 * Sone - AlbumAccessorTest.java - Copyright © 2013 David Roden
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

import static com.google.common.base.Optional.of;
import static net.pterodactylus.sone.template.SoneAccessor.getNiceName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.template.AlbumAccessor.Link;

import com.google.common.base.Optional;
import org.junit.Test;

/**
 * Unit test for {@link AlbumAccessor}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AlbumAccessorTest {

	private final Mocks mocks = new Mocks();
	private final Sone sone = mocks.mockSone("Sone").withName("Sone").withProfileName("S.", "O.", "Ne").create();
	private final AlbumAccessor albumAccessor = new AlbumAccessor();

	@Test
	public void generateSingleBacklinkForRootAlbum() {
		Album rootAlbum = createRootAlbum("AlbumId");
		List<Link> backlinks = (List<Link>) albumAccessor.get(null, rootAlbum, "backlinks");
		verifyAlbums(backlinks, rootAlbum);
	}

	@Test
	public void generateBacklinksForNestedAlbums() {
		Album rootAlbum = createRootAlbum("RootAlbum");
		Album firstLevelAlbum = createNestedAlbum(rootAlbum, "FirstLevel");
		Album secondLevelAlbum = createNestedAlbum(firstLevelAlbum, "SecondLevel");
		List<Link> backlinks = (List<Link>) albumAccessor.get(null, secondLevelAlbum, "backlinks");
		verifyAlbums(backlinks, secondLevelAlbum);
	}

	@Test
	public void returnAlbumImage() {
		Album album = createRootAlbum("AlbumId");
		Image albumImage = mock(Image.class);
		when(album.getAlbumImage()).thenReturn(of(albumImage));
		Image image = (Image) albumAccessor.get(null, album, "albumImage");
		assertThat(image, is(albumImage));
	}

	@Test
	public void returnNonExistingAlbumImage() {
		Album album = createRootAlbum("AlbumId");
		when(album.getAlbumImage()).thenReturn(Optional.<Image>absent());
		Image image = (Image) albumAccessor.get(null, album, "albumImage");
		assertThat(image, nullValue());
	}

	@Test
	public void reflectionAccessorIsUsed() {
		Album album = mock(Album.class);
		when(album.getId()).thenReturn("Album");
		assertThat(albumAccessor.get(null, album, "id"), is((Object) "Album"));
	}

	private void verifyAlbums(List<Link> backlinks, Album album) {
		assertThat(backlinks, hasSize(findAlbumLevel(album)));
		Album currentAlbum = album;
		int currentIndex = backlinks.size() - 1;
		while (!currentAlbum.isRoot()) {
			assertThat(backlinks.get(currentIndex).getTarget(), containsString(currentAlbum.getId()));
			assertThat(backlinks.get(currentIndex).getName(), is(currentAlbum.getTitle()));
			currentIndex--;
			currentAlbum = currentAlbum.getParent();
		}
		assertThat(backlinks.get(0).getTarget(), containsString(currentAlbum.getSone().getId()));
		assertThat(backlinks.get(0).getName(), is(getNiceName(currentAlbum.getSone())));
	}

	private int findAlbumLevel(Album album) {
		int albumLevel = 1;
		Album currentAlbum = album;
		while (!currentAlbum.isRoot()) {
			albumLevel++;
			currentAlbum = currentAlbum.getParent();
		}
		return albumLevel;
	}

	private Album createRootAlbum(String id) {
		Album album = mock(Album.class);
		when(album.getId()).thenReturn(id);
		when(album.isRoot()).thenReturn(true);
		when(album.getSone()).thenReturn(sone);
		return album;
	}

	private Album createNestedAlbum(Album parentAlbum, String id) {
		Album album = mock(Album.class);
		when(album.getId()).thenReturn(id);
		when(album.isRoot()).thenReturn(false);
		when(album.getSone()).thenReturn(sone);
		when(album.getParent()).thenReturn(parentAlbum);
		return album;
	}

}
