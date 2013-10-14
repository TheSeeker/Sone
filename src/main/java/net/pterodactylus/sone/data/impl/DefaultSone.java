/*
 * Sone - SoneImpl.java - Copyright © 2010–2013 David Roden
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.AlbumBuilder;

/**
 * Dumb, store-everything-in-memory {@link Sone} implementation.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultSone extends AbstractSone {

	/** All posts. */
	private final Set<Post> posts = new CopyOnWriteArraySet<Post>();

	/** All replies. */
	private final Set<PostReply> replies = new CopyOnWriteArraySet<PostReply>();

	/** The IDs of all liked posts. */
	private final Set<String> likedPostIds = new CopyOnWriteArraySet<String>();

	/** The IDs of all liked replies. */
	private final Set<String> likedReplyIds = new CopyOnWriteArraySet<String>();

	/**
	 * Creates a new Sone.
	 *
	 * @param id
	 * 		The ID of the Sone
	 * @param local
	 * 		{@code true} if the Sone is a local Sone, {@code false} otherwise
	 */
	public DefaultSone(String id, boolean local) {
		super(id, local);
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns whether this Sone is known.
	 *
	 * @return {@code true} if this Sone is known, {@code false} otherwise
	 */
	public boolean isKnown() {
		return known;
	}

	/**
	 * Sets whether this Sone is known.
	 *
	 * @param known
	 * 		{@code true} if this Sone is known, {@code false} otherwise
	 * @return This Sone
	 */
	public Sone setKnown(boolean known) {
		this.known = known;
		return this;
	}

	/**
	 * Returns all friend Sones of this Sone.
	 *
	 * @return The friend Sones of this Sone
	 */
	public List<String> getFriends() {
		List<String> friends = new ArrayList<String>(friendSones);
		return friends;
	}

	/**
	 * Returns whether this Sone has the given Sone as a friend Sone.
	 *
	 * @param friendSoneId
	 * 		The ID of the Sone to check for
	 * @return {@code true} if this Sone has the given Sone as a friend, {@code
	 *         false} otherwise
	 */
	public boolean hasFriend(String friendSoneId) {
		return friendSones.contains(friendSoneId);
	}

	/**
	 * Adds the given Sone as a friend Sone.
	 *
	 * @param friendSone
	 * 		The friend Sone to add
	 * @return This Sone (for method chaining)
	 */
	public Sone addFriend(String friendSone) {
		if (!friendSone.equals(id)) {
			friendSones.add(friendSone);
		}
		return this;
	}

	/**
	 * Removes the given Sone as a friend Sone.
	 *
	 * @param friendSoneId
	 * 		The ID of the friend Sone to remove
	 * @return This Sone (for method chaining)
	 */
	public Sone removeFriend(String friendSoneId) {
		friendSones.remove(friendSoneId);
		return this;
	}

	/**
	 * Returns the list of posts of this Sone, sorted by time, newest first.
	 *
	 * @return All posts of this Sone
	 */
	public List<Post> getPosts() {
		List<Post> sortedPosts;
		synchronized (this) {
			sortedPosts = new ArrayList<Post>(posts);
		}
		Collections.sort(sortedPosts, Post.TIME_COMPARATOR);
		return sortedPosts;
	}

	/**
	 * Sets all posts of this Sone at once.
	 *
	 * @param posts
	 * 		The new (and only) posts of this Sone
	 * @return This Sone (for method chaining)
	 */
	public Sone setPosts(Collection<Post> posts) {
		synchronized (this) {
			this.posts.clear();
			this.posts.addAll(posts);
		}
		return this;
	}

	/**
	 * Adds the given post to this Sone. The post will not be added if its {@link
	 * Post#getSone() Sone} is not this Sone.
	 *
	 * @param post
	 * 		The post to add
	 */
	public void addPost(Post post) {
		if (post.getSone().equals(this) && posts.add(post)) {
			logger.log(Level.FINEST, String.format("Adding %s to “%s”.", post, getName()));
		}
	}

	/**
	 * Removes the given post from this Sone.
	 *
	 * @param post
	 * 		The post to remove
	 */
	public void removePost(Post post) {
		if (post.getSone().equals(this)) {
			posts.remove(post);
		}
	}

	/**
	 * Returns all replies this Sone made.
	 *
	 * @return All replies this Sone made
	 */
	public Set<PostReply> getReplies() {
		return Collections.unmodifiableSet(replies);
	}

	/**
	 * Sets all replies of this Sone at once.
	 *
	 * @param replies
	 * 		The new (and only) replies of this Sone
	 * @return This Sone (for method chaining)
	 */
	public Sone setReplies(Collection<PostReply> replies) {
		this.replies.clear();
		this.replies.addAll(replies);
		return this;
	}

	/**
	 * Adds a reply to this Sone. If the given reply was not made by this Sone,
	 * nothing is added to this Sone.
	 *
	 * @param reply
	 * 		The reply to add
	 */
	public void addReply(PostReply reply) {
		if (reply.getSone().equals(this)) {
			replies.add(reply);
		}
	}

	/**
	 * Removes a reply from this Sone.
	 *
	 * @param reply
	 * 		The reply to remove
	 */
	public void removeReply(PostReply reply) {
		if (reply.getSone().equals(this)) {
			replies.remove(reply);
		}
	}

	/**
	 * Returns the IDs of all liked posts.
	 *
	 * @return All liked posts’ IDs
	 */
	public Set<String> getLikedPostIds() {
		return Collections.unmodifiableSet(likedPostIds);
	}

	/**
	 * Sets the IDs of all liked posts.
	 *
	 * @param likedPostIds
	 * 		All liked posts’ IDs
	 * @return This Sone (for method chaining)
	 */
	public Sone setLikePostIds(Set<String> likedPostIds) {
		this.likedPostIds.clear();
		this.likedPostIds.addAll(likedPostIds);
		return this;
	}

	public boolean isLikedPostId(String postId) {
		return likedPostIds.contains(postId);
	}

	public Sone addLikedPostId(String postId) {
		likedPostIds.add(postId);
		return this;
	}

	public Sone removeLikedPostId(String postId) {
		likedPostIds.remove(postId);
		return this;
	}

	public Set<String> getLikedReplyIds() {
		return Collections.unmodifiableSet(likedReplyIds);
	}

	public Sone setLikeReplyIds(Set<String> likedReplyIds) {
		this.likedReplyIds.clear();
		this.likedReplyIds.addAll(likedReplyIds);
		return this;
	}

	public boolean isLikedReplyId(String replyId) {
		return likedReplyIds.contains(replyId);
	}

	public Sone addLikedReplyId(String replyId) {
		likedReplyIds.add(replyId);
		return this;
	}

	public Sone removeLikedReplyId(String replyId) {
		likedReplyIds.remove(replyId);
		return this;
	}

	@Override
	public AlbumBuilder newAlbumBuilder() {
		return new DefaultAlbumBuilder(this, rootAlbum);
	}

}
