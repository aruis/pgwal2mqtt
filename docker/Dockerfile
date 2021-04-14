FROM openjdk:11.0-jre-slim

ENV VERTICLE_HOME /usr/verticles
ENV VERTICLE_FILE pgwal2mqtt-1.0.3-fat.jar

#RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt buster-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY target/ACCC4CF8.asc /

RUN apt-get update \
    && apt-get -y install vim gnupg \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get autoremove && apt-get clean

RUN  apt-key add /ACCC4CF8.asc

RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt buster-pgdg main" > /etc/apt/sources.list.d/pgdg.list'

RUN apt-get update \
    && apt-get -y install postgresql-client-13 \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get autoremove && apt-get clean

#EXPOSE 8080

# Copy your fat jar to the container
COPY target/$VERTICLE_FILE $VERTICLE_HOME/
COPY target/config.json $VERTICLE_HOME/
COPY target/.pgpass /root/

RUN chmod 600 /root/.pgpass

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE -conf config.json"]
