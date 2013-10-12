/*
 * Sone - MemoryImageBuilder.java - Copyright © 2013 David Roden
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

import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.impl.AbstractImageBuilder;
import net.pterodactylus.sone.database.ImageBuilder;

/**
 * {@link ImageBuilder} implementation that creates {@link MemoryImage}s.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class MemoryImageBuilder extends AbstractImageBuilder {

	private final MemoryDatabase memoryDatabase;
	private final Sone sone;
	private final String albumId;

	public MemoryImageBuilder(MemoryDatabase memoryDatabase, Sone sone, String albumId) {
		this.memoryDatabase = memoryDatabase;
		this.sone = sone;
		this.albumId = albumId;
	}

	@Override
	public Image build() throws IllegalStateException {
		validate();
		return new MemoryImage(memoryDatabase, getId(), sone, albumId, key, getCreationTime(), width, height);
	}

}
