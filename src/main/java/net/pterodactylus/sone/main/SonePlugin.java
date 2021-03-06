/*
 * Sone - SonePlugin.java - Copyright © 2010–2013 David Roden
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

package net.pterodactylus.sone.main;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.core.FreenetInterface;
import net.pterodactylus.sone.core.WebOfTrustUpdater;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.PostBuilderFactory;
import net.pterodactylus.sone.database.PostProvider;
import net.pterodactylus.sone.database.PostReplyBuilderFactory;
import net.pterodactylus.sone.database.SoneProvider;
import net.pterodactylus.sone.database.memory.MemoryDatabase;
import net.pterodactylus.sone.fcp.FcpInterface;
import net.pterodactylus.sone.freenet.PluginStoreConfigurationBackend;
import net.pterodactylus.sone.freenet.plugin.PluginConnector;
import net.pterodactylus.sone.freenet.wot.IdentityManager;
import net.pterodactylus.sone.freenet.wot.WebOfTrustConnector;
import net.pterodactylus.sone.web.WebInterface;
import net.pterodactylus.util.config.Configuration;
import net.pterodactylus.util.config.ConfigurationException;
import net.pterodactylus.util.config.MapConfigurationBackend;
import net.pterodactylus.util.logging.Logging;
import net.pterodactylus.util.logging.LoggingListener;
import net.pterodactylus.util.version.Version;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import freenet.client.async.DatabaseDisabledException;
import freenet.l10n.BaseL10n.LANGUAGE;
import freenet.l10n.PluginL10n;
import freenet.node.Node;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginBaseL10n;
import freenet.pluginmanager.FredPluginFCP;
import freenet.pluginmanager.FredPluginL10n;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginReplySender;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;

/**
 * This class interfaces with Freenet. It is the class that is loaded by the
 * node and starts up the whole Sone system.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SonePlugin implements FredPlugin, FredPluginFCP, FredPluginL10n, FredPluginBaseL10n, FredPluginThreadless, FredPluginVersioned {

	static {
		/* initialize logging. */
		Logging.setup("sone");
		Logging.addLoggingListener(new LoggingListener() {

			@Override
			public void logged(LogRecord logRecord) {
				Class<?> loggerClass = Logging.getLoggerClass(logRecord.getLoggerName());
				int recordLevel = logRecord.getLevel().intValue();
				if (recordLevel < Level.FINE.intValue()) {
					freenet.support.Logger.debug(loggerClass, logRecord.getMessage(), logRecord.getThrown());
				} else if (recordLevel < Level.INFO.intValue()) {
					freenet.support.Logger.minor(loggerClass, logRecord.getMessage(), logRecord.getThrown());
				} else if (recordLevel < Level.WARNING.intValue()) {
					freenet.support.Logger.normal(loggerClass, logRecord.getMessage(), logRecord.getThrown());
				} else if (recordLevel < Level.SEVERE.intValue()) {
					freenet.support.Logger.warning(loggerClass, logRecord.getMessage(), logRecord.getThrown());
				} else {
					freenet.support.Logger.error(loggerClass, logRecord.getMessage(), logRecord.getThrown());
				}
			}
		});
	}

	/** The version. */
	public static final Version VERSION = new Version(0, 8, 9);

	/** The logger. */
	private static final Logger logger = Logging.getLogger(SonePlugin.class);

	/** The plugin respirator. */
	private PluginRespirator pluginRespirator;

	/** The core. */
	private Core core;

	/** The web interface. */
	private WebInterface webInterface;

	/** The FCP interface. */
	private FcpInterface fcpInterface;

	/** The l10n helper. */
	private PluginL10n l10n;

	/** The web of trust connector. */
	private WebOfTrustConnector webOfTrustConnector;

	//
	// ACCESSORS
	//

	/**
	 * Returns the plugin respirator for this plugin.
	 *
	 * @return The plugin respirator
	 */
	public PluginRespirator pluginRespirator() {
		return pluginRespirator;
	}

	/**
	 * Returns the core started by this plugin.
	 *
	 * @return The core
	 */
	public Core core() {
		return core;
	}

	/**
	 * Returns the plugin’s l10n helper.
	 *
	 * @return The plugin’s l10n helper
	 */
	public PluginL10n l10n() {
		return l10n;
	}

	//
	// FREDPLUGIN METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runPlugin(PluginRespirator pluginRespirator) {
		this.pluginRespirator = pluginRespirator;

		/* create a configuration. */
		Configuration oldConfiguration;
		Configuration newConfiguration = null;
		boolean firstStart = !new File("sone.properties").exists();
		boolean newConfig = false;
		try {
			oldConfiguration = new Configuration(new MapConfigurationBackend(new File("sone.properties"), false));
			newConfiguration = oldConfiguration;
		} catch (ConfigurationException ce1) {
			newConfig = true;
			logger.log(Level.INFO, "Could not load configuration file, trying plugin store…", ce1);
			try {
				newConfiguration = new Configuration(new MapConfigurationBackend(new File("sone.properties"), true));
				logger.log(Level.INFO, "Created new configuration file.");
			} catch (ConfigurationException ce2) {
				logger.log(Level.SEVERE, "Could not create configuration file, using Plugin Store!", ce2);
			}
			try {
				oldConfiguration = new Configuration(new PluginStoreConfigurationBackend(pluginRespirator));
				logger.log(Level.INFO, "Plugin store loaded.");
			} catch (DatabaseDisabledException dde1) {
				logger.log(Level.SEVERE, "Could not load any configuration, using empty configuration!");
				oldConfiguration = new Configuration(new MapConfigurationBackend());
			}
		}

		final Configuration startConfiguration = oldConfiguration;
		final EventBus eventBus = new EventBus();

		/* Freenet injector configuration. */
		AbstractModule freenetModule = new AbstractModule() {

			@Override
			@SuppressWarnings("synthetic-access")
			protected void configure() {
				bind(PluginRespirator.class).toInstance(SonePlugin.this.pluginRespirator);
				bind(Node.class).toInstance(SonePlugin.this.pluginRespirator.getNode());
			}
		};
		/* Sone injector configuration. */
		AbstractModule soneModule = new AbstractModule() {

			@Override
			protected void configure() {
				bind(Core.class).in(Singleton.class);
				bind(MemoryDatabase.class).in(Singleton.class);
				bind(EventBus.class).toInstance(eventBus);
				bind(Configuration.class).toInstance(startConfiguration);
				bind(FreenetInterface.class).in(Singleton.class);
				bind(PluginConnector.class).in(Singleton.class);
				bind(WebOfTrustConnector.class).in(Singleton.class);
				bind(WebOfTrustUpdater.class).in(Singleton.class);
				bind(IdentityManager.class).in(Singleton.class);
				bind(String.class).annotatedWith(Names.named("WebOfTrustContext")).toInstance("Sone");
				bind(SonePlugin.class).toInstance(SonePlugin.this);
				bind(FcpInterface.class).in(Singleton.class);
				bind(Database.class).to(MemoryDatabase.class);
				bind(PostBuilderFactory.class).to(MemoryDatabase.class);
				bind(PostReplyBuilderFactory.class).to(MemoryDatabase.class);
				bind(SoneProvider.class).to(Core.class).in(Singleton.class);
				bind(PostProvider.class).to(MemoryDatabase.class);
				bindListener(Matchers.any(), new TypeListener() {

					@Override
					public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
						typeEncounter.register(new InjectionListener<I>() {

							@Override
							public void afterInjection(I injectee) {
								eventBus.register(injectee);
							}
						});
					}
				});
			}

		};
		Injector injector = Guice.createInjector(freenetModule, soneModule);
		core = injector.getInstance(Core.class);

		/* create web of trust connector. */
		webOfTrustConnector = injector.getInstance(WebOfTrustConnector.class);

		/* create FCP interface. */
		fcpInterface = injector.getInstance(FcpInterface.class);
		core.setFcpInterface(fcpInterface);

		/* create the web interface. */
		webInterface = injector.getInstance(WebInterface.class);

		boolean startupFailed = true;
		try {

			/* start core! */
			core.start();
			if ((newConfiguration != null) && (oldConfiguration != newConfiguration)) {
				logger.log(Level.INFO, "Setting configuration to file-based configuration.");
				core.setConfiguration(newConfiguration);
			}
			webInterface.start();
			webInterface.setFirstStart(firstStart);
			webInterface.setNewConfig(newConfig);
			startupFailed = false;
		} finally {
			if (startupFailed) {
				/*
				 * we let the exception bubble up but shut the logging down so
				 * that the logfile is not swamped by the installed logging
				 * handlers of the failed instances.
				 */
				Logging.shutdown();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void terminate() {
		try {
			/* stop the web interface. */
			webInterface.stop();

			/* stop the core. */
			core.stop();

			/* stop the web of trust connector. */
			webOfTrustConnector.stop();
		} catch (Throwable t1) {
			logger.log(Level.SEVERE, "Error while shutting down!", t1);
		} finally {
			/* shutdown logger. */
			Logging.shutdown();
		}
	}

	//
	// INTERFACE FredPluginFCP
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handle(PluginReplySender pluginReplySender, SimpleFieldSet parameters, Bucket data, int accessType) {
		fcpInterface.handle(pluginReplySender, parameters, data, accessType);
	}

	//
	// INTERFACE FredPluginL10n
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getString(String key) {
		return l10n.getBase().getString(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLanguage(LANGUAGE newLanguage) {
		l10n = new PluginL10n(this, newLanguage);
	}

	//
	// INTERFACE FredPluginBaseL10n
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getL10nFilesBasePath() {
		return "i18n";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getL10nFilesMask() {
		return "sone.${lang}.properties";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getL10nOverrideFilesMask() {
		return "sone.${lang}.override.properties";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ClassLoader getPluginClassLoader() {
		return SonePlugin.class.getClassLoader();
	}

	//
	// INTERFACE FredPluginVersioned
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getVersion() {
		return VERSION.toString();
	}

}
