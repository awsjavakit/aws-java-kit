.DEFAULT_GOAL := build

.PHONY: cp cpt test build clean

cp:
	./gradlew compileJava

cpt: cp
	./gradlew compileTestJava

test: cpt
	./gradlew test
	./gradlew spotlessApply
	

build: test
	./gradlew build

clean:
	./gradlew clean
	rm -rf .gradle
	rm -rf build
	rm -rf .aws-sam
