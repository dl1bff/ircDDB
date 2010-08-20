

class-files:
	javac net/ircDDB/*.java

x: ircDDB2.jar

y: ircDDB2_beta.jar

ircDDB2.jar: class-files
	./mk_manifest.sh
	jar cmf ircDDB.manifest ircDDB2.jar net/ircDDB/*.class
	jar i ircDDB2.jar
	jarsigner ircDDB2.jar dl1bff

ircDDB2_beta.jar: class-files
	./mk_manifest.sh beta
	jar cmf ircDDB.manifest ircDDB2_beta.jar net/ircDDB/*.class
	jar i ircDDB2_beta.jar
	jarsigner ircDDB2_beta.jar dl1bff

