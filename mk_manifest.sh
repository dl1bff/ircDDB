#!/bin/sh
# 
# ircDDB
# 
# Copyright (C) 2010   Michael Dirska, DL1BFF (dl1bff@mdx.de)
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
# 

echo "Manifest-Version: 1.0" > ircDDB.manifest
echo "Main-Class: net.ircDDB.IRCDDBApp" >> ircDDB.manifest
echo "Specification-Title: ircDDB" >> ircDDB.manifest
echo "Specification-Version: 1.0" >> ircDDB.manifest
echo "Specification-Vendor: dl1bff@mdx.de" >> ircDDB.manifest
echo "Implementation-Title: ircDDB" >> ircDDB.manifest
echo "Implementation-Vendor: dl1bff@mdx.de" >> ircDDB.manifest
date '+Implementation-Version: %Y%m%d.%H%M'"$1" >> ircDDB.manifest

