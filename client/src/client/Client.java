package client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class Client {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void main(String args[]) throws Exception {
		Properties props = new Properties();
		InputStream in = Client.class.getResourceAsStream("client.properties");
		props.load(in);
		String serverlist = props.getProperty("servers");
		String servers[] = serverlist.split(",");
		String server = "";
		String port = "";
		String value = "";

		int option = 0;
		while (true) {
			System.out.println("Please select a server :");

			int i = 1;
			for (String serverEntry : servers) {
				System.out.println(i++ + " : " + serverEntry);
			}

			System.out.print("\nEnter Choice (ex: 3) :");
			Scanner sc = new Scanner(System.in);
			value = sc.nextLine();
			if ((Integer.valueOf(value) > servers.length)
					|| (Integer.valueOf(value) <= 0)) {
				System.out.println("Please enter a valid option\n");
			} else {
				server = servers[(Integer.valueOf(value)) - 1].split("@")[0];
				port = servers[(Integer.valueOf(value)) - 1].split("@")[1];
				break;
			}
		}

		System.out.println("Establishing socket connection to : "
				+ servers[(Integer.valueOf(value)) - 1]);
		Socket soc = new Socket(InetAddress.getByName(server),
				Integer.valueOf(port));
		System.out.println("Conneciton established to : "
				+ servers[(Integer.valueOf(value)) - 1]);

		transferfileClient t = new transferfileClient(soc);
		t.displayMenu();

	}
}

class transferfileClient {
	Socket ClientSoc;

	DataInputStream din;
	DataOutputStream dout;
	BufferedReader br;

	transferfileClient(Socket soc) {
		try {
			ClientSoc = soc;
			din = new DataInputStream(ClientSoc.getInputStream());
			dout = new DataOutputStream(ClientSoc.getOutputStream());
			br = new BufferedReader(new InputStreamReader(System.in));
		} catch (Exception ex) {
		}
	}

	void SendFile() throws Exception {

		String filename;
		System.out.print("Enter File Name :");
		filename = br.readLine();

		File f = new File(filename);
		if (!f.exists()) {
			System.out.println("File not Exists...");
			dout.writeUTF("File not found");
			return;
		}

		dout.writeUTF(filename);
		dout.writeLong(f.length());
		FileInputStream fin = new FileInputStream(f);

		String msgFromServer = din.readUTF();
		long skipBytes = din.readLong();
		long sentBytes = 0;
		String resorup = "";
		if (msgFromServer.contains("File Exists")) {
			String Option;
			if (msgFromServer.contains("100")) {
				System.out
						.println("File Already Exists. Want to OverWrite (Y/N) ?");
				resorup = "U";
			} else {
				String[] message = msgFromServer.split(":");
				System.out
						.println(message[1]
								+ "% of file was uploaded. \n Do you to resume upload? (Y/N) ?");
				fin.skip(skipBytes);
				sentBytes = skipBytes;
				resorup = "R";
			}
			Option = br.readLine();
			if (Option.equalsIgnoreCase("Y") && resorup.equals("U")) {
				dout.writeUTF("YU");
			} else if (Option.equalsIgnoreCase("Y") && resorup.equals("R")) {
				dout.writeUTF("YR");
			} else {
				dout.writeUTF("N");
				return;
			}
		}

		System.out.println("Sending File ...");

		int ch;
		do {
			ch = fin.read();
			dout.writeUTF(String.valueOf(ch));
			sentBytes++;
			if ((((double) sentBytes / (double) f.length()) * 100) % 10 == 0)
				System.out.print(((double) sentBytes / (double) f.length())
						* 100 + "% ==>");
		} while (ch != -1);
		fin.close();
		System.out.println(din.readUTF());

	}

	void ReceiveFile() throws Exception {
		String fileName;
		System.out.print("Enter File Name :");
		fileName = br.readLine();
		File f = new File(fileName);

		// send filename
		dout.writeUTF(fileName);// W1

		String msgFromServer = din.readUTF();// R1

		if (msgFromServer.compareTo("File Not Found") == 0) {
			System.out.println("File not found on Server ...");
			return;
		} else if (msgFromServer.compareTo("READY") == 0) {

			long fileSize = din.readLong();// R2
			FileOutputStream fout = null;
			long receivedByes = 0;

			if (f.exists()) {
				String Option;

				if (f.length() == fileSize) {
					System.out
							.println("File Exists. Want to download and overwrite (Y/N) ?");
					dout.writeLong(0);// W2
					fout = new FileOutputStream(f);
					receivedByes = 0;
				} else {
					System.out
							.println((((double) f.length() / (double) fileSize) * 100)
									+ "% of file was downloaded. \n Do you to resume donwload? (Y/N) ?");
					dout.writeLong(f.length());// W2
					fout = new FileOutputStream(f, true);
					receivedByes = f.length();
				}
				Option = br.readLine();
				if (Option.equalsIgnoreCase("N")) {
					dout.flush();// W
					return;
				}
			} else {
				// send the file length already read
				dout.writeLong(0);// W2
				fout = new FileOutputStream(f);
			}

			System.out.println("Receiving File ...");

			System.out.println("Size of the file: " + fileSize + "bytes");
			System.out.println("Downloading file...");

			int ch;

			String temp;
			do {
				temp = din.readUTF();// R3
				ch = Integer.parseInt(temp);
				if (ch != -1) {
					fout.write(ch);
					receivedByes++;
					if ((((double) receivedByes / (double) fileSize) * 100) % 10 == 0)
						System.out
								.print(((double) receivedByes / (double) fileSize)
										* 100 + "% ==>");
				}
			} while (ch != -1);
			fout.close();
			System.out.println(din.readUTF());// R4

		}

	}

	public void displayMenu() throws Exception {
		while (true) {
			System.out.println("[ MENU ]");
			System.out.println("1. Send File");
			System.out.println("2. Receive File");
			System.out.println("3. Exit");
			System.out.print("\nEnter Choice :");
			int choice;
			choice = Integer.parseInt(br.readLine());
			if (choice == 1) {
				dout.writeUTF("SEND");
				SendFile();
			} else if (choice == 2) {
				dout.writeUTF("GET");
				ReceiveFile();
			} else {
				dout.writeUTF("DISCONNECT");
				System.exit(1);
			}
		}
	}
}