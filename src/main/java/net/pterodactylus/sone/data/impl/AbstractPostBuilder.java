/*
 * Sone - AbstractPostBuilder.java - Copyright © 2013 David Roden
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
import static com.google.common.base.Preconditions.checkState;

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * Abstract {@link PostBuilder} implementation. It stores the state of the new
 * post and performs validation, you only need to implement {@link #build()}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public abstract class AbstractPostBuilder implements PostBuilder {

	protected final Database database;
	protected final String senderId;

	/** Wether to create a post with a random ID. */
	protected boolean randomId;

	/** The ID of the post. */
	protected String id;

	/** Whether to use the current time when creating the post. */
	protected boolean currentTime;

	/** The time of the post. */
	protected long time;

	/** The text of the post. */
	protected String text;

	/** The (optional) recipient of the post. */
	protected String recipientId;

	protected AbstractPostBuilder(Database database, String soneId) {
		this.database = checkNotNull(database, "database must not be null");
		this.senderId = checkNotNull(soneId, "sender ID must not be null");
	}

	//
	// POSTBUILDER METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PostBuilder randomId() {
		randomId = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PostBuilder withId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PostBuilder currentTime() {
		currentTime = true;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PostBuilder withTime(long time) {
		this.time = time;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PostBuilder withText(String text) {
		this.text = text;
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PostBuilder to(String recipientId) {
		this.recipientId = recipientId;
		return this;
	}

	//
	// PROTECTED METHODS
	//

	/**
	 * Validates the state of this post builder.
	 *
	 * @throws IllegalStateException
	 *             if the state is not valid for building a new post
	 */
	protected void validate() throws IllegalStateException {
		checkState((randomId && (id == null)) || (!randomId && (id != null)), "exactly one of random ID or custom ID must be set");
		checkState((currentTime && (time == 0)) || (!currentTime && (time > 0)), "one of current time or custom time must be set");
		checkState(!StringUtils.isBlank(text), "text must not be empty");
		checkState(!senderId.equals(recipientId), "sender and recipient must not be the same");
	}

}
