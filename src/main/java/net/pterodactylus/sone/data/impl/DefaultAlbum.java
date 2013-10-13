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

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkState;

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

	/** The parent album. */
	private final DefaultAlbum parent;

	/** Nested albums. */
	private final List<Album> albums = new ArrayList<Album>();

	/** The image IDs in order. */
	final List<String> imageIds = new ArrayList<String>();

	/** The images in this album. */
	final Map<String, Image> images = new HashMap<String, Image>();

	/** Creates a new album with a random ID. */
	public DefaultAlbum(Sone sone, DefaultAlbum parent) {
		this(UUID.randomUUID().toString(), sone, parent);
	}

	/**
	 * Creates a new album with the given ID.
	 *
	 * @param id
	 * 		The ID of the album
	 */
	public DefaultAlbum(String id, Sone sone, DefaultAlbum parent) {
		super(id);
		this.sone = sone;
		this.parent = parent;
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
	public Optional<Image> getAlbumImage() {
		if (albumImage == null) {
			return absent();
		}
		return fromNullable(fromNullable(images.get(albumImage)).or(images.values().iterator().next()));
	}

	@Override
	public Album getParent() {
		return parent;
	}

	@Override
	public AlbumBuilder newAlbumBuilder() {
		return new DefaultAlbumBuilder(sone, this) {
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

	@Override
	public void moveUp() {
		int oldIndex = parent.albums.indexOf(this);
		parent.albums.remove(this);
		parent.albums.add(Math.max(0, oldIndex - 1), this);
	}

	@Override
	public void moveDown() {
		int oldIndex = parent.albums.indexOf(this);
		parent.albums.remove(this);
		parent.albums.add(Math.min(parent.albums.size(), oldIndex + 1), this);
	}

	@Override
	public void remove() throws IllegalStateException {
		checkState(!isRoot(), "can not remove root album");
		removeAllAlbums();
		removeAllImages();
		parent.albums.remove(this);
	}

	private void removeAllImages() {
		for (Image image : images.values()) {
			image.remove();
		}
	}

	private void removeAllAlbums() {
		for (Album album: albums) {
			album.remove();
		}
	}

}
