#Build the application
FROM maven:3.8.6-openjdk-8 AS build
WORKDIR /app

# pom.xml from the subdirectory to the container root
COPY mavenproject1/pom.xml .
RUN mvn dependency:go-offline

# source code from the subdirectory
COPY mavenproject1/src ./src
RUN mvn clean package -DskipTests

# Deploy to Tomcat
FROM tomcat:9.0-jdk8-openjdk
WORKDIR /usr/local/tomcat

# Copy the WAR file from the build stage
COPY --from=build /app/target/mavenproject1-1.0-SNAPSHOT.war webapps/mavenproject1.war

# Expose port 8080
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]