# Full OAuth IDCS Assertion library

## Introduction

This code contains source code for a library that supports the full OAuth process to generate a valid Identity Cloud Service (IDCS) Access token to reach Fusion Apps Service. The purpose of is to help customers to Extend Fusion Applications instance applications by protecting their backend function using Full OAuth flow using IDCS Assertion.
This library provides an extra security layer that takes a valid token used to invoke a Resource (like a Function protected by an IDCS Application) and exchange it for another access token using Identity Cloud Service Assertion process. This new token is generated using the IDCS Application of the protected Resource which should be configured as Trusted Client of a target resource, in this case a Fusion Application.
The OAuth IDCS Assertion library is designed to be included in a Function environment which is configured as backend of a API Gateway Deployment endpoint.

This Function library has been developed and tested using JDK 11.0.6.

## Pre requisites

In order to use this Function library, you will require the next:
*  Maven 3.6.0, or higher
*  Java SDK 11
*  Oracle OCI Subscription with access to Oracle OCI API Gateway, Oracle Functions and Oracle Vault Services.
*  Oracle Functions SDK: Before start, it is suggested to follow the *Oracle Functions Quickstart* to ensure have a local environment configured for FN
*  Oracle Identity Cloud Service (IDCS) instance.

As the use case of this library is to extend Saas, ideally it is required to have the next:
*  Oracle Identity Cloud Service (IDCS) instance. Associated with Oracle Fusion Applications (FA), as this library was written for SaaS Extension cases.
*  An Oracle SaaS Subscription as this library was written for SaaS Extension cases.

## Usage Instructions

This guide is described based on the SaaS Extension case using Functions + API Gateway + IDCS. Before integrating the OAuth IDCS Assertion library to the Function implementation it is required to have prepared an IDCS Application configured as Trusted Client of Fusion Apps and Vault secrets that keeps the Kesytore with public certificate and private key that will allow to perform the OAuth Assertion with IDCS to get a new Access Token.

### Prepare IDCS Application

We need an IDCS Application associated with the resource that will be the client of the Fusion Applications instance and the Resource owner of the Function (FN) front by API Gateway Deployment (APGW).

