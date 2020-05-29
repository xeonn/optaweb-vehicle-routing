FROM fabric8/java-alpine-openjdk8-jre:1.6.3

ENV JAVA_OPTS="-Xms128m -Xmx128m -XX:MaxPermSize=64m -Djava.net.preferIPv4Stack=true -server -Djava.awt.headless=true -Xconcurrentio"

RUN mkdir -p /local && chmod -R 777 /local
COPY target/*-exec.jar /app.jar

VOLUME ["/local"]

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
