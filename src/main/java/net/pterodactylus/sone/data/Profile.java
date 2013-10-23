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

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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

	/**
	 * Returns the first name.
	 *
	 * @return The first name
	 */
	public String getFirstName() {
		return name.getFirst().orNull();
	}

	/**
	 * Returns the middle name(s).
	 *
	 * @return The middle name
	 */
	public String getMiddleName() {
		return name.getMiddle().orNull();
	}

	/**
	 * Returns the last name.
	 *
	 * @return The last name
	 */
	public String getLastName() {
		return name.getLast().orNull();
	}

	/**
	 * Returns the day of the birth date.
	 *
	 * @return The day of the birth date (from 1 to 31)
	 */
	public Integer getBirthDay() {
		return birthDate.getDay().orNull();
	}

	/**
	 * Returns the month of the birth date.
	 *
	 * @return The month of the birth date (from 1 to 12)
	 */
	public Integer getBirthMonth() {
		return birthDate.getMonth().orNull();
	}

	/**
	 * Returns the year of the birth date.
	 *
	 * @return The year of the birth date
	 */
	public Integer getBirthYear() {
		return birthDate.getYear().orNull();
	}

	/**
	 * Returns the ID of the currently selected avatar image.
	 *
	 * @return The ID of the currently selected avatar image, or {@code null} if
	 *         no avatar is selected.
	 */
	public String getAvatar() {
		return avatar;
	}

	/**
	 * Sets the avatar image.
	 *
	 * @param avatarId
	 * 		The ID of the new avatar image
	 * @return This profile
	 */
	public Profile setAvatar(Optional<String> avatarId) {
		this.avatar = avatarId.orNull();
		return this;
	}

	/**
	 * Returns the fields of this profile.
	 *
	 * @return The fields of this profile
	 */
	public List<Field> getFields() {
		return new ArrayList<Field>(fields);
	}

	/**
	 * Returns whether this profile contains the given field.
	 *
	 * @param field
	 *            The field to check for
	 * @return {@code true} if this profile contains the field, false otherwise
	 */
	public boolean hasField(Field field) {
		return fields.contains(field);
	}

	/**
	 * Returns the field with the given ID.
	 *
	 * @param fieldId
	 *            The ID of the field to get
	 * @return The field, or {@code null} if this profile does not contain a
	 *         field with the given ID
	 */
	public Field getFieldById(String fieldId) {
		checkNotNull(fieldId, "fieldId must not be null");
		for (Field field : fields) {
			if (field.getId().equals(fieldId)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Returns the field with the given name.
	 *
	 * @param fieldName
	 *            The name of the field to get
	 * @return The field, or {@code null} if this profile does not contain a
	 *         field with the given name
	 */
	public Field getFieldByName(String fieldName) {
		for (Field field : fields) {
			if (field.getName().equals(fieldName)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Appends a new field to the list of fields.
	 *
	 * @param fieldName
	 *            The name of the new field
	 * @return The new field
	 * @throws IllegalArgumentException
	 *             if the name is not valid
	 */
	public Field addField(String fieldName) throws IllegalArgumentException {
		checkNotNull(fieldName, "fieldName must not be null");
		checkArgument(fieldName.length() > 0, "fieldName must not be empty");
		checkState(getFieldByName(fieldName) == null, "fieldName must be unique");
		@SuppressWarnings("synthetic-access")
		Field field = new Field(fieldName);
		fields.add(field);
		return field;
	}

	public void renameField(Field field, String newName) {
		int indexOfField = fields.indexOf(field);
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
		fields.get(indexOfField).setValue(newValue);
	}

	/**
	 * Moves the given field up one position in the field list. The index of the
	 * field to move must be greater than {@code 0} (because you obviously can
	 * not move the first field further up).
	 *
	 * @param field
	 *            The field to move up
	 */
	public void moveFieldUp(Field field) {
		checkNotNull(field, "field must not be null");
		checkArgument(hasField(field), "field must belong to this profile");
		checkArgument(getFieldIndex(field) > 0, "field index must be > 0");
		int fieldIndex = getFieldIndex(field);
		fields.remove(field);
		fields.add(fieldIndex - 1, field);
	}

	/**
	 * Moves the given field down one position in the field list. The index of
	 * the field to move must be less than the index of the last field (because
	 * you obviously can not move the last field further down).
	 *
	 * @param field
	 *            The field to move down
	 */
	public void moveFieldDown(Field field) {
		checkNotNull(field, "field must not be null");
		checkArgument(hasField(field), "field must belong to this profile");
		checkArgument(getFieldIndex(field) < fields.size() - 1, "field index must be < " + (fields.size() - 1));
		int fieldIndex = getFieldIndex(field);
		fields.remove(field);
		fields.add(fieldIndex + 1, field);
	}

	/**
	 * Removes the given field.
	 *
	 * @param field
	 *            The field to remove
	 */
	public void removeField(Field field) {
		checkNotNull(field, "field must not be null");
		checkArgument(hasField(field), "field must belong to this profile");
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

	/**
	 * {@inheritDoc}
	 */
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
			hash.putString(field.getName()).putString("(").putString(field.getValue()).putString(")");
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

		/** The ID of the field. */
		private final String id;

		/** The name of the field. */
		private String name;

		/** The value of the field. */
		private String value;

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

		/**
		 * Returns the ID of this field.
		 *
		 * @return The ID of this field
		 */
		public String getId() {
			return id;
		}

		/**
		 * Returns the name of this field.
		 *
		 * @return The name of this field
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the value of this field.
		 *
		 * @return The value of this field
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Sets the value of this field. While {@code null} is allowed, no
		 * guarantees are made that {@code null} values are correctly persisted
		 * across restarts of the plugin!
		 *
		 * @param value
		 *            The new value of this field
		 * @return This field
		 */
		public Field setValue(String value) {
			this.value = value;
			return this;
		}

		//
		// OBJECT METHODS
		//

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object object) {
			if (!(object instanceof Field)) {
				return false;
			}
			Field field = (Field) object;
			return id.equals(field.id);
		}

		/**
		 * {@inheritDoc}
		 */
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
