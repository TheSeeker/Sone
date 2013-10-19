package net.pterodactylus.sone.data.impl;

import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.Database;
import net.pterodactylus.sone.database.SoneBuilder;

import com.google.common.base.Optional;

/**
 * {@link SoneBuilder} implementation that can create {@link DefaultSone}s.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DefaultSoneBuilder extends AbstractSoneBuilder {

	private final Database database;

	public DefaultSoneBuilder(Database database) {
		this.database = database;
	}

	@Override
	public Sone build(Optional<SoneCreated> soneCreated) throws IllegalStateException {
		validate();
		Sone sone = new DefaultSone(database, id, local, client);
		if (soneCreated.isPresent()) {
			soneCreated.get().soneCreated(sone);
		}
		return sone;
	}

}
