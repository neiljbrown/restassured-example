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
 * Constants relating to the User Realm APIs under test.
 */
public class UserRealmApiConstants {
  private static final String CONTEXT_URL_PATH = "/user";
  static final String REALM_RESOURCE_URL_PATH = CONTEXT_URL_PATH + "/realm";
  static final String CREATE_REALM_URL_PATH = REALM_RESOURCE_URL_PATH;
  static final String REALM_ID_PATH_VAR_NAME = "realmId";
  static final String GET_REALM_URL_PATH = REALM_RESOURCE_URL_PATH + "/{" + REALM_ID_PATH_VAR_NAME + "}";
  static final String DELETE_REALM_URL_PATH = REALM_RESOURCE_URL_PATH + "/{" + REALM_ID_PATH_VAR_NAME + "}";
}
