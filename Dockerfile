FROM eclipse-temurin:8-jammy

# Add all migration tools to path
RUN mkdir -p /assets/

# Download all migration dependencies
RUN cd /assets && \
    curl -OL https://downloads.datastax.com/dsbulk/dsbulk.tar.gz && \
    tar -xzf ./dsbulk.tar.gz && \
    rm ./dsbulk.tar.gz && \
    mv /assets/dsbulk-1.10.0 /assets/dsbulk  && \
    curl -OL https://downloads.datastax.com/enterprise/cqlsh-astra.tar.gz && \
    tar -xzf ./cqlsh-astra.tar.gz && \
    rm ./cqlsh-astra.tar.gz && \
    curl -OL https://archive.apache.org/dist/spark/spark-2.4.8/spark-2.4.8-bin-hadoop2.7.tgz && \
    tar -xzf ./spark-2.4.8-bin-hadoop2.7.tgz && \
    rm ./spark-2.4.8-bin-hadoop2.7.tgz

RUN apt-get update && apt-get install -y openssh-server vim --no-install-recommends && \
     rm -rf /var/lib/apt/lists/*
RUN service ssh start

# Copy CDM jar & template files
COPY ./target/cassandra-data-migrator-*.jar /assets/
COPY ./src/resources/sparkConf.properties /assets/
COPY ./src/resources/partitions.csv /assets/
COPY ./src/resources/primary_key_rows.csv /assets/
COPY ./src/resources/runCommands.txt /assets/

# Add all migration tools to path
ENV PATH="${PATH}:/assets/dsbulk/bin/:/assets/cqlsh-astra/bin/:/assets/spark-2.4.8-bin-hadoop2.7/bin/"

EXPOSE 22

CMD ["/usr/sbin/sshd","-D"]
