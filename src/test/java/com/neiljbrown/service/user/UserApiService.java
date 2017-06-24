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

/**
 * The service hosting and providing the Realm APIs under test.
 */
public class UserApiService {
  public static final String CREATE_REALM_URL_PATH = "/realm";
  public static final String REALM_ID_PATH_VAR_NAME = "realmId";
  public static final String GET_REALM_URL_PATH = "/realm/{" + REALM_ID_PATH_VAR_NAME + "}";

  private static final String PROTOCOL = "http://";
  private static final String CONTEXT_URL_PATH_PREFIX = "/user";
  private static final String SYS_PROP_PREFIX = "qatest.userApiService.";

  private static String domainName;
  private static int port;
  static {
    initDomainName();
    initPort();
  }
  private static String hostName = "www." + domainName;
  private static String rootUrl = PROTOCOL + hostName + CONTEXT_URL_PATH_PREFIX;

  public static String getRootUrl() {
    return rootUrl;
  }

  public static int getPort() {
    return port;
  }

  private static void initDomainName() {
    domainName = "test01.neiljbrown.net";
    String sysProp = System.getProperty(SYS_PROP_PREFIX + ".domainName");
    if (sysProp != null) {
      domainName = sysProp.trim();
    }
  }

  private static void initPort() {
    port = 8080;
    String sysProp = System.getProperty(SYS_PROP_PREFIX + ".port");
    if (sysProp != null) {
      port = Integer.parseInt(sysProp.trim());
    }
  }
}
