FROM benchflow/base-images:envconsul-java8_dev

MAINTAINER Vincenzo FERME <info@vincenzoferme.it>

ENV FABAN_DRIVERS_MAKER_VERSION v-dev

# Get benchflow-experiments-manager
RUN wget -q --no-check-certificate -O /app/benchflow-faban-drivers-maker.jar https://github.com/benchflow/faban-drivers-maker/releases/download/$FABAN_DRIVERS_MAKER_VERSION/benchflow-faban-drivers-maker.jar

COPY configuration.yml /app/

COPY ./services/300-faban-drivers-maker.conf /apps/chaperone.d/300-faban-drivers-maker.conf

#TODO: remove, it is for testing
RUN touch /app/test.tpl
#TODO: remove, here because of a problem killing the container
RUN rm /apps/chaperone.d/200-envconsul-envcp.conf
 
EXPOSE 8080
