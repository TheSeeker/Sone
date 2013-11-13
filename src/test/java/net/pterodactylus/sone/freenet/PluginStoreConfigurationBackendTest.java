/*
 * Sone - PluginStoreConfigurationBackendTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.freenet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.pterodactylus.util.config.ConfigurationException;

import freenet.client.async.DatabaseDisabledException;
import freenet.pluginmanager.PluginRespirator;
import freenet.pluginmanager.PluginStore;

import org.junit.Test;

/**
 * Unit test for {@link PluginStoreConfigurationBackend}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class PluginStoreConfigurationBackendTest {

	private final PluginRespirator pluginRespirator = mock(PluginRespirator.class);
	private final PluginStore pluginStore = new PluginStore();
	private final PluginStoreConfigurationBackend pluginStoreConfigurationBackend;

	public PluginStoreConfigurationBackendTest() throws DatabaseDisabledException {
		when(pluginRespirator.getStore()).thenReturn(pluginStore);
		pluginStoreConfigurationBackend = new PluginStoreConfigurationBackend(pluginRespirator);
	}

	@Test(expected = DatabaseDisabledException.class)
	public void notGettingAPluginStoreResultsInAnException() throws DatabaseDisabledException {
		PluginRespirator pluginRespirator = mock(PluginRespirator.class);
		new PluginStoreConfigurationBackend(pluginRespirator);
	}

	@Test
	public void gettingAnExistingStringValueRetunsTheValue() throws ConfigurationException {
		pluginStore.strings.put("Key", "Value");
		String value = pluginStoreConfigurationBackend.getValue("Key");
		assertThat(value, is("Value"));
	}

	@Test(expected = ConfigurationException.class)
	public void gettingANonExistingStringValueThrowsAnException() throws ConfigurationException {
		pluginStoreConfigurationBackend.getValue("Key");
	}

	@Test
	public void storingAStringValue() throws ConfigurationException {
		pluginStoreConfigurationBackend.putValue("Key", "Value");
		assertThat(pluginStore.strings, hasEntry("Key", "Value"));
	}

	@Test
	public void gettingAnExistingBooleanReturnsTheCorrectValue() throws ConfigurationException {
		pluginStore.booleans.put("Key", true);
		boolean value = pluginStoreConfigurationBackend.getBooleanValue("Key");
		assertThat(value, is(true));
	}

	@Test(expected = ConfigurationException.class)
	public void gettingANonExistingBooleanThrowsAnException() throws ConfigurationException {
		pluginStoreConfigurationBackend.getBooleanValue("Key");
	}

	@Test
	public void storingABooleanValue() throws ConfigurationException {
		pluginStoreConfigurationBackend.setBooleanValue("Key", true);
		assertThat(pluginStore.booleans, hasEntry("Key", true));
	}

	@Test
	public void gettingAnExistingAndCorrectlyFormattedStringAsDoubleReturnsTheCorrectValue() throws ConfigurationException {
		pluginStore.strings.put("Key", "1.234");
		double value = pluginStoreConfigurationBackend.getDoubleValue("Key");
		assertThat(value, is(1.234));
	}

	@Test(expected = ConfigurationException.class)
	public void gettingANonExistingStringAsDoubleCausesAnError() throws ConfigurationException {
		pluginStoreConfigurationBackend.getDoubleValue("Key");
	}

	@Test(expected = ConfigurationException.class)
	public void gettingAnExistingButNotCorrectlyFormattedStringAsDoubleValueCausesAnError() throws ConfigurationException {
		pluginStore.strings.put("Key", "foo");
		pluginStoreConfigurationBackend.getDoubleValue("Key");
	}

	@Test
	public void gettingANullStringAsDoubleValueReturnsNull() throws ConfigurationException {
		pluginStore.strings.put("Key", null);
		Double value = pluginStoreConfigurationBackend.getDoubleValue("Key");
		assertThat(value, nullValue());
	}

	@Test
	public void storingADoubleValue() throws ConfigurationException {
		pluginStoreConfigurationBackend.setDoubleValue("Key", 1.234);
		assertThat(pluginStore.strings, hasEntry("Key", "1.234"));
	}

	@Test
	public void gettingAnIntegerValueReturnsTheCorrectValue() throws ConfigurationException {
		pluginStore.integers.put("Key", 17);
		int value = pluginStoreConfigurationBackend.getIntegerValue("Key");
		assertThat(value, is(17));
	}

	@Test(expected = ConfigurationException.class)
	public void gettingANonExistingIntegerValueCausesAnError() throws ConfigurationException {
		pluginStoreConfigurationBackend.getIntegerValue("Key");
	}

	@Test
	public void storingAnIntegerValue() throws ConfigurationException {
		pluginStoreConfigurationBackend.setIntegerValue("Key", 17);
		assertThat(pluginStore.integers, hasEntry("Key", 17));
	}

	@Test
	public void gettingALongValueReturnsTheCorrectValue() throws ConfigurationException {
		pluginStore.longs.put("Key", 17L);
		long value = pluginStoreConfigurationBackend.getLongValue("Key");
		assertThat(value, is(17L));
	}

	@Test(expected = ConfigurationException.class)
	public void gettingANonExistingLongValueCausesAnError() throws ConfigurationException {
		pluginStoreConfigurationBackend.getLongValue("Key");
	}

	@Test
	public void storingALongValue() throws ConfigurationException {
		pluginStoreConfigurationBackend.setLongValue("Key", 17L);
		assertThat(pluginStore.longs, hasEntry("Key", 17L));
	}

	@Test
	public void savingStoresThePluginStoreInTheRespirator() throws ConfigurationException, DatabaseDisabledException {
		pluginStoreConfigurationBackend.save();
		verify(pluginRespirator).putStore(eq(pluginStore));
	}

	@Test(expected = ConfigurationException.class)
	public void savingWithADisabledDatabaseCausesAnError() throws DatabaseDisabledException, ConfigurationException {
		doThrow(DatabaseDisabledException.class).when(pluginRespirator).putStore(eq(pluginStore));
		pluginStoreConfigurationBackend.save();
	}

}
