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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
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
   * Regex used to match the URL path of a valid Get User Realm API request
   */
  private static final String GET_USER_REALM_URL_PATH_REGEX = "^/user/realm/\\d{1,4}$";

  /**
   * List of one or more realms created by a test. Supports deleting realms as part of tearing down tests.
   */
  private List<UserRealmDto> createdRealms = new ArrayList<>();

  @Override
  @Before
  public void setUp() throws Exception {
    RestAssured.basePath = UserRealmApiConstants.GET_REALM_URL_PATH;
    super.setUp();
  }

  @Override
  @After
  public void tearDown() {
    this.createdRealms.forEach(this::tearDownCreatedRealm);
  }

  /**
   * Tests the case when the HTTP method used for the request isn't supported by the API - in this case POST.
   */
  @Test
  public void whenUnsupportedHttpMethodPost() {
    stubGetRealmWhenUnsupportedHttpMethod();

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
    stubGetRealmWhenUnsupportedMediaType();

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
   */
  @Test
  public void givenRealmIdLessThanMinZero() {
    final int realmId = UserRealmConstants.ID_MIN - 1;
    stubGetRealmWhenInvalidRealmId(Integer.toString(realmId));

    RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, realmId)
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
      .body(
        // Example of using XPath to assert body contents. XPath seems a bit clunkier than GPath for this purpose. The
        // error diagnostics of the Hamcrest hasXPath() method it relies upon is also not as helpful as GPath - it
        // returns the whole XML document, rather than what was matched.
        // Note - Hamcrest's hasXPath() unexpectedly fails to return text value of node when using an XPath expression
        // of either /error/code/text() or /error/code, resulting in an assertion failure. Therefore resorted to passing
        // a full XPath condition to hasXPath() instead.
        //hasXPath("/error/code/text()"), equalTo("InvalidRealmId"),
        //hasXPath("/error/message/text()"), equalTo("Invalid realm id [" + realmId + "]."));
        hasXPath("/error/code[text()='InvalidRealmId']"),
        hasXPath("/error/message[text()='Invalid realm id [" + realmId + "].']"));

  }

  /**
   * Tests the case when the requested realm ID is greater than the documented max.
   */
  @Test
  public void givenRealmIdGreaterThanMax() {
    final int realmId = UserRealmConstants.ID_MAX + 1;
    stubGetRealmWhenInvalidRealmId(Integer.toString(realmId));

    RestAssured.given()
      .pathParam(UserRealmApiConstants.REALM_ID_PATH_VAR_NAME, realmId)
      .when()
      .get()
      .then()
      .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
      .body(
        // Uses REST Assured's support for Groovy's GPath expression language to match and extract XML
        "error.code", equalTo("InvalidRealmId"),
        "error.message", equalTo("Invalid realm id [" + realmId + "]."));
  }

  /**
   * Tests the case when the requested realm ID is a non-integer string.
   */
  @Test
  public void givenRealmIdNonInteger() {
    final String realmId = "foo";
    stubGetRealmWhenInvalidRealmId(realmId);

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
   */
  @Test
  public void givenRealmDoesNotExist() {
    stubDeleteRealmSuccess();
    final int realmId = UserRealmConstants.ID_MAX;
    stubGetRealmWhenRealmDoesNotExist(realmId);

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
    stubCreateRealmSuccessForRealm(realmToCreate, 123, generateRealmKey());
    final UserRealmDto createdRealm = createRealmResource(realmToCreate);
    this.createdRealms.add(createdRealm);

    stubGetRealmSuccessForRealm(createdRealm);

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
   * Tests the case when the requested realm exists and is only populated with mandatory fields - ID, key and name -
   * and not optional fields, e.g. description.
   */
  @Test
  public void givenRealmExistsWithMandatoryFieldsOnly() {
    final UserRealmDto realmToCreate = new UserRealmDto(generateUniqueRealmName());
    stubCreateRealmSuccessForRealm(realmToCreate, 123, generateRealmKey());
    final UserRealmDto createdRealm = createRealmResource(realmToCreate);
    assertThat(createdRealm.getDescription()).isNull();

    this.createdRealms.add(createdRealm);

    stubGetRealmSuccessForRealm(createdRealm);

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

  // -------------------------------------------------------------------------------------------------------------------
  // Methods which stub-out the APIs under test using WireMock, avoiding dependency on real implementation of the APIs
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Registers a stubbed HTTP response that should be returned if a Create Realm API request is made using an invalid
   * HTTP method - any method except GET.
   */
  private void stubGetRealmWhenUnsupportedHttpMethod() {
    // Instead of registering the same stub for every HTTP method which isn't supported (e.g. HTTP POST as commented
    // out below), register a single stub for all HTTP methods except GET. WireMock doesn't provide such a request
    // matcher out of the box, so use a custom request matcher.
    // See http://wiremock.org/docs/extending-wiremock/#custom-request-matchers
    /*
    WireMock.stubFor(
      post(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .willReturn(
          aResponse().withStatus(HttpStatus.SC_METHOD_NOT_ALLOWED).withHeader("Allow", "POST")));
    */
    this.wireMockRule.stubFor(requestMatching(
      new RequestMatcherExtension() {
        @Override
        public MatchResult match(Request request, Parameters parameters) {
          return MatchResult.of(request.getUrl().matches(GET_USER_REALM_URL_PATH_REGEX) &&
            request.getMethod() != RequestMethod.GET);
        }
      }
    ).willReturn(
      aResponse().withStatus(HttpStatus.SC_METHOD_NOT_ALLOWED).withHeader("Allow", "GET")));
  }

  private void stubGetRealmWhenUnsupportedMediaType() {
    WireMock.stubFor(
      any(urlMatching(GET_USER_REALM_URL_PATH_REGEX))
        .withHeader("Accept", WireMock.notMatching(ContentType.APPLICATION_XML.getMimeType()))
        .willReturn(
          aResponse().withStatus(HttpStatus.SC_NOT_ACCEPTABLE).withHeader("Accept", ContentType.APPLICATION_XML
            .getMimeType())));
  }

  private void stubGetRealmWhenInvalidRealmId(String realmId) {
    // Use an alternative, weaker regex to match requests with an invalid realm ID - don't constrain type or max length
    final String getUserRealmUrlPathRegex = "^/user/realm/.*$";

    WireMock.stubFor(
      get(urlMatching(getUserRealmUrlPathRegex))
        .withHeader("Accept", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_BAD_REQUEST)
            .withHeader("Content-Type", "application/xml")
            .withBody("<error><code>InvalidRealmId</code><message>Invalid realm id [" + realmId + "]" +
              ".</message></error>")));
  }

  private void stubGetRealmWhenRealmDoesNotExist(int realmId) {
    final String getUserRealmUrlPath = buildGetRealmUrlPath(realmId);
    WireMock.stubFor(
      get(urlMatching(getUserRealmUrlPath))
        .withHeader("Accept", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_NOT_FOUND)
            .withHeader("Content-Type", "application/xml")
            .withBody("<error><code>RealmNotFound</code><message>Realm [" + realmId + "] not found" +
              ".</message></error>")));

  }

  private void stubGetRealmSuccessForRealm(UserRealmDto realmToGet) {
    Objects.requireNonNull(realmToGet, "realmToGet must not be null.");
    int realmId;
    try {
      realmId = Integer.parseInt(realmToGet.getId());
    } catch (NumberFormatException e) {
      throw new RuntimeException("Test error. realmToGet must have an integer realm id, not [" + realmToGet.getId() +
        "].");
    }
    final String getUserRealmUrlPath = buildGetRealmUrlPath(realmId);
    String realmAsXmlString = serialiseUserRealmDtoToXml(realmToGet);
    WireMock.stubFor(
      get(urlMatching(getUserRealmUrlPath))
        .withHeader("Accept", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/xml")
            .withBody(realmAsXmlString)));
  }

  private static String buildGetRealmUrlPath(int realmId) {
    return UserRealmApiConstants.GET_REALM_URL_PATH.replaceFirst(
      "\\{" + UserRealmApiConstants.REALM_ID_PATH_VAR_NAME + "}", Integer.toString(realmId));
  }
}