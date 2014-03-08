package net.pterodactylus.sone.database.memory;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import net.pterodactylus.sone.database.IdentityDatabase;
import net.pterodactylus.sone.freenet.wot.Identity;

import com.google.common.base.Optional;

/**
 * {@link IdentityDatabase} implementation that keeps all {@link Identity}s in
 * memory.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class MemoryIdentityDatabase implements IdentityDatabase {

	private final ReadWriteLock lock;
	private final Map<String, Identity> identities = newHashMap();

	public MemoryIdentityDatabase(ReadWriteLock readWriteLock) {
		this.lock = readWriteLock;
	}

	@Override
	public Optional<Identity> getIdentity(String identityId) {
		lock.readLock().lock();
		try {
			return fromNullable(identities.get(identityId));
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void storeIdentity(Identity identitiy) {
		lock.writeLock().lock();
		try {
			identities.put(identitiy.getId(), identitiy);
		} finally {
			lock.writeLock().unlock();
		}
	}

}
