/*
 * Sone - PostDatabase.java - Copyright © 2013 David Roden
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

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Database for handling {@link Post}s.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public interface PostDatabase {

	Function<String, Optional<Post>> getPost();

	/**
	 * Returns the post with the given ID.
	 *
	 * @param postId
	 *            The ID of the post to return
	 * @return The post with the given ID, or {@code null}
	 */
	Optional<Post> getPost(String postId);

	/**
	 * Returns all posts from the given Sone.
	 *
	 * @param soneId
	 *            The ID of the Sone
	 * @return All posts from the given Sone
	 */
	Collection<Post> getPosts(String soneId);

	/**
	 * Returns all posts that have the given Sone as recipient.
	 *
	 * @see Post#getRecipient()
	 * @param recipientId
	 *            The ID of the recipient of the posts
	 * @return All posts that have the given Sone as recipient
	 */
	Collection<Post> getDirectedPosts(String recipientId);

	/**
	 * Adds the given post to the store.
	 *
	 * @param post
	 *            The post to store
	 */
	void storePost(Post post);

	/**
	 * Removes the given post.
	 *
	 * @param post
	 *            The post to remove
	 */
	void removePost(Post post);

	/**
	 * Stores the given posts as all posts of a single {@link Sone}. This method
	 * will removed all other posts from the Sone!
	 *
	 * @param sone
	 *            The Sone to store the posts for
	 * @param posts
	 *            The posts to store
	 * @throws IllegalArgumentException
	 *             if posts do not all belong to the same Sone
	 */
	void storePosts(Sone sone, Collection<Post> posts) throws IllegalArgumentException;

	/**
	 * Removes all posts of the given {@link Sone}
	 *
	 * @param sone
	 *            The Sone to remove all posts for
	 */
	void removePosts(Sone sone);

	void likePost(Post post, Sone localSone);
	void unlikePost(Post post, Sone localSone);

}
