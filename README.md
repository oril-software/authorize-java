## Java. Spring Boot. Authorize.Net payments processing
This repository is an example of how to use Authorize.Net for 
payment processing with `anet-java-sdk`

### Prerequisites
* Java 8+
* Maven ^3.6.0
* Authorize account on [Authorize Dashboard](http://sandbox.authorize.net/). 
You can find how to create it [here](https://developer.authorize.net/hello_world.html).

### Configuration
To use this code sample you need to change the following configuration properties in **__application.properties__** file according to your Authorize credentials:

* authorize.apiLoginId
* authorize.transactionKey

### Install and run
* From IDEA: run the **__AuthorizeApplication.class__**
* From CLI: run command `mvn spring-boot:run` 

### Resources
* [Authorize.Net Documentation](https://developer.authorize.net/api/reference/index.html)

### Community
* Please send us your suggestions on how we make this code even more useful for the development community or contribute to this repo!
* Check out our [blog](https://oril.co/blog) with more articles!
