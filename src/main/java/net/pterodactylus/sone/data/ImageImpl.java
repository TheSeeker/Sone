/*
 * Sone - ImageImpl.java - Copyright © 2011–2013 David Roden
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

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.UUID;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Container for image metadata.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ImageImpl implements Image {

	/** The ID of the image. */
	private final String id;

	/** The Sone the image belongs to. */
	private final Sone sone;

	/** The album this image belongs to. */
	private Album album;

	/** The request key of the image. */
	private String key;

	/** The creation time of the image. */
	private final long creationTime;

	/** The width of the image. */
	private final int width;

	/** The height of the image. */
	private final int height;

	/** The title of the image. */
	private String title;

	/** The description of the image. */
	private String description;

	/** Creates a new image with a random ID. */
	public ImageImpl(Sone sone, long creationTime, String key, int width, int height) {
		this(UUID.randomUUID().toString(), sone, creationTime, key, width, height);
	}

	/**
	 * Creates a new image.
	 *
	 * @param id
	 * 		The ID of the image
	 * @param creationTime
	 */
	public ImageImpl(String id, Sone sone, long creationTime, String key, int width, int height) {
		this.id = checkNotNull(id, "id must not be null");
		this.sone = sone;
		this.creationTime = creationTime;
		this.key = key;
		this.width = width;
		this.height = height;
	}

	//
	// ACCESSORS
	//

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
				checkState(!key.isPresent() || (ImageImpl.this.key == null), "key can not be changed");

				if (key.isPresent()) {
					ImageImpl.this.key = key.get();
				}
				if (title.isPresent()) {
					ImageImpl.this.title = title.get();
				}
				if (description.isPresent()) {
					ImageImpl.this.description = description.get();
				}

				return ImageImpl.this;
			}
		};
	}

	//
	// FINGERPRINTABLE METHODS
	//

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

	//
	// OBJECT METHODS
	//

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof ImageImpl)) {
			return false;
		}
		return ((ImageImpl) object).id.equals(id);
	}

}
