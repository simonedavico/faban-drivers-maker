REPONAME = drivers-maker
DOCKERIMAGENAME = benchflow/$(REPONAME)
VERSION = dev
JAVA_VERSION_FOR_COMPILATION = java-8-oracle
UNAME = $(shell uname)
JAVA_HOME := `update-java-alternatives -l | cut -d' ' -f3 | grep $(JAVA_VERSION_FOR_COMPILATION)`"/jre"

find_java:
ifeq ($(UNAME), Darwin)
	$(eval JAVA_HOME := $(shell /usr/libexec/java_home))
endif

.PHONY: all build_release

all: build_release

clean:
	mvn clean

build: find_java
	JAVA_HOME=$(JAVA_HOME) mvn package

build_release: find_java
	JAVA_HOME=$(JAVA_HOME) mvn package

install: find_java
	JAVA_HOME=$(JAVA_HOME) mvn package

test: find_java
	JAVA_HOME=$(JAVA_HOME) mvn test

build_container_local: find_java
	JAVA_HOME=$(JAVA_HOME) mvn package
	docker build -t $(DOCKERIMAGENAME):$(VERSION) -f Dockerfile.ci .
	rm target/benchflow-$(REPONAME).jar

test_container_local:
	docker run -ti --rm -e "ENVCONSUL_CONSUL=$(ENVCONSUL_CONSUL)" \
	-p 6060:8080 --name $(REPONAME) $(DOCKERIMAGENAME):$(VERSION)

rm_container_local:
	docker rm -f -v $(REPONAME)
