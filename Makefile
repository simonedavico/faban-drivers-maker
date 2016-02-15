REPONAME = drivers-maker
DOCKERIMAGENAME = benchflow/$(REPONAME)
VERSION = dev
JAVA_VERSION_FOR_COMPILATION = java-8-oracle
JAVA_HOME := `update-java-alternatives -l | cut -d' ' -f3 | grep $(JAVA_VERSION_FOR_COMPILATION)`"/jre"

.PHONY: all build_release 

all: build_release

clean:
	mvn clean

build:
	JAVA_HOME=$(JAVA_HOME) mvn package

build_release:
	JAVA_HOME=$(JAVA_HOME) mvn package

install:
	JAVA_HOME=$(JAVA_HOME) mvn package

test:
	JAVA_HOME=$(JAVA_HOME) mvn test

build_container_local:
	JAVA_HOME=$(JAVA_HOME) mvn package
	docker build -t $(DOCKERIMAGENAME):$(VERSION) -f Dockerfile.test .
	rm target/benchflow-$(REPONAME).jar

test_container_local:
	docker run -ti --rm -e "ENVCONSUL_CONSUL=$(ENVCONSUL_CONSUL)" \
	-p 6060:8080 --name $(REPONAME) $(DOCKERIMAGENAME):$(VERSION)

rm_container_local:
	docker rm -f -v $(REPONAME)