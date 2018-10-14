package com.iit;

import java.net.*;
import java.util.Properties;
import java.io.*;
import java.util.*;


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
            System.out.println("Peer " + peer_id + " stated with shared directory " + sharedDir);
            Properties prop = new Properties();                        //Properties class to read the configuration file
            fileName = args[0];
            System.out.println("Selected the " + fileName);
            InputStream is = new FileInputStream(fileName);
            prop.load(is);
            ports = Integer.parseInt(prop.getProperty("peer" + peer_id + ".serverport"));
            ServerDownload sd = new ServerDownload(ports, sharedDir);
            sd.start();
            portserver = Integer.parseInt(prop.getProperty("peer" + peer_id + ".port"));
            Superpeer cs = new Superpeer(portserver, sharedDir, peer_id);
            cs.start();
            System.out.println("Enter the filename to download a file");
            String f_name = new Scanner(System.in).nextLine();
            ++count;
            msgid = peer_id + "." + count;
            String[] neighbours = prop.getProperty("peer" + peer_id + ".next").split(",");
            ttl = neighbours.length;
            for (int i = 0; i < neighbours.length; i++) {
                int connectingport = Integer.parseInt(prop.getProperty("peer" + neighbours[i] + ".port"));
                int neighbouringpeer = Integer.parseInt(neighbours[i]);
                LeafNode cp = new LeafNode(connectingport, neighbouringpeer, f_name, msgid, peer_id, ttl);
                Thread t = new Thread(cp);
                t.start();
                thread.add(t);
                peers.add(cp);
            }
            for (int i = 0; i < thread.size(); i++) {
                try {
                    ((Thread) thread.get(i)).join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            int[] peerswithfiles;//part on how to send data from the ConnectingPeer

            System.out.println("Peers containing the file are: ");
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
            System.out.println("\n Selecting " + peerfromdownload + " To download file \n");
            int porttodownload = Integer.parseInt(prop.getProperty("peer" + peerfromdownload + ".serverport"));
            ClientasServer(peerfromdownload, porttodownload, f_name, sharedDir);
            System.out.println("File: " + f_name + " downloaded from Peer " + peerfromdownload + " to Peer " + peer_id);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static void ClientasServer(int cspeerid, int csportno, String filename, String sharedDir) {
        try {
            Socket clientasserversocket = new Socket("localhost", csportno);
            ObjectOutputStream ooos = new ObjectOutputStream(clientasserversocket.getOutputStream());
            ooos.flush();
            ObjectInputStream oois = new ObjectInputStream(clientasserversocket.getInputStream());
            ooos.writeObject(filename);
            int readbytes = (int) oois.readObject();
            System.out.println("bytes transferred: " + readbytes);
            byte[] myByteArray = new byte[readbytes];
            oois.readFully(myByteArray);
            String outputFile = sharedDir + "/" + filename;
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(myByteArray);
            fos.close();

            System.out.println(filename + " file has be downloaded to your directory " + sharedDir);
            //myByteArray.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void BroadcastInvaliedMsg(int cspeerid, int csportno, String filename) {
        try {
            Socket clientasserversocket = new Socket("localhost", csportno);
            ObjectOutputStream ooos = new ObjectOutputStream(clientasserversocket.getOutputStream());
            ooos.flush();
            ooos.writeObject("Invalied File " + filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
