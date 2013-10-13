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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;

import net.pterodactylus.sone.database.AlbumBuilder;
import net.pterodactylus.sone.database.ImageBuilder;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

/**
 * Container for images that can also contain nested {@link Album}s.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public interface Album extends Identified, Fingerprintable {

	/** Compares two {@link Album}s by {@link #getTitle()}. */
	Comparator<Album> TITLE_COMPARATOR = new Comparator<Album>() {

		@Override
		public int compare(Album leftAlbum, Album rightAlbum) {
			return leftAlbum.getTitle().compareToIgnoreCase(rightAlbum.getTitle());
		}
	};

	/** Function that flattens the given album and all albums beneath it. */
	Function<Album, List<Album>> FLATTENER = new Function<Album, List<Album>>() {

		@Override
		@Nonnull
		public List<Album> apply(Album album) {
			if (album == null) {
				return emptyList();
			}
			List<Album> albums = new ArrayList<Album>();
			albums.add(album);
			for (Album subAlbum : album.getAlbums()) {
				albums.addAll(FluentIterable.from(ImmutableList.of(subAlbum)).transformAndConcat(FLATTENER).toList());
			}
			return albums;
		}
	};

	/** Function that transforms an album into the images it contains. */
	Function<Album, List<Image>> IMAGES = new Function<Album, List<Image>>() {

		@Override
		@Nonnull
		public List<Image> apply(Album album) {
			return (album != null) ? album.getImages() : Collections.<Image>emptyList();
		}
	};

	/**
	 * Filter that removes all albums that do not have any images in any album
	 * below it.
	 */
	Predicate<Album> NOT_EMPTY = new Predicate<Album>() {

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

	/**
	 * Returns the ID of this album.
	 *
	 * @return The ID of this album
	 */
	String getId();

	/**
	 * Returns the Sone this album belongs to.
	 *
	 * @return The Sone this album belongs to
	 */
	Sone getSone();

	List<Album> getAlbums();

	/**
	 * Returns the images in this album.
	 *
	 * @return The images in this album
	 */
	List<Image> getImages();

	/**
	 * Returns the album image of this album, or {@code null} if no album image has
	 * been set.
	 *
	 * @return The image to show when this album is listed
	 */
	Image getAlbumImage();

	/**
	 * Returns whether this album contains any other albums or images.
	 *
	 * @return {@code true} if this album is empty, {@code false} otherwise
	 */
	boolean isEmpty();

	/**
	 * Returns whether this album is an identitiy’s root album.
	 *
	 * @return {@code true} if this album is an identity’s root album, {@code
	 *         false} otherwise
	 */
	boolean isRoot();

	/**
	 * Returns the parent album of this album.
	 *
	 * @return The parent album of this album, or {@code null} if this album does
	 *         not have a parent
	 */
	Album getParent();

	/**
	 * Returns the title of this album.
	 *
	 * @return The title of this album
	 */
	String getTitle();

	/**
	 * Returns the description of this album.
	 *
	 * @return The description of this album
	 */
	String getDescription();

	AlbumBuilder newAlbumBuilder() throws IllegalStateException;

	ImageBuilder newImageBuilder() throws IllegalStateException;

	/**
	 * Returns a modifier for this album.
	 *
	 * @return A modifier for this album
	 * @throws IllegalStateException
	 * 		if this album can not be modified
	 */
	Modifier modify() throws IllegalStateException;

	void moveUp();

	void moveDown();

	void remove() throws IllegalStateException;

	/**
	 * Allows modifying an album. Modifications are only performed once {@link
	 * #update()} has succesfully returned a new album with the modifications
	 * made.
	 *
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	interface Modifier {

		Modifier setTitle(String title);

		Modifier setDescription(String description);

		Modifier setAlbumImage(String imageId);

		Album update() throws IllegalStateException;

	}

}
