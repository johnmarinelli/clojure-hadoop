FROM sequenceiq/hadoop-docker:2.7.1
MAINTAINER sequenceiq

RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
RUN chmod 755 lein
RUN mv lein /usr/local/bin/
RUN export LEIN_ROOT="root"
RUN yum install git -y
RUN git clone https://github.com/johnmarinelli/clojure-hadoop
RUN cd clojure-hadoop
RUN lein deps
RUN java -cp examples.jar clojure_hadoop.examples.wordcount1 README.txt out1
#
