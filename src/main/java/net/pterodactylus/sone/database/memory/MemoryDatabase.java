/*
 * Sone - MemoryDatabase.java - Copyright © 2013 David Roden
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
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Collections.emptyList;
import static net.pterodactylus.sone.data.Sone.LOCAL_SONE_FILTER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.impl.DefaultSoneBuilder;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.DatabaseException;
import net.pterodactylus.sone.database.PostDatabase;
import net.pterodactylus.sone.database.SoneBuilder;
import net.pterodactylus.sone.freenet.wot.Identity;
import net.pterodactylus.util.config.Configuration;
import net.pterodactylus.util.config.ConfigurationException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;

/**
 * Memory-based {@link PostDatabase} implementation.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class MemoryDatabase extends AbstractService implements Database {

	/** The lock. */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/** The configuration. */
	private final Configuration configuration;

	private final Map<String, Identity> identities = Maps.newHashMap();
	private final Map<String, Sone> sones = new HashMap<String, Sone>();

	/** All posts by their ID. */
	private final Map<String, Post> allPosts = new HashMap<String, Post>();

	/** All posts by their Sones. */
	private final Multimap<String, Post> sonePosts = HashMultimap.create();
	private final SetMultimap<String, String> likedPostsBySone = HashMultimap.create();
	private final SetMultimap<String, String> postLikingSones = HashMultimap.create();

	/** All posts by their recipient. */
	private final Multimap<String, Post> recipientPosts = HashMultimap.create();

	/** Whether posts are known. */
	private final Set<String> knownPosts = new HashSet<String>();

	/** All post replies by their ID. */
	private final Map<String, PostReply> allPostReplies = new HashMap<String, PostReply>();
	private final SetMultimap<String, String> likedPostRepliesBySone = HashMultimap.create();
	private final SetMultimap<String, String> postReplyLikingSones = HashMultimap.create();

	/** Replies sorted by Sone. */
	private final SortedSetMultimap<String, PostReply> sonePostReplies = TreeMultimap.create(new Comparator<String>() {

		@Override
		public int compare(String leftString, String rightString) {
			return leftString.compareTo(rightString);
		}
	}, PostReply.TIME_COMPARATOR);

	/** Replies by post. */
	private final SortedSetMultimap<String, PostReply> postReplies = TreeMultimap.create(new Comparator<String>() {

		@Override
		public int compare(String leftString, String rightString) {
			return leftString.compareTo(rightString);
		}
	}, PostReply.TIME_COMPARATOR);

	/** Whether post replies are known. */
	private final Set<String> knownPostReplies = new HashSet<String>();

	private final Map<String, Album> allAlbums = new HashMap<String, Album>();
	private final ListMultimap<String, String> albumChildren = ArrayListMultimap.create();
	private final ListMultimap<String, String> albumImages = ArrayListMultimap.create();

	private final Map<String, Image> allImages = new HashMap<String, Image>();

	/**
	 * Creates a new memory database.
	 *
	 * @param configuration
	 * 		The configuration for loading and saving elements
	 */
	@Inject
	public MemoryDatabase(Configuration configuration) {
		this.configuration = configuration;
	}

	//
	// DATABASE METHODS
	//

	@Override
	public void save() throws DatabaseException {
		saveKnownPosts();
		saveKnownPostReplies();
	}

	//
	// SERVICE METHODS
	//

	@Override
	protected void doStart() {
		loadKnownPosts();
		loadKnownPostReplies();
		notifyStarted();
	}

	@Override
	protected void doStop() {
		try {
			save();
			notifyStopped();
		} catch (DatabaseException de1) {
			notifyFailed(de1);
		}
	}

	@Override
	public Optional<Identity> getIdentity(String identityId) {
		lock.readLock().lock();
		try {
			return fromNullable(identities.get(identityId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void storeIdentity(Identity identitiy) {
		lock.writeLock().lock();
		try {
			identities.put(identitiy.getId(), identitiy);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Function<String, Optional<Sone>> getSone() {
		return new Function<String, Optional<Sone>>() {
			@Override
			public Optional<Sone> apply(String soneId) {
				return (soneId == null) ? Optional.<Sone>absent() : getSone(soneId);
			}
		};
	}

	@Override
	public Optional<Sone> getSone(String soneId) {
		lock.readLock().lock();
		try {
			return fromNullable(sones.get(soneId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Collection<Sone> getSones() {
		lock.readLock().lock();
		try {
			return Collections.unmodifiableCollection(sones.values());
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Collection<Sone> getLocalSones() {
		lock.readLock().lock();
		try {
			return from(getSones()).filter(LOCAL_SONE_FILTER).toSet();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Collection<Sone> getRemoteSones() {
		lock.readLock().lock();
		try {
			return from(getSones()).filter(not(LOCAL_SONE_FILTER)).toSet();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public SoneBuilder newSoneBuilder() {
		return new DefaultSoneBuilder(this) {
			@Override
			public Sone build(Optional<SoneCreated> soneCreated) throws IllegalStateException {
				Sone sone = super.build(soneCreated);
				lock.writeLock().lock();
				try {
					sones.put(sone.getId(), sone);
				} finally {
					lock.writeLock().unlock();
				}
				return sone;
			}
		};
	}

	//
	// POSTPROVIDER METHODS
	//

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
		lock.readLock().lock();
		try {
			return fromNullable(allPosts.get(postId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Collection<Post> getPosts(String soneId) {
		lock.readLock().lock();
		try {
			return new HashSet<Post>(sonePosts.get(soneId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Collection<Post> getDirectedPosts(String recipientId) {
		lock.readLock().lock();
		try {
			Collection<Post> posts = recipientPosts.get(recipientId);
			return (posts == null) ? Collections.<Post>emptySet() : new HashSet<Post>(posts);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void likePost(Post post, Sone localSone) {
		lock.writeLock().lock();
		try {
			likedPostsBySone.put(localSone.getId(), post.getId());
			postLikingSones.put(post.getId(), localSone.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void unlikePost(Post post, Sone localSone) {
		lock.writeLock().lock();
		try {
			likedPostsBySone.remove(localSone.getId(), post.getId());
			postLikingSones.remove(post.getId(), localSone.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	public boolean isLiked(Post post, Sone sone) {
		lock.readLock().lock();
		try {
			return likedPostsBySone.containsEntry(sone.getId(), post.getId());
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<Sone> getLikes(Post post) {
		lock.readLock().lock();
		try {
			return from(postLikingSones.get(post.getId())).transform(getSone()).transformAndConcat(this.<Sone>unwrap()).toSet();
		} finally {
			lock.readLock().unlock();
		}
	}

	//
	// POSTSTORE METHODS
	//

	@Override
	public void storePost(Post post) {
		checkNotNull(post, "post must not be null");
		lock.writeLock().lock();
		try {
			allPosts.put(post.getId(), post);
			sonePosts.put(post.getSone().getId(), post);
			if (post.getRecipientId().isPresent()) {
				recipientPosts.put(post.getRecipientId().get(), post);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removePost(Post post) {
		checkNotNull(post, "post must not be null");
		lock.writeLock().lock();
		try {
			allPosts.remove(post.getId());
			sonePosts.remove(post.getSone().getId(), post);
			if (post.getRecipientId().isPresent()) {
				recipientPosts.remove(post.getRecipientId().get(), post);
			}
			post.getSone().removePost(post);
		} finally {
			lock.writeLock().unlock();
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

		lock.writeLock().lock();
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
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removePosts(Sone sone) {
		checkNotNull(sone, "sone must not be null");
		lock.writeLock().lock();
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
			lock.writeLock().unlock();
		}
	}

	//
	// POSTREPLYPROVIDER METHODS
	//

	@Override
	public Optional<PostReply> getPostReply(String id) {
		lock.readLock().lock();
		try {
			return fromNullable(allPostReplies.get(id));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<PostReply> getReplies(String postId) {
		lock.readLock().lock();
		try {
			if (!postReplies.containsKey(postId)) {
				return emptyList();
			}
			return new ArrayList<PostReply>(postReplies.get(postId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void likePostReply(PostReply postReply, Sone localSone) {
		lock.writeLock().lock();
		try {
			likedPostRepliesBySone.put(localSone.getId(), postReply.getId());
			postReplyLikingSones.put(postReply.getId(), localSone.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void unlikePostReply(PostReply postReply, Sone localSone) {
		lock.writeLock().lock();
		try {
			likedPostRepliesBySone.remove(localSone.getId(), postReply.getId());
			postReplyLikingSones.remove(postReply.getId(), localSone.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean isLiked(PostReply postReply, Sone sone) {
		lock.readLock().lock();
		try {
			return postReplyLikingSones.containsEntry(postReply.getId(), sone.getId());
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Set<Sone> getLikes(PostReply postReply) {
		lock.readLock().lock();
		try {
			return from(postReplyLikingSones.get(postReply.getId())).transform(getSone()).transformAndConcat(this.<Sone>unwrap()).toSet();
		} finally {
			lock.readLock().unlock();
		}
	}

	//
	// POSTREPLYSTORE METHODS
	//

	/**
	 * Returns whether the given post reply is known.
	 *
	 * @param postReply
	 * 		The post reply
	 * @return {@code true} if the given post reply is known, {@code false}
	 *         otherwise
	 */
	public boolean isPostReplyKnown(PostReply postReply) {
		lock.readLock().lock();
		try {
			return knownPostReplies.contains(postReply.getId());
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void setPostReplyKnown(PostReply postReply) {
		lock.writeLock().lock();
		try {
			knownPostReplies.add(postReply.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void storePostReply(PostReply postReply) {
		lock.writeLock().lock();
		try {
			allPostReplies.put(postReply.getId(), postReply);
			postReplies.put(postReply.getPostId(), postReply);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void storePostReplies(Sone sone, Collection<PostReply> postReplies) {
		checkNotNull(sone, "sone must not be null");
		/* verify that all posts are from the same Sone. */
		for (PostReply postReply : postReplies) {
			if (!sone.equals(postReply.getSone())) {
				throw new IllegalArgumentException(String.format("PostReply from different Sone found: %s", postReply));
			}
		}

		lock.writeLock().lock();
		try {
			/* remove all post replies of the Sone. */
			for (PostReply postReply : getRepliesFrom(sone.getId())) {
				removePostReply(postReply);
			}
			for (PostReply postReply : postReplies) {
				allPostReplies.put(postReply.getId(), postReply);
				sonePostReplies.put(postReply.getSone().getId(), postReply);
				this.postReplies.put(postReply.getPostId(), postReply);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removePostReply(PostReply postReply) {
		lock.writeLock().lock();
		try {
			allPostReplies.remove(postReply.getId());
			postReplies.remove(postReply.getPostId(), postReply);
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removePostReplies(Sone sone) {
		checkNotNull(sone, "sone must not be null");

		lock.writeLock().lock();
		try {
			for (PostReply postReply : sone.getReplies()) {
				removePostReply(postReply);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	//
	// ALBUMPROVDER METHODS
	//

	@Override
	public Optional<Album> getAlbum(String albumId) {
		lock.readLock().lock();
		try {
			return fromNullable(allAlbums.get(albumId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<Album> getAlbums(Album parent) {
		lock.readLock().lock();
		try {
			return from(albumChildren.get(parent.getId())).transformAndConcat(getAlbum()).toList();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void moveUp(Album album) {
		lock.writeLock().lock();
		try {
			List<String> albums = albumChildren.get(album.getParent().getId());
			int currentIndex = albums.indexOf(album.getId());
			if (currentIndex == 0) {
				return;
			}
			albums.remove(album.getId());
			albums.add(currentIndex - 1, album.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void moveDown(Album album) {
		lock.writeLock().lock();
		try {
			List<String> albums = albumChildren.get(album.getParent().getId());
			int currentIndex = albums.indexOf(album.getId());
			if (currentIndex == (albums.size() - 1)) {
				return;
			}
			albums.remove(album.getId());
			albums.add(currentIndex + 1, album.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	//
	// ALBUMSTORE METHODS
	//

	@Override
	public void storeAlbum(Album album) {
		lock.writeLock().lock();
		try {
			allAlbums.put(album.getId(), album);
			if (!album.isRoot()) {
				albumChildren.put(album.getParent().getId(), album.getId());
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removeAlbum(Album album) {
		lock.writeLock().lock();
		try {
			allAlbums.remove(album.getId());
			albumChildren.remove(album.getParent().getId(), album.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	//
	// IMAGEPROVIDER METHODS
	//

	@Override
	public Optional<Image> getImage(String imageId) {
		lock.readLock().lock();
		try {
			return fromNullable(allImages.get(imageId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public List<Image> getImages(Album parent) {
		lock.readLock().lock();
		try {
			return from(albumImages.get(parent.getId())).transformAndConcat(getImage()).toList();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void moveUp(Image image) {
		lock.writeLock().lock();
		try {
			List<String> images = albumImages.get(image.getAlbum().getId());
			int currentIndex = images.indexOf(image.getId());
			if (currentIndex == 0) {
				return;
			}
			images.remove(image.getId());
			images.add(currentIndex - 1, image.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void moveDown(Image image) {
		lock.writeLock().lock();
		try {
			List<String> images = albumChildren.get(image.getAlbum().getId());
			int currentIndex = images.indexOf(image.getId());
			if (currentIndex == (images.size() - 1)) {
				return;
			}
			images.remove(image.getId());
			images.add(currentIndex + 1, image.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	//
	// IMAGESTORE METHODS
	//

	@Override
	public void storeImage(Image image) {
		lock.writeLock().lock();
		try {
			allImages.put(image.getId(), image);
			albumImages.put(image.getAlbum().getId(), image.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void removeImage(Image image) {
		lock.writeLock().lock();
		try {
			allImages.remove(image.getId());
			albumImages.remove(image.getAlbum().getId(), image.getId());
		} finally {
			lock.writeLock().unlock();
		}
	}

	//
	// PACKAGE-PRIVATE METHODS
	//

	/**
	 * Returns whether the given post is known.
	 *
	 * @param post
	 * 		The post
	 * @return {@code true} if the post is known, {@code false} otherwise
	 */
	boolean isPostKnown(Post post) {
		lock.readLock().lock();
		try {
			return knownPosts.contains(post.getId());
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Sets whether the given post is known.
	 *
	 * @param post
	 * 		The post
	 * @param known
	 * 		{@code true} if the post is known, {@code false} otherwise
	 */
	void setPostKnown(Post post, boolean known) {
		lock.writeLock().lock();
		try {
			if (known) {
				knownPosts.add(post.getId());
			} else {
				knownPosts.remove(post.getId());
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	//
	// PRIVATE METHODS
	//

	/** Loads the known posts. */
	private void loadKnownPosts() {
		lock.writeLock().lock();
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
			lock.writeLock().unlock();
		}
	}

	/**
	 * Saves the known posts to the configuration.
	 *
	 * @throws DatabaseException
	 * 		if a configuration error occurs
	 */
	private void saveKnownPosts() throws DatabaseException {
		lock.readLock().lock();
		try {
			int postCounter = 0;
			for (String knownPostId : knownPosts) {
				configuration.getStringValue("KnownPosts/" + postCounter++ + "/ID").setValue(knownPostId);
			}
			configuration.getStringValue("KnownPosts/" + postCounter + "/ID").setValue(null);
		} catch (ConfigurationException ce1) {
			throw new DatabaseException("Could not save database.", ce1);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Returns all replies by the given Sone.
	 *
	 * @param id
	 * 		The ID of the Sone
	 * @return The post replies of the Sone, sorted by time (newest first)
	 */
	private Collection<PostReply> getRepliesFrom(String id) {
		lock.readLock().lock();
		try {
			if (sonePostReplies.containsKey(id)) {
				return Collections.unmodifiableCollection(sonePostReplies.get(id));
			}
			return Collections.emptySet();
		} finally {
			lock.readLock().unlock();
		}
	}

	/** Loads the known post replies. */
	private void loadKnownPostReplies() {
		lock.writeLock().lock();
		try {
			int replyCounter = 0;
			while (true) {
				String knownReplyId = configuration.getStringValue("KnownReplies/" + replyCounter++ + "/ID").getValue(null);
				if (knownReplyId == null) {
					break;
				}
				knownPostReplies.add(knownReplyId);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Saves the known post replies to the configuration.
	 *
	 * @throws DatabaseException
	 * 		if a configuration error occurs
	 */
	private void saveKnownPostReplies() throws DatabaseException {
		lock.readLock().lock();
		try {
			int replyCounter = 0;
			for (String knownReplyId : knownPostReplies) {
				configuration.getStringValue("KnownReplies/" + replyCounter++ + "/ID").setValue(knownReplyId);
			}
			configuration.getStringValue("KnownReplies/" + replyCounter + "/ID").setValue(null);
		} catch (ConfigurationException ce1) {
			throw new DatabaseException("Could not save database.", ce1);
		} finally {
			lock.readLock().unlock();
		}
	}

	private Function<String, Iterable<Album>> getAlbum() {
		return new Function<String, Iterable<Album>>() {
			@Override
			public Iterable<Album> apply(String input) {
				return (input == null) ? Collections.<Album>emptyList() : getAlbum(input).asSet();
			}
		};
	}

	private Function<String, Iterable<Image>> getImage() {
		return new Function<String, Iterable<Image>>() {
			@Override
			public Iterable<Image> apply(String input) {
				return (input == null) ? Collections.<Image>emptyList() : getImage(input).asSet();
			}
		};
	}

	private static <T> Function<Optional<T>, Iterable<T>> unwrap() {
		return new Function<Optional<T>, Iterable<T>>() {
			@Override
			public Iterable<T> apply(Optional<T> input) {
				return (input == null) ? Collections.<T>emptyList() : input.asSet();
			}
		};
	}

}
