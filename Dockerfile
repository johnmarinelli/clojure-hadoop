FROM sequenceiq/hadoop-docker:2.7.1
MAINTAINER sequenceiq

RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
RUN chmod 755 lein
RUN mv lein /usr/local/bin/
RUN export LEIN_ROOT="root"
RUN yum install git -y
COPY init.sh .
RUN chmod +x init.sh
ENTRYPOINT ["./init.sh"]
