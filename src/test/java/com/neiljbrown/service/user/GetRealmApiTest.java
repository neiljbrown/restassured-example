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
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;

import com.neiljbrown.service.user.dto.UserRealmDto;
import io.restassured.RestAssured;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of functional tests for the Get Realm API.
 */
public class GetRealmApiTest extends AbstractRealmApiTest {

  private static final Logger logger = LoggerFactory.getLogger(GetRealmApiTest.class);

  /**
   * List of one or more relams created by a test. Supports deleting realms as part of tearing down tests.
   */
  private List<UserRealmDto> createdRealms = new ArrayList<>();

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    RestAssured.basePath = UserRealmApiConstants.GET_REALM_URL_PATH;
  }

  @Override
  @After
  public void tearDown() {
    super.tearDown();
    this.createdRealms.forEach(this::tearDownCreatedRealm);
  }

  /**
   * Tests the case when the HTTP method used for the request isn't supported by the API - in this case POST.
   */
  @Test
  public void whenUnsupportedHttpMethodPost() {
    RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, 1)
      .when()
      .post()
      .then()
      .assertThat().statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED).body(isEmptyOrNullString());
  }

  /**
   * Tests the case when the resource is requested in a media-type that isn't supported by the API - in this case
   * application/json.
   */
  @Test
  public void givenUnsupportedMediaTypeJson() {
    RestAssured.given()
      .accept(ContentType.APPLICATION_JSON.getMimeType())
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, 1)
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_NOT_ACCEPTABLE).body(isEmptyOrNullString());
  }

  /**
   * Tests the case when the requested realm ID is less than the documented min - in this case zero.
   * <p>
   * BUG - API returns RealmNotFound instead of InvalidRealmId.
   */
  @Test
  public void givenRealmIdLessThanMinZero() {
    final int realmId = UserRealmConstants.ID_MIN - 1;
    RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, realmId)
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
      .body(
        // Example of using XPath. For this purpose, XPath is clunkier than GPath and also the failure diagnostics
        // are not as good - the failure error message reports the whole actual XML document rather than just the
        // element in the field being asserted (as is the case when using GPath).
        hasXPath("/error/code/text()"), equalTo("InvalidRealmId"),
        hasXPath("/error/message/text()"), equalTo("Invalid realm id [" + realmId + "]."));
  }

  /**
   * Tests the case when the requested realm ID is greater than the documented max.
   * <p>
   * BUG - API call fails with response status code of HTTP 500 instead of HTTP 400 Bad Request.
   */
  @Test
  public void givenRealmIdGreaterThanMax() {
    final int realmId = UserRealmConstants.ID_MAX + 1;
    RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, realmId)
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
      .body(
        "error.code", equalTo("InvalidRealmId"),
        "error.message", equalTo("Invalid realm id [" + realmId + "]."));
  }

  /**
   * Tests the case when the requested realm ID is a non-integer string.
   * <p>
   * BUG - Error message does not detail the requested invalid realm ID.
   */
  @Test
  public void givenRealmIdNonInteger() {
    final String realmId = "foo";
    RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, realmId)
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
      .body(
        "error.code", equalTo("InvalidRealmId"),
        "error.message", equalTo("Invalid realm id [" + realmId + "]."));
  }

  /**
   * Tests the case when the requested realm does not exist.
   * <p>
   * BUG - API request fails with response status of HTTP 400 instead of expected 404.
   */
  @Test
  public void givenRealmDoesNotExist() {
    final int realmId = UserRealmConstants.ID_MAX;
    this.deleteRealmResource(realmId);

    RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, realmId)
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_NOT_FOUND)
      .body(
        "error.code", equalTo("RealmNotFound"),
        "error.message", equalTo("Realm [" + realmId + "] not found."));
  }

  /**
   * Tests the case when the requested realm exists and all of its fields - mandatory and optional - are populated.
   */
  @Test
  public void givenRealmExistsWithAllFields() {
    final UserRealmDto realmToCreate = new UserRealmDto(generateUniqueRealmName(), generateRealmDescription());
    final UserRealmDto createdRealm = createRealmResource(realmToCreate);

    this.createdRealms.add(createdRealm);

    UserRealmDto gotRealm = RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, createdRealm.getId())
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_OK)
      .body(not(isEmptyOrNullString()))
      .extract().as(UserRealmDto.class);

    assertRealm(gotRealm, createdRealm);
  }

  /**
   * Tests the case when the requested realm exists and is only populated with mandatory fields - ID, key and name.
   */
  @Test
  public void givenRealmExistsWithMandatoryFieldsOnly() {
    final UserRealmDto realmToCreate = new UserRealmDto(generateUniqueRealmName());
    final UserRealmDto createdRealm = createRealmResource(realmToCreate);
    assertThat(createdRealm.getDescription()).isNull();

    this.createdRealms.add(createdRealm);

    UserRealmDto gotRealm = RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, createdRealm.getId())
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_OK)
      .body(not(isEmptyOrNullString()))
      .extract().as(UserRealmDto.class);

    assertRealm(gotRealm, createdRealm);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Logger getLogger() {
    return logger;
  }

  private void assertRealm(UserRealmDto actualRealm, UserRealmDto expectedRealm) {
    // Compare field by field rather than relying on equals() as on failure it identifies specific fields in error
    assertThat(actualRealm).isEqualToComparingFieldByField(expectedRealm);
  }
}