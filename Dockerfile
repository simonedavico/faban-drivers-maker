FROM benchflow/base-images:dns-envconsul-java8_dev

MAINTAINER Vincenzo FERME <info@vincenzoferme.it>

ENV FABAN_DRIVERS_MAKER_VERSION v-dev
ENV RELEASE_VERSION v-dev
ENV FABAN_VERSION dev
ENV FABAN_ROOT /app/
ENV FABAN_HOME ${FABAN_ROOT}faban

ENV CLIENT_VERSION v-dev
ENV SUT_LIBRARIES_VERSION v-dev
ENV SUT_PLUGINS_VERSION v-dev

# Get benchflow-faban-drivers-maker
RUN wget -q --no-check-certificate -O /app/benchflow-faban-drivers-maker.jar https://github.com/benchflow/faban-drivers-maker/releases/download/$FABAN_DRIVERS_MAKER_VERSION/benchflow-faban-drivers-maker.jar

COPY configuration.yml /app/

# Get benchflow-faban-drivers-maker dependencies
RUN apk --update add wget tar && \
	wget -q --no-check-certificate -O /tmp/faban.tar.gz https://github.com/benchflow/faban/releases/download/${RELEASE_VERSION}/faban-kit-${FABAN_VERSION}.tar.gz && \
	mkdir -p ${FABAN_ROOT}/ && \
	tar -xzvf /tmp/faban.tar.gz -C ${FABAN_ROOT}/ && \
	# Unpack faban master
	mkdir -p ${FABAN_HOME}/master/webapps/faban && \
	cd ${FABAN_HOME}/master/webapps/faban && \
	JAVA=`readlink -f $(which java) | sed 's|/bin/java$||'` && \
	#Unjar faban.war
	$JAVA/bin/jar xvf ../faban.war && \
	# Remove unused Faban assets
	rm -f ${FABAN_HOME}/*.* && \
	rm -rf ${FABAN_HOME}/benchmarks ${FABAN_HOME}/bin ${FABAN_HOME}/config ${FABAN_HOME}/legal \
	       ${FABAN_HOME}/logs ${FABAN_HOME}/output ${FABAN_HOME}/resources ${FABAN_HOME}/samples ${FABAN_HOME}/services && \
	find ${FABAN_HOME}/master/ -not -path "/app/faban/master/" -not -path "${FABAN_HOME}/master/webapps" -not -path "${FABAN_HOME}/master/webapps/faban" -not -path "${FABAN_HOME}/master/webapps/faban/WEB-INF"  -not -path "${FABAN_HOME}/master/webapps/faban/WEB-INF/classes*" | xargs rm -rf && \
	# Get test driver (TODO: remove after testing)
    mkdir -p /app/skeleton && \
    wget -q --no-check-certificate -O - https://github.com/benchflow/client/archive/$CLIENT_VERSION.tar.gz \
    | tar xz --strip-components=4 -C /app/skeleton client-$CLIENT_VERSION/test/camunda/wfmsbenchmark && \
    # Get sut-libraries
    mkdir -p /app/libraries && \
    wget -q --no-check-certificate -O - https://github.com/benchflow/sut-libraries/archive/$SUT_LIBRARIES_VERSION.tar.gz \
    | tar xz --strip-components=1 -C /app/libraries sut-libraries-$SUT_LIBRARIES_VERSION && \
    # Get sut-plugins
    mkdir -p /app/plugins && \
    wget -q --no-check-certificate -O - https://github.com/benchflow/sut-plugins/archive/$SUT_PLUGINS_VERSION.tar.gz \
    | tar xz --strip-components=1 -C /app/plugins sut-plugins-$SUT_PLUGINS_VERSION && \
	apk del --purge tar && \
    rm -rf /var/cache/apk/* /tmp/* /var/tmp/* *.gz

# Install Ant (Source: https://hub.docker.com/r/webratio/ant/~/dockerfile/)
ENV ANT_VERSION 1.9.4
RUN mkdir -p /opt/ant && \
    wget -q --no-check-certificate -O /apache-ant-${ANT_VERSION}-bin.tar.gz http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf /apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv /apache-ant-${ANT_VERSION} /opt/ant && \
    rm /apache-ant-${ANT_VERSION}-bin.tar.gz && \
    apk del --purge wget
ENV ANT_HOME /opt/ant
ENV PATH ${PATH}:/opt/ant/bin

COPY ./services/300-faban-drivers-maker.conf /apps/chaperone.d/300-faban-drivers-maker.conf

#TODO: remove, it is for testing
RUN touch /app/test.tpl
#TODO: remove, here because of a problem killing the container
RUN rm /apps/chaperone.d/200-envconsul-envcp.conf
 
EXPOSE 8080
