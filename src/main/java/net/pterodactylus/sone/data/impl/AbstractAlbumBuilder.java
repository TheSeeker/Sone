/*
 * Sone - AbstractAlbumBuilder.java - Copyright © 2013 David Roden
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
import static java.util.UUID.randomUUID;

import net.pterodactylus.sone.database.AlbumBuilder;

import com.google.common.base.Optional;

/**
 * Abstract {@link AlbumBuilder} implementation. It stores the state of the new
 * album and performs validation, you only need to implement {@link #build()}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public abstract class AbstractAlbumBuilder implements AlbumBuilder {

	private Optional<String> id = absent();

	@Override
	public AlbumBuilder withId(String id) {
		this.id = fromNullable(id);
		return this;
	}

	//
	// PROTECTED METHODS
	//

	protected String getId() {
		return id.isPresent() ? id.get() : randomUUID().toString();
	}

	/**
	 * Validates the state of this post builder.
	 *
	 * @throws IllegalStateException
	 * 		if the state is not valid for building a new post
	 */
	protected void validate() throws IllegalStateException {
	}

}
