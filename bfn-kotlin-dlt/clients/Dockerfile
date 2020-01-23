FROM openjdk:latest

COPY build/libs/clients-0.1.jar /app.jar
COPY bfn.json /bfn.json
ENV GOOGLE_APPLICATION_CREDENTIALS=/bfn.json
ENTRYPOINT [ "java",  "-jar", "/app.jar" ]

