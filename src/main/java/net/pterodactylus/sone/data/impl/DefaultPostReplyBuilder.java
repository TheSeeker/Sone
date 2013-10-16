/*
 * Sone - PostReplyBuilderImpl.java - Copyright © 2013 David Roden
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

import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostReplyBuilder;

import com.google.common.base.Optional;

/**
 * {@link PostReplyBuilder} implementation that creates {@link DefaultPostReply}
 * objects.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultPostReplyBuilder extends AbstractPostReplyBuilder {

	private final Database database;

	public DefaultPostReplyBuilder(Database database, String senderId, String postId) {
		super(senderId, postId);
		this.database = database;
	}

	@Override
	public PostReply build(Optional<PostReplyCreated> postReplyCreated) {
		validate();

		DefaultPostReply postReply = new DefaultPostReply(database, getId(), senderId, getTime(), text, postId);
		if (postReplyCreated.isPresent()) {
			postReplyCreated.get().postReplyCreated(postReply);
		}
		return postReply;
	}

}
