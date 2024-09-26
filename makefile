.DEFAULT_GOAL := build

.PHONY: cp cpt test build clean

cp:
	./gradlew compileJava

cpt: cp
	./gradlew compileTestJava

test: cpt
	./gradlew test

build: test
	./gradlew build

clean:
	./gradlew clean
	rm -rf .aws-sam
