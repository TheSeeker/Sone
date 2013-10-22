/*
 * Sone - AbstractAlbum.java - Copyright © 2013 David Roden
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
import static com.google.common.base.Preconditions.checkNotNull;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Abstract {@link Album} implementation that contains only the attributes that
 * are {@link String}s or primitives.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public abstract class AbstractAlbum implements Album {

	protected final String id;
	protected final String parentId;
	protected String title;
	protected String description;
	protected String albumImage;

	protected AbstractAlbum(String id, String parentId) {
		this.id = checkNotNull(id, "id must not be null");
		this.parentId = parentId;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isEmpty() {
		return getAlbums().isEmpty() && getImages().isEmpty();
	}

	@Override
	public boolean isRoot() {
		return parentId == null;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Modifier modify() throws IllegalStateException {
		// TODO: reenable check for local Sones
		return new Modifier() {
			private Optional<String> title = absent();
			private Optional<String> description = absent();
			private Optional<String> albumImage = absent();

			@Override
			public Modifier setTitle(String title) {
				this.title = fromNullable(title);
				return this;
			}

			@Override
			public Modifier setDescription(String description) {
				this.description = fromNullable(description);
				return this;
			}

			@Override
			public Modifier setAlbumImage(String imageId) {
				this.albumImage = fromNullable(imageId);
				return this;
			}

			@Override
			public Album update() throws IllegalStateException {
				if (title.isPresent()) {
					AbstractAlbum.this.title = title.get();
				}
				if (description.isPresent()) {
					AbstractAlbum.this.description = description.get();
				}
				if (albumImage.isPresent()) {
					AbstractAlbum.this.albumImage = albumImage.get();
				}
				return AbstractAlbum.this;
			}
		};
	}

	@Override
	public String getFingerprint() {
		Hasher hash = Hashing.sha256().newHasher();
		hash.putString("Album(");
		hash.putString("ID(").putString(id).putString(")");
		hash.putString("Title(").putString(title).putString(")");
		hash.putString("Description(").putString(description).putString(")");
		if (albumImage != null) {
			hash.putString("AlbumImage(").putString(albumImage).putString(")");
		}

		/* add nested albums. */
		hash.putString("Albums(");
		for (Album album : getAlbums()) {
			hash.putString(album.getFingerprint());
		}
		hash.putString(")");

		/* add images. */
		hash.putString("Images(");
		for (Image image : getImages()) {
			if (image.isInserted()) {
				hash.putString(image.getFingerprint());
			}
		}
		hash.putString(")");

		hash.putString(")");
		return hash.hash().toString();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof DefaultAlbum)) {
			return false;
		}
		DefaultAlbum album = (DefaultAlbum) object;
		return id.equals(album.id);
	}
}
