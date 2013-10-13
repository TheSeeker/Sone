/*
 * Sone - MemoryImage.java - Copyright © 2013 David Roden
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

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.impl.AbstractImage;

/**
 * In-memory implementation of an {@link Image}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class MemoryImage extends AbstractImage {

	private final MemoryDatabase memoryDatabase;
	private final Sone sone; /* TODO - store sone ID only. */
	private final String albumId;

	public MemoryImage(MemoryDatabase memoryDatabase, String id, Sone sone, String albumId, String key, long creationTime, int width, int height) {
		super(id, key, creationTime, width, height);
		this.memoryDatabase = memoryDatabase;
		this.sone = sone;
		this.albumId = albumId;
	}

	@Override
	public Sone getSone() {
		return sone;
	}

	@Override
	public Album getAlbum() {
		return memoryDatabase.getAlbum(albumId).get();
	}

	@Override
	public void moveUp() throws IllegalStateException {
		memoryDatabase.moveUp(this);
	}

	@Override
	public void moveDown() throws IllegalStateException {
		memoryDatabase.moveDown(this);
	}

	@Override
	public void remove() throws IllegalStateException {
		memoryDatabase.removeImage(this);
	}

}
