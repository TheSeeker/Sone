package net.pterodactylus.sone.data.impl;

import static com.google.common.base.Preconditions.checkState;

import net.pterodactylus.sone.database.SoneBuilder;
import net.pterodactylus.sone.freenet.wot.Identity;

import com.google.common.base.Preconditions;
import com.google.inject.internal.util.$Preconditions;

/**
 * Abstract {@link SoneBuilder} implementation.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public abstract class AbstractSoneBuilder implements SoneBuilder {

	protected String id;
	protected boolean local;

	@Override
	public SoneBuilder by(String id) {
		this.id = id;
		return this;
	}

	@Override
	public SoneBuilder local() {
		local = true;
		return this;
	}

	protected void validate() throws IllegalStateException {
		checkState(id != null, "id must not be null");
	}

}
