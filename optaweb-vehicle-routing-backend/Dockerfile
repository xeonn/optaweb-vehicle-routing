FROM fabric8/java-alpine-openjdk8-jre:1.6.3
ENV APP_ROUTING_ENGINE air

ENV DB_HOST=localhost
ENV DB_PORT=5432
ENV DB_NAME=optaroute
ENV DB_USER=postgres
ENV DB_PASSWORD=postgres

COPY target/*-exec.jar /deployments

EXPOSE 8080
VOLUME /deployments/local
