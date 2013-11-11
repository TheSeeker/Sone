/*
 * Sone - ImageLinkFilterTest.java - Copyright © 2013 David Roden
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

package net.pterodactylus.sone.template;

import static com.google.common.base.Optional.of;
import static net.pterodactylus.util.template.TemplateContextFactory.getInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import net.pterodactylus.sone.data.Image;
import net.pterodactylus.sone.data.Mocks;
import net.pterodactylus.util.template.HtmlFilter;
import net.pterodactylus.util.template.TemplateContextFactory;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link ImageLinkFilter}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ImageLinkFilterTest {

	private final Mocks mocks = new Mocks();
	private final TemplateContextFactory templateContextFactory = getInstance();
	private final ImageLinkFilter imageLinkFilter = new ImageLinkFilter(mocks.core, templateContextFactory);

	@Before
	public void setup() {
		templateContextFactory.addFilter("css", new CssClassNameFilter());
		templateContextFactory.addFilter("html", new HtmlFilter());
	}

	@Test
	public void nullImageReturnsANullLink() {
		assertThat(imageLinkFilter.format(null, null, Collections.<String, Object>emptyMap()), nullValue());
	}

	@Test
	public void linkForAnImageWithoutAdditionalParametersIsGenerated() {
		Image image = createImage("ImageId", "KSK@foo.png", "Title", "Description", 640, 480);
		String link = (String) imageLinkFilter.format(null, image, Collections.<String, Object>emptyMap());
		assertThat(link, is("<img src=\"/KSK@foo.png?forcedownload=true\" alt=\"Description\" title=\"Title\" width=\"640\" height=\"480\" style=\"position: relative;\"/>"));
	}

	@Test
	public void overridingTheTitleChangesBothAltAndTitleAttributes() {
		Image image = createImage("ImageId", "KSK@foo.png", "Title", "Description", 640, 480);
		String link = (String) imageLinkFilter.format(null, image, ImmutableMap.<String, Object>of("title", "Other Title"));
		assertThat(link, is("<img src=\"/KSK@foo.png?forcedownload=true\" alt=\"Other Title\" title=\"Other Title\" width=\"640\" height=\"480\" style=\"position: relative;\"/>"));
	}

	@Test
	public void classAttributeIsFilteredAndSet() {
		Image image = createImage("ImageId", "KSK@foo.png", "Title", "Description", 640, 480);
		String link = (String) imageLinkFilter.format(null, image, ImmutableMap.<String, Object>of("class", "some class"));
		assertThat(link, is("<img class=\"some_class\" src=\"/KSK@foo.png?forcedownload=true\" alt=\"Description\" title=\"Title\" width=\"640\" height=\"480\" style=\"position: relative;\"/>"));
	}

	@Test
	public void resizingImageToHalfItsSize() {
		Image image = createImage("ImageId", "KSK@foo.png", "Title", "Description", 640, 480);
		String link = (String) imageLinkFilter.format(null, image, ImmutableMap.<String, Object>of("max-width", "320", "max-height", "320"));
		assertThat(link, is("<img src=\"/KSK@foo.png?forcedownload=true\" alt=\"Description\" title=\"Title\" width=\"320\" height=\"240\" style=\"position: relative;\"/>"));
	}

	@Test
	public void enlargingAnImage() {
		Image image = createImage("ImageId", "KSK@foo.png", "Title", "Description", 640, 480);
		String link = (String) imageLinkFilter.format(null, image, ImmutableMap.<String, Object>of("mode", "enlarge", "max-width", "960", "max-height", "960"));
		assertThat(link, is("<img src=\"/KSK@foo.png?forcedownload=true\" alt=\"Description\" title=\"Title\" width=\"1280\" height=\"960\" style=\"position: relative;top: 0px;left: -160px;\"/>"));
	}

	@Test
	public void notInsertedImage() {
		Image image = createNotInsertedImage("ImageId", "KSK@foo.png", "Title", "Description", 640, 480);
		String link = (String) imageLinkFilter.format(null, image, Collections.<String, Object>emptyMap());
		assertThat(link, is("<img src=\"getImage.html?image=ImageId\" alt=\"Description\" title=\"Title\" width=\"640\" height=\"480\" style=\"position: relative;\"/>"));
	}

	@Test
	public void creatingALinkFromAnImageId() {
		Image image = createImage("ImageId", "KSK@foo.png", "Title", "Description", 640, 480);
		when(mocks.core.getImage(eq("ImageId"))).thenReturn(of(image));
		String link = (String) imageLinkFilter.format(null, image.getId(), Collections.<String, Object>emptyMap());
		assertThat(link, is("<img src=\"/KSK@foo.png?forcedownload=true\" alt=\"Description\" title=\"Title\" width=\"640\" height=\"480\" style=\"position: relative;\"/>"));
	}

	private Image createNotInsertedImage(String id, String key, String title, String description, int width, int height) {
		Image image = createBasicImage(id, key, title, description, width, height);
		when(image.isInserted()).thenReturn(false);
		return image;
	}

	private Image createImage(String id, String key, String title, String description, int width, int height) {
		Image image = createBasicImage(id, key, title, description, width, height);
		when(image.isInserted()).thenReturn(true);
		return image;
	}

	private Image createBasicImage(String id, String key, String title, String description, int width, int height) {
		Image image = mock(Image.class);
		when(image.getId()).thenReturn(id);
		when(image.getKey()).thenReturn(key);
		when(image.getTitle()).thenReturn(title);
		when(image.getDescription()).thenReturn(description);
		when(image.getWidth()).thenReturn(width);
		when(image.getHeight()).thenReturn(height);
		return image;
	}

}
