/*
 * Sone - CreateAlbumPage.java - Copyright © 2011–2013 David Roden
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

package net.pterodactylus.sone.web;

import static net.pterodactylus.sone.text.TextFilter.filter;

import net.pterodactylus.sone.data.Album;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.database.AlbumBuilder;
import net.pterodactylus.sone.web.page.FreenetRequest;
import net.pterodactylus.util.template.Template;
import net.pterodactylus.util.template.TemplateContext;
import net.pterodactylus.util.web.Method;

/**
 * Page that lets the user create a new album.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class CreateAlbumPage extends SoneTemplatePage {

	/**
	 * Creates a new “create album” page.
	 *
	 * @param template
	 *            The template to render
	 * @param webInterface
	 *            The Sone web interface
	 */
	public CreateAlbumPage(Template template, WebInterface webInterface) {
		super("createAlbum.html", template, "Page.CreateAlbum.Title", webInterface, true);
	}

	//
	// SONETEMPLATEPAGE METHODS
	//

	@Override
	protected void processTemplate(FreenetRequest request, TemplateContext templateContext) throws RedirectException {
		super.processTemplate(request, templateContext);
		if (request.getMethod() == Method.POST) {
			String name = request.getHttpRequest().getPartAsStringFailsafe("name", 64).trim();
			if (name.length() == 0) {
				templateContext.set("nameMissing", true);
				return;
			}
			Album album = createAlbum(request, name);
			webInterface.getCore().touchConfiguration();
			throw new RedirectException("imageBrowser.html?album=" + album.getId());
		}
	}

	private Album createAlbum(FreenetRequest request, String name) {
		Sone currentSone = getCurrentSone(request.getToadletContext());
		String parentId = request.getHttpRequest().getPartAsStringFailsafe("parent", 36);
		AlbumBuilder albumBuilder = parentId.equals("") ? currentSone.newAlbumBuilder() : webInterface.getCore().getAlbum(parentId).get().newAlbumBuilder();
		Album album = albumBuilder.build();
		return setTitleAndDescription(request, name, album);
	}

	private Album setTitleAndDescription(FreenetRequest request, String name, Album album) {
		String description = request.getHttpRequest().getPartAsStringFailsafe("description", 256).trim();
		return album.modify().setTitle(name).setDescription(filter(request.getHttpRequest().getHeader("host"), description)).update();
	}

}
