/*
 * Sone - SoneInserter.java - Copyright © 2010–2013 David Roden
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

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.EventBus;
import freenet.keys.FreenetURI;
import freenet.support.SimpleReadOnlyArrayBucket;
import freenet.support.api.ManifestElement;
import freenet.support.api.RandomAccessBucket;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.pterodactylus.sone.core.event.SoneInsertAbortedEvent;
import net.pterodactylus.sone.core.event.SoneInsertedEvent;
import net.pterodactylus.sone.core.event.SoneInsertingEvent;
import net.pterodactylus.sone.data.Album;
import static net.pterodactylus.sone.data.Album.NOT_EMPTY;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Reply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.data.Sone.SoneStatus;
import net.pterodactylus.sone.freenet.StringBucket;
import net.pterodactylus.sone.main.SonePlugin;
import net.pterodactylus.util.io.Closer;
import net.pterodactylus.util.logging.Logging;
import net.pterodactylus.util.service.AbstractService;
import net.pterodactylus.util.template.HtmlFilter;
import net.pterodactylus.util.template.ReflectionAccessor;
import net.pterodactylus.util.template.Template;
import net.pterodactylus.util.template.TemplateContext;
import net.pterodactylus.util.template.TemplateContextFactory;
import net.pterodactylus.util.template.TemplateException;
import net.pterodactylus.util.template.TemplateParser;
import net.pterodactylus.util.template.XmlFilter;

/**
 * A Sone inserter is responsible for inserting a Sone if it has changed.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneInserter extends AbstractService {

	/** The logger. */
	private static final Logger logger = Logging.getLogger(SoneInserter.class);

	/** The insertion delay (in seconds). */
	private static volatile int insertionDelay = 60;

	/** The template factory used to create the templates. */
	private static final TemplateContextFactory templateContextFactory = new TemplateContextFactory();

	static {
		templateContextFactory.addAccessor(Object.class, new ReflectionAccessor());
		templateContextFactory.addFilter("xml", new XmlFilter());
		templateContextFactory.addFilter("html", new HtmlFilter());
	}

	/** The UTF-8 charset. */
	private static final Charset utf8Charset = Charset.forName("UTF-8");

	/** The core. */
	private final Core core;

	/** The event bus. */
	private final EventBus eventBus;

	/** The Freenet interface. */
	private final FreenetInterface freenetInterface;

	/** The Sone to insert. */
	private volatile Sone sone;

	/** Whether a modification has been detected. */
	private volatile boolean modified = false;

	/** The fingerprint of the last insert. */
	private volatile String lastInsertFingerprint;

	/**
	 * Creates a new Sone inserter.
	 *
	 * @param core
	 *            The core
	 * @param eventBus
	 *            The event bus
	 * @param freenetInterface
	 *            The freenet interface
	 * @param sone
	 *            The Sone to insert
	 */
	public SoneInserter(Core core, EventBus eventBus, FreenetInterface freenetInterface, Sone sone) {
		super("Sone Inserter for “" + sone.getName() + "”", false);
		this.core = core;
		this.eventBus = eventBus;
		this.freenetInterface = freenetInterface;
		this.sone = sone;
	}

	//
	// ACCESSORS
	//

	/**
	 * Sets the Sone to insert.
	 *
	 * @param sone
	 * 		The Sone to insert
	 * @return This Sone inserter
	 */
	public SoneInserter setSone(Sone sone) {
		checkArgument((this.sone == null) || sone.equals(this.sone), "Sone to insert can not be set to a different Sone");
		this.sone = sone;
		return this;
	}

	/**
	 * Changes the insertion delay, i.e. the time the Sone inserter waits after it
	 * has noticed a Sone modification before it starts the insert.
	 *
	 * @param insertionDelay
	 *            The insertion delay (in seconds)
	 */
	public static void setInsertionDelay(int insertionDelay) {
		SoneInserter.insertionDelay = insertionDelay;
	}

	/**
	 * Returns the fingerprint of the last insert.
	 *
	 * @return The fingerprint of the last insert
	 */
	public String getLastInsertFingerprint() {
		return lastInsertFingerprint;
	}

	/**
	 * Sets the fingerprint of the last insert.
	 *
	 * @param lastInsertFingerprint
	 *            The fingerprint of the last insert
	 */
	public void setLastInsertFingerprint(String lastInsertFingerprint) {
		this.lastInsertFingerprint = lastInsertFingerprint;
	}

	/**
	 * Returns whether the Sone inserter has detected a modification of the
	 * Sone.
	 *
	 * @return {@code true} if the Sone has been modified, {@code false}
	 *         otherwise
	 */
	public boolean isModified() {
		return modified;
	}

	//
	// SERVICE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void serviceRun() {
		long lastModificationTime = 0;
		String lastInsertedFingerprint = lastInsertFingerprint;
		String lastFingerprint = "";
		Sone sone;
		while (!shouldStop()) {
			try {
				/* check every seconds. */
				sleep(1000);

				/* don’t insert locked Sones. */
				sone = this.sone;
				if (core.isLocked(sone)) {
					/* trigger redetection when the Sone is unlocked. */
					synchronized (sone) {
						modified = !sone.getFingerprint().equals(lastInsertedFingerprint);
					}
					lastFingerprint = "";
					lastModificationTime = 0;
					continue;
				}

				InsertInformation insertInformation = null;
				synchronized (sone) {
					String fingerprint = sone.getFingerprint();
					if (!fingerprint.equals(lastFingerprint)) {
						if (fingerprint.equals(lastInsertedFingerprint)) {
							modified = false;
							lastModificationTime = 0;
							logger.log(Level.FINE, String.format("Sone %s has been reverted to last insert state.", sone));
						} else {
							lastModificationTime = System.currentTimeMillis();
							modified = true;
							logger.log(Level.FINE, String.format("Sone %s has been modified, waiting %d seconds before inserting.", sone.getName(), insertionDelay));
						}
						lastFingerprint = fingerprint;
					}
					if (modified && (lastModificationTime > 0) && ((System.currentTimeMillis() - lastModificationTime) > (insertionDelay * 1000))) {
						lastInsertedFingerprint = fingerprint;
						insertInformation = new InsertInformation(sone);
					}
				}

				if (insertInformation != null) {
					logger.log(Level.INFO, String.format("Inserting Sone “%s”…", sone.getName()));

					boolean success = false;
					try {
						sone.setStatus(SoneStatus.inserting);
						long insertTime = System.currentTimeMillis();
						insertInformation.setTime(insertTime);
						eventBus.post(new SoneInsertingEvent(sone));
						FreenetURI finalUri = freenetInterface.insertDirectory(insertInformation.getInsertUri(), insertInformation.generateManifestEntries(), "index.html");
						eventBus.post(new SoneInsertedEvent(sone, System.currentTimeMillis() - insertTime));
						/* at this point we might already be stopped. */
						if (shouldStop()) {
							/* if so, bail out, don’t change anything. */
							break;
						}
						sone.setTime(insertTime);
						sone.setLatestEdition(finalUri.getEdition());
						core.touchConfiguration();
						success = true;
						logger.log(Level.INFO, String.format("Inserted Sone “%s” at %s.", sone.getName(), finalUri));
					} catch (SoneException se1) {
						eventBus.post(new SoneInsertAbortedEvent(sone, se1));
						logger.log(Level.WARNING, String.format("Could not insert Sone “%s”!", sone.getName()), se1);
					} finally {
						sone.setStatus(SoneStatus.idle);
					}

					/*
					 * reset modification counter if Sone has not been modified
					 * while it was inserted.
					 */
					if (success) {
						synchronized (sone) {
							if (lastInsertedFingerprint.equals(sone.getFingerprint())) {
								logger.log(Level.FINE, String.format("Sone “%s” was not modified further, resetting counter…", sone));
								lastModificationTime = 0;
								lastInsertFingerprint = lastInsertedFingerprint;
								core.touchConfiguration();
								modified = false;
							}
						}
					}
				}
			} catch (Throwable t1) {
				logger.log(Level.SEVERE, "SoneInserter threw an Exception!", t1);
			}
		}
	}

	/**
	 * Container for information that are required to insert a Sone. This
	 * container merely exists to copy all relevant data without holding a lock
	 * on the {@link Sone} object for too long.
	 *
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	private class InsertInformation {

		/** All properties of the Sone, copied for thread safety. */
		private final Map<String, Object> soneProperties = new HashMap<String, Object>();

		/**
		 * Creates a new insert information container.
		 *
		 * @param sone
		 *            The sone to insert
		 */
		public InsertInformation(Sone sone) {
			soneProperties.put("id", sone.getId());
			soneProperties.put("name", sone.getName());
			soneProperties.put("time", sone.getTime());
			soneProperties.put("requestUri", sone.getRequestUri());
			soneProperties.put("insertUri", sone.getInsertUri());
			soneProperties.put("profile", sone.getProfile());
			soneProperties.put("posts", Ordering.from(Post.TIME_COMPARATOR).sortedCopy(sone.getPosts()));
			soneProperties.put("replies", Ordering.from(Reply.TIME_COMPARATOR).reverse().sortedCopy(sone.getReplies()));
			soneProperties.put("likedPostIds", new HashSet<String>(sone.getLikedPostIds()));
			soneProperties.put("likedReplyIds", new HashSet<String>(sone.getLikedReplyIds()));
			soneProperties.put("albums", FluentIterable.from(sone.getRootAlbum().getAlbums()).transformAndConcat(Album.FLATTENER).filter(NOT_EMPTY).toList());
		}

		//
		// ACCESSORS
		//

		/**
		 * Returns the insert URI of the Sone.
		 *
		 * @return The insert URI of the Sone
		 */
		public FreenetURI getInsertUri() {
			return (FreenetURI) soneProperties.get("insertUri");
		}

		/**
		 * Sets the time of the Sone at the time of the insert.
		 *
		 * @param time
		 *            The time of the Sone
		 */
		public void setTime(long time) {
			soneProperties.put("time", time);
		}

		//
		// ACTIONS
		//

		/**
		 * Generates all manifest entries required to insert this Sone.
		 *
		 * @return The manifest entries for the Sone insert
		 */
		public HashMap<String, Object> generateManifestEntries() {
			HashMap<String, Object> manifestEntries = new HashMap<String, Object>();

			/* first, create an index.html. */
			manifestEntries.put("index.html", createManifestElement("index.html", "text/html; charset=utf-8", "/templates/insert/index.html"));

			/* now, store the sone. */
			manifestEntries.put("sone.xml", createManifestElement("sone.xml", "text/xml; charset=utf-8", "/templates/insert/sone.xml"));

			return manifestEntries;
		}

		//
		// PRIVATE METHODS
		//

		/**
		 * Creates a new manifest element.
		 *
		 * @param name
		 *            The name of the file
		 * @param contentType
		 *            The content type of the file
		 * @param templateName
		 *            The name of the template to render
		 * @return The manifest element
		 */
		@SuppressWarnings("synthetic-access")
		private ManifestElement createManifestElement(String name, String contentType, String templateName) {
			InputStreamReader templateInputStreamReader = null;
			Template template;
			try {
				templateInputStreamReader = new InputStreamReader(getClass().getResourceAsStream(templateName), utf8Charset);
				template = TemplateParser.parse(templateInputStreamReader);
			} catch (TemplateException te1) {
				logger.log(Level.SEVERE, String.format("Could not parse template “%s”!", templateName), te1);
				return null;
			} finally {
				Closer.close(templateInputStreamReader);
			}

			TemplateContext templateContext = templateContextFactory.createTemplateContext();
			templateContext.set("core", core);
			templateContext.set("currentSone", soneProperties);
			templateContext.set("currentEdition", core.getUpdateChecker().getLatestEdition());
			templateContext.set("version", SonePlugin.VERSION);
			StringWriter writer = new StringWriter();
			RandomAccessBucket bucket = null;
			try {
				template.render(templateContext, writer);
				bucket = new SimpleReadOnlyArrayBucket(writer.toString().getBytes(utf8Charset));
				return new ManifestElement(name, bucket, contentType, bucket.size());
			} catch (TemplateException te1) {
				logger.log(Level.SEVERE, String.format("Could not render template “%s”!", templateName), te1);
				return null;
			} finally {
				Closer.close(writer);
			}
		}

	}

}
