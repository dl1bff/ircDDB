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

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

import java.util.Properties;

import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.util.Date;
import java.util.TimeZone;
import java.util.NoSuchElementException;
import java.text.SimpleDateFormat;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.FileOutputStream;



public class IRCMemoryDB implements IRCDDBExtApp
{

	Map<String,DbObject> db;
        Pattern datePattern;
        Pattern timePattern;
	SimpleDateFormat parseDateFormat;

	String bootFile;

	IRCMessageQueue sendQ;

	IRCDDBEntryValidator validator;

	IRCMemoryDBQueryPlugin queryPlugin;

	
	public IRCMemoryDB()
	{
		db = Collections.synchronizedMap( new HashMap<String,DbObject>() );

		datePattern = Pattern.compile("20[0-9][0-9]-((1[0-2])|(0[1-9]))-((3[01])|([12][0-9])|(0[1-9]))");
                timePattern = Pattern.compile("((2[0-3])|([01][0-9])):[0-5][0-9]:[0-5][0-9]");

		parseDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                parseDateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

		sendQ = null;
		validator = null;
		queryPlugin = null;
	}
		
	public void setParams( Properties p, Pattern keyPattern, Pattern valuePattern,
	    IRCDDBEntryValidator v )
	{
		validator = v;

		bootFile = p.getProperty("memdb_bootfile", "db.txt");
		
		try
		{
			Scanner s = new Scanner (new File(bootFile));
		
			while (s.hasNext(datePattern))
			{
				processUpdate(s, keyPattern, valuePattern);
			}
			
		}
		catch (FileNotFoundException e)
		{
			System.out.println("file " + bootFile + " not found.");
		}

                String queryPluginName = p.getProperty("memdb_query_plugin", "none");

                if (!queryPluginName.equals("none"))
                {
                  try
                  {
                    Class queryPluginClass = Class.forName(queryPluginName);

                    queryPlugin = (IRCMemoryDBQueryPlugin) queryPluginClass.newInstance();

                    queryPlugin.setParams( p, this );

                  }
                  catch (Exception e)
                  {
                    Dbg.println(Dbg.ERR, "query plugin not loaded! " + e);
		    queryPlugin = null;
                  }
                }
	
	}

	public boolean needsDatabaseUpdate()
	{
		return true;
	}
	
	public void setCurrentNick (String nick)
	{
	}

	public void setCurrentServerNick (String nick)
	{
	}

