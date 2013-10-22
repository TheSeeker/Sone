/*
 * Sone - MemoryAlbum.java - Copyright © 2013 David Roden
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

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.AlbumBuilder;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.ImageBuilder;

import com.google.common.base.Optional;

/**
 * TODO
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultAlbum extends AbstractAlbum {

	private final Database database;
	private final Sone sone; /* TODO - only store sone ID. */

	protected DefaultAlbum(Database database, String id, Sone sone, String parentId) {
		super(id, parentId);
		this.database = database;
		this.sone = sone;
	}

	@Override
	public Sone getSone() {
		return sone;
	}

	@Override
	public List<Album> getAlbums() {
		return database.getAlbums(this);
	}

	@Override
	public List<Image> getImages() {
		return database.getImages(this);
	}

	@Override
	public Optional<Image> getAlbumImage() {
		return database.getImage(albumImage);
	}

	@Override
	public Album getParent() {
		return database.getAlbum(parentId).get();
	}

	@Override
	public AlbumBuilder newAlbumBuilder() throws IllegalStateException {
		return new AbstractAlbumBuilder() {
			@Override
			public Album build() throws IllegalStateException {
				validate();
				DefaultAlbum memoryAlbum = new DefaultAlbum(database, getId(), sone, DefaultAlbum.this.id);
				database.storeAlbum(memoryAlbum);
				return memoryAlbum;
			}
		};
	}

	@Override
	public ImageBuilder newImageBuilder() throws IllegalStateException {
		return new AbstractImageBuilder() {
			@Override
			public Image build(Optional<ImageCreated> imageCreated) throws IllegalStateException {
				validate();
				DefaultImage image = new DefaultImage(database, getId(), sone, DefaultAlbum.this.id, key, getCreationTime(), width, height);
				database.storeImage(image);
				if (imageCreated.isPresent()) {
					imageCreated.get().imageCreated(image);
				}
				return image;
			}
		};
	}

	@Override
	public void moveUp() {
		database.moveUp(this);
	}

	@Override
	public void moveDown() {
		database.moveDown(this);
	}

	@Override
	public void remove() throws IllegalStateException {
		checkState(!isRoot(), "can not remove root album");
		for (Album album : getAlbums()) {
			album.remove();
		}
		for (Image image : getImages()) {
			image.remove();
		}
		database.removeAlbum(this);
	}

}
