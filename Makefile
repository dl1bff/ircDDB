

class-files:
	javac de/mdx/ircDDB/*.java


ircDDB.jar: class-files
	./mk_manifest.sh
	jar cmf ircDDB.manifest ircDDB.jar de/mdx/ircDDB/*.class
	jar i ircDDB.jar
	jarsigner ircDDB.jar dl1bff

ircDDB_beta.jar: class-files
	./mk_manifest.sh beta
	jar cmf ircDDB.manifest ircDDB_beta.jar de/mdx/ircDDB/*.class
	jar i ircDDB_beta.jar
	jarsigner ircDDB_beta.jar dl1bff