Create an IDCS Confidential Application (FN/APGW IDCS App) based on the instructions provided [here](https://www.oracle.com/webfolder/technetwork/tutorials/obe/cloud/idcs/idcs_rest_postman_obe/rest_postman.html#RegisteraClientApplication).

*  Set *Client Type* as *Confidential* for now,
*  Set *Allowed Grant Type values*:  Client Credentials, JWT Assertion, and Resource Owner.


Add a *Primary audience* in *Resources* section. By doing this, we are configuring our FN/APGW IDCS Application as Oauth Resource owner of our Function. You can use API Gateway instance hostname generated in OCI Console as Primary Audience of the Resource. For example: *https://xxxxxxxxxxxxxxxx.apigateway.us-phoenix-1.oci.customer-oci.com*

In *Resources* section, add a *Scope* as well that could match with the path of the APGW Deployment to invoke your backend Function. For example: */saasextension*

In *Client Configuration*, add in *Resources* subsection in *Token Issuance Policy* the Scope of IDCS Application that corresponds to the target Resource, i.e. the FA instance. Example: *urn:opc:resource:fa:instanceid=xxxxxxxxx:opc:resource:consumer::all*

Now, verify that one of your users registered in IDCS, that also should exists in FA intstance, have access to FA instance by generating an OAuth token from IDCS Application.

Execute a sample curl like this:
```
curl -XGET -u "<user>:<password>" https://<FA_URL>/fscmRestApi/resources/11.13.18.05/expenses
```

Where `/fscmRestApi/resources/11.13.18.05/expenses` it is an FA endpoint. Here can be used an FA endpoint that you prefer.

The next step is to generate an access token with our IDCS Application to verify can be used to reach FA Endpoint. The access token should be generated using credentials of an existent user in IDCS and FA environments.  We can generate the access token using Postman Request Token feature by specifying the scope that match with the IDCS FA Application. For that, we use password credentials as grant type in order to use a username as subject in the generated token.

1.  In Postman, create a request to  FA Endpoint: `https://<FA_URL>/fscmRestApi/resources/11.13.18.05/expenses`
2.  Generate a new Access Token using OAuth 2.0, in Authorization Tab, select Type OAuth 2.0 and click on *Get New Access Token* button.
3.  Use the *Password Credentials* Grant type for this testing purpose. In a Production scenario you should use something like *JWT Assertion* or *Client Credentials*
4.  Use the IDCS URL Access token endpoint: https://<IDCS_URL>/oauth2/v1/token
5.  Fill the *Cliend ID* and *Client Secret* values using the date of FN/APGW IDCS Application created earlier. This information can be found in Application's *Configuration* section.
6.  Use *Username* and *Password* of user that exists in IDCS and FA instance.
7.  Use the *Scope* of FA instance, the same assigned to your FN/APGW IDCS App earlier. Example: *urn:opc:resource:fa:instanceid=xxxxxxxxx:opc:resource:consumer::all*
8.  Click on *Request Token* and then *Use Token*.
9.  Invoke the Request

With this generated token we can use it in the invocation to FA Endpoint and we should expect same output as using generated credentials.

The curl form of the above request:

```
curl --location --request GET 'https://<FA_URL>/fscmRestApi/resources/11.13.18.05/expenses' \
--header 'Content-Type: application/json' \
--header 'Accept: application/json' \
--header 'Authorization: Bearer <JWT_TOKEN>'
```

Now, we need to get a SSL Certificate to be used in the IDCS Assertion for OAuth flow process of your Function. For that, we need to configure our IDCS Application as *Trusted Client* to follow OAuth flow.
You can use a certificate that you’ve obtained from a Certificate Authority (CA) or use a custom, self-signed SSL certificate.

For the custom self signed ssl certificate case, the helper script ./genSelfSignedKeypair.sh can be used for that purpose. Execute it in the next way:

```
./genSelfSignedKeypair.sh --tenant <YourFATenant>  --keyalias <KeyIdAlias>
```

The above comannd will ask you for 2 passwords, keystore password and private key password. Please take note of this.

The next generated files are relevant to be used in our Function code and in the IDCS Application configuration respectively:

*  **\<YourFATenant\>-keystore.p12**: Generated keystore used in library code to extract the certificate and private key for assertion process.
*  **\<YourFATenant\>-\<KeyIdAlias\>-cert.pem**: Public certificate: Used in IDCS Application configuration.

The next pem file is not directly needed for the IDCS Application confgiruation, but the same will be extracted from keystore in library code, and it is exported for reference:

*  **\<YourFATenant\>-\<KeyIdAlias\>-pkcs8-key.pem**: Private Key in PKCS8 Format


Now, configure IDCS Application as *Trusted Client* using your SSL certificate:

* Go to your IDCS Application *Client Configuration* section, and set *Trusted Client* in *Client Type*.
* Import your public certificate in *Certificate* field in IDCS Application *Client Configuration* section. If it is self-signed, use the generated \<YourFATenant\>-\<KeyIdAlias\>-cert.pem) and use the alias you specified as argument of the `genSelfSignedKeypair.sh` script: \<KeyIdAlias\>.

### Prepare Vault Secrets

We need to use Oracle Vault Secrets service to protect sensitive data to be used during the OAuth process. The data to protect will be the related to keystore and certificate access.

Add the next policies to allow Functions to access vaults at root compartment level.

```
allow group <Fn_Group> to manage secret-family in tenancy
allow group <Fn_Group> to manage vaults in tenancy
allow group <Fn_Group> to manage keys in tenancy
```

Where `Fn_Group` is about Functions and created during the *Oracle Functions Quickstart*.

Created a Vault instance in your compartment.

Create a Key instance needed to be associated with the Secrets.

Create the Secrets needed for our Assertion case:
*  Create secret for Keystore Password.
*  Create secret for Private Key Password.
*  Create secret for Keystore file.

In order to achieve Secret for Keystore file, we need to add a Base64 encoded representation of the content of the Keystore file. This can be achieved in the next way:

