/*
 * Sone - ListNotificationTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.notify;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import net.pterodactylus.util.template.Template;
import net.pterodactylus.util.template.TemplateContext;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link ListNotification}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ListNotificationTest {

	private final TemplateContext initialContext = new TemplateContext();
	private final Template template = mock(Template.class);
	private ListNotification<Object> listNotification;

	@Before
	public void setup() {
		when(template.getInitialContext()).thenReturn(initialContext);
		listNotification = new ListNotification<Object>("id", "key", template);
	}

	@Test
	public void newListNotificationDoesNotContainElements() {
		assertThat(listNotification.getElements(), empty());
		assertThat(listNotification.isEmpty(), is(true));
	}

	@Test
	public void addingAnElement() {
		Object newObject = new Object();
		listNotification.add(newObject);
		assertThat(listNotification.isEmpty(), is(false));
		assertThat(listNotification.getElements(), contains(newObject));
	}

	@Test
	public void removingAnEelement() {
		Object newObject = new Object();
		listNotification.add(newObject);
		listNotification.remove(newObject);
		assertThat(listNotification.getElements(), empty());
		assertThat(listNotification.isEmpty(), is(true));
	}

	@Test
	public void settingElements() {
		Object object1 = new Object();
		Object object2 = new Object();
		Collection<Object> elements = Arrays.asList(object1, object2);
		listNotification.setElements(elements);
		assertThat(listNotification.getElements(), contains(object1, object2));
	}

	@Test
	public void addingElementsChangesHashCode() {
		int emptyListNoficiationHashCode=listNotification.hashCode();
		listNotification.add(new Object());
		assertThat(listNotification.hashCode(), not(is(emptyListNoficiationHashCode)));
	}

	@Test
	public void listNotificationsWithDifferentIdsAreNotEqual() {
		ListNotification<Object> secondListNotification = new ListNotification<Object>("otherId", "key", template);
		assertThat(listNotification, not(is(secondListNotification)));
	}

	@Test
	public void listNotificationsWithDifferentKeysAreNotEqual() {
		ListNotification<Object> secondListNotification = new ListNotification<Object>("id", "otherKey", template);
		assertThat(listNotification, not(is(secondListNotification)));
	}

}
