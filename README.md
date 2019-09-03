How to run the application
==========================
Required software to build and run this project

1. [Java Development Kit 11](https://adoptopenjdk.net/?variant=openjdk11&jvmVariant=hotspot)
2. [Maven](https://maven.apache.org/download.cgi) (Optional)
3. [Payara](https://www.payara.fish/software/downloads) (Optional)

Below is several ways to build and run the application. 


Start with maven
----------------
1. Build & run: mvnw package payara-micro:start
   - Context is http://localhost:8080/fant

Create bundle and start with java (Jakarta EE - Boot)
-------------------------------
1. Build: mvnw payara-micro:bundle 
2. Run: java -jar target/fant-microbundle.jar
   - Context is http://localhost:8080/

Create war and deploy in Payara server:
---------------------------------------
1. Build: mvnw package 
2. Run: Deploy target/jakarta-template.war in Payara server
   - Context is http://localhost:8080/jakarta-template
    
Create Docker deployment & start with Docker
--------------------------------------------
See [Jib maven plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin)
for more information

1. mvnw package jib:dockerBuild
2. docker run -d -p 8080:8080 jakarta-template
    - Context is http://localhost:8080/
