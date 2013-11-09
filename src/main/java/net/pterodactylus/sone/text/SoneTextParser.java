/*
 * Sone - SoneTextParser.java - Copyright © 2010–2013 David Roden
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

package net.pterodactylus.sone.text;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.util.io.Closer;
import net.pterodactylus.util.logging.Logging;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import freenet.keys.FreenetURI;

/**
 * {@link Parser} implementation that can recognize Freenet URIs.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneTextParser implements Parser<SoneTextParserContext> {

	/** The logger. */
	private static final Logger logger = Logging.getLogger(SoneTextParser.class);

	/** Pattern to detect whitespace. */
	private static final Pattern whitespacePattern = Pattern.compile("[\\u000a\u0020\u00a0\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a\u200b\u200c\u200d\u202f\u205f\u2060\u2800\u3000]");

	/**
	 * Enumeration for all recognized link types.
	 *
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	private enum LinkType {

		KSK("KSK@"),
		CHK("CHK@"),
		SSK("SSK@") {
			@Override
			public boolean isSigned() {
				return true;
			}
		},
		USK("USK@") {
			@Override
			public boolean isSigned() {
				return true;
			}
		},
		HTTP("http://"),
		HTTPS("https://"),
		SONE("sone://"),
		POST("post://");

		private final String scheme;

		private LinkType(String scheme) {
			this.scheme = scheme;
		}

		public String getScheme() {
			return scheme;
		}

		public boolean isSigned() {
			return false;
		}

	}

	private final PartCreators partCreators = new PartCreators();
	private final Database database;

	/**
	 * Creates a new freenet link parser.
	 *
	 * @param database
	 */
	public SoneTextParser(Database database) {
		this.database = database;
	}

	//
	// PART METHODS
	//

	@Override
	public Iterable<Part> parse(SoneTextParserContext context, Reader source) throws IOException {
		PartContainer parts = new PartContainer();
		BufferedReader bufferedReader = (source instanceof BufferedReader) ? (BufferedReader) source : new BufferedReader(source);
		try {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				for (String pieceOfLine : splitLine(line)) {
					parts.add(createPart(pieceOfLine, context));
				}
			}
		} finally {
			if (bufferedReader != source) {
				Closer.close(bufferedReader);
			}
		}
		removeTrailingWhitespaceParts(parts);
		return optimizeParts(parts);
	}

	private Iterable<Part> optimizeParts(PartContainer partContainer) {
		PartContainer parts = new PartContainer();
		boolean firstPart = true;
		Part lastPart = null;
		int emptyLines = 0;
		for (Part part : partContainer) {
			if (firstPart) {
				if ("\n".equals(part.getText())) {
					continue;
				}
				firstPart = false;
			}
			if ("\n".equals(part.getText())) {
				emptyLines++;
				if (emptyLines > 2) {
					continue;
				}
			} else {
				emptyLines = 0;
			}
			if ((lastPart != null) && lastPart.isPlainText() && part.isPlainText()) {
				parts.removePart(parts.size() - 1);
				PlainTextPart combinedPart = new PlainTextPart(lastPart.getText() + part.getText());
				parts.add(combinedPart);
				lastPart = combinedPart;
			} else if ((lastPart != null) && part.isFreenetLink() && lastPart.isPlainText() && lastPart.getText().endsWith("freenet:")) {
				parts.removePart(parts.size() - 1);
				String lastPartText = lastPart.getText();
				lastPartText = lastPartText.substring(0, lastPartText.length() - "freenet:".length());
				if (lastPartText.length() > 0) {
					parts.add(new PlainTextPart(lastPartText));
				}
				lastPart = part;
				parts.add(part);
			} else {
				lastPart = part;
				parts.add(part);
			}
		}
		return parts;
	}

	private Part createPart(String line, SoneTextParserContext context) {
		Optional<Part> linkPart = createLinkPart(line, context);
		return linkPart.or(new PlainTextPart(line));
	}

	private Optional<Part> createLinkPart(String line, SoneTextParserContext context) {
		Optional<NextLink> nextLink = findNextLink(line);
		if (!nextLink.isPresent()) {
			return absent();
		}
		return partCreators.createPart(nextLink.get().getLinkType(), line, context);
	}

	private List<String> splitLine(String line) {
		List<String> linePieces = newArrayList();
		int currentIndex = 0;
		while (currentIndex < line.length()) {
			Optional<NextLink> nextLink = findNextLink(line.substring(currentIndex));
			if (!nextLink.isPresent()) {
				linePieces.add(line.substring(currentIndex));
				break;
			}
			int nextIndex = currentIndex + nextLink.get().getNextIndex();
			if (nextIndex > currentIndex) {
				linePieces.add(line.substring(currentIndex, nextIndex));
			}
			int nextWhitespace = nextIndex + findNextWhitespaceOrEndOfLine(line.substring(nextIndex));
			linePieces.add(line.substring(nextIndex, nextWhitespace));
			currentIndex = nextWhitespace;
		}
		linePieces.add("\n");
		return linePieces;
	}

	private void removeTrailingWhitespaceParts(PartContainer parts) {
		for (int partIndex = parts.size() - 1; partIndex >= 0; --partIndex) {
			Part part = parts.getPart(partIndex);
			if (!(part instanceof PlainTextPart) || !"\n".equals(part.getText())) {
				break;
			}
			parts.removePart(partIndex);
		}
	}

	private boolean linkMatchesPostingSone(SoneTextParserContext context, String link) {
		return (context != null) && (context.getPostingSone() != null) && link.substring(4, Math.min(link.length(), 47)).equals(context.getPostingSone().getId());
	}

	private boolean lineIsLongEnoughToContainAPostLink(String line) {
		return line.length() >= (7 + 36);
	}

	private static boolean lineIsLongEnoughToContainASoneLink(String line) {
		return line.length() >= (7 + 43);
	}

	private int findNextWhitespaceOrEndOfLine(String line) {
		Matcher matcher = whitespacePattern.matcher(line);
		return matcher.find(0) ? matcher.start() : line.length();
	}

	private Optional<NextLink> findNextLink(String line) {
		EnumMap<LinkType, Integer> linkTypeIndexes = new EnumMap<LinkType, Integer>(LinkType.class);
		for (LinkType linkType : LinkType.values()) {
			int index = line.indexOf(linkType.getScheme());
			if (index != -1) {
				linkTypeIndexes.put(linkType, index);
			}
		}
		if (linkTypeIndexes.isEmpty()) {
			return absent();
		}
		Entry<LinkType, Integer> smallestEntry = from(linkTypeIndexes.entrySet()).toSortedList(locateSmallestIndex()).get(0);
		return of(new NextLink(smallestEntry.getValue(), smallestEntry.getKey()));
	}

	private Comparator<Entry<LinkType, Integer>> locateSmallestIndex() {
		return new Comparator<Entry<LinkType, Integer>>() {
			@Override
			public int compare(Entry<LinkType, Integer> leftEntry, Entry<LinkType, Integer> rightEntry) {
				return leftEntry.getValue() - rightEntry.getValue();
			}
		};
	}

	private class PartCreators {

		private final Map<LinkType, PartCreator> partCreators = ImmutableMap.<LinkType, PartCreator>builder()
				.put(LinkType.SONE, new SonePartCreator())
				.put(LinkType.POST, new PostPartCreator())
				.put(LinkType.KSK, new FreenetLinkPartCreator(LinkType.KSK))
				.put(LinkType.CHK, new FreenetLinkPartCreator(LinkType.CHK))
				.put(LinkType.SSK, new FreenetLinkPartCreator(LinkType.SSK))
				.put(LinkType.USK, new FreenetLinkPartCreator(LinkType.USK))
				.put(LinkType.HTTP, new InternetLinkPartCreator(LinkType.HTTP))
				.put(LinkType.HTTPS, new InternetLinkPartCreator(LinkType.HTTPS))
				.build();

		public Optional<Part> createPart(LinkType linkType, String line, SoneTextParserContext context) {
			if (line.equals(linkType.getScheme())) {
				return of((Part) new PlainTextPart(line));
			}
			return partCreators.get(linkType).createPart(line, context);
		}

	}

	private class SonePartCreator implements PartCreator {

		@Override
		public Optional<Part> createPart(String line, SoneTextParserContext context) {
			if (!lineIsLongEnoughToContainASoneLink(line)) {
				return absent();
			}
			String soneId = line.substring(7, 50);
			Optional<Sone> sone = database.getSone(soneId);
			if (!sone.isPresent()) {
				return absent();
			}
			return Optional.<Part>of(new SonePart(sone.get()));
		}

	}

	private class PostPartCreator implements PartCreator {

		@Override
		public Optional<Part> createPart(String line, SoneTextParserContext context) {
			if (!lineIsLongEnoughToContainAPostLink(line)) {
				return absent();
			}
			String postId = line.substring(7, 43);
			Optional<Post> post = database.getPost(postId);
			if (!post.isPresent()) {
				return absent();
			}
			return Optional.<Part>of(new PostPart(post.get()));
		}

	}

	private class FreenetLinkPartCreator implements PartCreator {

		private final LinkType linkType;

		protected FreenetLinkPartCreator(LinkType linkType) {
			this.linkType = linkType;
		}

		@Override
		public Optional<Part> createPart(String link, SoneTextParserContext context) {
			String name = link;
			if (name.indexOf('?') > -1) {
				name = name.substring(0, name.indexOf('?'));
			}
			if (name.endsWith("/")) {
				name = name.substring(0, name.length() - 1);
			}
			try {
				FreenetURI uri = new FreenetURI(name);
				name = uri.lastMetaString();
				if (name == null) {
					name = uri.getDocName();
				}
				if (name == null) {
					name = link.substring(0, Math.min(9, link.length()));
				}
				boolean fromPostingSone = linkType.isSigned() && linkMatchesPostingSone(context, link);
				return Optional.<Part>of(new FreenetLinkPart(link, name, fromPostingSone));
			} catch (MalformedURLException mue1) {
				/* ignore. */
			} catch (NullPointerException npe1) {
				/* ignore. */
			} catch (ArrayIndexOutOfBoundsException aioobe1) {
				/* ignore. */
			}
			return absent();
		}

	}

	private class InternetLinkPartCreator implements PartCreator {

		private final LinkType linkType;

		private InternetLinkPartCreator(LinkType linkType) {
			this.linkType = linkType;
		}

		@Override
		public Optional<Part> createPart(String link, SoneTextParserContext context) {
			String name = link;
			name = link.substring(linkType.getScheme().length());
			int firstSlash = name.indexOf('/');
			int lastSlash = name.lastIndexOf('/');
			if ((lastSlash - firstSlash) > 3) {
				name = name.substring(0, firstSlash + 1) + "…" + name.substring(lastSlash);
			}
			if (name.endsWith("/")) {
				name = name.substring(0, name.length() - 1);
			}
			if (((name.indexOf('/') > -1) && (name.indexOf('.') < name.lastIndexOf('.', name.indexOf('/'))) || ((name.indexOf('/') == -1) && (name.indexOf('.') < name.lastIndexOf('.')))) && name.startsWith("www.")) {
				name = name.substring(4);
			}
			if (name.indexOf('?') > -1) {
				name = name.substring(0, name.indexOf('?'));
			}
			return Optional.<Part>of(new LinkPart(link, name));
		}

	}

	private interface PartCreator {

		Optional<Part> createPart(String line, SoneTextParserContext context);

	}

	/**
	 * Container for position and type of the next link in a line.
	 *
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	private static class NextLink {

		private final int nextIndex;
		private final LinkType linkType;

		private NextLink(int nextIndex, LinkType linkType) {
			this.nextIndex = nextIndex;
			this.linkType = linkType;
		}

		private int getNextIndex() {
			return nextIndex;
		}

		private LinkType getLinkType() {
			return linkType;
		}

	}

}
