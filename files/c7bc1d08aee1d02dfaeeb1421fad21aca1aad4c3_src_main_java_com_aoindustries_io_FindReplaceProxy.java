/*
 * aocode-public - Reusable Java library of general tools with minimal external dependencies.
 * Copyright (C) 2012, 2013, 2016, 2018, 2019  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of aocode-public.
 *
 * aocode-public is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * aocode-public is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with aocode-public.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Listens on a socket, connects to another socket, while finding and replacing
 * values in the communication.
 *
 * @author  AO Industries, Inc.
 */
public class FindReplaceProxy {

	private static final Charset CHARSET = Charset.forName("ISO-8859-1");

	private FindReplaceProxy() {
	}

	static class FindReplace {
		private final String find;
		private final byte[] findBytes;
		private final String replace;
		private final byte[] replaceBytes;

		FindReplace(String find, String replace) {
			this.find = find;
			this.findBytes = find.getBytes(CHARSET);
			this.replace = replace;
			this.replaceBytes = replace.getBytes(CHARSET);
		}
	}

	public static void main(String[] args) {
		// Must have an even number of arguments
		boolean showArgs = false;
		if(((args.length-4)%3)!=0) {
			showArgs = true;
		} else {
			try {
				final int listenPort = Integer.parseInt(args[1]);
				final int connectPort = Integer.parseInt(args[3]);

				List<FindReplace> inFindReplaces = new ArrayList<>();
				List<FindReplace> outFindReplaces = new ArrayList<>();
				for(int pos=4; pos<args.length; pos+=3) {
					String find = args[pos];
					String replace = args[pos+1];
					String mode = args[pos+2];
					FindReplace findReplace = new FindReplace(find, replace);
					if("in".equals(mode)) inFindReplaces.add(findReplace);
					else if("out".equals(mode)) outFindReplaces.add(findReplace);
					else if("both".equals(mode)) {
						inFindReplaces.add(findReplace);
						outFindReplaces.add(findReplace);
					} else {
						showArgs = true;
						break;
					}
				}
				if(!showArgs) {
					while(true) {
						try {
							InetAddress listenAddress = InetAddress.getByName(args[0]);
							InetAddress connectAddress = InetAddress.getByName(args[2]);
							try (ServerSocket ss = new ServerSocket(listenPort, 50, listenAddress)) {
								while(true) {
									Socket socketIn = ss.accept();
									new FindReplaceProxyThread(
										socketIn,
										listenAddress,
										connectAddress,
										connectPort,
										Collections.unmodifiableList(inFindReplaces),
										Collections.unmodifiableList(outFindReplaces)
									).start();
								}
							}
						} catch(IOException e) {
							e.printStackTrace(System.err);
							try {
								Thread.sleep(1000);
							} catch(InterruptedException ie) {
								ie.printStackTrace(System.err);
							}
						}
					}
				}
			} catch(NumberFormatException e) {
				System.err.println(e.toString());
				showArgs = true;
			}
		}
		if(showArgs) {
			System.err.println("Usage: " + FindReplaceProxy.class.getName() + "listen_address listen_port connect_address connect_port [find replace {in|out|both}]...");
			System.exit(1);
		}
	}

	static class FindReplaceProxyThread extends Thread {
		private final Socket socketIn;
		private final InetAddress sourceAddress;
		private final InetAddress connectAddress;
		private final int connectPort;
		private final List<FindReplace> inFindReplaces;
		private final List<FindReplace> outFindReplaces;

		FindReplaceProxyThread(
			Socket socketIn,
			InetAddress sourceAddress,
			InetAddress connectAddress,
			int connectPort,
			List<FindReplace> inFindReplaces,
			List<FindReplace> outFindReplaces
		) {
			this.socketIn = socketIn;
			this.sourceAddress = sourceAddress;
			this.connectAddress = connectAddress;
			this.connectPort = connectPort;
			this.inFindReplaces = inFindReplaces;
			this.outFindReplaces = outFindReplaces;
		}

		@Override
		public void run() {
			try {
				try {
					try (Socket socketOut = new Socket(connectAddress, connectPort, sourceAddress, 0)) {
						FindReplaceReadThread inThread = new FindReplaceReadThread(socketIn.getInputStream(), socketOut.getOutputStream(), inFindReplaces);
						try {
							inThread.start();
							FindReplaceReadThread outThread = new FindReplaceReadThread(socketOut.getInputStream(), socketIn.getOutputStream(), outFindReplaces);
							try {
								outThread.start();
							} finally {
								try {
									inThread.join();
								} catch(InterruptedException e) {
									e.printStackTrace(System.err);
								}
							}
						} finally {
							try {
								inThread.join();
							} catch(InterruptedException e) {
								e.printStackTrace(System.err);
							}
						}
					}
				} finally {
					socketIn.close();
				}
			} catch(IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	static int indexOf(byte[] buff, int numBytes, byte[] findBytes, int pos) {
		final int findLen = findBytes.length;
		if(findLen>0) {
			while((pos+findLen)<numBytes) {
				boolean found = true;
				for(int i=0; i < findLen; i++) {
					if(buff[pos+i]!=findBytes[i]) {
						found = false;
						break;
					}
				}
				if(found) return pos;
				pos++;
			}
		}
		return -1;
	}

	static class FindReplaceReadThread extends Thread {
		private final InputStream in;
		private final OutputStream out;
		private final List<FindReplace> findReplaces;

		FindReplaceReadThread(InputStream in, OutputStream out, List<FindReplace> findReplaces) {
			this.in = in;
			this.out = out;
			this.findReplaces = findReplaces;
		}

		@Override
		public void run() {
			try {
				try {
					try {
						byte[] buff = new byte[4096];
						int numBytes;
						while((numBytes = in.read(buff, 0, 4096))!=-1) {
							// Do find/replace
							int pos = 0;
							while(pos<numBytes) {
								// Look for the matching find/replace with the lowest index
								int lowestIndex = -1;
								FindReplace lowestFindReplace = null;
								for(FindReplace findReplace : findReplaces) {
									int index = indexOf(buff, numBytes, findReplace.findBytes, pos);
									if(index!=-1 && (lowestIndex==-1 || index<lowestIndex)) {
										lowestIndex = index;
										lowestFindReplace = findReplace;
									}
								}
								if(lowestIndex!=-1) {
									assert lowestFindReplace != null;
									out.write(buff, pos, lowestIndex - pos);
									out.write(lowestFindReplace.replaceBytes);
									pos = lowestIndex + lowestFindReplace.findBytes.length;
								} else {
									// No more find/replace in this block
									out.write(buff, pos, numBytes-pos);
									pos = numBytes;
								}
							}
							out.flush();
						}
					} finally {
						out.close();
					}
				} finally {
					in.close();
				}
			} catch(IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
}
