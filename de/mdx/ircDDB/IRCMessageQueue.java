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



public class IRCMessageQueue
{

	class Item
	{

		Item prev;
		Item next;

		IRCMessage msg;

		Item(IRCMessage m)
		{	
			msg = m;
		}

	}

	boolean eof;

	Item first;
	Item last;


	public IRCMessageQueue()
	{
		eof = false;
		first = null;
		last = null;
	}


	public boolean isEOF()
	{
		return eof;
	}

	public void signalEOF()
	{
		eof = true;
	}

	public boolean messageAvailable()
	{
		return (first != null);
	}


	public synchronized IRCMessage getMessage()
	{
		Item k;

		if (first == null)
		{
			return null;
		}

		k = first;

		first = k.next;

		if (k.next == null)
		{
			last = null;
		}
		else
		{
			k.next.prev = null;
		}
		
		return k.msg;
	}


	public synchronized void putMessage( IRCMessage m )
	{
		Item k = new Item(m);

		k.prev = last;
		k.next = null;

		if (last == null)
		{
			first = k;
		}
		else
		{
			last.next = k;
		}

		last = k;
	}
}
