# LiV backend
## Built with
* [Java 11](https://www.java.com/en/)
* [Spring Boot 2.4.0](https://spring.io/)
* [Quorum](https://consensys.net/quorum/)

## Software used
* [IntelliJ IDEA](https://www.jetbrains.com/idea/)
* [Postman](https://www.postman.com/)
* [GitHub](https://github.com/)
* [Kaleido](https://www.kaleido.io/)

## Dependencies
* spring-boot-starter-web: Spring Boot components for building web applications
* spring-boot-devtools: additional development tools
* spring-boot-starter-test: Spring Boot components for running application tests
* okhttp: HTTP client
* logging-interceptor: OkHttp's logger
* json: library for working with JSON
* maven-compiler-plugin: Apache Maven plugin
* java-jwt: library for generation and verification of JSON Web Tokens
* mysql-connector-java: library for working with MySQL database

## Directory structure
* contracts: directory with Solidity smart contracts
* src/main: Java source code
    * java/com/liv/cryptomodule: application codebase
        * controllers: REST API controllers
        * dto: Data Transfer Objects
        * util: utility classes for connectivity and configurations
    * resources/META-INF: application auto-configuration
    
## Starting a development environment
## Prerequisities
* [JDK 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html) or [Java SE 11](https://www.oracle.com/java/technologies/javase-downloads.html)
* [Apache Maven](https://maven.apache.org/install.html)

## Configure the environment
Open the terminal and clone the repository:

`https://github.com/LedgerProject/LiV_backend`

Before running the aplication, you need to complete the configuration files `application.properties` and `config.properties` according to the comments in them

When configuration files are complete, open the terminal, go to the project folder and run

`mvn spring-boot:run`

If everything went well you should see the following message:

`Started CryptoModuleApplication in 1.74 seconds (JVM running for 3.208)`
