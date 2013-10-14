/*
 * Sone - AbstractSone.java - Copyright © 2013 David Roden
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
import net.pterodactylus.sone.freenet.wot.Identity;
import net.pterodactylus.util.logging.Logging;

import freenet.keys.FreenetURI;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * Abstract base implementation of a {@link Sone}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public abstract class AbstractSone implements Sone {

	/** The logger. */
	protected static final Logger logger = Logging.getLogger(DefaultSone.class);
	/** The ID of this Sone. */
	protected final String id;
	/** Whether the Sone is local. */
	protected final boolean local;
	/** All friend Sones. */
	protected final Set<String> friendSones = new CopyOnWriteArraySet<String>();
	/** The root album containing all albums. */
	protected final DefaultAlbum rootAlbum = new DefaultAlbum(this, null);
	/** Whether this Sone is known. */
	protected volatile boolean known;
	/** The identity of this Sone. */
	private Identity identity;
	/** The URI under which the Sone is stored in Freenet. */
	private volatile FreenetURI requestUri;
	/** The URI used to insert a new version of this Sone. */
	/* This will be null for remote Sones! */
	private volatile FreenetURI insertUri;
	/** The latest edition of the Sone. */
	private volatile long latestEdition;
	/** The time of the last inserted update. */
	private volatile long time;
	/** The status of this Sone. */
	private volatile SoneStatus status = SoneStatus.unknown;
	/** The profile of this Sone. */
	private volatile Profile profile = new Profile(this);
	/** The client used by the Sone. */
	private volatile Client client;
	/** Sone-specific options. */
	private Options options = new Options();

	public AbstractSone(String id, boolean local) {
		this.id = id;
		this.local = local;
	}

	/**
	 * Returns the identity of this Sone.
	 *
	 * @return The identity of this Sone
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the identity of this Sone.
	 *
	 * @return The identity of this Sone
	 */
	public Identity getIdentity() {
		return identity;
	}

	/**
	 * Sets the identity of this Sone. The {@link Identity#getId() ID} of the
	 * identity has to match this Sone’s {@link #getId()}.
	 *
	 * @param identity
	 * 		The identity of this Sone
	 * @return This Sone (for method chaining)
	 * @throws IllegalArgumentException
	 * 		if the ID of the identity does not match this Sone’s ID
	 */
	public Sone setIdentity(Identity identity) throws IllegalArgumentException {
		if (!identity.getId().equals(id)) {
			throw new IllegalArgumentException("Identity’s ID does not match Sone’s ID!");
		}
		this.identity = identity;
		return this;
	}

	/**
	 * Returns the name of this Sone.
	 *
	 * @return The name of this Sone
	 */
	public String getName() {
		return (identity != null) ? identity.getNickname() : null;
	}

	/**
	 * Returns whether this Sone is a local Sone.
	 *
	 * @return {@code true} if this Sone is a local Sone, {@code false} otherwise
	 */
	public boolean isLocal() {
		return local;
	}

	/**
	 * Returns the request URI of this Sone.
	 *
	 * @return The request URI of this Sone
	 */
	public FreenetURI getRequestUri() {
		return (requestUri != null) ? requestUri.setSuggestedEdition(latestEdition) : null;
	}

	/**
	 * Sets the request URI of this Sone.
	 *
	 * @param requestUri
	 * 		The request URI of this Sone
	 * @return This Sone (for method chaining)
	 */
	public Sone setRequestUri(FreenetURI requestUri) {
		if (this.requestUri == null) {
			this.requestUri = requestUri.setKeyType("USK").setDocName("Sone").setMetaString(new String[0]);
			return this;
		}
		if (!this.requestUri.equalsKeypair(requestUri)) {
			logger.log(Level.WARNING, String.format("Request URI %s tried to overwrite %s!", requestUri, this.requestUri));
			return this;
		}
		return this;
	}

	/**
	 * Returns the insert URI of this Sone.
	 *
	 * @return The insert URI of this Sone
	 */
	public FreenetURI getInsertUri() {
		return (insertUri != null) ? insertUri.setSuggestedEdition(latestEdition) : null;
	}

	/**
	 * Sets the insert URI of this Sone.
	 *
	 * @param insertUri
	 * 		The insert URI of this Sone
	 * @return This Sone (for method chaining)
	 */
	public Sone setInsertUri(FreenetURI insertUri) {
		if (this.insertUri == null) {
			this.insertUri = insertUri.setKeyType("USK").setDocName("Sone").setMetaString(new String[0]);
			return this;
		}
		if (!this.insertUri.equalsKeypair(insertUri)) {
			logger.log(Level.WARNING, String.format("Request URI %s tried to overwrite %s!", insertUri, this.insertUri));
			return this;
		}
		return this;
	}

	/**
	 * Returns the latest edition of this Sone.
	 *
	 * @return The latest edition of this Sone
	 */
	public long getLatestEdition() {
		return latestEdition;
	}

	/**
	 * Sets the latest edition of this Sone. If the given latest edition is not
	 * greater than the current latest edition, the latest edition of this Sone is
	 * not changed.
	 *
	 * @param latestEdition
	 * 		The latest edition of this Sone
	 */
	public void setLatestEdition(long latestEdition) {
		if (!(latestEdition > this.latestEdition)) {
			logger.log(Level.FINE, String.format("New latest edition %d is not greater than current latest edition %d!", latestEdition, this.latestEdition));
			return;
		}
		this.latestEdition = latestEdition;
	}

	/**
	 * Return the time of the last inserted update of this Sone.
	 *
	 * @return The time of the update (in milliseconds since Jan 1, 1970 UTC)
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the time of the last inserted update of this Sone.
	 *
	 * @param time
	 * 		The time of the update (in milliseconds since Jan 1, 1970 UTC)
	 * @return This Sone (for method chaining)
	 */
	public Sone setTime(long time) {
		this.time = time;
		return this;
	}

	/**
	 * Returns the status of this Sone.
	 *
	 * @return The status of this Sone
	 */
	public SoneStatus getStatus() {
		return status;
	}

	/**
	 * Sets the new status of this Sone.
	 *
	 * @param status
	 * 		The new status of this Sone
	 * @return This Sone
	 * @throws IllegalArgumentException
	 * 		if {@code status} is {@code null}
	 */
	public Sone setStatus(SoneStatus status) {
		this.status = checkNotNull(status, "status must not be null");
		return this;
	}

	/**
	 * Returns a copy of the profile. If you want to update values in the profile
	 * of this Sone, update the values in the returned {@link Profile} and use
	 * {@link #setProfile(Profile)} to change the profile in this Sone.
	 *
	 * @return A copy of the profile
	 */
	public Profile getProfile() {
		return new Profile(profile);
	}

	/**
	 * Sets the profile of this Sone. A copy of the given profile is stored so that
	 * subsequent modifications of the given profile are not reflected in this
	 * Sone!
	 *
	 * @param profile
	 * 		The profile to set
	 */
	public void setProfile(Profile profile) {
		this.profile = new Profile(profile);
	}

	/**
	 * Returns the client used by this Sone.
	 *
	 * @return The client used by this Sone, or {@code null}
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * Sets the client used by this Sone.
	 *
	 * @param client
	 * 		The client used by this Sone, or {@code null}
	 * @return This Sone (for method chaining)
	 */
	public Sone setClient(Client client) {
		this.client = client;
		return this;
	}

	/**
	 * Returns the root album that contains all visible albums of this Sone.
	 *
	 * @return The root album of this Sone
	 */
	public Album getRootAlbum() {
		return rootAlbum;
	}

	/**
	 * Returns Sone-specific options.
	 *
	 * @return The options of this Sone
	 */
	public Options getOptions() {
		return options;
	}

	/**
	 * Sets the options of this Sone.
	 *
	 * @param options
	 * 		The options of this Sone
	 */
	/* TODO - remove this method again, maybe add an option provider */
	public void setOptions(Options options) {
		this.options = options;
	}

	/** {@inheritDoc} */
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

	/** {@inheritDoc} */
	@Override
	public int compareTo(Sone sone) {
		return NICE_NAME_COMPARATOR.compare(this, sone);
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Sone)) {
			return false;
		}
		return ((Sone) object).getId().equals(id);
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return getClass().getName() + "[identity=" + identity + ",requestUri=" + requestUri + ",insertUri(" + String.valueOf(insertUri).length() + "),friends(" + friendSones.size() + "),posts(" + getPosts().size() + "),replies(" + getReplies().size() + "),albums(" + getRootAlbum().getAlbums().size() + ")]";
	}

}
