#!/bin/sh


echo "Manifest-Version: 1.0" > ircDDB.manifest
echo "Main-Class: de.mdx.ircDDB.IRCDDBApp" >> ircDDB.manifest

echo "Specification-Title: ircDDB" >> ircDDB.manifest
echo "Specification-Version: 1.0" >> ircDDB.manifest
echo "Specification-Vendor: dl1bff@mdx.de" >> ircDDB.manifest
echo "Implementation-Title: ircDDB" >> ircDDB.manifest
echo "Implementation-Vendor: dl1bff@mdx.de" >> ircDDB.manifest

date '+Implementation-Version: %Y%m%d.%H%M' >> ircDDB.manifest

