

class-files:
	javac net/ircDDB/*.java


ircDDB2.jar: class-files
	./mk_manifest.sh
	jar cmf ircDDB.manifest ircDDB2.jar net/ircDDB/*.class
	jar i ircDDB2.jar
	jarsigner ircDDB2.jar dl1bff

ircDDB_beta.jar: class-files
	./mk_manifest.sh beta
	jar cmf ircDDB.manifest ircDDB_beta.jar net/ircDDB/*.class
	jar i ircDDB_beta.jar
	jarsigner ircDDB_beta.jar dl1bff

