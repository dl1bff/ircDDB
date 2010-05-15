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



package de.mdx.ircDDB;

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




public class IRCDDBApp implements IRCApplication, Runnable
{

	IRCDDBExtApp extApp;

	IRCMessageQueue sendQ;
	Map<String,UserObject> user;
	String currentServer;
	
	String myNick;
	
	int state;
	int timer;
	
	Pattern datePattern;
	Pattern timePattern;
	Pattern keyPattern;
	Pattern valuePattern;
	SimpleDateFormat parseDateFormat;

	String updateChannel;
	
	IRCDDBApp(Pattern k, Pattern v, String u, IRCDDBExtApp ea)
	{
		extApp = ea;

		sendQ = null;
		currentServer = null;

		userListReset();
		
		state = 0;
		timer = 0;
		myNick = "none";
		
		parseDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		parseDateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));
		
		datePattern = Pattern.compile("20[0-9][0-9]-((1[0-2])|(0[1-9]))-((3[01])|([12][0-9])|(0[1-9]))");
		timePattern = Pattern.compile("((2[0-3])|([01][0-9])):[0-5][0-9]:[0-5][0-9]");
		keyPattern = k;
		valuePattern = v;

		updateChannel = u;
		
	}
	
	
	class UserObject
	{
		String nick;
		String name;
		String host;
		boolean op;
		
		UserObject ( String n, String nm, String h )
		{
			nick = n;
			name = nm;
			host = h;
			op = false;
		}
	}
	
	public void userJoin (String nick, String name, String host)
	{
		// System.out.println("APP: join " + nick + " " + name + " " + host);
		UserObject u = new UserObject( nick, name, host );
		
		user.put( nick, u );

		if (extApp != null)
		{
			extApp.userJoin(nick, name, host);
		}
	}
	
	public void userLeave (String nick)
	{
		// System.out.println("APP: leave " + nick );

		if (extApp != null)
		{
			if (user.containsKey(nick))
			{
				extApp.userLeave(nick);
			}
		}
		
		user.remove ( nick );
		
		if (currentServer != null)
		{
			if (currentServer.equals(nick))
			{
				// currentServer = null;
				state = 2;  // choose new server
				timer = 200;
			}
		}
		
	}

	public void userListReset()
	{
		user = Collections.synchronizedMap( new HashMap<String,UserObject>() );

		if (extApp != null)
		{
			extApp.userListReset();
		}
	}

	public void setCurrentNick(String nick)
	{
		myNick = nick;

		if (extApp != null)
		{
			extApp.setCurrentNick(nick);
		}
	}

	
	
	boolean findServerUser()
	{
		boolean found = false;
		
		Collection<UserObject> v = user.values();
		
		Iterator<UserObject> i = v.iterator();
		
		while (i.hasNext())
		{
			UserObject u = i.next();
			
			// System.out.println("LIST: " + u.nick + " " + u.op);
			
			if (u.nick.startsWith("s-") && u.op && !myNick.equals(u.nick))
			{
				currentServer = u.nick;
				found = true;
				break;
			}
		}
		
		return found;
	}
	
	
	public void userChanOp (String nick, boolean op)
	{
		UserObject u = user.get( nick );
		
		if (u != null)
		{
			// System.out.println("APP: op " + nick + " " + op);
			if ((extApp != null) && (u.op != op))
			{
				extApp.userChanOp(nick, op);
			}

			u.op = op;
		}
	}
	

	IRCDDBExtApp.UpdateResult processUpdate ( Scanner s)
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
						
						if (extApp != null)
						{
							return extApp.dbUpdate( dbDate, key, value );
						}
					}
				}
			}
		}
		
		return null;
	}
	 
	
	public void msgChannel (IRCMessage m)
	{
		// System.out.println("APP: chan");
		
		if (m.getPrefixNick().startsWith("s-"))  // server msg
		{
			String msg = m.params[1];
			
			Scanner s = new Scanner(msg);
			
			if (s.hasNext(datePattern))
			{
				processUpdate(s);
			}
			else
			{
				if (extApp != null)
				{
					extApp.msgChannel( m );
				}
			}
		}
	}
	
	public void msgQuery (IRCMessage m)
	{
	
		String msg = m.params[1];
		
		Scanner s = new Scanner(msg);
		
		String command;
		
		if (s.hasNext())
		{
			command = s.next();
		}
		else
		{
			return; // no command
		}
		
		if (command.equals("UPDATE"))
		{	
			if (s.hasNext(datePattern))
			{
				IRCDDBExtApp.UpdateResult result = processUpdate(s);
				
				if (result != null)
				{
					boolean sendUpdate = false;
					
					if (result.keyWasNew)
					{
						sendUpdate = true;
					}
					else
					{

						if (result.newObj.value.equals(result.oldObj.value)) // value is the same
						{
							long newMillis = result.newObj.modTime.getTime();
							long oldMillis = result.oldObj.modTime.getTime();
							
							if (newMillis > (oldMillis + 300000))  // update max. every 5 min
							{
								sendUpdate = true;
							}
						}
						else
						{
							sendUpdate = true;  // value has changed, send update via channel
						}
				
					}

					UserObject me = user.get(myNick); 
					
					if ((me != null) && me.op && sendUpdate)  // send only if i am operator
					{
				
						IRCMessage m2 = new IRCMessage();
						m2.command = "PRIVMSG";
						m2.numParams = 2;
						m2.params[0] = updateChannel;
						m2.params[1] = parseDateFormat.format(result.newObj.modTime) + " " + 
							result.newObj.key + " " + result.newObj.value + "  (from: " + m.getPrefixNick() + ")";
						
						IRCMessageQueue q = getSendQ();
						if (q != null)
						{
							q.putMessage(m2);
						}
					}
				}
			}
			
			
	
		}
		else if (command.equals("SENDLIST"))
		{
		
			String answer = "LIST_END";
			
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
						
					if ((dbDate != null) && (extApp != null))
					{
						final int NUM_ENTRIES = 4;

						LinkedList<IRCDDBExtApp.DatabaseObject> l = 
							extApp.getDatabaseObjects( dbDate, NUM_ENTRIES );

						int count = 0;
				
						for (IRCDDBExtApp.DatabaseObject o : l)
						{
							IRCMessage m3 = new IRCMessage(
								m.getPrefixNick(),
								"UPDATE " + parseDateFormat.format(o.modTime) + " "
                                                                        + o.key + " " + o.value	);

								
							IRCMessageQueue q = getSendQ();
							if (q != null)
							{
								q.putMessage(m3);
							}
								
							count ++;
						}

						if (count > NUM_ENTRIES)
						{
							answer = "LIST_MORE";
						}
					}
				}
			}
			
			IRCMessage m2 = new IRCMessage();
			m2.command = "PRIVMSG";
			m2.numParams = 2;
			m2.params[0] = m.getPrefixNick();
			m2.params[1] = answer;
			
			IRCMessageQueue q = getSendQ();
			if (q != null)
			{
				q.putMessage(m2);
			}
		}
		else if (command.equals("LIST_END"))
		{
			UserObject me = user.get(myNick);
			UserObject other = user.get(m.getPrefixNick()); // nick of other user
			
			if ((me != null) && (other != null) && !me.op && other.op
				&& other.nick.startsWith("s-") && me.nick.startsWith("s-") )
			{
				IRCMessage m2 = new IRCMessage();
				m2.command = "PRIVMSG";
				m2.numParams = 2;
				m2.params[0] = m.getPrefixNick();
				m2.params[1] = "OP_BEG";
				
				IRCMessageQueue q = getSendQ();
				if (q != null)
				{
					q.putMessage(m2);
				}
			}
		}
		else if (command.equals("LIST_MORE"))
		{
			if (extApp != null)
			{
				IRCMessage m2 = new IRCMessage(
					currentServer, "SENDLIST " + getLastEntryTime());
				
				IRCMessageQueue q = getSendQ();
				if (q != null)
				{
					q.putMessage(m2);
				}
			}
		}
		else if (command.equals("OP_BEG"))
		{
			UserObject me = user.get(myNick); 
			UserObject other = user.get(m.getPrefixNick()); // nick of other user

			if ((me != null) && (other != null) && me.op && !other.op
				&& other.nick.startsWith("s-") && me.nick.startsWith("s-") )
			{
				IRCMessage m2 = new IRCMessage();
				m2.command = "MODE";
				m2.numParams = 3;
				m2.params[0] = updateChannel;
				m2.params[1] = "+o";
				m2.params[2] = other.nick;
				
				IRCMessageQueue q = getSendQ();
				if (q != null)
				{
					q.putMessage(m2);
				}
			}
		}
		else
		{
			if (extApp != null)
			{
				extApp.msgQuery(m);
			}
		}
	}
	
	
	public synchronized void setSendQ( IRCMessageQueue s )
	{
		// System.out.println("APP: setQ " + s);
		
		sendQ = s;
		
		if (extApp != null)
		{
			extApp.setSendQ(s);
		}
	}
	
	public synchronized IRCMessageQueue getSendQ ()
	{
		return sendQ;
	}
	
	
	String getLastEntryTime()
	{

		if (extApp != null)
		{

			IRCDDBExtApp.DatabaseObject obj;

			obj = extApp.getLastEntry();

			if (obj != null)
			{
				return parseDateFormat.format( obj.modTime );
			}
		}
		
		return "2000-01-01 10:00:00";
	}
	
	
	public void run()
	{
		
		while (true)
		{
			
			if (timer > 0)
			{
				timer --;
			}
			
			// System.out.println("state " + state);
			
			
			switch(state)
			{
			case 0:  // wait for network to start
					
				if (getSendQ() != null)
				{
					state = 1;
				}
				break;
				
			case 1:
			  // connect to db
			  state = 2;
			  timer = 200;
			  break;
			
			case 2:   // choose server
				if (getSendQ() == null)
				{
					state = 10;
				}
				else
				{	
					if (findServerUser())
					{
					
						state = 3;
						IRCMessage m = new IRCMessage();
						m.command = "PRIVMSG";
						m.numParams = 2;
						m.params[0] = currentServer;
						m.params[1] = "SENDLIST " + getLastEntryTime();
						
						IRCMessageQueue q = getSendQ();
						if (q != null)
						{
							q.putMessage(m);
						}
					}
					else if (timer == 0)
					{
						state = 10;
						
						IRCMessage m = new IRCMessage();
						m.command = "QUIT";
						m.numParams = 1;
						m.params[0] = "ENUMSERVER (num < 2)";
						
						IRCMessageQueue q = getSendQ();
						if (q != null)
						{
							q.putMessage(m);
						}
					}
				}
				break;
				
			case 3:
				if (getSendQ() == null)
				{
					state = 10; // disconnect DB
				}
				break;
				
			
			case 10:
				// disconnect db
				state = 0;
				timer = 0;
				break;
			
			}
			
			
			try
			{
				Thread.sleep(1000);
			}
			catch ( InterruptedException e )
			{
				System.out.println(e);
			}
		}
	}
	










