/*
 * Sone - PostImpl.java - Copyright © 2010–2013 David Roden
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

import static com.google.common.collect.FluentIterable.from;

import java.util.List;
import java.util.UUID;

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Reply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.Database;

import com.google.common.base.Optional;

/**
 * A post is a short message that a user writes in his Sone to let other users
 * know what is going on.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultPost implements Post {

	private final Database database;

	/** The GUID of the post. */
	private final UUID id;

	/** The ID of the owning Sone. */
	private final String soneId;

	/** The ID of the recipient Sone. */
	private final String recipientId;

	/** The time of the post (in milliseconds since Jan 1, 1970 UTC). */
	private final long time;

	/** The text of the post. */
	private final String text;

	/** Whether the post is known. */
	private volatile boolean known;

	/**
	 * Creates a new post.
	 *
	 * @param database
	 *            The database
	 * @param id
	 *            The ID of the post
	 * @param soneId
	 *            The ID of the Sone this post belongs to
	 * @param recipientId
	 *            The ID of the recipient of the post
	 * @param time
	 *            The time of the post (in milliseconds since Jan 1, 1970 UTC)
	 * @param text
	 *            The text of the post
	 */
	public DefaultPost(Database database, String id, String soneId, String recipientId, long time, String text) {
		this.database = database;
		this.id = UUID.fromString(id);
		this.soneId = soneId;
		this.recipientId = recipientId;
		this.time = time;
		this.text = text;
	}

	//
	// ACCESSORS
	//

	@Override
	public String getId() {
		return id.toString();
	}

	@Override
	public Sone getSone() {
		return database.getSone(soneId).get();
	}

	@Override
	public Optional<String> getRecipientId() {
		return Optional.fromNullable(recipientId);
	}

	@Override
	public Optional<Sone> getRecipient() {
		return database.getSone(recipientId);
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public boolean isKnown() {
		return known;
	}

	@Override
	public DefaultPost setKnown(boolean known) {
		this.known = known;
		return this;
	}

	@Override
	public List<PostReply> getReplies() {
		return from(database.getReplies(getId())).toSortedList(Reply.TIME_COMPARATOR);
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
		if (!(object instanceof DefaultPost)) {
			return false;
		}
		DefaultPost post = (DefaultPost) object;
		return post.id.equals(id);
	}

	@Override
	public String toString() {
		return String.format("%s[id=%s,sone=%s,recipient=%s,time=%d,text=%s]", getClass().getName(), id, soneId, recipientId, time, text);
	}

}
