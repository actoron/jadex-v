#!/bin/sh
find . | grep .project | xargs rm
find . | grep .classpath$ | xargs rm
find . | grep .org.eclipse | xargs rm