`openssl base64 -in <YourFATenant>-keystore.p12 -out ksBase64Encoded.txt | cat ksBase64Encoded.txt`

Copy the content of `ksBase64Encoded.txt` as value of the Keystore file Secret. Notice that the Secret have a limit of the size allowed. The keystore file generated using `genSelfSignedKeypair.sh` script does not exceed but just take in mind.

Create Dynamic group with the next rule:

`ALL { resource.type = 'fnfunc', resource.compartment.id = '<OCID>'}`

Where the OCID is for the compartment to be used to create the Secrets.

Add the next Statement to the your Function Tenancy Policy (created during *Oracle Functions Quickstart*) at Tenancy level:

`allow dynamic-group <DynamicGroupName> to read secret-family in tenancy`

### Prepare Function

With the above requirements addressed, now we can integrate the OAuth IDCS Assertion library to your Function.

The function that will use this library, require the next Configuration variables related to IDCS:

| **Config Name** | **Description** | **Mandatory** | **Example** |
| ------ | ------ | ------ | ------ |
| IDCS_URL | Your IDCS Stripe URL | YES | https://idcs-XXXXXXXXXXXXXXXXXXXX.identity.c9dev2.oc9qadev.com/ |
| CLIENT_ID | Your IDCS Application Client Id associated with FN/APGW | YES | 1a2b3c4d5e6f7g8h9i01j2k3l4m5o6p7 |
| KEY_ID | This is the alias of the certificates imported to the Trusted IDCS App | YES | fnassertionkey |
| SCOPE | This scope should match with the target Oauth resource, which is the FA IDCS App one | YES | urn:opc:resource:fa:instanceid=xxxxxxxxxurn:opc:resource:consumer::all |
| AUDIENCE | Audiences for the Assertion process. Multiple values separated by comma  | YES | https://identity.oraclecloud.com/, https://<FA_URL> |
| IDDOMAIN | Name of the FA Instance tenant | YES | faehyp71 |

The above properties should be populated using the information from the `Prepare IDCS Application` section.

The function that will use this library, require the next configuration properties to access to Secrets for Assertion:

| **Config Name** | **Description** | **Mandatory** | **Example** |
| ------ | ------ | ------ | ------ |
| V_KEYSTORE | Secret that contains the secure stored content of the Keystore | YES | ocid1.vaultsecret.oc1.phx.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx |
| V_KS_PASS | Secret that contains the secure stored Password for Keystore | YES | ocid1.vaultsecret.oc1.phx.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx |
| V_PK_PASS | Secret that contains the secure stored Password for Private Key | YES | ocid1.vaultsecret.oc1.phx.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx |

The above properties should be populated using the information from the `Prepare Vault Secrets` section.


An additional configuration variable supported is:

| **Config Name** | **Description** | **Mandatory** | **Example** |
| ------ | ------ | ------ | ------ |
| USE_CACHE_TOKEN | Allows to store the generated idcs assertion token to be reused in future FA invocations if is still valid. By default the beavior value is true | NO | true/false |


With the above configuration variable, the bearer token generated during the IDCS Assertion is cached using the JVM system properties of the Function, with the method: `System.setProperty`. The cached token is validated before use it by comparing the Subject with the incoming token and verifying the expiry time to check if is still valid. If not,  a new access token is requested via OAuth Assertion. This feature can be disabled as mentioned before.

The IDCS JWT Assertion required the self signed certificate and private key to generate the client and user assertions. This library will retrieve the keystore from the Secret with OCID specified in *V_KEYSTORE*. The alias used to retrieve that information should match with *KEY_ID* value in configuration. The passphrase for both, keystore and privatekey, should be retrieved from Vault Secrets Service using the OCIDs specified in *V_KS_PASS* and *V_PK_PASS*.

