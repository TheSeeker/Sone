/*
 * Sone - IdentityManager.java - Copyright © 2010–2013 David Roden
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

package net.pterodactylus.sone.freenet.wot;

import static com.google.common.collect.HashMultimap.create;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pterodactylus.sone.freenet.plugin.PluginException;
import net.pterodactylus.sone.freenet.wot.IdentityChangeDetector.IdentityProcessor;
import net.pterodactylus.sone.freenet.wot.event.IdentityAddedEvent;
import net.pterodactylus.sone.freenet.wot.event.IdentityRemovedEvent;
import net.pterodactylus.sone.freenet.wot.event.IdentityUpdatedEvent;
import net.pterodactylus.sone.freenet.wot.event.OwnIdentityAddedEvent;
import net.pterodactylus.sone.freenet.wot.event.OwnIdentityRemovedEvent;
import net.pterodactylus.util.logging.Logging;
import net.pterodactylus.util.service.AbstractService;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * The identity manager takes care of loading and storing identities, their
 * contexts, and properties. It does so in a way that does not expose errors via
 * exceptions but it only logs them and tries to return sensible defaults.
 * <p/>
 * It is also responsible for polling identities from the Web of Trust plugin
 * and sending events to the {@link EventBus} when {@link Identity}s and {@link
 * OwnIdentity}s are discovered or disappearing.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class IdentityManager extends AbstractService {

	/** The logger. */
	private static final Logger logger = Logging.getLogger(IdentityManager.class);

	/** The event bus. */
	private final EventBus eventBus;

	/** The Web of Trust connector. */
	private final WebOfTrustConnector webOfTrustConnector;

	/** The context to filter for. */
	private final String context;

	/** The currently known own identities. */
	/* synchronize access on syncObject. */
	private final Set<OwnIdentity> currentOwnIdentities = Sets.newHashSet();

	/**
	 * Creates a new identity manager.
	 *
	 * @param eventBus
	 * 		The event bus
	 * @param webOfTrustConnector
	 * 		The Web of Trust connector
	 * @param context
	 * 		The context to focus on (may be {@code null} to ignore contexts)
	 */
	@Inject
	public IdentityManager(EventBus eventBus, WebOfTrustConnector webOfTrustConnector, @Named("WebOfTrustContext") String context) {
		super("Sone Identity Manager", false);
		this.eventBus = eventBus;
		this.webOfTrustConnector = webOfTrustConnector;
		this.context = context;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns whether the Web of Trust plugin could be reached during the last
	 * try.
	 *
	 * @return {@code true} if the Web of Trust plugin is connected, {@code false}
	 *         otherwise
	 */
	public boolean isConnected() {
		try {
			webOfTrustConnector.ping();
			return true;
		} catch (PluginException pe1) {
			/* not connected, ignore. */
			return false;
		}
	}

	/**
	 * Returns all own identities.
	 *
	 * @return All own identities
	 */
	public Set<OwnIdentity> getAllOwnIdentities() {
		synchronized (currentOwnIdentities) {
			return new HashSet<OwnIdentity>(currentOwnIdentities);
		}
	}

	//
	// SERVICE METHODS
	//

	@Override
	protected void serviceRun() {
		Multimap<OwnIdentity, Identity> oldIdentities = create();

		while (!shouldStop()) {
			try {
				Collection<OwnIdentity> currentOwnIdentities = webOfTrustConnector.loadAllOwnIdentities();
				Multimap<OwnIdentity, Identity> currentIdentities = loadTrustedIdentitiesForOwnIdentities(currentOwnIdentities);

				detectChangesInIdentities(currentOwnIdentities, currentIdentities, oldIdentities);
				oldIdentities = currentIdentities;

				synchronized (currentOwnIdentities) {
					this.currentOwnIdentities.clear();
					this.currentOwnIdentities.addAll(currentOwnIdentities);
				}
			} catch (WebOfTrustException wote1) {
				logger.log(Level.WARNING, "WoT has disappeared!", wote1);
			}

			/* wait a minute before checking again. */
			sleep(60 * 1000);
		}
	}

	private void detectChangesInIdentities(Collection<OwnIdentity> currentOwnIdentities, Multimap<OwnIdentity, Identity> newIdentities, Multimap<OwnIdentity, Identity> oldIdentities) {
		IdentityChangeDetector identityChangeDetector = new IdentityChangeDetector(getAllOwnIdentities());
		identityChangeDetector.onNewIdentity(addNewOwnIdentityAndItsTrustedIdentities(newIdentities));
		identityChangeDetector.onRemovedIdentity(removeOwnIdentityAndItsTrustedIdentities(oldIdentities));
		identityChangeDetector.onUnchangedIdentity(detectChangesInTrustedIdentities(newIdentities, oldIdentities));
		identityChangeDetector.detectChanges(currentOwnIdentities);
	}

	private IdentityProcessor detectChangesInTrustedIdentities(Multimap<OwnIdentity, Identity> newIdentities, Multimap<OwnIdentity, Identity> oldIdentities) {
		return new DefaultIdentityProcessor(oldIdentities, newIdentities);
	}

	private IdentityProcessor removeOwnIdentityAndItsTrustedIdentities(final Multimap<OwnIdentity, Identity> oldIdentities) {
		return new IdentityProcessor() {
			@Override
			public void processIdentity(Identity identity) {
				eventBus.post(new OwnIdentityRemovedEvent((OwnIdentity) identity));
				for (Identity removedIdentity : oldIdentities.get((OwnIdentity) identity)) {
					eventBus.post(new IdentityRemovedEvent((OwnIdentity) identity, removedIdentity));
				}
			}
		};
	}

	private IdentityProcessor addNewOwnIdentityAndItsTrustedIdentities(final Multimap<OwnIdentity, Identity> newIdentities) {
		return new IdentityProcessor() {
			@Override
			public void processIdentity(Identity identity) {
				eventBus.post(new OwnIdentityAddedEvent((OwnIdentity) identity));
				for (Identity newIdentity : newIdentities.get((OwnIdentity) identity)) {
					eventBus.post(new IdentityAddedEvent((OwnIdentity) identity, newIdentity));
				}
			}
		};
	}

	private Multimap<OwnIdentity, Identity> loadTrustedIdentitiesForOwnIdentities(Collection<OwnIdentity> ownIdentities) throws PluginException {
		Multimap<OwnIdentity, Identity> currentIdentities = create();

		for (OwnIdentity ownIdentity : ownIdentities) {
			if ((context != null) && !ownIdentity.hasContext(context)) {
				continue;
			}

			logger.finer(String.format("Getting trusted identities for %s...", ownIdentity.getId()));
			Set<Identity> trustedIdentities = webOfTrustConnector.loadTrustedIdentities(ownIdentity, context);
			logger.finest(String.format("Got %d trusted identities.", trustedIdentities.size()));
			currentIdentities.putAll(ownIdentity, trustedIdentities);
		}

		return currentIdentities;
	}

	private class DefaultIdentityProcessor implements IdentityProcessor {

		private final Multimap<OwnIdentity, Identity> oldIdentities;
		private final Multimap<OwnIdentity, Identity> newIdentities;

		public DefaultIdentityProcessor(Multimap<OwnIdentity, Identity> oldIdentities, Multimap<OwnIdentity, Identity> newIdentities) {
			this.oldIdentities = oldIdentities;
			this.newIdentities = newIdentities;
		}

		@Override
		public void processIdentity(Identity ownIdentity) {
			IdentityChangeDetector identityChangeDetector = new IdentityChangeDetector(oldIdentities.get((OwnIdentity) ownIdentity));
			identityChangeDetector.onNewIdentity(notifyForAddedIdentities((OwnIdentity) ownIdentity));
			identityChangeDetector.onRemovedIdentity(notifyForRemovedIdentities((OwnIdentity) ownIdentity));
			identityChangeDetector.onChangedIdentity(notifyForChangedIdentities((OwnIdentity) ownIdentity));
			identityChangeDetector.detectChanges(newIdentities.get((OwnIdentity) ownIdentity));
		}

		private IdentityProcessor notifyForChangedIdentities(final OwnIdentity ownIdentity) {
			return new IdentityProcessor() {
				@Override
				public void processIdentity(Identity identity) {
					eventBus.post(new IdentityUpdatedEvent(ownIdentity, identity));
				}
			};
		}

		private IdentityProcessor notifyForRemovedIdentities(final OwnIdentity ownIdentity) {
			return new IdentityProcessor() {
				@Override
				public void processIdentity(Identity identity) {
					eventBus.post(new IdentityRemovedEvent(ownIdentity, identity));
				}
			};
		}

		private IdentityProcessor notifyForAddedIdentities(final OwnIdentity ownIdentity) {
			return new IdentityProcessor() {
				@Override
				public void processIdentity(Identity identity) {
					eventBus.post(new IdentityAddedEvent(ownIdentity, identity));
				}
			};
		}

	}

}
