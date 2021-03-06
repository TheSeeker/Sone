/*
 * Sone - ParserFilter.java - Copyright © 2011–2013 David Roden
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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.text.FreenetLinkPart;
import net.pterodactylus.sone.text.LinkPart;
import net.pterodactylus.sone.text.Part;
import net.pterodactylus.sone.text.PlainTextPart;
import net.pterodactylus.sone.text.PostPart;
import net.pterodactylus.sone.text.SonePart;
import net.pterodactylus.sone.text.SoneTextParser;
import net.pterodactylus.sone.text.SoneTextParserContext;
import net.pterodactylus.sone.web.page.FreenetRequest;
import net.pterodactylus.util.number.Numbers;
import net.pterodactylus.util.template.Filter;
import net.pterodactylus.util.template.Template;
import net.pterodactylus.util.template.TemplateContext;
import net.pterodactylus.util.template.TemplateContextFactory;
import net.pterodactylus.util.template.TemplateParser;

/**
 * Filter that filters a given text through a {@link SoneTextParser}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ParserFilter implements Filter {

	/** The core. */
	private final Core core;

	/** The link parser. */
	private final SoneTextParser soneTextParser;

	/** The template context factory. */
	private final TemplateContextFactory templateContextFactory;

	/** The template for {@link PlainTextPart}s. */
	private static final Template plainTextTemplate = TemplateParser.parse(new StringReader("<%text|html>"));

	/** The template for {@link FreenetLinkPart}s. */
	private static final Template linkTemplate = TemplateParser.parse(new StringReader("<a class=\"<%cssClass|html>\" href=\"<%link|html>\" title=\"<%title|html>\"><%text|html></a>"));

	/**
	 * Creates a new filter that runs its input through a {@link SoneTextParser}
	 * .
	 *
	 * @param core
	 *            The core
	 * @param templateContextFactory
	 *            The context factory for rendering the parts
	 * @param soneTextParser
	 *            The Sone text parser
	 */
	public ParserFilter(Core core, TemplateContextFactory templateContextFactory, SoneTextParser soneTextParser) {
		this.core = core;
		this.templateContextFactory = templateContextFactory;
		this.soneTextParser = soneTextParser;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object format(TemplateContext templateContext, Object data, Map<String, Object> parameters) {
		String text = String.valueOf(data);
		int length = Numbers.safeParseInteger(parameters.get("length"), Numbers.safeParseInteger(templateContext.get(String.valueOf(parameters.get("length"))), -1));
		int cutOffLength = Numbers.safeParseInteger(parameters.get("cut-off-length"), Numbers.safeParseInteger(templateContext.get(String.valueOf(parameters.get("cut-off-length"))), length));
		Object sone = parameters.get("sone");
		if (sone instanceof String) {
			sone = core.getSone((String) sone).orNull();
		}
		FreenetRequest request = (FreenetRequest) templateContext.get("request");
		SoneTextParserContext context = new SoneTextParserContext(request, (Sone) sone);
		StringWriter parsedTextWriter = new StringWriter();
		try {
			Iterable<Part> parts = soneTextParser.parse(context, new StringReader(text));
			if (length > -1) {
				int allPartsLength = 0;
				List<Part> shortenedParts = new ArrayList<Part>();
				for (Part part : parts) {
					if (part instanceof PlainTextPart) {
						String longText = ((PlainTextPart) part).getText();
						if (allPartsLength < cutOffLength) {
							if ((allPartsLength + longText.length()) > cutOffLength) {
								shortenedParts.add(new PlainTextPart(longText.substring(0, cutOffLength - allPartsLength) + "…"));
							} else {
								shortenedParts.add(part);
							}
						}
						allPartsLength += longText.length();
					} else if (part instanceof LinkPart) {
						if (allPartsLength < cutOffLength) {
							shortenedParts.add(part);
						}
						allPartsLength += ((LinkPart) part).getText().length();
					} else {
						if (allPartsLength < cutOffLength) {
							shortenedParts.add(part);
						}
					}
				}
				if (allPartsLength >= length) {
					parts = shortenedParts;
				}
			}
			render(parsedTextWriter, parts);
		} catch (IOException ioe1) {
			/* no exceptions in a StringReader or StringWriter, ignore. */
		}
		return parsedTextWriter.toString();
	}

	//
	// PRIVATE METHODS
	//

	/**
	 * Renders the given parts.
	 *
	 * @param writer
	 *            The writer to render the parts to
	 * @param parts
	 *            The parts to render
	 */
	private void render(Writer writer, Iterable<Part> parts) {
		for (Part part : parts) {
			render(writer, part);
		}
	}

	/**
	 * Renders the given part.
	 *
	 * @param writer
	 *            The writer to render the part to
	 * @param part
	 *            The part to render
	 */
	@SuppressWarnings("unchecked")
	private void render(Writer writer, Part part) {
		if (part instanceof PlainTextPart) {
			render(writer, (PlainTextPart) part);
		} else if (part instanceof FreenetLinkPart) {
			render(writer, (FreenetLinkPart) part);
		} else if (part instanceof LinkPart) {
			render(writer, (LinkPart) part);
		} else if (part instanceof SonePart) {
			render(writer, (SonePart) part);
		} else if (part instanceof PostPart) {
			render(writer, (PostPart) part);
		} else if (part instanceof Iterable<?>) {
			render(writer, (Iterable<Part>) part);
		}
	}

	/**
	 * Renders the given plain-text part.
	 *
	 * @param writer
	 *            The writer to render the part to
	 * @param plainTextPart
	 *            The part to render
	 */
	private void render(Writer writer, PlainTextPart plainTextPart) {
		TemplateContext templateContext = templateContextFactory.createTemplateContext();
		templateContext.set("text", plainTextPart.getText());
		plainTextTemplate.render(templateContext, writer);
	}

	/**
	 * Renders the given freenet link part.
	 *
	 * @param writer
	 *            The writer to render the part to
	 * @param freenetLinkPart
	 *            The part to render
	 */
	private void render(Writer writer, FreenetLinkPart freenetLinkPart) {
		renderLink(writer, "/" + freenetLinkPart.getLink(), freenetLinkPart.getText(), freenetLinkPart.getTitle(), freenetLinkPart.isTrusted() ? "freenet-trusted" : "freenet");
	}

	/**
	 * Renders the given link part.
	 *
	 * @param writer
	 *            The writer to render the part to
	 * @param linkPart
	 *            The part to render
	 */
	private void render(Writer writer, LinkPart linkPart) {
		try {
			renderLink(writer, "/external-link/?_CHECKED_HTTP_=" + URLEncoder.encode(linkPart.getLink(), "UTF-8"), linkPart.getText(), linkPart.getTitle(), "internet");
		} catch (UnsupportedEncodingException uee1) {
			/* not possible for UTF-8. */
			throw new RuntimeException("The JVM does not support UTF-8 encoding!", uee1);
		}
	}

	/**
	 * Renders the given Sone part.
	 *
	 * @param writer
	 *            The writer to render the part to
	 * @param sonePart
	 *            The part to render
	 */
	private void render(Writer writer, SonePart sonePart) {
		if ((sonePart.getSone() != null) && (sonePart.getSone().getName() != null)) {
			renderLink(writer, "viewSone.html?sone=" + sonePart.getSone().getId(), SoneAccessor.getNiceName(sonePart.getSone()), SoneAccessor.getNiceName(sonePart.getSone()), "in-sone");
		} else {
			renderLink(writer, "/WebOfTrust/ShowIdentity?id=" + sonePart.getSone().getId(), sonePart.getSone().getId(), sonePart.getSone().getId(), "in-sone");
		}
	}

	/**
	 * Renders the given post part.
	 *
	 * @param writer
	 *            The writer to render the part to
	 * @param postPart
	 *            The part to render
	 */
	private void render(Writer writer, PostPart postPart) {
		SoneTextParser parser = new SoneTextParser(core, core);
		SoneTextParserContext parserContext = new SoneTextParserContext(null, postPart.getPost().getSone());
		try {
			Iterable<Part> parts = parser.parse(parserContext, new StringReader(postPart.getPost().getText()));
			StringBuilder excerpt = new StringBuilder();
			for (Part part : parts) {
				excerpt.append(part.getText());
				if (excerpt.length() > 20) {
					int lastSpace = excerpt.lastIndexOf(" ", 20);
					if (lastSpace > -1) {
						excerpt.setLength(lastSpace);
					} else {
						excerpt.setLength(20);
					}
					excerpt.append("…");
					break;
				}
			}
			renderLink(writer, "viewPost.html?post=" + postPart.getPost().getId(), excerpt.toString(), SoneAccessor.getNiceName(postPart.getPost().getSone()), "in-sone");
		} catch (IOException ioe1) {
			/* StringReader shouldn’t throw. */
		}
	}

	/**
	 * Renders the given link.
	 *
	 * @param writer
	 *            The writer to render the link to
	 * @param link
	 *            The link to render
	 * @param text
	 *            The text of the link
	 * @param title
	 *            The title of the link
	 * @param cssClass
	 *            The CSS class of the link
	 */
	private void renderLink(Writer writer, String link, String text, String title, String cssClass) {
		TemplateContext templateContext = templateContextFactory.createTemplateContext();
		templateContext.set("cssClass", cssClass);
		templateContext.set("link", link);
		templateContext.set("text", text);
		templateContext.set("title", title);
		linkTemplate.render(templateContext, writer);
	}

}
