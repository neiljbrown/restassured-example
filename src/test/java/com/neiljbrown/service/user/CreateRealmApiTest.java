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
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;

import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neiljbrown.service.user.dto.UserRealmDto;

/**
 * A set of (out-of-process) functional tests for the Create Realm API implemented using the REST-assured library.
 * <p>
 * To make this example set of tests easy to run the API under test has been stubbed-out (using WireMock), rather
 * than depend on a real API service being deployed and running. When reviewing the use of REST-assured in the
 * test methods you can ignore any references to stubbed methods (and WireMock).
 */
public class CreateRealmApiTest extends AbstractRealmApiTest {

  private static final Logger logger = LoggerFactory.getLogger(CreateRealmApiTest.class);

  /**
   * List of one or more realms created by a test. Supports deleting realms as part of tearing down tests.
   */
  private List<UserRealmDto> createdRealms = new ArrayList<>();

  @Override
  @Before
  public void setUp() throws Exception {
    RestAssured.basePath = UserRealmApiConstants.CREATE_REALM_URL_PATH;
    super.setUp();
  }

  @After
  public void tearDown() {
    this.createdRealms.forEach(this::tearDownCreatedRealm);
  }

  /**
   * Tests the case when the HTTP method used for the request isn't supported by the API - in this case GET.
   */
  @Test
  public void whenUnsupportedHttpMethodGet() {
    stubCreateRealmWhenUnsupportedHttpMethod();

    RestAssured
      .when()
        .get()
      .then()
        .assertThat().statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED).body(isEmptyOrNullString());
  }

  /**
   * Tests the case when the posted media-type isn't supported by the API - in this case application/json.
   */
  @Test
  public void givenUnsupportedMediaTypeJson() {
    stubCreateRealmWhenUnsupportedMediaType();

    RestAssured
      .given()
        .contentType(ContentType.APPLICATION_JSON.getMimeType())
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
    stubCreateRealmWhenInvalidRealmNameMissingOrEmpty();

    UserRealmDto requestedUserRealm = new UserRealmDto((String) null);
    RestAssured
      .given()
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
   */
  @Test
  public void givenUserRealmWithBlankName() {
    stubCreateRealmWhenInvalidRealmNameMissingOrEmpty();

    final String realmName = " ";
    UserRealmDto requestedUserRealm = new UserRealmDto(realmName);

    RestAssured
      .given()
        .body(requestedUserRealm)
      .when()
        .post()
      .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
          "error.code", equalTo("MissingRealmName"),
          "error.message", equalTo("Realm name is mandatory and must be supplied."));
  }

  /**
   * Tests the case when the posted realm resource has a name longer than the specified max.
   */
  @Test
  public void givenUserRealmWithNameLongerThanMax() {
    stubCreateRealmWhenInvalidRealmNameLength();

    UserRealmDto requestedUserRealm =
      new UserRealmDto(generateRandomAlphabeticString(UserRealmConstants.NAME_MAX_LEN + 1));

    RestAssured
      .given()
        .body(requestedUserRealm)
      .when()
        .post()
      .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
          "error.code", equalTo("InvalidRealmName"),
          "error.message", equalTo("Realm name should not be longer than " + UserRealmConstants.NAME_MAX_LEN + " chars."));
  }

  /**
   * Tests the case when the posted realm resource has a description longer than the specified max.
   */
  @Test
  public void givenUserRealmWithDescriptionLongerThanMax() {
    stubCreateRealmWhenInvalidRealmDescriptionLength();

    UserRealmDto requestedUserRealm = new UserRealmDto(generateUniqueRealmName(),
      generateRandomAlphabeticString(UserRealmConstants.DESCRIPTION_MAX_LEN + 1));

    RestAssured
      .given()
        .body(requestedUserRealm)
      .when()
        .post()
      .then()
        .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
        .body(
          "error.code", equalTo("InvalidRealmDescription"),
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

    stubCreateRealmWhenDuplicateRealmName(requestedUserRealm.getName(), requestedUserRealm.getDescription());

    UserRealmDto createdRealm = doTestCreateRealmSuccess(requestedUserRealm);
    assertCreatedRealm(createdRealm, requestedUserRealm);

    RestAssured
      .given()
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

    stubCreateRealmSuccessForRealm(requestedUserRealm, 123, generateRealmKey());
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

    stubCreateRealmSuccessForRealm(requestedUserRealm, 123, generateRealmKey());
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
   * @param createdRealm  The created {@link UserRealmDto realm}.
   * @param expectedRealm A {@link UserRealmDto realm} containing the fields that the {@code createdRealm} should
   * match. Null fields are excluded.
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
  protected RequestSpecification createDefaultRequestSpecification(boolean alwaysLogRequestAndResponse) {
    RequestSpecification defaultRequestSpec = super.createDefaultRequestSpecification(alwaysLogRequestAndResponse);
    defaultRequestSpec.contentType(ContentType.APPLICATION_XML.getMimeType());
    return defaultRequestSpec;
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Methods which stub-out the APIs under test using WireMock, avoiding dependency on real implementation of the APIs
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Registers a stubbed HTTP response that should be returned if a Create Realm API request is made using an invalid
   * HTTP method - any method except POST.
   */
  private void stubCreateRealmWhenUnsupportedHttpMethod() {
    // Instead of registering the same stub for every HTTP method which isn't supported (e.g. HTTP GET as commented
    // out below), register a single stub for all HTTP methods except POST. WireMock doesn't provide such a request
    // matcher out of the box, so use a custom request matcher.
    // See http://wiremock.org/docs/extending-wiremock/#custom-request-matchers
    /*
    WireMock.stubFor(
      get(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .willReturn(
          aResponse().withStatus(HttpStatus.SC_METHOD_NOT_ALLOWED).withHeader("Allow", "POST")));
    */
    this.wireMockRule.stubFor(requestMatching(
      new RequestMatcherExtension() {
        @Override
        public MatchResult match(Request request, Parameters parameters) {
          return MatchResult.of(request.getUrl().equals(UserRealmApiConstants.CREATE_REALM_URL_PATH) &&
            request.getMethod() != RequestMethod.POST);
        }
      }
    ).willReturn(
      aResponse().withStatus(HttpStatus.SC_METHOD_NOT_ALLOWED).withHeader("Allow", "POST")));
  }

  private void stubCreateRealmWhenUnsupportedMediaType() {
    WireMock.stubFor(
      any(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .withHeader("Content-Type", WireMock.notMatching(ContentType.APPLICATION_XML.getMimeType()))
        .willReturn(
          aResponse().withStatus(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE).withHeader("Accept", ContentType
            .APPLICATION_XML.getMimeType())));
  }

  private void stubCreateRealmWhenInvalidRealmNameMissingOrEmpty() {
    WireMock.stubFor(
      post(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .withHeader("Content-Type", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .withRequestBody(matchingXPath("/realm[string-length(normalize-space(@name)) = 0]"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_BAD_REQUEST)
            .withHeader("Content-Type", "application/xml")
            .withBody("<error><code>MissingRealmName</code><message>Realm name is mandatory and must be supplied" +
              ".</message></error>")));
  }

  private void stubCreateRealmWhenInvalidRealmNameLength() {
    WireMock.stubFor(
      post(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .withHeader("Content-Type", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .withRequestBody(matchingXPath("/realm[string-length(@name) >" + UserRealmConstants.NAME_MAX_LEN + "]"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_BAD_REQUEST)
            .withHeader("Content-Type", "application/xml")
            .withBody("<error><code>InvalidRealmName</code><message>Realm name should not be longer than " +
              UserRealmConstants.NAME_MAX_LEN + " chars.</message></error>")));
  }

  private void stubCreateRealmWhenInvalidRealmDescriptionLength() {
    WireMock.stubFor(
      post(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .withHeader("Content-Type", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .withRequestBody(matchingXPath("/realm[string-length(description) > " + UserRealmConstants
          .DESCRIPTION_MAX_LEN + "]"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_BAD_REQUEST)
            .withHeader("Content-Type", "application/xml")
            .withBody("<error><code>InvalidRealmDescription</code><message>Realm description should not be longer " +
              "than " + UserRealmConstants.DESCRIPTION_MAX_LEN + " chars.</message></error>")));
  }

  private void stubCreateRealmWhenDuplicateRealmName(String realmName, String realmDescription) {
    final String stubScenarioName = "duplicateRealmName";
    final String stubScenarioUpdatedState = "realmCreated";
    WireMock.stubFor(
      post(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .inScenario(stubScenarioName)
        .whenScenarioStateIs(STARTED)
        .withHeader("Content-Type", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .withRequestBody(matchingXPath("/realm[string-length(@name) <=" + UserRealmConstants.NAME_MAX_LEN + "]"))
        .withRequestBody(matchingXPath("/realm[@name='" + realmName + "']"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_CREATED)
            .withHeader("Content-Type", "application/xml")
            .withBody("<realm id='123' name='" + realmName +
              "'><key>12345678901234567890123456789012</key><description>" + realmDescription +
              "</description></realm>"))
        .willSetStateTo(stubScenarioUpdatedState));

    WireMock.stubFor(
      post(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .inScenario(stubScenarioName)
        .whenScenarioStateIs(stubScenarioUpdatedState)
        .withHeader("Content-Type", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        .withRequestBody(matchingXPath("/realm[string-length(@name) <=" + UserRealmConstants.NAME_MAX_LEN + "]"))
        .withRequestBody(matchingXPath("/realm[@name='" + realmName + "']"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_BAD_REQUEST)
            .withHeader("Content-Type", "application/xml")
            .withBody("<error><code>DuplicateRealmName</code><message>Duplicate realm name [" + realmName + "]" +
              ".</message></error>")));

    stubDeleteRealmSuccess();
  }
}