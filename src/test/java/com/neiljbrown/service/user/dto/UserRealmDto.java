/*
 * Copyright 2017-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neiljbrown.service.user.dto;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Data Transfer Object (DTO) for a User Realm API resource. Deserialised, object representation of representations of
 * the resource exchanged in remote API calls.
 * <p>
 * Provides support for data-binding (automated marshalling and unmarshalling) of representations to this object using
 * JAXB (annotations).
 */
@XmlRootElement(name = "realm")
public final class UserRealmDto {

  private String id;
  private String name;
  private String description;
  private String key;

  // Zero-arg constructor required by JAXB
  @SuppressWarnings("unused")
  private UserRealmDto() {
    // Do nothing
  }

  /**
   * @param name the name of the realm.
   */
  public UserRealmDto(String name) {
    this.name = name;
  }

  /**
   * @param name the name of the realm.
   * @param description the description of the realm.
   */
  public UserRealmDto(String name, String description) {
    this.name = name;
    this.description = description;
  }

  /**
   * @param id the unique ID of the realm.
   * @param name the name of the realm.
   * @param description the description of the realm.
   * @param key the realm's encryption key.
   */
  public UserRealmDto(String id, String name, String description, String key) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.key = key;
  }

  /**
   * Copy constructor.
   *
   * @param userRealmDto The instance of {@link UserRealmDto} to be copied.
   */
  public UserRealmDto(UserRealmDto userRealmDto) {
    this.id = userRealmDto.getId();
    this.name = userRealmDto.getName();
    this.description = userRealmDto.getDescription();
    this.key = userRealmDto.getKey();
  }

  /**
   * @return the id
   */
  @XmlAttribute
  public final String getId() {
    return this.id;
  }

  /**
   * @param id the id to set
   */
  public final void setId(String id) {
    this.id = id;
  }

  /**
   * @return the name
   */
  @XmlAttribute
  public final String getName() {
    return this.name;
  }

  /**
   * @param name the name to set
   */
  public final void setName(String name) {
    this.name = name;
  }

  /**
   * @return the description
   */
  @XmlElement
  public final String getDescription() {
    return this.description;
  }

  /**
   * @param description the description to set
   */
  public final void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the key
   */
  @XmlElement
  public final String getKey() {
    return this.key;
  }

  /**
   * @param key the key to set
   */
  public final void setKey(String key) {
    this.key = key;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Auto-generated method.
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Auto-generated method.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof UserRealmDto)) {
      return false;
    }
    UserRealmDto other = (UserRealmDto) obj;
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "UserRealmDto{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", key='" + key + '\'' +
      '}';
  }
}