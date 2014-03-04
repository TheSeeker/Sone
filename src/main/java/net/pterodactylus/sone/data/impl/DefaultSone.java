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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pterodactylus.sone.core.Options;
import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Client;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Profile;
import net.pterodactylus.sone.data.Reply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.AlbumBuilder;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostBuilder;
import net.pterodactylus.sone.database.PostReplyBuilder;
import net.pterodactylus.sone.freenet.wot.Identity;
import net.pterodactylus.util.logging.Logging;

import freenet.keys.FreenetURI;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Dumb, store-everything-in-memory {@link Sone} implementation.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultSone implements Sone {

	/** The logger. */
	private static final Logger logger = Logging.getLogger(DefaultSone.class);

	/** The database. */
	private final Database database;

	/** The ID of this Sone. */
	private final String id;

	/** Whether the Sone is local. */
	private final boolean local;

	/** The latest edition of the Sone. */
	private volatile long latestEdition;

	/** The time of the last inserted update. */
	private volatile long time;

	/** The status of this Sone. */
	private volatile SoneStatus status = SoneStatus.unknown;

	/** The profile of this Sone. */
	private volatile Profile profile = new Profile(this);

	/** The client used by the Sone. */
	private final Client client;

	/** Whether this Sone is known. */
	private volatile boolean known;

	/** All friend Sones. */
	private final Set<String> friendSones = new CopyOnWriteArraySet<String>();

	/** All posts. */
	private final Set<Post> posts = new CopyOnWriteArraySet<Post>();

	/** All replies. */
	private final Set<PostReply> replies = new CopyOnWriteArraySet<PostReply>();

	/** The IDs of all liked posts. */
	private final Set<String> likedPostIds = new CopyOnWriteArraySet<String>();

	/** The IDs of all liked replies. */
	private final Set<String> likedReplyIds = new CopyOnWriteArraySet<String>();

	/** The root album containing all albums. */
	private final Album rootAlbum;

	/** Sone-specific options. */
	private Options options = new Options();

	/**
	 * Creates a new Sone.
	 *
	 * @param id
	 * 		The ID of the Sone
	 * @param local
	 * 		{@code true} if the Sone is a local Sone, {@code false} otherwise
	 */
	public DefaultSone(Database database, String id, boolean local, Client client) {
		this.database = database;
		this.id = id;
		this.local = local;
		this.client = client;
		rootAlbum = new DefaultAlbumBuilder(database, this, null).build();
	}

	//
	// ACCESSORS
	//

	public String getId() {
		return id;
	}

	public Identity getIdentity() {
		return database.getIdentity(id).get();
	}

	public String getName() {
		return getIdentity().getNickname();
	}

	public boolean isLocal() {
		return local;
	}

	public long getLatestEdition() {
		return latestEdition;
	}

	public void setLatestEdition(long latestEdition) {
		if (!(latestEdition > this.latestEdition)) {
			logger.log(Level.FINE, String.format("New latest edition %d is not greater than current latest edition %d!", latestEdition, this.latestEdition));
			return;
		}
		this.latestEdition = latestEdition;
	}

	public long getTime() {
		return time;
	}

	public Sone setTime(long time) {
		this.time = time;
		return this;
	}

	public SoneStatus getStatus() {
		return status;
	}

	public Sone setStatus(SoneStatus status) {
		this.status = checkNotNull(status, "status must not be null");
		return this;
	}

	public Profile getProfile() {
		return new Profile(profile);
	}

	public void setProfile(Profile profile) {
		this.profile = new Profile(profile);
	}

	public Client getClient() {
		return client;
	}

	public boolean isKnown() {
		return known;
	}

	public Sone setKnown(boolean known) {
		this.known = known;
		return this;
	}

	public List<String> getFriends() {
		List<String> friends = new ArrayList<String>(friendSones);
		return friends;
	}

	public boolean hasFriend(String friendSoneId) {
		return friendSones.contains(friendSoneId);
	}

	public Sone addFriend(String friendSone) {
		if (!friendSone.equals(id)) {
			friendSones.add(friendSone);
		}
		return this;
	}

	public Sone removeFriend(String friendSoneId) {
		friendSones.remove(friendSoneId);
		return this;
	}

	public List<Post> getPosts() {
		List<Post> sortedPosts;
		synchronized (this) {
			sortedPosts = new ArrayList<Post>(posts);
		}
		Collections.sort(sortedPosts, Post.TIME_COMPARATOR);
		return sortedPosts;
	}

	public Sone setPosts(Collection<Post> posts) {
		synchronized (this) {
			this.posts.clear();
			this.posts.addAll(posts);
		}
		return this;
	}

	public void addPost(Post post) {
		if (post.getSone().equals(this) && posts.add(post)) {
			logger.log(Level.FINEST, String.format("Adding %s to “%s”.", post, getName()));
		}
	}

	public void removePost(Post post) {
		if (post.getSone().equals(this)) {
			posts.remove(post);
		}
	}

	public Set<PostReply> getReplies() {
		return Collections.unmodifiableSet(replies);
	}

	public Sone setReplies(Collection<PostReply> replies) {
		this.replies.clear();
		this.replies.addAll(replies);
		return this;
	}

	public void addReply(PostReply reply) {
		if (reply.getSone().equals(this)) {
			replies.add(reply);
		}
	}

	public void removeReply(PostReply reply) {
		if (reply.getSone().equals(this)) {
			replies.remove(reply);
		}
	}

	public Set<String> getLikedPostIds() {
		return Collections.unmodifiableSet(likedPostIds);
	}

	public Sone setLikePostIds(Set<String> likedPostIds) {
		this.likedPostIds.clear();
		this.likedPostIds.addAll(likedPostIds);
		return this;
	}

	public boolean isLikedPostId(String postId) {
		return likedPostIds.contains(postId);
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

	public Album getRootAlbum() {
		return rootAlbum;
	}

	public Options getOptions() {
		return options;
	}

	/* TODO - remove this method again, maybe add an option provider */
	public void setOptions(Options options) {
		this.options = options;
	}

	@Override
	public AlbumBuilder newAlbumBuilder() {
		return new DefaultAlbumBuilder(database, this, rootAlbum.getId());
	}

	public PostBuilder newPostBuilder() {
		return new DefaultPostBuilder(database, getId()) {
			@Override
			public Post build(Optional<PostCreated> postCreated) {
				Post post = super.build(postCreated);
				database.storePost(post);
				return post;
			}
		};
	}

	@Override
	public PostReplyBuilder newPostReplyBuilder(String postId) throws IllegalStateException {
		return new DefaultPostReplyBuilder(database, getId(), postId) {
			@Override
			public PostReply build(Optional<PostReplyCreated> postReplyCreated) {
				PostReply postReply = super.build(postReplyCreated);
				database.storePostReply(postReply);
				return postReply;
			}
		};
	}

	public Modifier modify() {
		return new Modifier() {
			private long latestEdition = DefaultSone.this.latestEdition;
			@Override
			public Modifier setLatestEdition(long latestEdition) {
				this.latestEdition = latestEdition;
				return this;
			}

			@Override
			public Sone update() {
				DefaultSone.this.latestEdition = latestEdition;
				return DefaultSone.this;
			}
		};
	}

	//
	// FINGERPRINTABLE METHODS
	//

	@Override
	public synchronized String getFingerprint() {
		Hasher hash = Hashing.sha256().newHasher();
		hash.putString(profile.getFingerprint());

		hash.putString("Posts(");
		for (Post post : getPosts()) {
			hash.putString("Post(").putString(post.getId()).putString(")");
		}
		hash.putString(")");

		List<PostReply> replies = new ArrayList<PostReply>(getReplies());
		Collections.sort(replies, Reply.TIME_COMPARATOR);
		hash.putString("Replies(");
		for (PostReply reply : replies) {
			hash.putString("Reply(").putString(reply.getId()).putString(")");
		}
		hash.putString(")");

		List<String> likedPostIds = new ArrayList<String>(getLikedPostIds());
		Collections.sort(likedPostIds);
		hash.putString("LikedPosts(");
		for (String likedPostId : likedPostIds) {
			hash.putString("Post(").putString(likedPostId).putString(")");
		}
		hash.putString(")");

		List<String> likedReplyIds = new ArrayList<String>(getLikedReplyIds());
		Collections.sort(likedReplyIds);
		hash.putString("LikedReplies(");
		for (String likedReplyId : likedReplyIds) {
			hash.putString("Reply(").putString(likedReplyId).putString(")");
		}
		hash.putString(")");

		hash.putString("Albums(");
		for (Album album : rootAlbum.getAlbums()) {
			if (!Album.NOT_EMPTY.apply(album)) {
				continue;
			}
			hash.putString(album.getFingerprint());
		}
		hash.putString(")");

		return hash.hash().toString();
	}

	//
	// INTERFACE Comparable<Sone>
	//

	@Override
	public int compareTo(Sone sone) {
		return NICE_NAME_COMPARATOR.compare(this, sone);
	}

	//
	// OBJECT METHODS
	//

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Sone)) {
			return false;
		}
		return ((Sone) object).getId().equals(id);
	}

	@Override
	public String toString() {
		return getClass().getName() + "[id=" + id + ",friends(" + friendSones.size() + "),posts(" + posts.size() + "),replies(" + replies.size() + "),albums(" + getRootAlbum().getAlbums().size() + ")]";
	}

}
