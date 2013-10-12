/*
 * Sone - Album.java - Copyright © 2011–2013 David Roden
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.AlbumBuilder;
import net.pterodactylus.sone.database.ImageBuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

/**
 * Dumb, store-everything-in-memory implementation of an {@link Album}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultAlbum extends AbstractAlbum {

	/** The Sone this album belongs to. */
	private Sone sone;

	/** Nested albums. */
	private final List<Album> albums = new ArrayList<Album>();

	/** The image IDs in order. */
	final List<String> imageIds = new ArrayList<String>();

	/** The images in this album. */
	final Map<String, Image> images = new HashMap<String, Image>();

	/** The parent album. */
	private Album parent;

	/** Creates a new album with a random ID. */
	public DefaultAlbum(Sone sone) {
		this(UUID.randomUUID().toString(), sone);
	}

	/**
	 * Creates a new album with the given ID.
	 *
	 * @param id
	 * 		The ID of the album
	 */
	public DefaultAlbum(String id, Sone sone) {
		super(id);
		this.sone = sone;
	}

	//
	// ACCESSORS
	//

	@Override
	public Sone getSone() {
		return sone;
	}

	@Override
	public List<Album> getAlbums() {
		return new ArrayList<Album>(albums);
	}

	@Override
	public void removeAlbum(Album album) {
		checkNotNull(album, "album must not be null");
		checkArgument(album.getSone().equals(sone), "album must belong this album’s Sone");
		checkArgument(equals(album.getParent()), "album must belong to this album");
		albums.remove(album);
		album.removeParent();
	}

	@Override
	public Album moveAlbumUp(Album album) {
		checkNotNull(album, "album must not be null");
		checkArgument(album.getSone().equals(sone), "album must belong to the same Sone as this album");
		checkArgument(equals(album.getParent()), "album must belong to this album");
		int oldIndex = albums.indexOf(album);
		if (oldIndex <= 0) {
			return null;
		}
		albums.remove(oldIndex);
		albums.add(oldIndex - 1, album);
		return albums.get(oldIndex);
	}

	@Override
	public Album moveAlbumDown(Album album) {
		checkNotNull(album, "album must not be null");
		checkArgument(album.getSone().equals(sone), "album must belong to the same Sone as this album");
		checkArgument(equals(album.getParent()), "album must belong to this album");
		int oldIndex = albums.indexOf(album);
		if ((oldIndex < 0) || (oldIndex >= (albums.size() - 1))) {
			return null;
		}
		albums.remove(oldIndex);
		albums.add(oldIndex + 1, album);
		return albums.get(oldIndex);
	}

	@Override
	public List<Image> getImages() {
		return new ArrayList<Image>(Collections2.filter(Collections2.transform(imageIds, new Function<String, Image>() {

			@Override
			@SuppressWarnings("synthetic-access")
			public Image apply(String imageId) {
				return images.get(imageId);
			}
		}), Predicates.notNull()));
	}

	@Override
	public Image moveImageUp(Image image) {
		checkNotNull(image, "image must not be null");
		checkNotNull(image.getSone(), "image must have an owner");
		checkArgument(image.getSone().equals(sone), "image must belong to the same Sone as this album");
		checkArgument(image.getAlbum().equals(this), "image must belong to this album");
		int oldIndex = imageIds.indexOf(image.getId());
		if (oldIndex <= 0) {
			return null;
		}
		imageIds.remove(image.getId());
		imageIds.add(oldIndex - 1, image.getId());
		return images.get(imageIds.get(oldIndex));
	}

	@Override
	public Image moveImageDown(Image image) {
		checkNotNull(image, "image must not be null");
		checkNotNull(image.getSone(), "image must have an owner");
		checkArgument(image.getSone().equals(sone), "image must belong to the same Sone as this album");
		checkArgument(image.getAlbum().equals(this), "image must belong to this album");
		int oldIndex = imageIds.indexOf(image.getId());
		if ((oldIndex == -1) || (oldIndex >= (imageIds.size() - 1))) {
			return null;
		}
		imageIds.remove(image.getId());
		imageIds.add(oldIndex + 1, image.getId());
		return images.get(imageIds.get(oldIndex));
	}

	@Override
	public Image getAlbumImage() {
		if (albumImage == null) {
			return null;
		}
		return Optional.fromNullable(images.get(albumImage)).or(images.values().iterator().next());
	}

	@Override
	public Album getParent() {
		return parent;
	}

	@Override
	public Album setParent(Album parent) {
		this.parent = checkNotNull(parent, "parent must not be null");
		return this;
	}

	@Override
	public Album removeParent() {
		this.parent = null;
		return this;
	}

	@Override
	public AlbumBuilder newAlbumBuilder() {
		return new DefaultAlbumBuilder(sone) {
			@Override
			public Album build() throws IllegalStateException {
				Album album = super.build();
				albums.add(album);
				return album;
			}
		};
	}

	@Override
	public ImageBuilder newImageBuilder() throws IllegalStateException {
		return new DefaultImageBuilder(sone, this) {
			@Override
			public Image build() throws IllegalStateException {
				Image image = super.build();
				if (images.isEmpty() && (albumImage == null)) {
					albumImage = image.getId();
				}
				images.put(image.getId(), image);
				imageIds.add(image.getId());
				return image;
			}
		};
	}

}
