/*
 * Sone - AbstractImage.java - Copyright © 2013 David Roden
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

import net.pterodactylus.sone.data.Image;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Abstract {@link Image} implementation that contains only the attributes that
 * are {@link String}s or primitives.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public abstract class AbstractImage implements Image {

	protected final String id;
	protected final long creationTime;
	protected final int width;
	protected final int height;
	protected String key;
	protected String title;
	protected String description;

	public AbstractImage(String id, String key, long creationTime, int width, int height) {
		this.id = checkNotNull(id, "id must not be null");
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
				checkState(!key.isPresent() || (AbstractImage.this.key == null), "key can not be changed");

				if (key.isPresent()) {
					AbstractImage.this.key = key.get();
				}
				if (title.isPresent()) {
					AbstractImage.this.title = title.get();
				}
				if (description.isPresent()) {
					AbstractImage.this.description = description.get();
				}

				return AbstractImage.this;
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
		if (!(object instanceof AbstractImage)) {
			return false;
		}
		return ((AbstractImage) object).id.equals(id);
	}

}
