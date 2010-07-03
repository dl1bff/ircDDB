/*

ircDDB

Copyright (C) 2010   Michael Dirska, DL1BFF (dl1bff@mdx.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package net.ircDDB;


import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class Dbg
{
  public static final int ERR  = 10;
  public static final int WARN = 20;
  public static final int INFO = 30;
  public static final int DBG1 = 40;
  public static final int DBG2 = 50;
  public static final int DBG3 = 60;

  static PrintStream p = null;
  static DateFormat d = null;
  static int maxLevel = 35;

  public static void setDebugLevel ( int level )
  {
    maxLevel = level;
  }

  public static void println( int level, String s )
  {
    if (p == null)
    {
      p = System.out;
    }

    if (d == null)
    {
      d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      d.setTimeZone( TimeZone.getTimeZone("GMT"));
    }

    if (level < maxLevel)
    {
      p.println(d.format(new Date()) + " #" + level + " " + s );
    }
  }
}

