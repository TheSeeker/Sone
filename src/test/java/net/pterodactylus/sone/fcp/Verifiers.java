/*
 * Sone - Verifiers.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.fcp;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

/**
 * Verifiers used throughout the {@link net.pterodactylus.sone.fcp} package.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Verifiers {

	static void verifyPost(SimpleFieldSet replyParameters, String prefix, Post post) throws FSParseException {
		assertThat(replyParameters.get(format("%sID", prefix)), is(post.getId()));
		assertThat(replyParameters.get(format("%sSone", prefix)), is(post.getSone().getId()));
		assertThat(replyParameters.get(format("%sRecipient", prefix)), is(post.getRecipientId().orNull()));
		assertThat(replyParameters.getLong(format("%sTime", prefix)), is(post.getTime()));
		assertThat(replyParameters.get(format("%sText", prefix)), is(post.getText()));
	}

	static void verifyPostReply(SimpleFieldSet replyParameters, String prefix, PostReply postReply) throws FSParseException {
		assertThat(replyParameters.get(format("%sID", prefix)), is(postReply.getId()));
		assertThat(replyParameters.get(format("%sSone", prefix)), is(postReply.getSone().getId()));
		assertThat(replyParameters.getLong(format("%sTime", prefix)), is(postReply.getTime()));
		assertThat(replyParameters.get(format("%sText", prefix)), is(postReply.getText()));
	}

}
