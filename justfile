default:
    @just --list

build:
    gradle build

run:
    gradle run

test:
    gradle logicTest

check:
    gradle check

clean:
    gradle clean
    rm -rf out out-test

tasks:
    @gradle tasks --group verification
