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

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;

import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostBuilder;

import com.google.common.base.Optional;
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

	/** The ID of the post. */
	protected Optional<String> id = absent();

	/** The time of the post. */
	protected Optional<Long> time = absent();

	/** The text of the post. */
	protected String text;

	/** The (optional) recipient of the post. */
	protected Optional<String> recipientId = absent();

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
	public PostBuilder withId(String id) {
		this.id = fromNullable(id);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PostBuilder withTime(long time) {
		this.time = of(time);
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
	public PostBuilder to(Optional<String> recipientId) {
		this.recipientId = recipientId;
		return this;
	}

	//
	// PROTECTED METHODS
	//

	protected String getId() {
		return id.isPresent() ? id.get() : randomUUID().toString();
	}

	protected long getTime() {
		return time.isPresent() ? time.get() : currentTimeMillis();
	}

	/**
	 * Validates the state of this post builder.
	 *
	 * @throws IllegalStateException
	 *             if the state is not valid for building a new post
	 */
	protected void validate() throws IllegalStateException {
		checkState(!StringUtils.isBlank(text), "text must not be empty");
		checkState(!recipientId.isPresent() || !senderId.equals(recipientId.get()), "sender and recipient must not be the same");
	}

}
