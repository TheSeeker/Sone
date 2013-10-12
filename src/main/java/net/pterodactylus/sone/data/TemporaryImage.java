/*
 * Sone - TemporaryImage.java - Copyright © 2011–2013 David Roden
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;

/**
 * A temporary image stores an uploaded image in memory until it has been
 * inserted into Freenet and is subsequently loaded from there.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class TemporaryImage {

	/** The ID of the temporary image. */
	private final String id;

	/** The MIME type of the image. */
	private final String mimeType;

	/** The encoded image data. */
	private final byte[] imageData;

	private final int width;

	private final int height;

	/**
	 * Creates a new temporary image with a random ID.
	 * @param imageData
	 * @param mimeType
	 */
	public TemporaryImage(String mimeType, byte[] imageData, int width, int height) {
		this.id = UUID.randomUUID().toString();
		this.mimeType = checkNotNull(mimeType, "mime type must not be null");
		this.imageData = checkNotNull(imageData, "image data must not be null");
		this.width = width;
		this.height = height;
	}

	/**
	 * Returns the ID of the temporary image.
	 *
	 * @return The ID of the temporary image
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the MIME type of the image.
	 *
	 * @return The MIME type of the image
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Returns the encoded image data.
	 *
	 * @return The encoded image data
	 */
	public byte[] getImageData() {
		return imageData;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
