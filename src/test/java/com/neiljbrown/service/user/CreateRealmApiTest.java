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
package com.neiljbrown.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;

import java.util.ArrayList;
import java.util.List;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neiljbrown.service.user.dto.UserRealmDto;

/**
 * A set of functional tests for the Create Realm API.
 */
public class CreateRealmApiTest extends AbstractRealmApiTest {

  private static final Logger logger = LoggerFactory.getLogger(CreateRealmApiTest.class);

  /** List of one or more relams created by a test. Supports deleting realms as part of tearing down tests. */
  private List<UserRealmDto> createdRealms = new ArrayList<>();

  @After
  public void tearDown() {
    this.createdRealms.forEach(this::tearDownCreatedRealm);
  }

  /**
   * Tests the case when the HTTP method used for the request isn't supported by the API - in this case GET.
   */
  @Test
  public void whenUnsupportedHttpMethodGet() {
    RestAssured.when()
        .get()
        .then()
        .assertThat().statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED).body(isEmptyOrNullString());
  }

  /**
   * Tests the case when the posted media-type isn't supported by the API - in this case application/json.
   */
  @Test
  public void givenUnsupportedMediaTypeJson() {
    RestAssured.given().contentType(ContentType.APPLICATION_JSON.getMimeType())
        .when()
        .post()
        .then()
        .assertThat().statusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE).body(isEmptyOrNullString());
  }

  /**
   * Tests the case when the posted realm resource is empty - doesn't include any component fields.
   */
  @Test
  public void givenUserRealmEmpty() {
    UserRealmDto requestedUserRealm = new UserRealmDto(null);
    RestAssured.given()
        .body(requestedUserRealm)
        .when()
        .post()
        .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
            // Uses Groovy's GPath expression language to match and extract XML elements
            "error.code", equalTo("MissingRealmName"),
            "error.message", equalTo("Realm name is mandatory and must be supplied."));
  }

  /**
   * Tests the case when the posted realm resource has a blank (non-empty, whitespace) name.
   * <p>
   * BUG - API call fails with respinse status code of HTTP 500 instead of expected HTTP 400. 
   */
  @Test
  public void givenUserRealmWithBlankName() {
    final String realmName = " ";
    UserRealmDto requestedUserRealm = new UserRealmDto(realmName);

    RestAssured.given()
        .body(requestedUserRealm)
        .when()
        .post()
        .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
            "error.code", equalTo("MissingRealName"),
            "error.message", equalTo("Realm name is mandatory and must be supplied."));
  }

  /**
   * Tests the case when the posted realm resource has a name longer than the specified max.
   */
  @Test
  public void givenUserRealmWithNameLongerThanMax() {
    UserRealmDto requestedUserRealm =
        new UserRealmDto(generateRandomAlphabeticString(UserRealmConstants.NAME_MAX_LEN + 1));

    RestAssured.given()
        .body(requestedUserRealm)
        .when()
        .post()
        .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
            "error.code", equalTo("InvalidRealmName"),
            "error.message",
            equalTo("Realm name should not be longer than " + UserRealmConstants.NAME_MAX_LEN + " chars."));
  }

  /**
   * Tests the case when the posted realm resource has a description longer than the specified max.
   * <p>
   * BUG - API call fails with respinse status code of HTTP 500 instead of expected HTTP 400.
   */
  @Test
  public void givenUserRealmWithDescriptionLongerThanMax() {
    UserRealmDto requestedUserRealm = new UserRealmDto(generateUniqueRealmName(),
        generateRandomAlphabeticString(UserRealmConstants.DESCRIPTION_MAX_LEN + 1));

    RestAssured.given()
        .body(requestedUserRealm)
        .when()
        .post()
        .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
            "error.code",
            equalTo("InvalidRealmDescription"),
            "error.message",
            equalTo("Realm description should not be longer than " + UserRealmConstants.DESCRIPTION_MAX_LEN + " chars."));
  }

  /**
   * Tests the case when the posted realm resource has a non-unique name - another realm with the same name already
   * exists.
   */
  @Test
  public void givenUserRealmWithDuplicateName() {
    UserRealmDto requestedUserRealm = new UserRealmDto(generateUniqueRealmName(), generateRealmDescription());

    UserRealmDto createdRealm = doTestCreateRealmSuccess(requestedUserRealm);
    assertCreatedRealm(createdRealm, requestedUserRealm);

    RestAssured.given()
        .body(requestedUserRealm)
        .when()
        .post()
        .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
            "error.code", equalTo("DuplicateRealmName"),
            "error.message", equalTo("Duplicate realm name [" + requestedUserRealm.getName() + "]."));
  }

  /**
   * Tests the case when the posted realm resource contains only a unique (mandatory) name field.
   */
  @Test
  public void givenUserRealmWithNameOnly() {
    UserRealmDto requestedUserRealm = new UserRealmDto(generateUniqueRealmName());

    UserRealmDto createdRealm = doTestCreateRealmSuccess(requestedUserRealm);

    assertCreatedRealm(createdRealm, requestedUserRealm);
  }

  /**
   * Tests the case when the posted realm resource contains both the mandatory (unique) name and the optional
   * description.
   */
  @Test
  public void givenUserRealmWithNameAndDescription() {
    UserRealmDto requestedUserRealm = new UserRealmDto(generateUniqueRealmName(), generateRealmDescription());

    UserRealmDto createdRealm = doTestCreateRealmSuccess(requestedUserRealm);

    assertCreatedRealm(createdRealm, requestedUserRealm);
  }

  /**
   * Factored-out common logic for executing a test of the Create Realm API in the success case, when a realm is
   * expected to be created. Executes the API call to create the supplied realm, asserts the response code indicates
   * success, and that body contains a created realm.
   * 
   * @param userRealm Details of the {@link UserRealmDto realm} to be created.
   * @return The created realm, returned by the API, supporting further assertions to be applied.
   */
  private UserRealmDto doTestCreateRealmSuccess(UserRealmDto userRealm) {
    final UserRealmDto createdRealm = this.createRealmResource(userRealm);
    this.createdRealms.add(createdRealm);
    return createdRealm;
  }

  /**
   * Asserts that a newly created realm returned by the Create Realm API contains a valid realm ID and key, and that the
   * other fields of the realm, such as the name and description, match those supplied in the expected realm.
   * 
   * @param createdRealm The created {@link UserRealmDto realm}.
   * @param expectedRealm A {@link UserRealmDto realm} containing the fields that the {@code createdRealm} should match.
   * Null fields are excluded.
   */
  private void assertCreatedRealm(UserRealmDto createdRealm, UserRealmDto expectedRealm) {
    assertRealmIdIsValid(createdRealm.getId());
    assertRealmKeyIsValid(createdRealm.getKey());
    assertThat(createdRealm).isEqualToIgnoringNullFields(expectedRealm);
  }

  private void assertRealmIdIsValid(String realmId) {
    assertThat(realmId).matches("^[1-9][0-9]{0,3}$");
  }

  private void assertRealmKeyIsValid(String realmKey) {
    assertThat(realmKey).matches("^[a-fA-F0-9]{32}$");
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the API base path for the Create Realm APi.
   */
  @Override
  protected String getApiUrlPath() {
    return UserApiService.CREATE_REALM_URL_PATH;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Logger getLogger() {
    return logger;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Additionally sets the Content-Type header for these class of tests.
   */
  @Override
  protected RequestSpecification createDefaultRequestSpecification() {
    RequestSpecification defaultRequestSpec = super.createDefaultRequestSpecification();
    defaultRequestSpec.contentType(ContentType.APPLICATION_XML.getMimeType());
    return defaultRequestSpec;
  }
}