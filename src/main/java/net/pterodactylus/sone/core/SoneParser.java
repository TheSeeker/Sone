/*
 * Sone - SoneParser.java - Copyright © 2010–2013 David Roden
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

package net.pterodactylus.sone.core;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Client;
import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.PostReply;
import net.pterodactylus.sone.data.Profile;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.impl.DefaultSone;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.ImageBuilder.ImageCreated;
import net.pterodactylus.sone.database.PostBuilder;
import net.pterodactylus.sone.database.PostBuilder.PostCreated;
import net.pterodactylus.sone.database.PostReplyBuilder;
import net.pterodactylus.sone.database.PostReplyBuilder.PostReplyCreated;
import net.pterodactylus.util.number.Numbers;
import net.pterodactylus.util.xml.SimpleXML;
import net.pterodactylus.util.xml.XML;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import org.w3c.dom.Document;

/**
 * Parses the inserted XML representation of a {@link Sone} into a Sone.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneParser {

	private static final Logger logger = Logger.getLogger(SoneParser.class.getName());
	private static final int MAX_PROTOCOL_VERSION = 0;

	/**
	 * Parses a Sone from the given input stream and creates a new Sone from the
	 * parsed data.
	 *
	 * @param originalSone
	 * 		The Sone to update
	 * @param soneInputStream
	 * 		The input stream to parse the Sone from
	 * @return The parsed Sone
	 */
	public Sone parseSone(Database database, Sone originalSone, InputStream soneInputStream) {
		/* TODO - impose a size limit? */

		Document document;
		/* XML parsing is not thread-safe. */
		synchronized (this) {
			document = XML.transformToDocument(soneInputStream);
		}
		if (document == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Could not parse XML for Sone %s!", originalSone.getId()));
			throw new InvalidXml();
		}

		SimpleXML soneXml = SimpleXML.fromDocument(document);
		Optional<Client> parsedClient = parseClient(originalSone, soneXml);
		Sone sone = new DefaultSone(database, originalSone.getId(), originalSone.isLocal(), parsedClient.or(originalSone.getClient()));

		Optional<Integer> protocolVersion = parseProtocolVersion(soneXml);
		if (protocolVersion.isPresent()) {
			if (protocolVersion.get() < 0) {
				logger.log(Level.WARNING, String.format("Invalid protocol version: %d! Not parsing Sone.", protocolVersion.get()));
				throw new InvalidProtocolVersion();
			}
			if (protocolVersion.get() > MAX_PROTOCOL_VERSION) {
				logger.log(Level.WARNING, String.format("Unknown protocol version: %d! Not parsing Sone.", protocolVersion.get()));
				throw new SoneTooNew();
			}
		}

		String soneTime = soneXml.getValue("time", null);
		if (soneTime == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded time for Sone %s was null!", sone));
			throw new MalformedXml();
		}
		try {
			sone.setTime(Long.parseLong(soneTime));
		} catch (NumberFormatException nfe1) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s with invalid time: %s", sone, soneTime));
			throw new MalformedTime();
		}

		SimpleXML profileXml = soneXml.getNode("profile");
		if (profileXml == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s has no profile!", sone));
			throw new MalformedXml();
		}

		/* parse profile. */
		String profileFirstName = profileXml.getValue("first-name", null);
		String profileMiddleName = profileXml.getValue("middle-name", null);
		String profileLastName = profileXml.getValue("last-name", null);
		Integer profileBirthDay = Numbers.safeParseInteger(profileXml.getValue("birth-day", null));
		Integer profileBirthMonth = Numbers.safeParseInteger(profileXml.getValue("birth-month", null));
		Integer profileBirthYear = Numbers.safeParseInteger(profileXml.getValue("birth-year", null));
		Profile profile = new Profile(sone).modify().setFirstName(profileFirstName).setMiddleName(profileMiddleName).setLastName(profileLastName).update();
		profile.modify().setBirthDay(profileBirthDay).setBirthMonth(profileBirthMonth).setBirthYear(profileBirthYear).update();
		/* avatar is processed after images are loaded. */
		String avatarId = profileXml.getValue("avatar", null);

		/* parse profile fields. */
		SimpleXML profileFieldsXml = profileXml.getNode("fields");
		if (profileFieldsXml != null) {
			for (SimpleXML fieldXml : profileFieldsXml.getNodes("field")) {
				String fieldName = fieldXml.getValue("field-name", null);
				String fieldValue = fieldXml.getValue("field-value", "");
				if (fieldName == null) {
					logger.log(Level.WARNING, String.format("Downloaded profile field for Sone %s with missing data! Name: %s, Value: %s", sone, fieldName, fieldValue));
					throw new MalformedXml();
				}
				try {
					profile.setField(profile.addField(fieldName), fieldValue);
				} catch (IllegalArgumentException iae1) {
					logger.log(Level.WARNING, String.format("Duplicate field: %s", fieldName), iae1);
					throw new DuplicateField();
				}
			}
		}

		/* parse posts. */
		SimpleXML postsXml = soneXml.getNode("posts");
		Set<Post> posts = new HashSet<Post>();
		if (postsXml == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s has no posts!", sone));
		} else {
			for (SimpleXML postXml : postsXml.getNodes("post")) {
				String postId = postXml.getValue("id", null);
				String postRecipientId = postXml.getValue("recipient", null);
				String postTime = postXml.getValue("time", null);
				String postText = postXml.getValue("text", null);
				if ((postId == null) || (postTime == null) || (postText == null)) {
					/* TODO - mark Sone as bad. */
					logger.log(Level.WARNING, String.format("Downloaded post for Sone %s with missing data! ID: %s, Time: %s, Text: %s", sone, postId, postTime, postText));
					throw new MalformedXml();
				}
				try {
					PostBuilder postBuilder = sone.newPostBuilder();
					/* TODO - parse time correctly. */
					postBuilder.withId(postId).withTime(Long.parseLong(postTime)).withText(postText);
					if ((postRecipientId != null) && (postRecipientId.length() == 43)) {
						postBuilder.to(of(postRecipientId));
					}
					posts.add(postBuilder.build(Optional.<PostCreated>absent()));
				} catch (NumberFormatException nfe1) {
					/* TODO - mark Sone as bad. */
					logger.log(Level.WARNING, String.format("Downloaded post for Sone %s with invalid time: %s", sone, postTime));
					throw new MalformedTime();
				}
			}
		}

		/* parse replies. */
		SimpleXML repliesXml = soneXml.getNode("replies");
		Set<PostReply> replies = new HashSet<PostReply>();
		if (repliesXml == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s has no replies!", sone));
		} else {
			for (SimpleXML replyXml : repliesXml.getNodes("reply")) {
				String replyId = replyXml.getValue("id", null);
				String replyPostId = replyXml.getValue("post-id", null);
				String replyTime = replyXml.getValue("time", null);
				String replyText = replyXml.getValue("text", null);
				if ((replyId == null) || (replyPostId == null) || (replyTime == null) || (replyText == null)) {
					/* TODO - mark Sone as bad. */
					logger.log(Level.WARNING, String.format("Downloaded reply for Sone %s with missing data! ID: %s, Post: %s, Time: %s, Text: %s", sone, replyId, replyPostId, replyTime, replyText));
					throw new MalformedXml();
				}
				try {
					/* TODO - parse time correctly. */
					PostReplyBuilder postReplyBuilder = sone.newPostReplyBuilder(replyPostId).withId(replyId).withTime(Long.parseLong(replyTime)).withText(replyText);
					replies.add(postReplyBuilder.build(Optional.<PostReplyCreated>absent()));
				} catch (NumberFormatException nfe1) {
					/* TODO - mark Sone as bad. */
					logger.log(Level.WARNING, String.format("Downloaded reply for Sone %s with invalid time: %s", sone, replyTime));
					throw new MalformedTime();
				}
			}
		}

		/* parse liked post IDs. */
		SimpleXML likePostIdsXml = soneXml.getNode("post-likes");
		Set<String> likedPostIds = new HashSet<String>();
		if (likePostIdsXml == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s has no post likes!", sone));
		} else {
			for (SimpleXML likedPostIdXml : likePostIdsXml.getNodes("post-like")) {
				String postId = likedPostIdXml.getValue();
				likedPostIds.add(postId);
			}
		}

		/* parse liked reply IDs. */
		SimpleXML likeReplyIdsXml = soneXml.getNode("reply-likes");
		Set<String> likedReplyIds = new HashSet<String>();
		if (likeReplyIdsXml == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s has no reply likes!", sone));
		} else {
			for (SimpleXML likedReplyIdXml : likeReplyIdsXml.getNodes("reply-like")) {
				String replyId = likedReplyIdXml.getValue();
				likedReplyIds.add(replyId);
			}
		}

		/* parse albums. */
		SimpleXML albumsXml = soneXml.getNode("albums");
		Map<String, Album> albums = Maps.newHashMap();
		if (albumsXml != null) {
			for (SimpleXML albumXml : albumsXml.getNodes("album")) {
				String id = albumXml.getValue("id", null);
				String parentId = albumXml.getValue("parent", null);
				String title = albumXml.getValue("title", null);
				String description = albumXml.getValue("description", "");
				String albumImageId = albumXml.getValue("album-image", null);
				if ((id == null) || (title == null) || (description == null)) {
					logger.log(Level.WARNING, String.format("Downloaded Sone %s contains invalid album!", sone));
					throw new MalformedXml();
				}
				Album parent = sone.getRootAlbum();
				if (parentId != null) {
					parent = albums.get(parentId);
					if (parent == null) {
						logger.log(Level.WARNING, String.format("Downloaded Sone %s has album with invalid parent!", sone));
						throw new InvalidParentAlbum();
					}
				}
				Album album = parent.newAlbumBuilder().withId(id).build().modify().setTitle(title).setDescription(description).update();
				albums.put(album.getId(), album);
				SimpleXML imagesXml = albumXml.getNode("images");
				if (imagesXml != null) {
					for (SimpleXML imageXml : imagesXml.getNodes("image")) {
						String imageId = imageXml.getValue("id", null);
						String imageCreationTimeString = imageXml.getValue("creation-time", null);
						String imageKey = imageXml.getValue("key", null);
						String imageTitle = imageXml.getValue("title", null);
						String imageDescription = imageXml.getValue("description", "");
						String imageWidthString = imageXml.getValue("width", null);
						String imageHeightString = imageXml.getValue("height", null);
						if ((imageId == null) || (imageCreationTimeString == null) || (imageKey == null) || (imageTitle == null) || (imageWidthString == null) || (imageHeightString == null)) {
							logger.log(Level.WARNING, String.format("Downloaded Sone %s contains invalid images!", sone));
							throw new MalformedXml();
						}
						long creationTime = Numbers.safeParseLong(imageCreationTimeString, 0L);
						int imageWidth = Numbers.safeParseInteger(imageWidthString, 0);
						int imageHeight = Numbers.safeParseInteger(imageHeightString, 0);
						if ((imageWidth < 1) || (imageHeight < 1)) {
							logger.log(Level.WARNING, String.format("Downloaded Sone %s contains image %s with invalid dimensions (%s, %s)!", sone, imageId, imageWidthString, imageHeightString));
							throw new MalformedDimension();
						}
						Image image = album.newImageBuilder().withId(imageId).at(imageKey).created(creationTime).sized(imageWidth, imageHeight).build(Optional.<ImageCreated>absent());
						image = image.modify().setTitle(imageTitle).setDescription(imageDescription).update();
					}
				}
				album.modify().setAlbumImage(albumImageId).update();
			}
		}

		/* process avatar. */
		profile.setAvatar(fromNullable(avatarId));

		/* okay, apparently everything was parsed correctly. Now import. */
		sone.setProfile(profile);
		sone.setPosts(posts);
		sone.setReplies(replies);
		sone.setLikePostIds(likedPostIds);
		sone.setLikeReplyIds(likedReplyIds);

		return sone;
	}

	private Optional<Integer> parseProtocolVersion(SimpleXML soneXml) {
		String soneProtocolVersion = soneXml.getValue("protocol-version", null);
		if (soneProtocolVersion == null) {
			logger.log(Level.INFO, "No protocol version found, assuming 0.");
			return absent();
		}
		return fromNullable(Ints.tryParse(soneProtocolVersion));
	}

	private Optional<Client> parseClient(Sone sone, SimpleXML soneXml) {
		SimpleXML clientXml = soneXml.getNode("client");
		if (clientXml == null) {
			return absent();
		}
		String clientName = clientXml.getValue("name", null);
		String clientVersion = clientXml.getValue("version", null);
		if ((clientName == null) || (clientVersion == null)) {
			logger.log(Level.WARNING, String.format("Download Sone %s with client XML but missing name or version!", sone));
			return absent();
		}
		return of(new Client(clientName, clientVersion));
	}

	public static class InvalidXml extends RuntimeException {

	}

	public static class InvalidProtocolVersion extends RuntimeException {

	}

	public static class SoneTooNew extends RuntimeException {

	}

	public static class MalformedXml extends RuntimeException {

	}

	public static class DuplicateField extends RuntimeException {

	}

	public static class MalformedTime extends RuntimeException {

	}

	public static class InvalidParentAlbum extends RuntimeException {

	}

	public static class MalformedDimension extends RuntimeException {

	}

}
