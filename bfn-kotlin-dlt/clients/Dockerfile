FROM openjdk:latest

COPY build/libs/clients-0.1.jar /app.jar
COPY bfn.json /bfn.json
ENV GOOGLE_APPLICATION_CREDENTIALS=/bfn.json
ENTRYPOINT [ "java",  "-jar", "/app.jar", "--server.port=10050","--config.rpc.host=nd-601-154-124.rg-788-479.p2pify.com", "--config.rpc.port=10201", "--config.rpc.username=mystifying_cray", "--config.rpc.password=device-grimy-herbs-kelp-cramp-sprout" ]

