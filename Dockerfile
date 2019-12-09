FROM openjdk:8-jdk-alpine

WORKDIR /home
COPY *.jar app.jar
COPY *.properties ./
COPY start.sh start.sh
RUN chmod +x start.sh

EXPOSE 80

ENTRYPOINT ["/home/start.sh"]