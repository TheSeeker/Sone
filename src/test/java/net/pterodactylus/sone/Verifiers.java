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

package net.pterodactylus.sone;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static net.pterodactylus.sone.data.Reply.FUTURE_REPLY_FILTER;
import static net.pterodactylus.sone.template.SoneAccessor.getNiceName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Profile.Field;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.fcp.Command.Response;
import net.pterodactylus.sone.web.ajax.JsonErrorReturnObject;
import net.pterodactylus.sone.web.ajax.JsonReturnObject;

import freenet.node.FSParseException;
import freenet.support.SimpleFieldSet;

import org.hamcrest.CoreMatchers;

/**
 * Verifiers used throughout the {@link net.pterodactylus.sone.fcp} package.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Verifiers {

	public static void verifyAnswer(Response response, String messageName) {
		assertThat(response, notNullValue());
		assertThat(response.getReplyParameters(), notNullValue());
		assertThat(response.getReplyParameters().get("Message"), is(messageName));
	}

	public static void verifyPost(SimpleFieldSet replyParameters, String prefix, Post post) throws FSParseException {
		assertThat(replyParameters.get(format("%sID", prefix)), is(post.getId()));
		assertThat(replyParameters.get(format("%sSone", prefix)), is(post.getSone().getId()));
		assertThat(replyParameters.get(format("%sRecipient", prefix)), is(post.getRecipientId().orNull()));
		assertThat(replyParameters.getLong(format("%sTime", prefix)), is(post.getTime()));
		assertThat(replyParameters.get(format("%sText", prefix)), is(post.getText()));
	}

	public static void verifyPosts(SimpleFieldSet postFieldSet, String prefix, List<Post> posts) throws FSParseException {
		assertThat(postFieldSet.getInt(prefix + "Count"), CoreMatchers.is(posts.size()));
		int postIndex = 0;
		for (Post post : posts) {
			verifyPost(postFieldSet, prefix + postIndex + ".", post);
			postIndex++;
		}
	}

	public static void verifyPostReply(SimpleFieldSet replyParameters, String prefix, PostReply postReply) throws FSParseException {
		assertThat(replyParameters.get(format("%sID", prefix)), is(postReply.getId()));
		assertThat(replyParameters.get(format("%sSone", prefix)), is(postReply.getSone().getId()));
		assertThat(replyParameters.getLong(format("%sTime", prefix)), is(postReply.getTime()));
		assertThat(replyParameters.get(format("%sText", prefix)), is(postReply.getText()));
	}

	public static void verifyPostReplies(SimpleFieldSet postFieldSet, String prefix, List<PostReply> postReplies) throws FSParseException {
		assertThat(postFieldSet.getInt(prefix + "Count"), CoreMatchers.is(from(postReplies).filter(FUTURE_REPLY_FILTER).size()));
		int postReplyIndex = 0;
		for (PostReply postReply : from(postReplies).filter(FUTURE_REPLY_FILTER)) {
			verifyPostReply(postFieldSet, prefix + postReplyIndex + ".", postReply);
			postReplyIndex++;
		}
	}

	public static void verifyPostsWithReplies(SimpleFieldSet postFieldSet, String prefix, List<Post> posts) throws FSParseException {
		assertThat(postFieldSet.getInt(prefix + "Count"), CoreMatchers.is(posts.size()));
		int postIndex = 0;
		for (Post post : posts) {
			verifyPost(postFieldSet, prefix + postIndex + ".", post);
			verifyPostReplies(postFieldSet, prefix + postIndex + ".Replies.", post.getReplies());
			postIndex++;
		}
	}

	public static void verifyPostWithReplies(SimpleFieldSet postFieldSet, String prefix, Post post) throws FSParseException {
		verifyPost(postFieldSet, prefix, post);
		verifyPostReplies(postFieldSet, prefix + "Replies.", post.getReplies());
	}

	public static void verifyFollowedSone(SimpleFieldSet simpleFieldSet, String prefix, Sone sone) throws FSParseException {
		verifyNotFollowedSone(simpleFieldSet, prefix, sone);
		assertThat(simpleFieldSet.getBoolean(prefix + "Followed"), is(true));
	}

	public static void verifyNotFollowedSone(SimpleFieldSet simpleFieldSet, String prefix, Sone sone) throws FSParseException {
		assertThat(simpleFieldSet.get(prefix + "Name"), is(sone.getName()));
		assertThat(simpleFieldSet.get(prefix + "NiceName"), is(getNiceName(sone)));
		assertThat(simpleFieldSet.getLong(prefix + "LastUpdated"), is(sone.getTime()));
		assertThat(simpleFieldSet.getInt(prefix + "Field.Count"), is(sone.getProfile().getFields().size()));
		int fieldIndex = 0;
		for (Field field : sone.getProfile().getFields()) {
			assertThat(simpleFieldSet.get(prefix + "Field." + fieldIndex + ".Name"), is(field.getName()));
			assertThat(simpleFieldSet.get(prefix + "Field." + fieldIndex + ".Value"), is(field.getValue()));
			fieldIndex++;
		}
	}

	public static void verifySones(SimpleFieldSet simpleFieldSet, String prefix, List<Sone> sones) throws FSParseException {
		assertThat(simpleFieldSet.getInt(prefix + "Count"), is(sones.size()));
		int soneIndex = 0;
		for (Sone sone : sones) {
			assertThat(simpleFieldSet.get(prefix + soneIndex + ".ID"), is(sone.getId()));
			assertThat(simpleFieldSet.get(prefix + soneIndex + ".Name"), is(sone.getName()));
			assertThat(simpleFieldSet.get(prefix + soneIndex + ".NiceName"), is(getNiceName(sone)));
			assertThat(simpleFieldSet.getLong(prefix + soneIndex + ".Time"), is(sone.getTime()));
			soneIndex++;
		}
	}

	public static void verifySuccessfulJsonResponse(JsonReturnObject jsonReturnObject) {
		assertThat(jsonReturnObject, notNullValue());
		assertThat(jsonReturnObject.isSuccess(), is(true));
	}

	public static void verifyJsonError(JsonReturnObject jsonReturnObject, String error) {
		assertThat(jsonReturnObject, notNullValue());
		assertThat(jsonReturnObject.isSuccess(), is(false));
		assertThat(((JsonErrorReturnObject) jsonReturnObject).getError(), is(error));
	}

}
