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

import java.io.InputStream;
import java.net.MalformedURLException;
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
import net.pterodactylus.sone.database.ImageBuilder.ImageCreated;
import net.pterodactylus.sone.database.PostBuilder;
import net.pterodactylus.sone.database.PostBuilder.PostCreated;
import net.pterodactylus.sone.database.PostReplyBuilder;
import net.pterodactylus.sone.database.PostReplyBuilder.PostReplyCreated;
import net.pterodactylus.sone.database.memory.MemoryDatabase;
import net.pterodactylus.util.number.Numbers;
import net.pterodactylus.util.xml.SimpleXML;
import net.pterodactylus.util.xml.XML;

import freenet.keys.FreenetURI;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.w3c.dom.Document;

/**
 * Parses the inserted XML representation of a {@link Sone} into a Sone.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneParser {

	private static final Logger logger = Logger.getLogger(SoneParser.class.getName());
	private static final int MAX_PROTOCOL_VERSION = 0;
	private final Core core;

	public SoneParser(Core core) {
		this.core = core;
	}

	/**
	 * Parses a Sone from the given input stream and creates a new Sone from the
	 * parsed data.
	 *
	 * @param originalSone
	 * 		The Sone to update
	 * @param soneInputStream
	 * 		The input stream to parse the Sone from
	 * @return The parsed Sone
	 * @throws SoneException
	 * 		if a parse error occurs, or the protocol is invalid
	 */
	public Sone parseSone(Sone originalSone, InputStream soneInputStream) throws SoneException {
		/* TODO - impose a size limit? */

		Document document;
		/* XML parsing is not thread-safe. */
		synchronized (this) {
			document = XML.transformToDocument(soneInputStream);
		}
		if (document == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Could not parse XML for Sone %s!", originalSone));
			return null;
		}

		SimpleXML soneXml;
		try {
			soneXml = SimpleXML.fromDocument(document);
		} catch (NullPointerException npe1) {
			/* for some reason, invalid XML can cause NPEs. */
			logger.log(Level.WARNING, String.format("XML for Sone %s can not be parsed!", originalSone), npe1);
			return null;
		}

		SimpleXML clientXml = soneXml.getNode("client");
		Client soneClient = originalSone.getClient();
		if (clientXml != null) {
			String clientName = clientXml.getValue("name", null);
			String clientVersion = clientXml.getValue("version", null);
			if ((clientName == null) || (clientVersion == null)) {
				logger.log(Level.WARNING, String.format("Download Sone %s with client XML but missing name or version!", originalSone));
				return null;
			}
			soneClient = new Client(clientName, clientVersion);
		}

		Sone sone = new DefaultSone(new MemoryDatabase(null), originalSone.getId(), originalSone.isLocal(), soneClient);

		Integer protocolVersion = null;
		String soneProtocolVersion = soneXml.getValue("protocol-version", null);
		if (soneProtocolVersion != null) {
			protocolVersion = Numbers.safeParseInteger(soneProtocolVersion);
		}
		if (protocolVersion == null) {
			logger.log(Level.INFO, "No protocol version found, assuming 0.");
			protocolVersion = 0;
		}

		if (protocolVersion < 0) {
			logger.log(Level.WARNING, String.format("Invalid protocol version: %d! Not parsing Sone.", protocolVersion));
			return null;
		}

		/* check for valid versions. */
		if (protocolVersion > MAX_PROTOCOL_VERSION) {
			logger.log(Level.WARNING, String.format("Unknown protocol version: %d! Not parsing Sone.", protocolVersion));
			return null;
		}

		String soneTime = soneXml.getValue("time", null);
		if (soneTime == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded time for Sone %s was null!", sone));
			return null;
		}
		try {
			sone.setTime(Long.parseLong(soneTime));
		} catch (NumberFormatException nfe1) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s with invalid time: %s", sone, soneTime));
			return null;
		}

		SimpleXML profileXml = soneXml.getNode("profile");
		if (profileXml == null) {
			/* TODO - mark Sone as bad. */
			logger.log(Level.WARNING, String.format("Downloaded Sone %s has no profile!", sone));
			return null;
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
					return null;
				}
				try {
					profile.addField(fieldName).setValue(fieldValue);
				} catch (IllegalArgumentException iae1) {
					logger.log(Level.WARNING, String.format("Duplicate field: %s", fieldName), iae1);
					return null;
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
					return null;
				}
				try {
					PostBuilder postBuilder = sone.newPostBuilder();
					/* TODO - parse time correctly. */
					postBuilder.withId(postId).withTime(Long.parseLong(postTime)).withText(postText);
					if ((postRecipientId != null) && (postRecipientId.length() == 43)) {
						postBuilder.to(Optional.of(postRecipientId));
					}
					posts.add(postBuilder.build(Optional.<PostCreated>absent()));
				} catch (NumberFormatException nfe1) {
					/* TODO - mark Sone as bad. */
					logger.log(Level.WARNING, String.format("Downloaded post for Sone %s with invalid time: %s", sone, postTime));
					return null;
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
					return null;
				}
				try {
					/* TODO - parse time correctly. */
					PostReplyBuilder postReplyBuilder = sone.newPostReplyBuilder(replyPostId).withId(replyId).withTime(Long.parseLong(replyTime)).withText(replyText);
					replies.add(postReplyBuilder.build(Optional.<PostReplyCreated>absent()));
				} catch (NumberFormatException nfe1) {
					/* TODO - mark Sone as bad. */
					logger.log(Level.WARNING, String.format("Downloaded reply for Sone %s with invalid time: %s", sone, replyTime));
					return null;
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
					return null;
				}
				Album parent = sone.getRootAlbum();
				if (parentId != null) {
					parent = albums.get(parentId);
					if (parent == null) {
						logger.log(Level.WARNING, String.format("Downloaded Sone %s has album with invalid parent!", sone));
						return null;
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
							return null;
						}
						long creationTime = Numbers.safeParseLong(imageCreationTimeString, 0L);
						int imageWidth = Numbers.safeParseInteger(imageWidthString, 0);
						int imageHeight = Numbers.safeParseInteger(imageHeightString, 0);
						if ((imageWidth < 1) || (imageHeight < 1)) {
							logger.log(Level.WARNING, String.format("Downloaded Sone %s contains image %s with invalid dimensions (%s, %s)!", sone, imageId, imageWidthString, imageHeightString));
							return null;
						}
						Image image = album.newImageBuilder().withId(imageId).at(imageKey).created(creationTime).sized(imageWidth, imageHeight).build(Optional.<ImageCreated>absent());
						image = image.modify().setTitle(imageTitle).setDescription(imageDescription).update();
					}
				}
				album.modify().setAlbumImage(albumImageId).update();
			}
		}

		/* process avatar. */
		if (avatarId != null) {
			profile.setAvatar(core.getImage(avatarId).orNull());
		}

		/* okay, apparently everything was parsed correctly. Now import. */
		sone.setProfile(profile);
		sone.setPosts(posts);
		sone.setReplies(replies);
		sone.setLikePostIds(likedPostIds);
		sone.setLikeReplyIds(likedReplyIds);

		return sone;
	}
}
