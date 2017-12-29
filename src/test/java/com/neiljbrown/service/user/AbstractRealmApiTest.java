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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.neiljbrown.service.user.dto.UserRealmDto;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.Objects;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Abstract base class providing common behaviour and state for functional tests of Realm APIs.
 */
public abstract class AbstractRealmApiTest {

  private static final String SYS_PROP_PREFIX = "qatest.";
  private static final String SYS_PROP_ALWAYS_LOG_REQ_AND_RESP = SYS_PROP_PREFIX + "alwaysLogReqAndResp";

  /**
   * Automate the startup and shutdown of the WireMock mock HTTP server before and after the execution of each test to
   * support stubbing HTTP response and verifying HTTP requests.
   * <p>
   * Configures WireMock to pick a random free HTTP(S) port, rather than the default of always listening on 8080, which
   * may be in use. The utilised port can subsequently be discovered using wireMockRule.port() and httpsPort().
   */
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

  /**
   * Flag controlling whether HTTP requests and response issued by tests are always logged. Intended to support
   * debugging exact behaviour of tests, even when they pass. Defaults to false - requests and responses will only be
   * logged if assertions fail. Can be set at runtime using system property {@link #SYS_PROP_ALWAYS_LOG_REQ_AND_RESP}.
   */
  private boolean alwaysLogRequestAndResponse;

  @Before
  public void setUp() throws Exception {
    initAlwaysLogRequestAndResponse();
    configureRestAssuredRequestAndResponseDefaults();
  }

  @After
  public void tearDown() {
    WireMock.reset();
  }

  /**
   * @return The Logger for this class.
   */
  protected abstract Logger getLogger();

  private void configureRestAssuredRequestAndResponseDefaults() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    configureRestAssuredRequestDefaults();
    configureRestAssuredResponseDefaults();
  }

  private void configureRestAssuredRequestDefaults() {
    // Rest Assured's default baseURI (http://localhost) suffices, because it's default hostname (localhost) matches the
    // IP address that the stubbed WireMock server binds to / listens on by default (0.0.0.0)
    //RestAssured.baseURI = "http://localhost";

    RestAssured.port = wireMockRule.port();
    RestAssured.requestSpecification = createDefaultRequestSpecification();

    // RestAssured.basePath is config'd in concrete sub-classes as it's the easiest way to configure the full URL path
    // for every test method, once, and once set RestAssured doesn't provide an easy way to query it or append to it.
  }

  private void configureRestAssuredResponseDefaults() {
    if (this.isAlwaysLogRequestAndResponse()) {
      // RestAssurred doesn't currently support enabling logging using a ResponseSpecification as follows -
      //   ResponseSpecification defaultResponseSpecification = new ResponseSpecBuilder().build().log().all();
      //   RestAssured.responseSpecification = defaultResponseSpecification;
      // It throws an IllegalStatException with the message
      //   "Cannot configure logging since request specification is not defined. You may be misusing the API"
      // So, instead, enable response logging by adding a filter to the default list of filters.
      RestAssured.filters(new ResponseLoggingFilter());
    }
  }

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
      .delete(UserRealmApiConstants.DELETE_REALM_URL_PATH)
      .then()
      .assertThat().statusCode(HttpStatus.SC_NO_CONTENT);
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
      .basePath(UserRealmApiConstants.CREATE_REALM_URL_PATH)
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

  /**
   * @return A valid, unique realm name.
   */
  static String generateUniqueRealmName() {
    return "realm-" + UUID.randomUUID();
  }

  /**
   * @return A valid realm description.
   */
  static String generateRealmDescription() {
    return generateRandomAlphabeticString(UserRealmConstants.DESCRIPTION_MAX_LEN);
  }

  static String generateRandomAlphabeticString(int length) {
    return RandomStringUtils.randomAlphabetic(length);
  }

  /**
   * @return A valid realm key.
   */
  static String generateRealmKey() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * Utility method for serialising a {@link UserRealmDto} to an XML representation.
   *
   * @param userRealmDto the {@link UserRealmDto} to serialise.
   * @return a string containing the XML representation.
   */
  static String serialiseUserRealmDtoToXml(UserRealmDto userRealmDto) {
    try {
      StringWriter writer = new StringWriter();
      JAXBContext context = JAXBContext.newInstance(UserRealmDto.class);
      Marshaller marshaller = context.createMarshaller();
      marshaller.marshal(userRealmDto, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new RuntimeException("Error serialising UserRealmDto [" + userRealmDto + "]. Cause [" + e.toString() + "" +
        ".", e);
    }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Methods which stub-out the APIs under test using WireMock, avoiding dependency on real implementation of the APIs
  // -------------------------------------------------------------------------------------------------------------------

  /**
   * Stubs the HTTP response for a successful Delete Realm API request.
   */
  void stubDeleteRealmSuccess() {
    WireMock.stubFor(
      delete(urlPathMatching("^/user/realm/\\d{1,4}$"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_NO_CONTENT)));
  }

  /**
   * Stubs the HTTP response for a valid Create Realm API request - one containing a valid XML representation of a
   * realm to be created.
   *
   * @param requestedUserRealm a {@link UserRealmDto} containing the details of the realm to be created, as specified
   * in the body of the Create Realm API request for which the response is being stubbed.
   * @param realmId the ID of the created realm to be returned in the stubbed response, as would be generated by the
   * Create Realm API.
   * @param realmKey the key of the created realm to be returned in the stubbed response, as would be generated by
   * the Create Realm API.
   */
  void stubCreateRealmSuccessForRealm(UserRealmDto requestedUserRealm, int realmId, String realmKey) {
    Objects.requireNonNull(requestedUserRealm, "requestedUserRealm must not be null.");

    // Create the fully populated realm to be returned by the stub from a _copy_ of the requested realm to avoid
    // mutating the requested realm
    UserRealmDto realmToCreate = new UserRealmDto(requestedUserRealm);
    realmToCreate.setId(Integer.toString(realmId));
    realmToCreate.setKey(realmKey);

    String requestedRealmAsXmlString = serialiseUserRealmDtoToXml(realmToCreate);

    WireMock.stubFor(
      post(urlEqualTo(UserRealmApiConstants.CREATE_REALM_URL_PATH))
        .withHeader("Content-Type", WireMock.containing(ContentType.APPLICATION_XML.getMimeType()))
        // Only match requests which have realm representations with valid length fields
        .withRequestBody(matchingXPath("/realm[string-length(@name) > 0]")) // name field is mandatory
        .withRequestBody(matchingXPath("/realm[string-length(@name) <=" + UserRealmConstants.NAME_MAX_LEN + "]"))
        .withRequestBody(matchingXPath("/realm[string-length(description) <= " + UserRealmConstants
          .DESCRIPTION_MAX_LEN + "]"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.SC_CREATED)
            .withHeader("Content-Type", "application/xml")
            .withBody(requestedRealmAsXmlString)));

    stubDeleteRealmSuccess();
  }
}