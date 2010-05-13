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


import java.util.Random;


class IRCProtocol
{

	String name;
	String nicks[];
	String password;
	String channel;
	String currentNick;
	
	int state;
	int timer;
	int pingTimer;
	
	Random r;
	
	IRCApplication app;
	
	IRCProtocol(IRCApplication a, String ch, String n, String[] u, String pass)
	{
	
		app = a;
		
		name = n;
		nicks = u;
		password = pass;
		channel = ch;
		
		state = 0;
		timer = 0;
		pingTimer = 60;
		
		r = new Random();
		chooseNewNick();
	}

	
	void chooseNewNick()
	{
		int k = r.nextInt(nicks.length);
		
		// System.out.println("nick: " + k);
		
		currentNick = nicks[ k ];
	}
	
	void setNetworkReady(boolean b)
	{
		if (b == true)
		{
			if (state != 0)
			{
				System.out.println("IRCProtocol/netReady: unexpected");
			}
			
			state = 1;
			chooseNewNick();
		}
		else
		{
			state = 0;
		}
	}
	
	
	boolean processQueues ( IRCMessageQueue recvQ, IRCMessageQueue sendQ )
	{
		
		if (timer > 0)
		{
			timer --;
		}
		
		while (recvQ.messageAvailable())
		{
			IRCMessage m = recvQ.getMessage();

		/*	
			System.out.print("[" + m.prefix + "]" );
			System.out.print(" [" + m.command +"]" );

			for (int i=0; i < m.numParams; i++)
			{
				System.out.print(" [" + m.params[i] + "]" );
			}
			System.out.println();
			*/
	
			if (m.command.equals("004"))
			{
				if (state == 4)
				{
					state = 5;  // next: JOIN
				}
			}
			else if (m.command.equals("PING"))
			{
				IRCMessage m2 = new IRCMessage();
				m2.command = "PONG";
				m2.numParams = 1;
				m2.params[0] = m.params[0];
				sendQ.putMessage(m2);
			}
			else if (m.command.equals("JOIN"))
			{
				if ((m.numParams >= 1) && m.params[0].equals(channel))
				{
					if (m.getPrefixNick().equals(currentNick) && (state == 6))
					{
						state = 10; // next: WHO *
					}
					else if (app != null)
					{
						app.userJoin( m.getPrefixNick(), m.getPrefixName(), m.getPrefixHost());
					}
				}
			}
			else if (m.command.equals("PONG"))
			{
				if (state == 12)
				{
					timer = pingTimer;
					state = 11;
				}
			}
			else if (m.command.equals("PART"))
			{
				if ((m.numParams >= 1) && m.params[0].equals(channel))
				{
					if (app != null)
					{
						app.userLeave( m.getPrefixNick() );
					}
				}
			}
			else if (m.command.equals("KICK"))
			{
				if ((m.numParams >= 2) && m.params[0].equals(channel))
				{
					if (m.params[1].equals(currentNick))
					{
						// i was kicked!!
						return false;
					}
					else if (app != null)
					{
						app.userLeave( m.params[1] );
					}
				}
			}
			else if (m.command.equals("QUIT"))
			{
				if (app != null)
				{
					app.userLeave( m.getPrefixNick() );
				}
			}
			else if (m.command.equals("MODE"))
			{
				if ((m.numParams >= 3) && m.params[0].equals(channel))
				{
					if (app != null)
					{
						if ( m.params[1].equals("+o") )
						{
							app.userChanOp(m.params[2], true);
						}
						else if ( m.params[1].equals("-o") )
						{
							app.userChanOp(m.params[2], false);
						}
					}
				}
			}
			else if (m.command.equals("PRIVMSG"))
			{
				if ((m.numParams == 2) && (app != null))
				{
					if (m.params[0].equals(channel))
					{
						app.msgChannel(m);
					}
					else if (m.params[0].equals(currentNick))
					{
						app.msgQuery(m);
					}
				}
			}
			else if (m.command.equals("352"))  // WHO list
			{
				if ((m.numParams >= 7) && m.params[0].equals(currentNick)
					&& m.params[1].equals(channel))
				{
					if (app != null)
					{
						app.userJoin( m.params[5], m.params[2], m.params[3]);
						app.userChanOp ( m.params[5], m.params[6].equals("H@"));
					}
				}
			}
			else if (m.command.equals("433"))  // nick collision
			{
				if (state == 2)
				{
					state = 3;  // nick collision, choose new nick
					timer = 10; // wait 5 seconds..
				}
			}

		}
		
		IRCMessage m;
		
		switch (state)
		{
		case 1:
			m = new IRCMessage();
			m.command = "PASS";
			m.numParams = 1;
			m.params[0] = password;
			sendQ.putMessage(m);

			m = new IRCMessage();
			m.command = "NICK";
			m.numParams = 1;
			m.params[0] = currentNick;
			sendQ.putMessage(m);
			
			timer = 10;  // wait for possible nick collision message
			state = 2;
			break;
			
		case 2:
			if (timer == 0)
			{
				m = new IRCMessage();
				m.command = "USER";
				m.numParams = 4;
				m.params[0] = name;
				m.params[1] = "0";
				m.params[2] = "*";
				m.params[3] = name;
				sendQ.putMessage(m);
				
				timer = 30;
				state = 4; // wait for login message
			}
			break;
		
		case 3:
			if (timer == 0)
			{
				chooseNewNick();
				m = new IRCMessage();
				m.command = "NICK";
				m.numParams = 1;
				m.params[0] = currentNick;
				sendQ.putMessage(m);
				
				timer = 10;  // wait for possible nick collision message
				state = 2;
			}
			break;
	
		case 4:
			if (timer == 0)
			{
				// no login message received -> disconnect
				return false;
			}
			break;
			
		case 5:
			m = new IRCMessage();
			m.command = "JOIN";
			m.numParams = 1;
			m.params[0] = channel;
			sendQ.putMessage(m);
			
			timer = 30;
			state = 6; // wait for join message
			break;
			
		case 6:
			if (timer == 0)
			{
				// no join message received -> disconnect
				return false;
			}
			break;
			
		case 10:
			m = new IRCMessage();
			m.command = "WHO";
			m.numParams = 2;
			m.params[0] = channel;
			m.params[1] = "*";
			sendQ.putMessage(m);
			
			timer = pingTimer;
			state = 11; // wait for timer and then send ping
			
			if (app != null)
			{
				app.setSendQ(sendQ);  // this switches the application on
			}
			break;
			
		case 11:
			if (timer == 0)
			{
				m = new IRCMessage();
				m.command = "PING";
				m.numParams = 1;
				m.params[0] = currentNick;
				sendQ.putMessage(m);
				
				timer = 10;
				state = 12; // wait for pong
			}
			break;
		
		case 12:
			if (timer == 0)
			{
				// no pong message received -> disconnect
				return false;
			}
			break;
		}
	
		return true;
	}
	
}
