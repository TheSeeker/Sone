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

package net.pterodactylus.sone.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Container for images that can also contain nested {@link Album}s.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Album implements Identified, Fingerprintable {

	/** Compares two {@link Album}s by {@link #getTitle()}. */
	public static final Comparator<Album> TITLE_COMPARATOR = new Comparator<Album>() {

		@Override
		public int compare(Album leftAlbum, Album rightAlbum) {
			return leftAlbum.getTitle().compareToIgnoreCase(rightAlbum.getTitle());
		}
	};

	/** Function that flattens the given album and all albums beneath it. */
	public static final Function<Album, List<Album>> FLATTENER = new Function<Album, List<Album>>() {

		@Override
		public List<Album> apply(Album album) {
			List<Album> albums = new ArrayList<Album>();
			albums.add(album);
			for (Album subAlbum : album.getAlbums()) {
				albums.addAll(FluentIterable.from(ImmutableList.of(subAlbum)).transformAndConcat(FLATTENER).toList());
			}
			return albums;
		}
	};

	/** Function that transforms an album into the images it contains. */
	public static final Function<Album, List<Image>> IMAGES = new Function<Album, List<Image>>() {

		@Override
		public List<Image> apply(Album album) {
			return album.getImages();
		}
	};

	/**
	 * Filter that removes all albums that do not have any images in any album
	 * below it.
	 */
	public static final Predicate<Album> NOT_EMPTY = new Predicate<Album>() {

		@Override
		public boolean apply(Album album) {
			/* so, we flatten all albums below the given one and check whether at least one album… */
			return FluentIterable.from(asList(album)).transformAndConcat(FLATTENER).anyMatch(new Predicate<Album>() {

				@Override
				public boolean apply(Album album) {
					/* …contains any inserted images. */
					return !album.getImages().isEmpty() && FluentIterable.from(album.getImages()).allMatch(new Predicate<Image>() {

						@Override
						public boolean apply(Image input) {
							return input.isInserted();
						}
					});
				}
			});
		}
	};

	/** The ID of this album. */
	private final String id;

	/** The Sone this album belongs to. */
	private Sone sone;

	/** Nested albums. */
	private final List<Album> albums = new ArrayList<Album>();

	/** The image IDs in order. */
	private final List<String> imageIds = new ArrayList<String>();

	/** The images in this album. */
	private final Map<String, Image> images = new HashMap<String, Image>();

	/** The parent album. */
	private Album parent;

	/** The title of this album. */
	private String title;

	/** The description of this album. */
	private String description;

	/** The ID of the album picture. */
	private String albumImage;

	/**
	 * Creates a new album with a random ID.
	 */
	public Album() {
		this(UUID.randomUUID().toString());
	}

	/**
	 * Creates a new album with the given ID.
	 *
	 * @param id
	 *            The ID of the album
	 */
	public Album(String id) {
		this.id = checkNotNull(id, "id must not be null");
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the ID of this album.
	 *
	 * @return The ID of this album
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the Sone this album belongs to.
	 *
	 * @return The Sone this album belongs to
	 */
	public Sone getSone() {
		return sone;
	}

	/**
	 * Sets the owner of the album. The owner can only be set as long as the
	 * current owner is {@code null}.
	 *
	 * @param sone
	 *            The album owner
	 * @return This album
	 */
	public Album setSone(Sone sone) {
		checkNotNull(sone, "sone must not be null");
		checkState((this.sone == null) || (this.sone.equals(sone)), "album owner must not already be set to some other Sone");
		this.sone = sone;
		return this;
	}

	/**
	 * Returns the nested albums.
	 *
	 * @return The nested albums
	 */
	public List<Album> getAlbums() {
		return new ArrayList<Album>(albums);
	}

	/**
	 * Adds an album to this album.
	 *
	 * @param album
	 *            The album to add
	 */
	public void addAlbum(Album album) {
		checkNotNull(album, "album must not be null");
		checkArgument(album.getSone().equals(sone), "album must belong to the same Sone as this album");
		album.setParent(this);
		if (!albums.contains(album)) {
			albums.add(album);
		}
	}

	/**
	 * Removes an album from this album.
	 *
	 * @param album
	 *            The album to remove
	 */
	public void removeAlbum(Album album) {
		checkNotNull(album, "album must not be null");
		checkArgument(album.sone.equals(sone), "album must belong this album’s Sone");
		checkArgument(equals(album.parent), "album must belong to this album");
		albums.remove(album);
		album.removeParent();
	}

	/**
	 * Moves the given album up in this album’s albums. If the album is already
	 * the first album, nothing happens.
	 *
	 * @param album
	 *            The album to move up
	 * @return The album that the given album swapped the place with, or
	 *         <code>null</code> if the album did not change its place
	 */
	public Album moveAlbumUp(Album album) {
		checkNotNull(album, "album must not be null");
		checkArgument(album.sone.equals(sone), "album must belong to the same Sone as this album");
		checkArgument(equals(album.parent), "album must belong to this album");
		int oldIndex = albums.indexOf(album);
		if (oldIndex <= 0) {
			return null;
		}
		albums.remove(oldIndex);
		albums.add(oldIndex - 1, album);
		return albums.get(oldIndex);
	}

	/**
	 * Moves the given album down in this album’s albums. If the album is
	 * already the last album, nothing happens.
	 *
	 * @param album
	 *            The album to move down
	 * @return The album that the given album swapped the place with, or
	 *         <code>null</code> if the album did not change its place
	 */
	public Album moveAlbumDown(Album album) {
		checkNotNull(album, "album must not be null");
		checkArgument(album.sone.equals(sone), "album must belong to the same Sone as this album");
		checkArgument(equals(album.parent), "album must belong to this album");
		int oldIndex = albums.indexOf(album);
		if ((oldIndex < 0) || (oldIndex >= (albums.size() - 1))) {
			return null;
		}
		albums.remove(oldIndex);
		albums.add(oldIndex + 1, album);
		return albums.get(oldIndex);
	}

	/**
	 * Returns the images in this album.
	 *
	 * @return The images in this album
	 */
	public List<Image> getImages() {
		return new ArrayList<Image>(Collections2.filter(Collections2.transform(imageIds, new Function<String, Image>() {

			@Override
			@SuppressWarnings("synthetic-access")
			public Image apply(String imageId) {
				return images.get(imageId);
			}
		}), Predicates.notNull()));
	}

	/**
	 * Adds the given image to this album.
	 *
	 * @param image
	 *            The image to add
	 */
	public void addImage(Image image) {
		checkNotNull(image, "image must not be null");
		checkNotNull(image.getSone(), "image must have an owner");
		checkArgument(image.getSone().equals(sone), "image must belong to the same Sone as this album");
		if (image.getAlbum() != null) {
			image.getAlbum().removeImage(image);
		}
		image.setAlbum(this);
		if (imageIds.isEmpty() && (albumImage == null)) {
			albumImage = image.getId();
		}
		if (!imageIds.contains(image.getId())) {
			imageIds.add(image.getId());
			images.put(image.getId(), image);
		}
	}

	/**
	 * Removes the given image from this album.
	 *
	 * @param image
	 *            The image to remove
	 */
	public void removeImage(Image image) {
		checkNotNull(image, "image must not be null");
		checkNotNull(image.getSone(), "image must have an owner");
		checkArgument(image.getSone().equals(sone), "image must belong to the same Sone as this album");
		imageIds.remove(image.getId());
		images.remove(image.getId());
		if (image.getId().equals(albumImage)) {
			if (images.isEmpty()) {
				albumImage = null;
			} else {
				albumImage = images.values().iterator().next().getId();
			}
		}
	}

	/**
	 * Moves the given image up in this album’s images. If the image is already
	 * the first image, nothing happens.
	 *
	 * @param image
	 *            The image to move up
	 * @return The image that the given image swapped the place with, or
	 *         <code>null</code> if the image did not change its place
	 */
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

	/**
	 * Moves the given image down in this album’s images. If the image is
	 * already the last image, nothing happens.
	 *
	 * @param image
	 *            The image to move down
	 * @return The image that the given image swapped the place with, or
	 *         <code>null</code> if the image did not change its place
	 */
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

	/**
	 * Returns the album image of this album, or {@code null} if no album image
	 * has been set.
	 *
	 * @return The image to show when this album is listed
	 */
	public Image getAlbumImage() {
		if (albumImage == null) {
			return null;
		}
		return Optional.fromNullable(images.get(albumImage)).or(images.values().iterator().next());
	}

	/**
	 * Sets the ID of the album image.
	 *
	 * @param id
	 *            The ID of the album image
	 * @return This album
	 */
	public Album setAlbumImage(String id) {
		this.albumImage = id;
		return this;
	}

	/**
	 * Returns whether this album contains any other albums or images.
	 *
	 * @return {@code true} if this album is empty, {@code false} otherwise
	 */
	public boolean isEmpty() {
		return albums.isEmpty() && images.isEmpty();
	}

	/**
	 * Returns whether this album is an identitiy’s root album.
	 *
	 * @return {@code true} if this album is an identity’s root album, {@code
	 *         false} otherwise
	 */
	public boolean isRoot() {
		return parent == null;
	}

	/**
	 * Returns the parent album of this album.
	 *
	 * @return The parent album of this album, or {@code null} if this album
	 *         does not have a parent
	 */
	public Album getParent() {
		return parent;
	}

	/**
	 * Sets the parent album of this album.
	 *
	 * @param parent
	 *            The new parent album of this album
	 * @return This album
	 */
	protected Album setParent(Album parent) {
		this.parent = checkNotNull(parent, "parent must not be null");
		return this;
	}

	/**
	 * Removes the parent album of this album.
	 *
	 * @return This album
	 */
	protected Album removeParent() {
		this.parent = null;
		return this;
	}

	/**
	 * Returns the title of this album.
	 *
	 * @return The title of this album
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title of this album.
	 *
	 * @param title
	 *            The title of this album
	 * @return This album
	 */
	public Album setTitle(String title) {
		this.title = checkNotNull(title, "title must not be null");
		return this;
	}

	/**
	 * Returns the description of this album.
	 *
	 * @return The description of this album
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this album.
	 *
	 * @param description
	 *            The description of this album
	 * @return This album
	 */
	public Album setDescription(String description) {
		this.description = checkNotNull(description, "description must not be null");
		return this;
	}

	//
	// FINGERPRINTABLE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
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
		for (Album album : albums) {
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

	//
	// OBJECT METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Album)) {
			return false;
		}
		Album album = (Album) object;
		return id.equals(album.id);
	}

}
