FROM maven:3.8.4-jdk-8-slim

RUN apt-get -y update
RUN apt-get -y install git

RUN echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \n\
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \n\
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \n\
                      https://maven.apache.org/xsd/settings-1.0.0.xsd"> \n\
  <localRepository>/RS-FISH/.m2/repository</localRepository> \n\
</settings>' > $MAVEN_HOME/conf/settings.xml

RUN git clone https://github.com/PreibischLab/RS-FISH
WORKDIR /RS-FISH

RUN sed -i 's|\\\$|\$|g' install
RUN HOME=/RS-FISH ./install