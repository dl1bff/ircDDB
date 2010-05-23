

all:
	javac de/mdx/ircDDB/*.java
	./mk_manifest.sh
	jar cmf ircDDB.manifest ircDDB.jar de/mdx/ircDDB/*.class
	jar i ircDDB.jar

