package net.pterodactylus.sone.database;

import net.pterodactylus.sone.freenet.wot.Identity;

import com.google.common.base.Optional;

/**
 * Database for handling {@link Identity}s.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ’Bombe‘ Roden</a>
 */
public interface IdentityDatabase {

	Optional<Identity> getIdentity(String identityId);
	void storeIdentity(Identity identitiy);

}
