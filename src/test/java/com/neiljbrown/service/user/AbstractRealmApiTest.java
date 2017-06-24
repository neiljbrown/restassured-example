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

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.slf4j.Logger;

import com.neiljbrown.service.user.dto.UserRealmDto;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;

/**
 * Abstract base class providing common behaviour and state for functional tests of Realm APIs.
 */
public abstract class AbstractRealmApiTest {

  private static final String SYS_PROP_PREFIX = "qatest.";
  private static final String SYS_PROP_ALWAYS_LOG_REQ_AND_RESP = SYS_PROP_PREFIX + "alwaysLogReqAndResp";

  /**
   * Flag controlling whether HTTP requests and response issued by tests are always logged. Intended to support
   * debugging exact behaviour of tests, even when they pass. Defaults to false - requests and responses will only be
   * logged if assertions fail. Can be set at runtime using system property {@link #SYS_PROP_ALWAYS_LOG_REQ_AND_RESP}.
   */
  private boolean alwaysLogRequestAndResponse;

  @Before
  public void setUp() {
    initAlwaysLogRequestAndResponse();
    configureRestAssuredRequestAndResponseDefaults();
  }

  private void configureRestAssuredRequestAndResponseDefaults() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    configureRestAssuredRequestDefaults();
    configureRestAssuredResponseDefaults();
  }

  private void configureRestAssuredRequestDefaults() {
    RestAssured.baseURI = UserApiService.getRootUrl();
    RestAssured.port = UserApiService.getPort();
    RestAssured.basePath = this.getApiUrlPath();
    RestAssured.requestSpecification = createDefaultRequestSpecification();
  }

  /**
   * @return The URL path of the API under test.
   */
  protected abstract String getApiUrlPath();

  /**
   * @return The Logger for this class.
   */
  protected abstract Logger getLogger();

  /**
   * Create and configure a default RequestSpecification which ensures that by default all requests have the typically
   * required Accept request header for the API under test.
   * 
   * @return The REST Assured {@link RequestSpecification}
   */
  protected RequestSpecification createDefaultRequestSpecification() {
    RequestSpecification defaultRequestSpec = new RequestSpecBuilder()
        .setAccept(ContentType.APPLICATION_XML.getMimeType())
        .build();
    if (this.isAlwaysLogRequestAndResponse()) {
      defaultRequestSpec.log().all();
    }
    return defaultRequestSpec;
  }

  private void configureRestAssuredResponseDefaults() {
    if (this.isAlwaysLogRequestAndResponse()) {
      // RestAssurred doesn't currently support enabling logging using a ResponseSpecification as follows -
      // ResponseSpecification defaultResponseSpecification = new ResponseSpecBuilder().build().log().all();
      // RestAssured.responseSpecification = defaultResponseSpecification;
      // It throws an IllegalStatException with the message
      // "Cannot configure logging since request specification is not defined. You may be misusing the API"
      // So, instead, enable response logging by adding a filter to the default list of filters.
      RestAssured.filters(new ResponseLoggingFilter());
    }
  }

  /**
   * Invokes a Create Realm API call to the User service to create a realm using the supplied realm details, asserts the
   * call was successful, and if so returns an object representation of the created realm resource.
   * 
   * @param userRealm A {@link UserRealmDto} containing the details of the realm resource to create
   * @return A {@link UserRealmDto} containing the details of the created realm resource.
   */
  // package protected
  UserRealmDto createRealmResource(UserRealmDto userRealm) {
    return RestAssured.given()
        .basePath(UserApiService.CREATE_REALM_URL_PATH)
        .contentType(ContentType.APPLICATION_XML.getMimeType())
        .body(userRealm)
        .when()
        .post()
        .then()
        .assertThat().statusCode(HttpStatus.SC_CREATED)
        .body(not(isEmptyOrNullString()))
        .extract().body().as(UserRealmDto.class);
  }

  /**
   * Invokes a Delete Realm API call to the User service to delete an identified realm resource, and asserts the call
   * was successful.
   * 
   * @param realmId The ID of the realm to delete.
   */
  // package protected
  void deleteRealmResource(int realmId) {
    RestAssured.given()
        .basePath("")
        .pathParam("realmId", realmId)
        .when()
        .delete("/realm/{realmId}")
        .then()
        .assertThat().statusCode(HttpStatus.SC_NO_CONTENT);
  }

  /**
   * Tears down a realm created as part of a test. Deletes the realm. If the deletion fails for any reason, logs an
   * error and continues.
   * 
   * @param realm The {@link UserRealmDto} to be deleted. Must include the ID of the realm, which must be an integer
   * string.
   */
  // package protected
  void tearDownCreatedRealm(UserRealmDto realm) {
    Validate.notNull(realm, "realm must not be null.");
    Validate.matchesPattern(realm.getId(), "^\\d+$");
    try {
      deleteRealmResource(Integer.parseInt(realm.getId()));
    } catch (Exception e) {
      this.getLogger().error("Error tearing down realm {}. Exception {}. Continuing...", realm, e.toString(), e);
    }
  }

  /**
   * @return A valid, unique realm name.
   */
  protected static String generateUniqueRealmName() {
    return "realm-" + UUID.randomUUID();
  }

  /**
   * @return A valid realm description.
   */
  protected static String generateRealmDescription() {
    return generateRandomAlphabeticString(UserRealmConstants.DESCRIPTION_MAX_LEN);
  }

  protected static String generateRandomAlphabeticString(int length) {
    return RandomStringUtils.randomAlphabetic(length);
  }

  /**
   * @return {@code true} if HTTP requests and responses should always be logged, even if no assertions fail.
   */
  private boolean isAlwaysLogRequestAndResponse() {
    return this.alwaysLogRequestAndResponse;
  }

  private void initAlwaysLogRequestAndResponse() {
    this.alwaysLogRequestAndResponse = false;
    String sysProp = System.getProperty(SYS_PROP_ALWAYS_LOG_REQ_AND_RESP);
    if (sysProp != null) {
      this.alwaysLogRequestAndResponse = Boolean.parseBoolean(sysProp.trim());
    }
  }
}