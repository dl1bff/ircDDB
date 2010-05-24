

class-files:
	javac de/mdx/ircDDB/*.java


ircDDB.jar: class-files
	./mk_manifest.sh
	jar cmf ircDDB.manifest ircDDB.jar de/mdx/ircDDB/*.class
	jar i ircDDB.jar
	jarsigner ircDDB.jar dl1bff

