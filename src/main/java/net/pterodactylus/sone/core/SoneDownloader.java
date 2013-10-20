/*
 * Sone - SoneDownloader.java - Copyright © 2010–2013 David Roden
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

import static net.pterodactylus.sone.data.Sone.TO_FREENET_URI;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pterodactylus.sone.core.FreenetInterface.Fetched;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.Sone.SoneStatus;
import net.pterodactylus.util.io.Closer;
import net.pterodactylus.util.logging.Logging;
import net.pterodactylus.util.service.AbstractService;

import freenet.client.FetchResult;
import freenet.keys.FreenetURI;
import freenet.support.api.Bucket;

/**
 * The Sone downloader is responsible for download Sones as they are updated.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneDownloader extends AbstractService {

	/** The logger. */
	private static final Logger logger = Logging.getLogger(SoneDownloader.class);

	/** The maximum protocol version. */
	private static final int MAX_PROTOCOL_VERSION = 0;

	/** The core. */
	private final Core core;

	/** The Freenet interface. */
	private final FreenetInterface freenetInterface;

	/** The sones to update. */
	private final Set<Sone> sones = new HashSet<Sone>();

	/**
	 * Creates a new Sone downloader.
	 *
	 * @param core
	 *            The core
	 * @param freenetInterface
	 *            The Freenet interface
	 */
	public SoneDownloader(Core core, FreenetInterface freenetInterface) {
		super("Sone Downloader", false);
		this.core = core;
		this.freenetInterface = freenetInterface;
	}

	//
	// ACTIONS
	//

	/**
	 * Adds the given Sone to the set of Sones that will be watched for updates.
	 *
	 * @param sone
	 *            The Sone to add
	 */
	public void addSone(Sone sone) {
		if (!sones.add(sone)) {
			freenetInterface.unregisterUsk(sone);
		}
		freenetInterface.registerUsk(sone, this);
	}

	/**
	 * Removes the given Sone from the downloader.
	 *
	 * @param sone
	 *            The Sone to stop watching
	 */
	public void removeSone(Sone sone) {
		if (sones.remove(sone)) {
			freenetInterface.unregisterUsk(sone);
		}
	}

	/**
	 * Fetches the updated Sone. This method is a callback method for
	 * {@link FreenetInterface#registerUsk(Sone, SoneDownloader)}.
	 *
	 * @param sone
	 *            The Sone to fetch
	 */
	public void fetchSone(Sone sone) {
		fetchSone(sone, TO_FREENET_URI.apply(sone).sskForUSK());
	}

	/**
	 * Fetches the updated Sone. This method can be used to fetch a Sone from a
	 * specific URI.
	 *
	 * @param sone
	 *            The Sone to fetch
	 * @param soneUri
	 *            The URI to fetch the Sone from
	 */
	public void fetchSone(Sone sone, FreenetURI soneUri) {
		fetchSone(sone, soneUri, false);
	}

	/**
	 * Fetches the Sone from the given URI.
	 *
	 * @param sone
	 *            The Sone to fetch
	 * @param soneUri
	 *            The URI of the Sone to fetch
	 * @param fetchOnly
	 *            {@code true} to only fetch and parse the Sone, {@code false}
	 *            to {@link Core#updateSone(Sone) update} it in the core
	 * @return The downloaded Sone, or {@code null} if the Sone could not be
	 *         downloaded
	 */
	public Sone fetchSone(Sone sone, FreenetURI soneUri, boolean fetchOnly) {
		logger.log(Level.FINE, String.format("Starting fetch for Sone “%s” from %s…", sone, soneUri));
		FreenetURI requestUri = soneUri.setMetaString(new String[] { "sone.xml" });
		sone.setStatus(SoneStatus.downloading);
		try {
			Fetched fetchResults = freenetInterface.fetchUri(requestUri);
			if (fetchResults == null) {
				/* TODO - mark Sone as bad. */
				return null;
			}
			logger.log(Level.FINEST, String.format("Got %d bytes back.", fetchResults.getFetchResult().size()));
			Sone parsedSone = parseSone(sone, fetchResults.getFetchResult(), fetchResults.getFreenetUri());
			if (parsedSone != null) {
				if (!fetchOnly) {
					parsedSone.setStatus((parsedSone.getTime() == 0) ? SoneStatus.unknown : SoneStatus.idle);
					core.updateSone(parsedSone);
					addSone(parsedSone);
				}
			}
			return parsedSone;
		} finally {
			sone.setStatus((sone.getTime() == 0) ? SoneStatus.unknown : SoneStatus.idle);
		}
	}

	/**
	 * Parses a Sone from a fetch result.
	 *
	 * @param originalSone
	 *            The sone to parse, or {@code null} if the Sone is yet unknown
	 * @param fetchResult
	 *            The fetch result
	 * @param requestUri
	 *            The requested URI
	 * @return The parsed Sone, or {@code null} if the Sone could not be parsed
	 */
	public Sone parseSone(Sone originalSone, FetchResult fetchResult, FreenetURI requestUri) {
		logger.log(Level.FINEST, String.format("Parsing FetchResult (%d bytes, %s) for %s…", fetchResult.size(), fetchResult.getMimeType(), originalSone));
		Bucket soneBucket = fetchResult.asBucket();
		InputStream soneInputStream = null;
		try {
			soneInputStream = soneBucket.getInputStream();
			Sone parsedSone = parseSone(originalSone, soneInputStream);
			if (parsedSone != null) {
				parsedSone.modify().setLatestEdition(requestUri.getEdition()).update();
			}
			return parsedSone;
		} catch (Exception e1) {
			logger.log(Level.WARNING, String.format("Could not parse Sone from %s!", requestUri), e1);
		} finally {
			Closer.close(soneInputStream);
			soneBucket.free();
		}
		return null;
	}

	/**
	 * Parses a Sone from the given input stream and creates a new Sone from the
	 * parsed data.
	 *
	 * @param originalSone
	 *            The Sone to update
	 * @param soneInputStream
	 *            The input stream to parse the Sone from
	 * @return The parsed Sone
	 * @throws SoneException
	 *             if a parse error occurs, or the protocol is invalid
	 */
	public Sone parseSone(Sone originalSone, InputStream soneInputStream) throws SoneException {
		return new SoneParser().parseSone(core.getDatabase(), originalSone, soneInputStream);
	}

	//
	// SERVICE METHODS
	//

	@Override
	protected void serviceStop() {
		for (Sone sone : sones) {
			freenetInterface.unregisterUsk(sone);
		}
	}

}
