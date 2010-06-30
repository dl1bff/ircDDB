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


import java.net.Socket;
import java.net.UnknownHostException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;


public class IRCClient implements Runnable
{

	IRCReceiver recv;
	IRCMessageQueue recvQ;
	IRCMessageQueue sendQ;
	Socket socket;
	Thread recvThread;
	OutputStream outputStream;

	IRCApplication app;
	
	String host;
	int port;
	IRCProtocol proto;
	
	boolean debug;

	
	public IRCClient(IRCApplication a, String h, int p, String ch,
		 String dbg_chan, String n, String[] u, String pass,
		 boolean dbg, String version)
	{
		recv = null;
		recvQ = null;
		sendQ = null;
		socket = null;
		recvThread = null;
		outputStream = null;
		app = a;
		
		host = h;
		port = p;

		debug = dbg;
		
		proto = new IRCProtocol(a, ch, dbg_chan, n, u, pass, debug, version);
	}

	boolean init()
	{

		recvQ = new IRCMessageQueue();
		sendQ = new IRCMessageQueue();

		try
		{
			socket = new Socket(host, port);
		}
		catch (UnknownHostException e)
		{
			System.out.println("IRCClient/socket: " + e);
			return false;
		}
		catch (IOException e)
		{
			System.out.println("IRCClient/socket: " + e);
			return false;
		}

		try
		{
			outputStream = socket.getOutputStream();
		}
		catch (IOException e)
		{
			System.out.println("IRCClient/getOutputStream: " + e);
			return false;
		}

		InputStream is;

		try
		{
			is = socket.getInputStream();
		}
		catch (IOException e)
		{
			System.out.println("IRCClient/getInputStream: " + e);
			return false;
		}

		recv = new IRCReceiver( is, recvQ );

		recvThread = new Thread(recv);

		try
		{
			recvThread.start();
		}
		catch (IllegalThreadStateException e)
		{
			System.out.println("IRCClient/Thread.start: " + e);
			return false;
		}

	
		proto.setNetworkReady(true);

		return true;
	}


	void closeConnection()
	{
		if (app != null)
		{
			app.setSendQ(null);
			app.userListReset();
		}

		if (socket != null)
		{
			try
			{
				socket.shutdownInput();
			}
			catch (IOException e)
			{
				System.out.println("IRCClient/socket.shutdownInput: " + e);
			}

			try
			{
				socket.close();
			}
			catch (IOException e)
			{
				System.out.println("IRCClient/socket.close: " + e);
			}
	
		}

		socket = null;
		recv = null;
		recvThread = null;
		recvQ = null;
		sendQ = null;
		outputStream = null;
		
		proto.setNetworkReady(false);
	}

	public void run()
	{
		int timer = 0;
		int state = 0;


		while(true)
		{
		    if (timer == 0)
		    {
				switch(state)
				{
				case 0:
					System.out.println("IRCClient: connect request");
					if (init())
					{
						System.out.println("IRCClient: connected");
						state = 1;
						timer = 1;
					}
					else
					{
						timer = 1;
						state = 2;
					}
					break;
					
				case 1:
					if (recvQ.isEOF())
					{
						timer = 0;
						state = 2;
					}
					else if (proto.processQueues(recvQ, sendQ) == false)
					{
						timer = 0;
						state = 2;
					}

					while ((state == 1) && sendQ.messageAvailable())
					{
						IRCMessage m = sendQ.getMessage();

						try
						{
							m.writeMessage(outputStream, debug);
						}
						catch(IOException e)
						{
							System.out.println("IRCClient/write: " +e);
							timer = 0;
							state = 2;
						}
					}
					break;

				case 2:
					closeConnection();
					timer = 30; // wait 15 seconds
					state = 0;
					break;
				}
		    }
		    else
		    {
				timer--;
		    }

			try
			{
				Thread.sleep(500);
			}
			catch ( InterruptedException e )
			{
				System.out.println(e);
			}
		}

	}	
}

