/*
 * Sone - DefaultImage.java - Copyright © 2013 David Roden
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
import static com.google.common.base.Preconditions.checkState;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Dumb, store-everything-in-memory implementation of an {@link Image}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultImage implements Image {

	private final String id;
	private final Sone sone;
	private final Album album;
	private final long creationTime;
	private final int width;
	private final int height;
	private String key;
	private String title;
	private String description;

	public DefaultImage(String id, Sone sone, Album album, String key, long creationTime, int width, int height) {
		this.id = checkNotNull(id, "id must not be null");
		this.sone = sone;
		this.album = album;
		this.key = key;
		this.creationTime = creationTime;
		this.width = width;
		this.height = height;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Sone getSone() {
		return sone;
	}

	@Override
	public Album getAlbum() {
		return album;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public boolean isInserted() {
		return key != null;
	}

	@Override
	public long getCreationTime() {
		return creationTime;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
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
		// TODO: reenable check for local images
		return new Modifier() {
			private Optional<String> key = absent();
			private Optional<String> title = absent();
			private Optional<String> description = absent();

			@Override
			public Modifier setKey(String key) {
				this.key = fromNullable(key);
				return this;
			}

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
			public Image update() throws IllegalStateException {
				checkState(!key.isPresent() || (DefaultImage.this.key == null), "key can not be changed");

				if (key.isPresent()) {
					DefaultImage.this.key = key.get();
				}
				if (title.isPresent()) {
					DefaultImage.this.title = title.get();
				}
				if (description.isPresent()) {
					DefaultImage.this.description = description.get();
				}

				return DefaultImage.this;
			}
		};
	}

	@Override
	public String getFingerprint() {
		Hasher hash = Hashing.sha256().newHasher();
		hash.putString("Image(");
		hash.putString("ID(").putString(id).putString(")");
		hash.putString("Title(").putString(title).putString(")");
		hash.putString("Description(").putString(description).putString(")");
		hash.putString(")");
		return hash.hash().toString();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof DefaultImage)) {
			return false;
		}
		return ((DefaultImage) object).id.equals(id);
	}

}
