/*
 * Sone - MemoryPostDatabase.java - Copyright © 2014 David Roden
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

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.HashMultimap.create;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.DatabaseException;
import net.pterodactylus.sone.database.PostDatabase;
import net.pterodactylus.util.config.Configuration;
import net.pterodactylus.util.config.ConfigurationException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

/**
 * Memory-based {@link PostDatabase} implementation.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class MemoryPostDatabase implements PostDatabase {

	private final MemoryDatabase memoryDatabase;
	private final ReadWriteLock readWriteLock;
	private final Configuration configuration;
	private final Map<String, Post> allPosts = new HashMap<String, Post>();
	private final Multimap<String, Post> sonePosts = create();
	private final SetMultimap<String, String> likedPostsBySone = create();
	private final SetMultimap<String, String> postLikingSones = create();
	private final Multimap<String, Post> recipientPosts = create();
	private final Set<String> knownPosts = new HashSet<String>();

	public MemoryPostDatabase(MemoryDatabase memoryDatabase, ReadWriteLock readWriteLock, Configuration configuration) {
		this.memoryDatabase = memoryDatabase;
		this.readWriteLock = readWriteLock;
		this.configuration = configuration;
	}

	@Override
	public Function<String, Optional<Post>> getPost() {
		return new Function<String, Optional<Post>>() {
			@Override
			public Optional<Post> apply(String postId) {
				return (postId == null) ? Optional.<Post>absent() : getPost(postId);
			}
		};
	}

	@Override
	public Optional<Post> getPost(String postId) {
		readWriteLock.readLock().lock();
		try {
			return fromNullable(allPosts.get(postId));
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public Collection<Post> getPosts(String soneId) {
		readWriteLock.readLock().lock();
		try {
			return new HashSet<Post>(sonePosts.get(soneId));
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public Collection<Post> getDirectedPosts(String recipientId) {
		readWriteLock.readLock().lock();
		try {
			Collection<Post> posts = recipientPosts.get(recipientId);
			return (posts == null) ? Collections.<Post>emptySet() : new HashSet<Post>(posts);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Returns whether the given post is known.
	 *
	 * @param post
	 * 		The post
	 * @return {@code true} if the post is known, {@code false} otherwise
	 */
	@Override
	public boolean isPostKnown(Post post) {
		readWriteLock.readLock().lock();
		try {
			return knownPosts.contains(post.getId());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Sets whether the given post is known.
	 *
	 * @param post
	 * 		The post
	 */
	@Override
	public void setPostKnown(Post post) {
		readWriteLock.writeLock().lock();
		try {
			knownPosts.add(post.getId());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public void likePost(Post post, Sone localSone) {
		readWriteLock.writeLock().lock();
		try {
			likedPostsBySone.put(localSone.getId(), post.getId());
			postLikingSones.put(post.getId(), localSone.getId());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public void unlikePost(Post post, Sone localSone) {
		readWriteLock.writeLock().lock();
		try {
			likedPostsBySone.remove(localSone.getId(), post.getId());
			postLikingSones.remove(post.getId(), localSone.getId());
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public boolean isLiked(Post post, Sone sone) {
		readWriteLock.readLock().lock();
		try {
			return likedPostsBySone.containsEntry(sone.getId(), post.getId());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public Set<Sone> getLikes(Post post) {
		readWriteLock.readLock().lock();
		try {
			return from(postLikingSones.get(post.getId())).transform(memoryDatabase.getSone()).transformAndConcat(MemoryDatabase.<Sone>unwrap()).toSet();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public void storePost(Post post) {
		checkNotNull(post, "post must not be null");
		readWriteLock.writeLock().lock();
		try {
			allPosts.put(post.getId(), post);
			sonePosts.put(post.getSone().getId(), post);
			if (post.getRecipientId().isPresent()) {
				recipientPosts.put(post.getRecipientId().get(), post);
			}
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public void removePost(Post post) {
		checkNotNull(post, "post must not be null");
		readWriteLock.writeLock().lock();
		try {
			allPosts.remove(post.getId());
			sonePosts.remove(post.getSone().getId(), post);
			if (post.getRecipientId().isPresent()) {
				recipientPosts.remove(post.getRecipientId().get(), post);
			}
			post.getSone().removePost(post);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public void storePosts(Sone sone, Collection<Post> posts) throws IllegalArgumentException {
		checkNotNull(sone, "sone must not be null");
		/* verify that all posts are from the same Sone. */
		for (Post post : posts) {
			if (!sone.equals(post.getSone())) {
				throw new IllegalArgumentException(String.format("Post from different Sone found: %s", post));
			}
		}

		readWriteLock.writeLock().lock();
		try {
			/* remove all posts by the Sone. */
			sonePosts.removeAll(sone.getId());
			for (Post post : posts) {
				allPosts.remove(post.getId());
				if (post.getRecipientId().isPresent()) {
					recipientPosts.remove(post.getRecipientId().get(), post);
				}
			}

			/* add new posts. */
			sonePosts.putAll(sone.getId(), posts);
			for (Post post : posts) {
				allPosts.put(post.getId(), post);
				if (post.getRecipientId().isPresent()) {
					recipientPosts.put(post.getRecipientId().get(), post);
				}
			}
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public void removePosts(Sone sone) {
		checkNotNull(sone, "sone must not be null");
		readWriteLock.writeLock().lock();
		try {
			/* remove all posts by the Sone. */
			sonePosts.removeAll(sone.getId());
			for (Post post : sone.getPosts()) {
				allPosts.remove(post.getId());
				if (post.getRecipientId().isPresent()) {
					recipientPosts.remove(post.getRecipientId().get(), post);
				}
			}
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public void start() {
		readWriteLock.writeLock().lock();
		try {
			int postCounter = 0;
			while (true) {
				String knownPostId = configuration.getStringValue("KnownPosts/" + postCounter++ + "/ID").getValue(null);
				if (knownPostId == null) {
					break;
				}
				knownPosts.add(knownPostId);
			}
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	public void stop() throws DatabaseException {
		save();
	}

	public void save() throws DatabaseException {
		readWriteLock.readLock().lock();
		try {
			int postCounter = 0;
			for (String knownPostId : knownPosts) {
				configuration.getStringValue("KnownPosts/" + postCounter++ + "/ID").setValue(knownPostId);
			}
			configuration.getStringValue("KnownPosts/" + postCounter + "/ID").setValue(null);
		} catch (ConfigurationException ce1) {
			throw new DatabaseException("Could not save database.", ce1);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

}