More information about IDCS Assertion Grant Type process [here](https://docs.oracle.com/en/cloud/paas/identity-cloud/rest-api/AssertionClientSideAppAuth.html).


## Integrate library to Function

Compile first this code before your function to generate the required dependency:

```
cd <PATH_TO>/idcsOAuthAsserter
mvn clean install
```

Create your Function and add the next dependency to your Function `pom.xml`:

```
<dependency>
    <groupId>com.oracle.idcs.oauth</groupId>
    <artifactId>idcsOAuthAsserter</artifactId>
    <version>1.0.0</version>
</dependency>

```

Write the logic for your function to invoke the Fusion Application resource. In order to perform the IDCS OAuth Assertion, we need the access token that we expect comes from API Gateway `Authorization` header and with the `SecurityHelper` methods will return a token result of IDCS Assertion.

In order to use this library to retrieve the access token, you need to use the `RuntimeContext ctx` object from the your Function to initilize the `SecurityHelper` object. The `RuntimeContext` should have the configuration properties described above:

```
    private SecurityHelper idcsSecurityHelper;

    @FnConfiguration
    public void config(RuntimeContext ctx) {
        context = ctx;
        idcsSecurityHelper = new SecurityHelper(context);
    }
```

This library needs to retrieve data from Vault Secrets services from Function environment at runtime. For that, we need to specify the OCI Region where the OCI Service are created:

```
    private SecurityHelper idcsSecurityHelper;

    @FnConfiguration
    public void config(RuntimeContext ctx) {
        context = ctx;
        idcsSecurityHelper = new SecurityHelper(context)
                             .setOciRegion(Region.US_PHOENIX_1);
    }
```
**NOTE**: By default, `OciRegion` is *US_PHOENIX_1*.

If you want to test locally the `SecurityHelper` in a JUnit test, you should add your *OCI Profile* and *OCI Config File* path to the `SecurityHelper` during initialization as follows:

```
    private SecurityHelper idcsSecurityHelper;

    @FnConfiguration
    public void config(RuntimeContext ctx) {
        context = ctx;
        idcsSecurityHelper = new SecurityHelper(context)
                            .setOciRegion(Region.US_PHOENIX_1)
                            .setLocalOciProfile("custom-profile")               // Only for testing locally
                            .setLocalOciConfigFilePath(/path/to/oci/config);    // Only for testing locally
    }
```
**NOTE**: By default, `LocalOCIProfile` is *DEFAULT* and `LocalOciConfigFilePath` is *~/.oci/config*.

Another option to initialize the `SecurityHelper` object is by defining your own `Map` object with the needed properties values, using the contants values in `Constants` class as keys:

```

import static com.oracle.idcs.oauth.util.Constants.*;

...

public OutputEvent handleRequest(InputEvent rawInput)  {
        Map<String,String> securityProps = new HashMap<String, String>();
        // Properties for assertion
        securityProps.put(CLIENT_ID, "<VALUE>");
        securityProps.put(IDCS_URL,  "<VALUE>");
        securityProps.put(KEY_ID,    "<VALUE>");
        securityProps.put(SCOPE,     "<VALUE>");
        securityProps.put(AUDIENCE,  "<VALUE>");
        securityProps.put(IDDOMAIN,  "<VALUE>");

        // Using Secrets Vault Service IDs
        securityProps.put(SECRET_KEYSTORE_ID      , "<VALUE>");
        securityProps.put(SECRET_KS_PASS_ID       , "<VALUE>");
        securityProps.put(SECRET_PK_PASS_ID       , "<VALUE>");

        // Optionals
        securityProps.put(USE_CACHE_TOKEN,  "<VALUE>");

        idcsSecurityHelper = new SecurityHelper(securityProps);
}
```

To start the process of generate the new access token, `SecurityHelper` should use the `InputEvent` object from the `handleRequest` Function method to extract the Bearer token that comes in the API Gateway `Authorization` header. Then you should be able to retrieve an Access Token as result of IDCS Assertion:

```
public OutputEvent handleRequest(InputEvent rawInput)  {
    // Extracts the subject from Token in Fn-Http-H-Authorization.
    idcsSecurityHelper.extractSubFromJwtTokenHeader(rawInput);

    // Get OAuth Access token with JWT Assertion using the principal extracted from Fn-Http-H-Authorization-Token Header
    String bearedAccessToken = idcsSecurityHelper.getAssertedAccessToken();

    // DO Stuff to invoke Fusion Apps endpoint

    // Return OutputEvent object
    return out;
}
```


Create a `lib` diretory in the same level as your  `func.yaml` file Function and add the next Maven plugin to your Function `pom.xml`:

```
<build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>2.2</version>
                <executions>
                    <execution>
                        <id>download-required-libs</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>

                        <configuration>
                            <includeArtifactIds>idcsOAuthAsserter</includeArtifactIds>
                            <overWriteReleases>true</overWriteReleases>
                            <overWriteSnapshots>true</overWriteSnapshots>
                            <overWriteIfNewer>true</overWriteIfNewer>
                            <copyPom>true</copyPom>
                            <outputDirectory>
                                ${project.basedir}/lib
                            </outputDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

Before deploy your function to OCI Function Application, compile your code using Maven commands in order to generate needed files, `idcsOAuthAsserter-1.0.0.jar` and `idcsOAuthAsserter-1.0.0.pom`, in `lib/` directory

```
$ cd <PATH_TO_YOUR_FUNCTION>/
$ mkdir -pv lib/
$ mvn clean install

$ ls lib/
idcsOAuthAsserter-1.0.0.jar  idcsOAuthAsserter-1.0.0.pom
```

Create a *Dockerfile* to your Function dir and add the next content:

```
FROM fnproject/fn-java-fdk-build:jdk11-1.0.107 as build-stage
WORKDIR /function

COPY lib /function/lib

# Uncomment this line and populate if you are behind a proxy
# ENV MAVEN_OPTS -Dhttp.proxyHost=<ProxyHost> -Dhttp.proxyPort=<ProxyPort> -Dhttps.proxyHost=<ProxyHost> -Dhttps.proxyPort=<ProxyPort>

ADD lib/idcsOAuthAsserter*.pom /function/pom.xml
RUN ["mvn", "org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file", "-Dfile=/function/lib/idcsOAuthAsserter-1.0.0.jar", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=false", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target"]

ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=false", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target"]

