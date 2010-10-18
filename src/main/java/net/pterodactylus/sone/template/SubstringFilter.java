/*
 * Sone - SubstringFilter.java - Copyright © 2010 David Roden
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

import java.util.Map;

import net.pterodactylus.util.template.DataProvider;
import net.pterodactylus.util.template.Filter;

/**
 * {@link Filter} implementation that executes
 * {@link String#substring(int, int)} on the given data. It has two parameters:
 * “start” and “length.” “length” is optional and defaults to “the rest of the
 * string.” “start” starts at {@code 0} and can be negative to denote starting
 * at the end of the string.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SubstringFilter implements Filter {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object format(DataProvider dataProvider, Object data, Map<String, String> parameters) {
		String startString = parameters.get("start");
		String lengthString = parameters.get("length");
		int start = 0;
		try {
			start = Integer.parseInt(startString);
		} catch (NumberFormatException nfe1) {
			/* ignore. */
		}
		String dataString = String.valueOf(data);
		int dataLength = dataString.length();
		if (lengthString == null) {
			if (start < 0) {
				return dataString.substring(dataLength + start);
			}
			return dataString.substring(start);
		}
		int length = Integer.MAX_VALUE;
		try {
			length = Integer.parseInt(lengthString);
		} catch (NumberFormatException nfe1) {
			/* ignore. */
		}
		if (start < 0) {
			return dataString.substring(dataLength + start, Math.min(dataLength, dataLength + start + length));
		}
		return dataString.substring(start, Math.min(dataLength, start + length));
	}

}