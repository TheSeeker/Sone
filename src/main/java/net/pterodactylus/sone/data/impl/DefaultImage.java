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

import static com.google.common.collect.FluentIterable.from;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;

/**
 * Dumb, store-everything-in-memory implementation of an {@link Image}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultImage extends AbstractImage {

	private final Sone sone;
	private final DefaultAlbum album;

	public DefaultImage(String id, Sone sone, DefaultAlbum album, String key, long creationTime, int width, int height) {
		super(id, key, creationTime, width, height);
		this.sone = sone;
		this.album = album;
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
	public void moveUp() throws IllegalStateException {
		int oldIndex = album.imageIds.indexOf(getId());
		album.imageIds.remove(getId());
		album.imageIds.add(Math.max(0, oldIndex - 1), getId());
	}

	@Override
	public void moveDown() throws IllegalStateException {
		int oldIndex = album.imageIds.indexOf(getId());
		album.imageIds.remove(getId());
		album.imageIds.add(Math.min(album.imageIds.size(), oldIndex + 1), getId());
	}

	@Override
	public void remove() throws IllegalStateException {
		synchronized (album) {
			album.images.remove(getId());
			album.imageIds.remove(getId());
			if (getId().equals(album.albumImage)) {
				album.albumImage = from(album.images.values()).transform(GET_ID).first().orNull();
			}
		}
	}

}