ADD src /function/src
RUN ["mvn", "package"]
FROM fnproject/fn-java-fdk:jre11-1.0.107
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/

CMD ["<INSERT_FUNCTION_PACKAGE_AND_CLASSNAME>::handleRequest"]
```

Modify the proxy properties `MAVEN_OPTS` variable in the *Dockerfile* if needed.

Optionally, you can set the in Function the required Configuration variables for the OAuth IDCS Assertion library via the `func.yaml` configuration or if you prefer, you can do it in OCI Console or via FN CLI:

```
config:
  AUDIENCE: <AUDIENCE_VALUES>
  CLIENT_ID: <YOUR_CLIENT_ID>
  IDCS_URL: <YOUR_IDCS_URL>
  IDDOMAIN: <YOUR_FA_TENANT_NAME>
  KEY_ID: <YOUR_IDCS_URL>
  SCOPE: <FA_RESOURCE_SCOPE>
  V_KEYSTORE: <YOUR_KS_OCID>
  V_KS_PASS: <YOUR_KSPASS_OCID>
  V_PK_PASS: <YOUR_PKPASS_OCID>
```
**NOTE:** The above properties are mandatory in order to invoke the `SecurityHelper.getAssertedAccessToken` method.


Deploy your function to your Application:

```
fn deploy --app <APP_NAME> --verbose
```

Test invoke your function from CLI. The expected output is the next as we expect the call to the function from from an API Gateway Deployment:
```
fn invoke <yourapp> <yourFunction>

{"error": "Message: No Authentication Bearer token found"}

```


At this point your function is prepared to perform the full OAuth flow with IDCS to invoke Fusion Apps endpoint. Now, you need to configure a Deployment in your API Gateway instance to point to your function in order to be called from a client like Visual Builder Cloud Service.

Additional details in: [Extend SaaS applications with a cloud native approach](https://docs.oracle.com/en/solutions/extend-saas-cloud-native/index.html#GUID-B1C4064D-C47E-4277-93F0-C004F6CF1C00)

Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.