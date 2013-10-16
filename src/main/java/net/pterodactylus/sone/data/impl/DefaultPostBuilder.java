/*
 * Sone - PostBuilderImpl.java - Copyright © 2013 David Roden
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

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostBuilder;

import com.google.common.base.Optional;

/**
 * {@link PostBuilder} implementation that creates {@link DefaultPost} objects.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultPostBuilder extends AbstractPostBuilder {

	/**
	 * Creates a new post builder.
	 *
	 * @param database
	 */
	public DefaultPostBuilder(Database database, String soneId) {
		super(database, soneId);
	}

	@Override
	public Post build(Optional<PostCreated> postCreated) {
		validate();
		DefaultPost post = new DefaultPost(database, getId(), senderId, recipientId.orNull(), getTime(), text);
		if (postCreated.isPresent()) {
			postCreated.get().postCreated(post);
		}
		return post;
	}

}
