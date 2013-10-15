/*
 * Sone - AbstractPostReplyBuilder.java - Copyright © 2013 David Roden
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

import org.apache.commons.lang.StringUtils;

import net.pterodactylus.sone.database.PostReplyBuilder;

/**
 * Abstract {@link PostReplyBuilder} implementation. It stores the state of the
 * new post and performs validation, implementations only need to implement
 * {@link #build()}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public abstract class AbstractPostReplyBuilder extends AbstractReplyBuilder<PostReplyBuilder> implements PostReplyBuilder {

	protected String postId;

	protected AbstractPostReplyBuilder(String senderId, String postId) {
		super(senderId);
		this.postId = checkNotNull(postId, "post ID must not be null");
	}

}