	public void setTopic (String topic)
	{
	}

	
	public void userJoin (String nick, String name, String host)
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.userJoin(nick, name, host);
	  }
	}
	
	public void userLeave (String nick)
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.userLeave(nick);
	  }
	}

	public void userListReset()
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.userListReset();
	  }
	}

	public void userChanOp (String nick, boolean op)
	{
	}
	
	class DbObject
	{
		Date dbDate;
		String key;
		String value;
		
		DbObject( Date d, String k, String v )
		{
			dbDate = d;
			key = k;
			value = v;
		}
	}

	void processUpdate ( Scanner s, Pattern keyPattern, Pattern valuePattern)
	{
		if (s.hasNext(datePattern))
		{
			String d = s.next(datePattern);
			
			if (s.hasNext(timePattern))
			{
				String t = s.next(timePattern);
				
				
				Date dbDate = null;

				try
				{
					dbDate = parseDateFormat.parse(d + " " + t);
				}
				catch (java.text.ParseException e)
				{
					dbDate = null;
				}
					
				if ((dbDate != null) && s.hasNext(keyPattern))
				{
					String key = s.next(keyPattern);
					
					
					if (s.hasNext(valuePattern))
					{
						String value = s.next(valuePattern);
						
						// DbObject o = new DbObject( dbDate, key, value );
						// db.put(key, o);

						dbUpdate( dbDate, key, value, null );
					}
				}
			}
		}
		
	}
	
	public void msgChannel (IRCMessage m)
	{
	}
	
	public void msgQuery (IRCMessage m)
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.processQuery(m);
	  }
	}
	
	
	public synchronized void setSendQ( IRCMessageQueue s )
	{
		sendQ = s;
	}
	
	public synchronized IRCMessageQueue getSendQ ()
	{
		return sendQ;
	}
	
	
	class DbObjectComparator implements Comparator<DbObject>
	{
		public int compare(DbObject o1, DbObject o2)
		{
			return o1.dbDate.compareTo(o2.dbDate);
		}
	}
	
	
	LinkedList<DbObject> getSortedDbEntries()
	{
		Collection<DbObject> c = db.values();
				
		LinkedList<DbObject> l = new LinkedList<DbObject>();
				
		l.addAll(c);
				
		Collections.sort(l, new DbObjectComparator() );
		
		return l;
	}
	
	public Date getLastEntryDate()
	{
		LinkedList<DbObject> l = getSortedDbEntries();

		DbObject o = null;
		
		try
		{
			o = l.getLast();
		}
		catch (NoSuchElementException e)
		{
			o = null;
		}
			
		if (o != null)
		{
			return o.dbDate;
		}
		
		return new Date(950000000000L); // February 2000
	}

	public LinkedList<IRCDDBExtApp.DatabaseObject> getDatabaseObjects(
		Date beginDate, int numberOfObjects)
	{
		LinkedList<IRCDDBExtApp.DatabaseObject> result;

		result = new LinkedList<IRCDDBExtApp.DatabaseObject>();
		
        	LinkedList<DbObject> l = getSortedDbEntries();
		int count = 0;

		for (DbObject o : l)
		{
			if (beginDate.compareTo(o.dbDate) <= 0)
			{
				if (count > numberOfObjects)
				{
					break;
				}

				IRCDDBExtApp.DatabaseObject obj;

				obj = new IRCDDBExtApp.DatabaseObject();
				obj.modTime = o.dbDate;
				obj.key = o.key;
				obj.value = o.value;
				
				result.add(obj);

				count ++;
			}
		}

		return result;
	}

	public IRCDDBExtApp.UpdateResult dbUpdate( Date d, String k, String v, String ircUser )
	{
		if (validator != null)
		{
		  if (!validator.isValid(k, v, ircUser))
		  {
		    Dbg.println(Dbg.DBG1, "invalid " + k + " " + v);
		    return null;
		  }
		}

		IRCDDBExtApp.UpdateResult result;
		IRCDDBExtApp.DatabaseObject newObj;

		result = new IRCDDBExtApp.UpdateResult();

		newObj = new IRCDDBExtApp.DatabaseObject();
		newObj.modTime = d;
		newObj.key = k;
		newObj.value = v;

		DbObject n = new DbObject( d, k, v );

		if (db.containsKey(k))
		{
			DbObject o = db.get(k);

			IRCDDBExtApp.DatabaseObject oldObj;
			oldObj = new IRCDDBExtApp.DatabaseObject();
			oldObj.modTime = o.dbDate;
			oldObj.key = o.key;
			oldObj.value = o.value;

			result.oldObj = oldObj;
			result.keyWasNew = false;

			if (o.dbDate.getTime() > d.getTime())
			{
				// System.out.println("old entry was newer");
				return null;
			}
		}
		else
		{
			result.oldObj = null;
			result.keyWasNew = true;
		}

		Date nowDate = new Date();

		if (d.getTime() > (nowDate.getTime() + 300000))
		{
			Dbg.println(Dbg.WARN, "new entry more than 5 min in future - ignoring");
			return null;
		}

		db.put (k, n);

		result.newObj = newObj;
		
		return result;
	}


	
	public void run()
	{
		
		while (true)
		{
			
			try
			{
				Thread.sleep(300000);
			}
			catch ( InterruptedException e )
			{
				System.out.println(e);
			}

			

			try
			{
				PrintWriter p = new PrintWriter(new FileOutputStream(bootFile));

				LinkedList<DbObject> l = getSortedDbEntries();

				for (DbObject o : l)
				{
					p.println(parseDateFormat.format(o.dbDate) + " " + o.key + " " + o.value);
				}

				p.close();

				
			}
			catch (IOException e)
			{
				System.out.println("dumpDb failed " + e);
			}

		}
	}
	




}




