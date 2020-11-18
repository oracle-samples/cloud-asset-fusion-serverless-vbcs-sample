
# Serverless SaaS Extensions using Oracle Functions, OCI API Gateway and VBCS

## Introduction

This repository contains source code to accompany a recent blog article by Angelo Santagata and Mike Muller on a [Cloud Native Approach to Extending Your SaaS applications ](https://www.ateam-oracle.com/the-cloud-native-approach-to-extending-your-saas-applications). The sample demonstrates how customers can develop extensions for SaaS applications using [Visual Builder Cloud Service](https://www.oracle.com/uk/application-development/cloud-services/visual-builder/) (VBCS) and when needed call backend code using Oracle Functions instead of having to deploy a full blown customer managed middle tier (e.g. a Weblogic cluster).  A YouTube video explaining the solution can be found at this URL https://www.youtube.com/watch?v=f2sLHsphR3I 

When using products like VBCS to extend SaaS many of the REST requests go direct to the SaaS Service but there are many scenarios where they will need to go via a middle tier.

These reasons may include:

- Requests where complex business, or integration logic, is required
- When VBCS needs to get its data from non REST data sources, e.g. SOAP, SOCKET, ATOM feeds etc.
- Where Middle-Tier Caching required

Developers often deploy these APIs in full featured application servers (e.g. Oracle Java Cloud Service SaaS Extensions (JCSSX), Oracle Java Cloud Service (JCS), Weblogic (WLS) ), these services work well for many customers but they do have the following disadvantages:

- Management of the service is usually required. e.g. JCS requires patching of the Weblogic Server, the operating system and even the database
- Configuration of the service can be complicated, e.g. for Kubernetes many customers install [ISTIO](https://istio.io/) for flexibility but this adds to the complexity, additionally balancers are also required
- Cost: The services tend to be running 24x7 even when not being used, for some SaaS extensions where a only few users the extension this can be very wasteful

The use of Serverless technologies, such as [Oracle Functions](https://www.oracle.com/cloud/cloud-native/functions/),  can alleviate this problem:

- Being serverless means there are no costs incurred when the code is not being used. Oracle's generous elastic pricing for Oracle Functions means that the first 2 Million requests per month are free (see [elastic pricing section](https://www.oracle.com/cloud/cloud-native/functions/) for more details)
- Being servlerless No management of the server (SaaS Customers like this!)
- Infrastructure stuff like elastic scaling and routing is done for the user by Oracle




# Solution Architecture

### ![](https://cdn.app.compendium.com/uploads/user/e7c690e8-6ff9-102a-ac6d-e4aebca50425/3dc1ac90-852a-4ad2-9e7e-0679d4946124/File/0f43a4b2839818e9ecbfd4c1bbe589eb/cloudnativesaas_fn_architecture2.png)

This architecture above demonstrates the following features:

1. Execution of code within Oracle Functions exposed using the REST protocol which is compatible with VBCS
2. Enforcing authentication security based on the Fusion Applications user using Oracle API Gateway
3. The user identity is propagated throughout the flow, from the API Gateway all the way to Oracle Fusion SaaS 

### Services Used

[Oracle API Gateway](https://www.oracle.com/cloud/cloud-native/api-gateway/):  This service is used to front end our REST requests, check the requests contain the right authentication credentials and then relay the request to the appropriate Oracle Functions service

[Oracle Functions](https://www.oracle.com/cloud/cloud-native/functions/): Oracle Functions is Oracle's server-less infrastructure for storing business logic in a variety of languages, e.g. JavaScript, Go and Java 

[Oracle Identity Cloud](https://www.oracle.com/cloud/security/cloud-services/identity-cloud.html) (IDCS):  Oracle's Identity Management service supporting LDAP requests and a variety of other identity management functions

[Key Management Service](https://www.oracle.com/cloud/security/cloud-services/key-management.html) (KMS): Oracle Key Management is a managed service, so you can focus on your encryption needs rather than on procuring, provisioning, configuring, updating and maintaining HSMs and key management software. For this example we use Key Management Service to store our OAuth Client Secret's in an encrypted form

[Visual Builder Cloud Service (VBCS)](https://www.oracle.com/application-development/cloud-services/visual-builder/): Is Oracle's Low Code HTML development environment, ideal for extending Oracle SaaS.

[Oracle Fusion SaaS](https://www.oracle.com/applications/): Oracle Fusion SaaS is a market leading set of enterprise applications covering ERP, HCM, Customer experience and many more business application areas

 

### Solution Walk Through

- The user launches the VBCS application standalone or from within Oracle Fusion SaaS

- The sample application issues  a REST request to the Oracle OCI API Gateway first 

- The API Gateway introspects the URI and recognize  it is a "secured" endpoint and calls the custom authentication function (*idcs_ocigw*) passing the users authentication token as a parameter

- The authentication function calls IDCS and checks that the received token is valid (including OAuth  scopes). If it is not then the request is rejected with a unauthenticated error

- Upon successful authentication the request is then passed to the business logic function requested, in our case  saasopportunitiesfn`

- *`saasopportunitiesfn`* is a service based function, in that it is a single function that deals with multiple requests. Depending on the request URI (`GET /opportunities`, `GET /opportunities/{id}` or `PATCH /opportunities/{id}` ), the function will perform slightly different business logic and return the results to the user

  **NOTE**:  Functions are often designed to do ONE thing and one thing only. In this design the "one thing" the function is doing is managing the three related requests. Some developers prefer to create a separate function for each method and sharing the code using build scripts.  Both methods are OK (separate functions being more Function purist) and both have their advantages. If the reader does decide to go with a service pattern, like this example, then we do recommend that the number of "sub functions" is kept to a minimum e.g. less than 5
  
  The added advantage of using a service pattern is that the Oracle Function is kept "warm" and thus reduces the number of cold starts the user may face. The premise is that if the user lists a number of opportunities then they are very likely to want to query the details of a single opportunity, in this scenario the service function is warmed up on the first request and deals with all the requests.

- The business logic function calls Oracle SaaS, passing the authentication token, retrieves the data and then proxies the data back to the caller via the API Gateway

- If an error occurs, e.g. the record does not exist, then the business logic function returns the appropriate error message in a way that API Gateway understands to relay to the client

- VBCS receives the data and displays it on the user interface

### Solution Notes

- The client above is VBCS however it could be anything which can consume REST, E.g. Oracle ADF Faces, ReactJS, Oracle Fusion CX groovy functions, a mobile app Etc. 

## Installation and Running the Example

Please refer to the [installation guide](docs/INSTALLATION.md) for instructions on how to install and run the sample.


## Security
For more information on security please see [SECURITY.md](SECURITY.md) 

## Help
If you need help with this sample please log an issue and we'll try and help



Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.




