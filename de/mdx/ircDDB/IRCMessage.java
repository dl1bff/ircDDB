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



public class IRCMessage
{

	public String prefix;
	public String command;
	public String params[];

	public int numParams;
	
	StringBuffer prefixComponents[];
	boolean prefixParsed;

	public IRCMessage ()
	{
		prefix = "";
		command = "";
		params  = new String[15];
		numParams = 0;
		
		prefixParsed = false;
		
		prefixComponents = new StringBuffer[3];
	}

	public IRCMessage (String toNick, String msg)
	{
		this();
		
		command = "PRIVMSG";
		numParams = 2;
		params[0] = toNick;
		params[1] = msg;
	}
	
	void parsePrefix()
	{
		int i;
		
		for (i=0; i < 3; i++)
		{
			prefixComponents[i] = new StringBuffer(20);
		}

		int state = 0;
		
		for (i=0; i < prefix.length(); i++)
		{
			char c = prefix.charAt(i);
			
			switch (c)
			{
			case '!': 
				state = 1; // next is name
				break;
				
			case '@':
				state = 2; // next is host
				break;
				
			default:
				prefixComponents[state].append(c);
				break;
			}
		}
	
		prefixParsed = true;
	}

	public String getPrefixNick()
	{
		if (!prefixParsed)
		{
			parsePrefix();
		}
		
		return prefixComponents[0].toString();
	}

	public String getPrefixName()
	{
		if (!prefixParsed)
		{
			parsePrefix();
		}
		
		return prefixComponents[1].toString();
	}

	public String getPrefixHost()
	{
		if (!prefixParsed)
		{
			parsePrefix();
		}
		
		return prefixComponents[2].toString();
	}

	void writeMessage ( java.io.OutputStream os, boolean debug ) throws java.io.IOException
	{

		if (debug)
		{
			System.out.print("T [" + prefix + "]" );

                        System.out.print(" [" + command +"]" );

                        for (int i=0; i < numParams; i++)
                        {
                        	System.out.print(" [" + params[i] + "]" );
                        }
                        System.out.println();
		}

		java.io.PrintWriter p = new java.io.PrintWriter(os);

		if (prefix.length() > 0)
		{
			p.format(":%s ", prefix);
		}

		p.print(command);

		for (int i=0; i < numParams; i++)
		{
			if (i == (numParams - 1))
			{
				p.format(" :%s", params[i]);
			}
			else
			{
				p.format(" %s", params[i]);
			}
		}

		p.print("\r\n");
		p.flush();

	}
}
