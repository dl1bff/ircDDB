

all:
	javac de/mdx/ircDDB/*.java
	jar cvf ircDDB.jar de/mdx/ircDDB/*.class

