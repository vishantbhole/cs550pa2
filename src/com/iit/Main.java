package com.iit;

import java.net.*;
import java.util.Properties;
import java.io.*;
import java.util.*;

class LeafNode extends Thread {

    int portofconnection;
    int peertoconnect;
    String filetodownload;
    Socket socket = null;
    int[] peersArray;
    MessageFormat MF = new MessageFormat();
    String msgid;
    int frompeer_id;
    int TTL_value;

    public LeafNode(int portofconnection, int peertoconnect, String filetodownload, String msgid, int frompeer_id, int TTL_value) {
        this.portofconnection = portofconnection;
        this.peertoconnect = peertoconnect;
        this.filetodownload = filetodownload;
        this.msgid = msgid;
        this.frompeer_id = frompeer_id;
        this.TTL_value = TTL_value;
    }

    public void run() {
        try {
            socket = new Socket("localhost", portofconnection);
            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);
            MF.file_name = filetodownload;
            MF.message_ID = msgid;
            MF.fromPeerId = frompeer_id;
            MF.ttl = TTL_value;
            oos.writeObject(MF);

            peersArray = (int[]) ois.readObject();
        } catch (IOException io) {
            io.printStackTrace();
        } catch (ClassNotFoundException cp) {
            cp.printStackTrace();
        }
    }

    public int[] getarray() {
        return peersArray;
    }
}

class FileDownloader extends Thread {

    int portno;
    String FileDirectory;
    ServerSocket serverSocket;

    FileDownloader(int portno, String FileDirectory) {
        this.portno = portno;
        this.FileDirectory = FileDirectory;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(portno);
        } catch (IOException io) {
            io.printStackTrace();
        }
        new FileSender(serverSocket, portno, FileDirectory).start();
    }
}

class FileSender extends Thread {

    int portno;
    String sharedDirectory;
    ServerSocket socket;
    String filename;

    FileSender(ServerSocket socket, int portno, String FileDir) {
        this.socket = socket;
        this.portno = portno;
        this.sharedDirectory = FileDir;
    }

    public void run() {
        try {
            while (true) {
                System.out.println("\n\n Waiting for new File download Request \n\n");
                Socket sock = socket.accept();
                InputStream is = sock.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(is);
                filename = (String) ois.readObject();
                File myFile = new File(sharedDirectory + "/" + filename);
                byte[] mybytearray = new byte[(int) myFile.length()];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
                bis.read(mybytearray, 0, mybytearray.length);
                OutputStream os = sock.getOutputStream();
                os.write(mybytearray, 0, mybytearray.length);
                os.flush();
                sock.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class Main {
    static String fileName;

    public static void main(String[] args) {
        try {
            int ports;
            int portserver;
            int count = 0;
            int ttl;
            String msgid;
            String sharedDir;
            ArrayList<Thread> thread = new ArrayList<Thread>();
            ArrayList<LeafNode> peers = new ArrayList<LeafNode>();

            int peer_id = Integer.parseInt(args[1]);
            sharedDir = args[2];
            Properties prop = new Properties();
            fileName = args[0];
            System.out.println("Super-peer " + peer_id + " stated with private storage " + sharedDir + " Topology: " + fileName);
            InputStream is = new FileInputStream(fileName);
            prop.load(is);
            ports = Integer.parseInt(prop.getProperty("peer" + peer_id + ".serverport"));
            FileDownloader sd = new FileDownloader(ports, sharedDir);
            sd.start();
            portserver = Integer.parseInt(prop.getProperty("peer" + peer_id + ".port"));
            Superpeer cs = new Superpeer(portserver, sharedDir, peer_id);
            cs.start();
            System.out.println("Enter the filename to download a file");
            String filetodownload = new Scanner(System.in).nextLine();
            ++count;
            msgid = peer_id + "." + count;
            String[] neighbours = prop.getProperty("peer" + peer_id + ".next").split(",");
            ttl = neighbours.length;
            for (int i = 0; i < neighbours.length; i++) {
                int connectingport = Integer.parseInt(prop.getProperty("peer" + neighbours[i] + ".port"));
                int neighbouringpeer = Integer.parseInt(neighbours[i]);
                LeafNode cp = new LeafNode(connectingport, neighbouringpeer, filetodownload, msgid, peer_id, ttl);
                Thread t = new Thread(cp);
                t.start();
                thread.add(t);
                peers.add(cp);
            }
            for (int i = 0; i < thread.size(); i++) {
                try {
                    ((Thread) thread.get(i)).join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int[] peerswithfiles;

            System.out.println("Leafnodes containing the file are: ");
            int peerfromdownload = 0;
            for (int i = 0; i < peers.size(); i++) {
                peerswithfiles = ((LeafNode) peers.get(i)).getarray();
                for (int j = 0; j < peerswithfiles.length; j++) {
                    if (peerswithfiles[j] == 0)
                        break;
                    System.out.println(peerswithfiles[j]);
                    peerfromdownload = peerswithfiles[j];
                }
            }
            System.out.println("\n Selecting leafnode: " + peerfromdownload + " To download file \n");
            int porttodownload = Integer.parseInt(prop.getProperty("peer" + peerfromdownload + ".serverport"));
            StreamProcessor(peerfromdownload, porttodownload, filetodownload, sharedDir);
            System.out.println("File: " + filetodownload + " downloaded from Leafnode: " + peerfromdownload + " to Leafnode:" + peer_id);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static void StreamProcessor(int cspeerid, int csportno, String filename, String sharedDir) {
        try {
              Socket clientsocket = new Socket("localhost", csportno);
            ObjectOutputStream ooos = new ObjectOutputStream(clientsocket.getOutputStream());
            ooos.flush();

            ooos.writeObject(filename);

            String outputFile = sharedDir + "/" + filename;
            byte[] mybytearray = new byte[1024];
            InputStream is = clientsocket.getInputStream();
            FileOutputStream fos = new FileOutputStream(outputFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            int bytesRead = is.read(mybytearray, 0, mybytearray.length);
            bos.write(mybytearray, 0, bytesRead);
            bos.close();
            clientsocket.close();
            System.out.println(filename + " file is transferred to your private storage: " + sharedDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
