/*
 * Sone - PostReplyDatabase.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.database;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;

import com.google.common.base.Optional;

/**
 * Database for handling {@link PostReply}s.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public interface PostReplyDatabase {

	/**
	 * Returns the reply with the given ID.
	 *
	 * @param id
	 *            The ID of the reply to get
	 * @return The reply, or {@code null} if there is no such reply
	 */
	Optional<PostReply> getPostReply(String id);

	/**
	 * Returns all replies for the given post, order ascending by time.
	 *
	 * @param postId
	 *            The ID of the post to get all replies for
	 * @return All replies for the given post
	 */
	List<PostReply> getReplies(String postId);

	boolean isPostReplyKnown(PostReply postReply);
	void setPostReplyKnown(PostReply postReply);

	/**
	 * Stores the given post reply.
	 *
	 * @param postReply
	 *            The post reply
	 */
	void storePostReply(PostReply postReply);

	/**
	 * Stores the given post replies as exclusive collection of post replies for
	 * the given Sone. This will remove all other post replies from this Sone!
	 *
	 * @param sone
	 *            The Sone to store all post replies for
	 * @param postReplies
	 *            The post replies of the Sone
	 * @throws IllegalArgumentException
	 *             if one of the replies does not belong to the given Sone
	 */
	void storePostReplies(Sone sone, Collection<PostReply> postReplies) throws IllegalArgumentException;

	/**
	 * Removes the given post reply from this store.
	 *
	 * @param postReply
	 *            The post reply to remove
	 */
	void removePostReply(PostReply postReply);

	/**
	 * Removes all post replies of the given Sone.
	 *
	 * @param sone
	 *            The Sone to remove all post replies for
	 */
	void removePostReplies(Sone sone);

	void likePostReply(PostReply postReply, Sone localSone);
	void unlikePostReply(PostReply postReply, Sone localSone);

	boolean isLiked(PostReply postReply, Sone sone);
	Set<Sone> getLikes(PostReply postReply);

}
