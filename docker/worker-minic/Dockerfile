FROM ghcr.io/graalvm/graalvm-ce:java11-21.1.0

# add the worker jar
RUN mkdir /opt/worker
RUN mkdir /opt/logs 
ADD minic-optimizer.jar /opt/worker
ADD access-c.jar /opt/worker

# set variables
ENV at_fh_hagenberg_aist_gce_neo4j=neo4j
ENV DIST_DIR=/opt/worker
ENV GRAAL_LOC=java
ENV LOG_LOC=/opt/logs
ENV seshat.loglevel=WARN

# install necessary software by reinstalling yum that the Graal devs remove
RUN microdnf -y install yum
RUN yum -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
RUN yum -y install clinfo

# publish mq port
EXPOSE 5570

# start worker
CMD java --illegal-access=permit -Dgraalvm.locatorDisabled=true --add-opens org.graalvm.sdk/org.graalvm.polyglot=ALL-UNNAMED --add-opens jdk.internal.vm.compiler/org.graalvm.compiler.truffle.runtime=ALL-UNNAMED --add-opens org.graalvm.truffle/com.oracle.truffle.api.nodes=ALL-UNNAMED --add-exports java.base/jdk.internal.module=ALL-UNNAMED -jar /opt/worker/minic-optimizer.jar
