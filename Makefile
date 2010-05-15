

all:
	javac de/mdx/ircDDB/*.java
	jar cmf ircDDB.manifest ircDDB.jar de/mdx/ircDDB/*.class
	jar i ircDDB.jar

