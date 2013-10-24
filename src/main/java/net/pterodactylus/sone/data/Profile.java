/*
 * Sone - Profile.java - Copyright © 2010–2013 David Roden
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

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.UUID.randomUUID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/**
 * A profile stores personal information about a {@link Sone}. All information
 * is optional and can be {@code null}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Profile implements Fingerprintable {

	/** The Sone this profile belongs to. */
	private final Sone sone;

	private volatile Name name = new Name();
	private volatile BirthDate birthDate = new BirthDate();

	/** The ID of the avatar image. */
	private volatile String avatar;

	/** Additional fields in the profile. */
	private final List<Field> fields = Collections.synchronizedList(new ArrayList<Field>());

	/**
	 * Creates a new empty profile.
	 *
	 * @param sone
	 *            The Sone this profile belongs to
	 */
	public Profile(Sone sone) {
		this.sone = sone;
	}

	/**
	 * Creates a copy of a profile.
	 *
	 * @param profile
	 *            The profile to copy
	 */
	public Profile(Profile profile) {
		this.sone = profile.sone;
		this.name = profile.name;
		this.birthDate = profile.birthDate;
		this.avatar = profile.avatar;
		this.fields.addAll(profile.fields);
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the Sone this profile belongs to.
	 *
	 * @return The Sone this profile belongs to
	 */
	public Sone getSone() {
		return sone;
	}

	public String getFirstName() {
		return name.getFirst().orNull();
	}

	public String getMiddleName() {
		return name.getMiddle().orNull();
	}

	public String getLastName() {
		return name.getLast().orNull();
	}

	public Integer getBirthDay() {
		return birthDate.getDay().orNull();
	}

	public Integer getBirthMonth() {
		return birthDate.getMonth().orNull();
	}

	public Integer getBirthYear() {
		return birthDate.getYear().orNull();
	}

	public String getAvatar() {
		return avatar;
	}

	public Profile setAvatar(Optional<String> avatarId) {
		this.avatar = avatarId.orNull();
		return this;
	}

	public List<Field> getFields() {
		return new ArrayList<Field>(fields);
	}

	public boolean hasField(Field field) {
		return fields.contains(field);
	}

	public Optional<Field> getFieldById(String fieldId) {
		checkNotNull(fieldId, "fieldId must not be null");
		for (Field field : fields) {
			if (field.getId().equals(fieldId)) {
				return of(field);
			}
		}
		return absent();
	}

	public Optional<Field> getFieldByName(String fieldName) {
		for (Field field : fields) {
			if (field.getName().equals(fieldName)) {
				return of(field);
			}
		}
		return absent();
	}

	public Field addField(String fieldName) throws IllegalArgumentException {
		checkNotNull(fieldName, "fieldName must not be null");
		checkArgument(fieldName.length() > 0, "fieldName must not be empty");
		checkArgument(!getFieldByName(fieldName).isPresent(), "fieldName must be unique");
		@SuppressWarnings("synthetic-access")
		Field field = new Field(fieldName);
		fields.add(field);
		return field;
	}

	public void renameField(Field field, String newName) {
		int indexOfField = getFieldIndex(field);
		if (indexOfField == -1) {
			return;
		}
		fields.set(indexOfField, new Field(field.getId(), newName, field.getValue()));
	}

	public void setField(Field field, String newValue) {
		int indexOfField = getFieldIndex(field);
		if (indexOfField == -1) {
			return;
		}
		fields.set(indexOfField, new Field(field.getId(), field.getName(), newValue));
	}

	public void moveFieldUp(Field field) {
		checkNotNull(field, "field must not be null");
		checkArgument(hasField(field), "field must belong to this profile");
		int fieldIndex = getFieldIndex(field);
		fields.remove(field);
		fields.add(max(fieldIndex - 1, 0), field);
	}

	public void moveFieldDown(Field field) {
		checkNotNull(field, "field must not be null");
		checkArgument(hasField(field), "field must belong to this profile");
		int fieldIndex = getFieldIndex(field);
		fields.remove(field);
		fields.add(min(fieldIndex + 1, fields.size()), field);
	}

	public void removeField(Field field) {
		checkNotNull(field, "field must not be null");
		fields.remove(field);
	}

	public Modifier modify() {
		return new Modifier() {
			private Optional<String> firstName = name.getFirst();
			private Optional<String> middleName = name.getMiddle();
			private Optional<String> lastName = name.getLast();
			private Optional<Integer> birthYear = birthDate.getYear();
			private Optional<Integer> birthMonth = birthDate.getMonth();
			private Optional<Integer> birthDay = birthDate.getDay();

			@Override
			public Modifier setFirstName(String firstName) {
				this.firstName = fromNullable(firstName);
				return this;
			}

			@Override
			public Modifier setMiddleName(String middleName) {
				this.middleName = fromNullable(middleName);
				return this;
			}

			@Override
			public Modifier setLastName(String lastName) {
				this.lastName = fromNullable(lastName);
				return this;
			}

			@Override
			public Modifier setBirthYear(Integer birthYear) {
				this.birthYear = fromNullable(birthYear);
				return this;
			}

			@Override
			public Modifier setBirthMonth(Integer birthMonth) {
				this.birthMonth = fromNullable(birthMonth);
				return this;
			}

			@Override
			public Modifier setBirthDay(Integer birthDay) {
				this.birthDay = fromNullable(birthDay);
				return this;
			}

			@Override
			public Profile update() {
				Profile.this.name = new Name(firstName, middleName, lastName);
				Profile.this.birthDate = new BirthDate(birthYear, birthMonth, birthDay);
				return Profile.this;
			}
		};
	}

	public interface Modifier {

		Modifier setFirstName(String firstName);
		Modifier setMiddleName(String middleName);
		Modifier setLastName(String lastName);
		Modifier setBirthYear(Integer birthYear);
		Modifier setBirthMonth(Integer birthMonth);
		Modifier setBirthDay(Integer birthDay);
		Profile update();

	}

	//
	// PRIVATE METHODS
	//

	/**
	 * Returns the index of the field with the given name.
	 *
	 * @param field
	 *            The name of the field
	 * @return The index of the field, or {@code -1} if there is no field with
	 *         the given name
	 */
	private int getFieldIndex(Field field) {
		return fields.indexOf(field);
	}

	//
	// INTERFACE Fingerprintable
	//

	@Override
	public String getFingerprint() {
		Hasher hash = Hashing.sha256().newHasher();
		hash.putString("Profile(");
		hash.putString(name.getFingerprint());
		hash.putString(birthDate.getFingerprint());
		if (avatar != null) {
			hash.putString("Avatar(").putString(avatar).putString(")");
		}
		hash.putString("ContactInformation(");
		for (Field field : fields) {
			if (field.getValue() != null) {
				hash.putString(field.getName()).putString("(").putString(field.getValue()).putString(")");
			}
		}
		hash.putString(")");
		hash.putString(")");

		return hash.hash().toString();
	}

	/**
	 * Container for a profile field.
	 *
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	public static class Field {

		private final String id;
		private final String name;
		private final String value;

		public Field(String name) {
			this(name, null);
		}

		public Field(String name, String value) {
			this(randomUUID().toString(), name, value);
		}

		public Field(String id, String name, String value) {
			this.id = checkNotNull(id, "id must not be null");
			this.name = name;
			this.value = value;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(Object object) {
			if (!(object instanceof Field)) {
				return false;
			}
			Field field = (Field) object;
			return id.equals(field.id);
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

	}

	public static class Name implements Fingerprintable {

		private final Optional<String> first;
		private final Optional<String> middle;
		private final Optional<String> last;

		public Name() {
			this(Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent());
		}

		public Name(Optional<String> first, Optional<String> middle, Optional<String> last) {
			this.first = first;
			this.middle = middle;
			this.last = last;
		}

		public Optional<String> getFirst() {
			return first;
		}

		public Optional<String> getMiddle() {
			return middle;
		}

		public Optional<String> getLast() {
			return last;
		}

		@Override
		public String getFingerprint() {
			Hasher hash = Hashing.sha256().newHasher();
			hash.putString("Name(");
			if (first.isPresent()) {
				hash.putString("First(").putString(first.get()).putString(")");
			}
			if (middle.isPresent()) {
				hash.putString("Middle(").putString(middle.get()).putString(")");
			}
			if (last.isPresent()) {
				hash.putString("Last(").putString(last.get()).putString(")");
			}
			hash.putString(")");
			return hash.hash().toString();
		}

	}

	public static class BirthDate implements Fingerprintable {

		private final Optional<Integer> year;
		private final Optional<Integer> month;
		private final Optional<Integer> day;

		public BirthDate() {
			this(Optional.<Integer>absent(), Optional.<Integer>absent(), Optional.<Integer>absent());
		}

		public BirthDate(Optional<Integer> year, Optional<Integer> month, Optional<Integer> day) {
			this.year = year;
			this.month = month;
			this.day = day;
		}

		public Optional<Integer> getYear() {
			return year;
		}

		public Optional<Integer> getMonth() {
			return month;
		}

		public Optional<Integer> getDay() {
			return day;
		}

		@Override
		public String getFingerprint() {
			Hasher hash = Hashing.sha256().newHasher();
			hash.putString("Birthdate(");
			if (year.isPresent()) {
				hash.putString("Year(").putInt(year.get()).putString(")");
			}
			if (month.isPresent()) {
				hash.putString("Month(").putInt(month.get()).putString(")");
			}
			if (day.isPresent()) {
				hash.putString("Day(").putInt(day.get()).putString(")");
			}
			hash.putString(")");
			return hash.hash().toString();
		}

	}

}
