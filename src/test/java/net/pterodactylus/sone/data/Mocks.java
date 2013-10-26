/*
 * Sone - Mocks.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.data;

import static com.google.common.base.Optional.of;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.data.impl.DefaultPostBuilder;
import net.pterodactylus.sone.database.Database;

import com.google.common.base.Optional;

/**
 * Mocks reusable in multiple tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Mocks {

	public static Core mockCore(Database database) {
		Core core = mock(Core.class);
		when(core.getDatabase()).thenReturn(database);
		when(core.getSone(anyString())).thenReturn(Optional.<Sone>absent());
		return core;
	}

	public static Database mockDatabase() {
		Database database = mock(Database.class);
		when(database.getSone(anyString())).thenReturn(Optional.<Sone>absent());
		return database;
	}

	public static Sone mockLocalSone(Core core, String id) {
		Sone sone = mock(Sone.class);
		when(sone.getId()).thenReturn(id);
		when(sone.isLocal()).thenReturn(true);
		Database database = core.getDatabase();
		when(sone.newPostBuilder()).thenReturn(new DefaultPostBuilder(database, id));
		when(core.getSone(eq(id))).thenReturn(of(sone));
		when(database.getSone(eq(id))).thenReturn(of(sone));
		return sone;
	}

}
