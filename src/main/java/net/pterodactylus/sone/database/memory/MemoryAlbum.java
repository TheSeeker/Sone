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

package net.pterodactylus.sone.database.memory;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.impl.AbstractAlbum;
import net.pterodactylus.sone.data.impl.AbstractAlbumBuilder;
import net.pterodactylus.sone.data.impl.AbstractImageBuilder;
import net.pterodactylus.sone.database.AlbumBuilder;
import net.pterodactylus.sone.database.ImageBuilder;

import com.google.common.base.Optional;

/**
 * TODO
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class MemoryAlbum extends AbstractAlbum {

	private final MemoryDatabase memoryDatabase;
	private final Sone sone; /* TODO - only store sone ID. */
	private final String parentId;

	protected MemoryAlbum(MemoryDatabase memoryDatabase, String id, Sone sone, String parentId) {
		super(id);
		this.memoryDatabase = memoryDatabase;
		this.sone = sone;
		this.parentId = parentId;
	}

	@Override
	public Sone getSone() {
		return sone;
	}

	@Override
	public List<Album> getAlbums() {
		return memoryDatabase.getAlbums(this);
	}

	@Override
	public List<Image> getImages() {
		return memoryDatabase.getImages(this);
	}

	@Override
	public Optional<Image> getAlbumImage() {
		return memoryDatabase.getImage(albumImage);
	}

	@Override
	public Album getParent() {
		return memoryDatabase.getAlbum(parentId).get();
	}

	@Override
	public AlbumBuilder newAlbumBuilder() throws IllegalStateException {
		return new AbstractAlbumBuilder() {
			@Override
			public Album build() throws IllegalStateException {
				validate();
				MemoryAlbum memoryAlbum = new MemoryAlbum(memoryDatabase, getId(), sone, MemoryAlbum.this.id);
				memoryDatabase.storeAlbum(memoryAlbum);
				return memoryAlbum;
			}
		};
	}

	@Override
	public ImageBuilder newImageBuilder() throws IllegalStateException {
		return new AbstractImageBuilder() {
			@Override
			public Image build() throws IllegalStateException {
				validate();
				MemoryImage memoryImage = new MemoryImage(memoryDatabase, getId(), sone, MemoryAlbum.this.id, key, getCreationTime(), width, height);
				memoryDatabase.storeImage(memoryImage);
				return memoryImage;
			}
		};
	}

	@Override
	public void moveUp() {
		memoryDatabase.moveUp(this);
	}

	@Override
	public void moveDown() {
		memoryDatabase.moveDown(this);
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
		memoryDatabase.removeAlbum(this);
	}

}
