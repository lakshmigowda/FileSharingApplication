package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;

public class Server {
	public static void main(String args[]) throws Exception {
		Properties props = new Properties();
		InputStream in = Server.class.getResourceAsStream("server.properties");
		props.load(in);

		String port = props.getProperty("port");

		@SuppressWarnings("resource")
		ServerSocket soc = new ServerSocket(Integer.valueOf(port));
		System.out.println(" Server Started on Port Number:" + port);
		while (true) {
			System.out.println("Waiting for Connection ...");
			transferfile t = new transferfile(soc.accept());

		}
	}
}

class transferfile extends Thread {
	Socket ClientSoc;

	DataInputStream din;
	DataOutputStream dout;

	transferfile(Socket soc) {
		try {
			ClientSoc = soc;
			din = new DataInputStream(ClientSoc.getInputStream());
			dout = new DataOutputStream(ClientSoc.getOutputStream());
			System.out.println(" Client Connected ...");
			start();

		} catch (Exception ex) {
		}
	}

	@SuppressWarnings("deprecation")
	public void run() {
		while (true) {
			try {
				System.out.println("Waiting for Command ...");
				String Command = din.readUTF();
				if (Command.compareTo("GET") == 0) {
					System.out.println("\tGET Command Received ...");
					SendFile();
					System.out.println("\tFile sent");
					continue;
				} else if (Command.compareTo("SEND") == 0) {
					System.out.println("\tSEND Command Receiced ...");
					ReceiveFile();
					System.out.println("\tFile received");
					continue;
				} else if (Command.compareTo("DISCONNECT") == 0) {
					System.out.println("\tDisconnect Command Received ...");
					this.suspend();
				} else {
					System.out
							.println("Something went wrong from client side !! Suspending this thread ");
					this.suspend();
				}
			} catch (SocketException ex) {
				System.out
						.println("Socket conneciton was reset from client side !! Suspending this thread ");
				this.suspend();
			} catch (Exception ex) {
			}
		}
	}

	void SendFile() throws Exception {
		String filename = din.readUTF();// R1

		File f = new File(filename);
		if (!f.exists()) {
			dout.writeUTF("File Not Found");// W1
			return;
		} else {
			dout.writeUTF("READY");// W1
			FileInputStream fin = new FileInputStream(f);

			// send file length
			dout.writeLong(f.length());// W2

			// read skip bytes length
			long skipBytes = din.readLong();// R2
			fin.skip(skipBytes);

			int ch;
			do {
				ch = fin.read();
				dout.writeUTF(String.valueOf(ch));// W3
			} while (ch != -1);
			fin.close();
			dout.writeUTF("File Received Successfully");// W4
		}
	}

	void ReceiveFile() throws Exception {
		String filename = din.readUTF();
		long fileSize = din.readLong();

		if (filename.compareTo("File not found") == 0) {
			return;
		}
		File f = new File(filename);
		String option;

		if (f.exists()) {
			if (f.length() == fileSize) {
				dout.writeUTF("File Exists:100");

			} else {
				dout.writeUTF("File Exists:"
						+ (int) (((double) f.length() / (double) fileSize) * 100));

			}
			dout.writeLong(f.length());
			option = din.readUTF();
		} else {
			dout.writeUTF("SendFile");
			dout.writeLong(f.length());
			option = "YU";
		}

		FileOutputStream fout = null;
		if (option.compareTo("YU") == 0 || option.compareTo("YR") == 0) {
			if (option.compareTo("YU") == 0)
				fout = new FileOutputStream(f);
			else if (option.compareTo("YR") == 0)
				fout = new FileOutputStream(f, true);
			int ch;
			String temp;
			do {
				temp = din.readUTF();
				ch = Integer.parseInt(temp);
				if (ch != -1) {
					fout.write(ch);
				}
			} while (ch != -1);
			fout.close();
			dout.writeUTF("File uploaded Successfully");
		} else {
			return;
		}

	}
}
