/*
 * Freetalk - FreetalkTemplatePage.java - Copyright © 2010 David Roden
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

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.web.page.Page;
import net.pterodactylus.sone.web.page.TemplatePage;
import net.pterodactylus.util.template.Template;
import freenet.clients.http.SessionManager.Session;
import freenet.clients.http.ToadletContext;

/**
 * Base page for the Freetalk web interface.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneTemplatePage extends TemplatePage {

	/** The Sone core. */
	protected final WebInterface webInterface;

	/**
	 * Creates a new template page for Freetalk.
	 *
	 * @param path
	 *            The path of the page
	 * @param template
	 *            The template to render
	 * @param pageTitleKey
	 *            The l10n key of the page title
	 * @param webInterface
	 *            The Sone web interface
	 */
	public SoneTemplatePage(String path, Template template, String pageTitleKey, WebInterface webInterface) {
		super(path, template, webInterface.l10n(), pageTitleKey);
		this.webInterface = webInterface;
		template.set("webInterface", webInterface);
	}

	//
	// PROTECTED METHODS
	//

	/**
	 * Returns the current session, creating a new session if there is no
	 * current session.
	 *
	 * @param toadletContenxt
	 *            The toadlet context
	 * @return The current session, or {@code null} if there is no current
	 *         session
	 */
	protected Session getCurrentSession(ToadletContext toadletContenxt) {
		return getCurrentSession(toadletContenxt, true);
	}

	/**
	 * Returns the current session, creating a new session if there is no
	 * current session and {@code create} is {@code true}.
	 *
	 * @param toadletContenxt
	 *            The toadlet context
	 * @param create
	 *            {@code true} to create a new session if there is no current
	 *            session, {@code false} otherwise
	 * @return The current session, or {@code null} if there is no current
	 *         session
	 */
	protected Session getCurrentSession(ToadletContext toadletContenxt, boolean create) {
		try {
			Session session = webInterface.sessionManager().useSession(toadletContenxt);
			if (create && (session == null)) {
				session = webInterface.sessionManager().createSession(UUID.randomUUID().toString(), toadletContenxt);
			}
			return session;
		} catch (freenet.clients.http.RedirectException re1) {
			return null;
		}
	}

	/**
	 * Returns the currently logged in Sone.
	 *
	 * @param toadletContenxt
	 *            The toadlet context
	 * @return The currently logged in Sone, or {@code null} if no Sone is
	 *         currently logged in
	 */
	protected Sone getCurrentSone(ToadletContext toadletContenxt) {
		Session session = getCurrentSession(toadletContenxt);
		if (session == null) {
			return null;
		}
		return (Sone) session.getAttribute("Sone.CurrentSone");
	}

	/**
	 * Sets the currently logged in Sone.
	 *
	 * @param toadletContext
	 *            The toadlet context
	 * @param sone
	 *            The Sone to set as currently logged in
	 */
	protected void setCurrentSone(ToadletContext toadletContext, Sone sone) {
		Session session = getCurrentSession(toadletContext);
		session.setAttribute("Sone.CurrentSone", sone);
	}

	//
	// TEMPLATEPAGE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Collection<String> getStyleSheets() {
		return Arrays.asList("css/sone.css");
	}

	/**
	 * Returns whether this page requires the user to log in.
	 *
	 * @return {@code true} if the user is required to be logged in to use this
	 *         page, {@code false} otherwise
	 */
	protected boolean requiresLogin() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getRedirectTarget(Page.Request request) {
		if (requiresLogin() && (getCurrentSone(request.getToadletContext()) == null)) {
			return "login.html";
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEnabled(ToadletContext toadletContext) {
		if (requiresLogin()) {
			return getCurrentSone(toadletContext) != null;
		}
		return true;
	}

}
