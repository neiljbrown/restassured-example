#REST-assured Example

##Overview
This project provides an example of how to use the [REST-assured](http://rest-assured.io/) library to write black-box, 
functional tests for REST APIs in Java.

REST-assured is a Java library that aims to make it simpler to write out-of-process, functional tests for REST APIs, 
that are written in Java (or other JVM languages). It runs on top of existing Java testing frameworks (JUnit), and 
includes a DSL for building API requests and asserting API responses.
   
In this example project, REST-assured is used to implement a suite of functional tests for a couple of REST APIs 
hosted by an imaginary User service that supports creating and retrieving a resource/entity known as a (user) 'realm', 
via HTTP POST and GET methods. An outline spec. of these APIs is included below. 
    
##Code
The example REST-assured test cases are implemented in Java (8.x) using JUnit (4.x), REST-assured (3.x) and AssertJ.

The tests can be found in two Java classes in the project's src/main/test/java folder:
* com.neiljbrown.service.user.CreateRealmApiTest - Test suite for the [Create Realm API](#createRealm)
* com.neiljbrown.service.user.GetRealmApiTest - Test suite for the [Get Realm API](#createRealm)

The APIs under test have been stubbed-out (using the [WireMock](http://wiremock.org/) library). This makes the example 
tests simple to run, by avoiding a dependency on a real API service having been deployed, and needing to be 
running in a separate process. For the purposes of this project you can ignore the stubbing and use of WireMock.

##Building and Running the Examples 
The example tests can be compiled and run from within the comfort of your IDE (or from the command line using 
Gradle).

First ensure you have JDK 8+ installed.  

Then cd to the directory into which you cloned/checked-out the project and use the supplied Gradle build script to 
generate an IDE project for either Eclipse or IntelliJ IDEA, using one of the following commands  
``./gradlew eclipse`` or ``./gradlew idea`` 

Import the generated project into your IDE. 

Open the project in your IDE and run the tests contained in one of the aforementioned test classes as you would a 
JUnit test. 

To compile and run the tests from the command line enter the command ``./gradlew test`` 

##Debugging the Examples
You can debug execution of the tests from within your IDE by setting breakpoints as you would with any other JUnit test.

The example tests also support logging of the HTTP requests and responses which they make to the console, using 
REST-assured's underlying logging support. By default only failed requests and responses are logged (which is also the
REST-assured default). You can additionally enable logging of successful requests and responses by setting the Java 
system property 'qatest.alwaysLogReqAndResp' to 'true', e.g. java ... -Dqatest.alwaysLogReqAndResp=true. 

An example of an HTTP request and response logged by REST-assured is shown below -   
```
Request method:	POST
Request URI:	http://localhost:52650/user/realm
Proxy:          <none>
Request params:	<none>
Query params:	<none>
Form params:	<none>
Path params:	<none>
Headers:        Accept=application/xml
                Content-Type=application/xml; charset=ISO-8859-1
Cookies:        <none>
Multiparts:     <none>
Body:
<realm name="realm-0be9c302-69e8-4e49-9fae-72d9e119bb1e">
  <description>iNuZRnYHQNvGIgxKnnBmfLbzuPvuRlpdIilHhFxmpKAKCaUMBlkYQePxcHJkfcEFqwhFyWcXLnWEDMUMTHXhBEzHyyIdipHceXhovqarMpPZUGCcpCKtGDRmJLckMuHmHYbNOCzdAAzvuLsBxucrmMnLFWbDvwsyZFNlEkzZRzBVJFUunXkDIjPnwoPlKceuOjwMsLOUPXdbKwQWmKoTIxXqTadTkEyxhfkATjsQkfElnPSZVwKSfXRfDyWaogUQ</description>
</realm>
```

##API Specification
This section contains the spec. of the couple of APIs for which the tests have been written.

The APIs specified support creating, retrieving and deleting a (User) Realm.
 
###Realm Resource
A (User) Realm is a context for the registration and authentication of a user.  The resource comprises the following 
fields. All fields are mandatory unless otherwise stated.

|Field Name|Description & Constraints|
|----------|-------------------------|
|id|Unique, system-generated identifier for the realm. An integer value in the range 1 to 9999.
|name|Unique name of the realm. Serves as an alias for the realm ID. Max length of 100 chars.|
|description|Description of the realm. Optional. Max length of 255 chars.| 
|key|System generated encryption key. Fixed length 32 char hex-encoded string.|

###Resource Representation
The APIs only support producing and consuming XML representations of a Realm. The schema for this representation, 
specified by example, is as follows -  

```xml
<realm id="123" name="Acme"> 
  <description>Realm for authenticated users of Acme corp.</description> 
  <key>92f1aea4bb92c3661a9c85ee81503e28</key>					
</realm>				
```

###API Error Handling
The APIs report errors by returning an HTTP response with a status code in the 4xx (client) or 5xx (server) range. An
 “error” resource may also be returned in the response body to distinguish errors reported by the API, and to 
further classify an error. An error resource contains a unique code and a message. The error code is a unique string 
in camel-case intended for interpretation by API clients. An example HTTP response for an error returned by the APIs 
is shown below -

```
HTTP/1.1 404 Not Found 
Content-type: application/xml; charset=utf-8 
...
<error>						
  <code>RealmNotFound</code>
  <message>Realm [123] not found.</message>	 
</error>
```

###<a name="createRealm"></a> Create Realm API
Creates a new realm including the generation of an encryption key. 

####Supported Methods
|Method|URL|
|------|---|
|POST|http://{host}:8080/user/realm|

####Example Request
```
POST /user/realm HTTP/1.1 
Content-Type: application/xml; charset=utf-8

<realm name=“{name}”> 
  <!-- Optional -->
  <description>{description}</description>
</realm>
```
####Example Success Response
```
HTTP/1.1 201 Created 
Content-Type: application/xml; charset=utf-8 
								
<realm id=“{id}” name=“{name}”> 
  <description>{description}</description> 
  <key>{key}</key>
</realm>
```

####Example Error Response
If the mandatory realm name is not supplied or if supplied is blank/empty:					
```
HTTP/1.1 400 Bad Request 
Content-type: application/xml; charset=utf-8 

<error> 
  <code>MissingRealmName</code>	
  <message>Realm name is mandatory and must be supplied.</message>	
</error>
```
	
If the requested realm name matches the name of an existing realm.
```
HTTP/1.1 400 Bad Request 
Content-type: application/xml; charset=utf-8 
						
<error> 
  <code>DuplicateRealmName</code>
  <message>Duplicate realm name [{realmName}].</message>	
</error> 
```

If the requested realm name is longer than 100 chars.
```
HTTP/1.1 400 Bad Request 
Content-type: application/xml; charset=utf-8 
						
<error> 
  <code>InvalidRealmName</code>
  <message>Realm name should not be longer than 100 chars.</message>	
</error> 
```

If the requested realm description is longer than 255 chars.
```
HTTP/1.1 400 Bad Request 
Content-type: application/xml; charset=utf-8 
						
<error> 
  <code>InvalidRealmDescription</code>
  <message>Realm description should not be longer than 255 chars.</message>
</error>
```

Other error responses

|HTTP Status Code & Phrase|Cause|
|-------------------------|-----|
|415 Unsupported Media Type|The entity supplied in the body of the request cannot be processed in the media-type specified in the Content-Type request header.|


###<a name="getRealm"></a> Get Realm API
Returns the details of an individual realm, identified by its unique id.

####Supported Methods
|Method|URL|
|------|---|
|GET|http://{host}:8080/user/realm/{realmId}|

####Example Request
```
GET /user/realm/{realmId} HTTP/1.1 
Accept: application/xml						
```
####Example Success Response
```
HTTP/1.1 200 OK 
Content-Type: application/xml; charset=utf-8
 
<realm id=“{id}” name=“{name}”> 
  <description>{description}</description> 
   <key>{key}</key>						
</realm>				
```

####Example Error Response
If the requested realm id is not an integer value ­or if it is an integer value larger than the allowed maximum (9999).					
```
HTTP/1.1 400 Bad Request 
Content-type: application/xml; charset=utf-8 

<error> 
  <code>InvalidRealmId</code>
  <message>Invalid realm id [{realmId}].</message>				
</error>
```

If the requested realm id does not identify an existing realm.
```						
HTTP/1.1 404 Not Found 
Content-type: application/xml; charset=utf-8 

<error>						
  <code>RealmNotFound</code>
  <message>Realm [123] not found.</message>	 
</error>
```

### Delete Realm API
Deletes a user realm by id.  

####Supported Methods
|Method|URL|
|------|---|
|DELETE|http://{host}:8080/user/realm/{realmId}|

####Example Request
```
DELETE /user/realm/{realmId} HTTP/1.1
```

####Example Success Response
```
HTTP/1.1 204 No Content 
Date: Tue, 15 Nov 2017 08:12:31 GMT
```

####Example Error Response
If the requested realm id is not an integer value ­
```						
HTTP/1.1 400 Bad Request 
Content-type: application/xml; charset=utf-8 

<error> 
  <code>InvalidRealmId</code>
  <message>Invalid realm id [{realmId}].</message>				
</error>
```

###Common Errors
The following error responses that could be returned by any of the APIs above:

|HTTP Status Code & Phrase|Cause|
|-------------------------|-----|
|405 Method Not Allowed|The specified HTTP method is not supported for the requested resource.|
|406 Not Acceptable|The requested resource cannot be returned in the media-type specified by  the Accept header.|