/*  ----------------------------------------------- */

	public static void main (String args[])
	{

		
		Properties properties = new Properties();

		try
		{ 
			properties.load (new FileInputStream("ircDDB.properties"));
		}
		catch (IOException e)
		{
			System.out.println("could not open file 'ircDDB.properties'");
			System.exit(1);
		} 
		
		String irc_nick = properties.getProperty("irc_nick", "guest");
		String rptr_call = properties.getProperty("rptr_call", "nocall");

		boolean debug = properties.getProperty("debug", "0").equals("1");
		
		String n[];
		
		String irc_name;

		if (rptr_call.equals("nocall"))
		{
			n = new String[1];
			n[0] = irc_nick;
			
			irc_name = irc_nick;
		}
		else
		{
			n = new String[4];
			
			n[0] = rptr_call + "-1";
			n[1] = rptr_call + "-2";
			n[2] = rptr_call + "-3";
			n[3] = rptr_call + "-4";
			
			irc_name = rptr_call;
		}
		
		String keyPatternString = properties.getProperty("ddb_key_pattern", "[A-Z0-9_]{8}");
		String valuePatternString = properties.getProperty("ddb_value_pattern", "[A-Z0-9_]{8}");

		Pattern keyPattern = null;
		Pattern valuePattern = null;
		
		try
		{
			keyPattern = Pattern.compile(keyPatternString);
			valuePattern = Pattern.compile(valuePatternString);
		}
		catch (PatternSyntaxException e)
		{
			System.out.println("pattern syntax error " + e);
			System.exit(1);
		} 

		String extAppName = properties.getProperty("ext_app", "none");
		IRCDDBExtApp extApp = null;

		if (!extAppName.equals("none"))
		{
			try
			{
				Class extAppClass =
					Class.forName(extAppName);

				extApp = (IRCDDBExtApp) extAppClass.newInstance();

				extApp.setParams( properties, keyPattern, valuePattern );

				Thread extappthr = new Thread(extApp);

		                extappthr.start();
				
			}
			catch (Exception e)
			{
				System.out.println("external application: " + e);
				System.exit(1);
			}
		}
		
		String irc_channel = properties.getProperty("irc_channel", "#chat");
		
		IRCDDBApp app = new IRCDDBApp (keyPattern, valuePattern,
			irc_channel, extApp );
		
		Thread appthr = new Thread(app);
		
		appthr.start();
		
		IRCClient irc = new IRCClient( app,
			properties.getProperty("irc_server_name", "localhost"),
			Integer.parseInt(properties.getProperty("irc_server_port", "9007")),
			irc_channel,
			irc_name, n,
			properties.getProperty("irc_password", "secret"), debug);

		

		Thread ircthr = new Thread(irc);

		ircthr.start();

	}



}



