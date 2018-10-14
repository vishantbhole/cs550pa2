package com.iit;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Properties;

public class Superpeer extends Thread {
    String FileDir;
    int port_no;
    ServerSocket serverSocket=null;
    Socket socket=null;
    int peer_id;
    static ArrayList<String> msg;
    Superpeer(int port, String SharedDir, int peer_id)
    {
        port_no=port;
        FileDir=SharedDir;
        this.peer_id=peer_id;
        msg=new ArrayList<String>();
    }

    public void run()
    {
        try
        {
            serverSocket=new ServerSocket(port_no);
        }
        catch(IOException ie)
        {
            ie.printStackTrace();
        }
        while(true)
        {
            try{
                socket=serverSocket.accept();
                System.out.println("Superpeer connected to Leafnode: " + peer_id + ", at "+socket.getRemoteSocketAddress());
                new Download(socket,FileDir,peer_id,msg).start();
            }
            catch(IOException io)
            {
                io.printStackTrace();
            }
        }
    }
}


class Download extends Thread
{
    protected Socket socket;
    String FileDirectory;
    int port;
    String fname;
    int peer_id;
    ArrayList<String> peermsg;
    ArrayList<Thread> thread=new ArrayList<Thread>();
    ArrayList<LeafNode> peerswithfiles=new ArrayList<LeafNode>();
    int[] peersArray_list=new int[20];
    int[] a=new int[20];
    int countofpeers=0;
    int messageId;
    int set=0;
    int TTL_value;
    MessageFormat MF=new MessageFormat();
    Download(Socket socket,String FileDirectory,int peer_id,ArrayList<String> peermsg)
    {
        this.socket=socket;
        this.FileDirectory=FileDirectory;
        this.peer_id=peer_id;
        this.peermsg=peermsg;
    }

    public void run()
    {
        try{
            System.out.println("Leaf node peer"+peer_id);

            InputStream is=socket.getInputStream();
            ObjectInputStream ois=new ObjectInputStream(is);
            OutputStream os=socket.getOutputStream();
            ObjectOutputStream oos=new ObjectOutputStream(os);
            boolean peerduplicate;

            MF=(MessageFormat)ois.readObject();

            System.out.println("Received query from "+MF.fromPeerId);

            peerduplicate=this.peermsg.contains(MF.message_ID);
            if(peerduplicate==false)
            {
                this.peermsg.add(MF.message_ID);
            }
            else
            {
                System.out.println("Recieved Same query before.");
            }

            fname=MF.file_name;
            System.out.println("queryhit: "+fname);

            if(!peerduplicate)
            {
                File newfind;
                File directoryObj = new File(FileDirectory);
                String[] filesList = directoryObj.list();
                for (int j = 0; j < filesList.length; j++)
                {
                    newfind = new File(filesList[j]);
                    if(newfind.getName().equals(fname))
                    {
                        peersArray_list[countofpeers++]=peer_id;
                        break;
                    }
                }
                System.out.println("Msg from Superpeer: Search in Leafnode completed");
                Properties prop = new Properties();
                Main M=new Main();
                String fileName = M.fileName;
                is = new FileInputStream(fileName);
                prop.load(is);
                String temp=prop.getProperty("peer"+peer_id+".next");
                if(temp!=null && MF.ttl >0)
                {
                    String[] neighbours=temp.split(",");

                    for(int i=0;i<neighbours.length;i++)
                    {
                        if(MF.fromPeerId==Integer.parseInt(neighbours[i]))
                        {
                            continue;
                        }
                        int connectingport=Integer.parseInt(prop.getProperty("peer"+neighbours[i]+".port"));
                        int neighbouringpeer=Integer.parseInt(neighbours[i]);

                        System.out.println(" File sent to "+neighbouringpeer);
                        LeafNode cp=new LeafNode(connectingport,neighbouringpeer,fname,MF.message_ID,peer_id,MF.ttl--);
                        Thread t=new Thread(cp);
                        t.start();
                        thread.add(t);
                        peerswithfiles.add(cp);

                    }
                }
                for(int i=0;i<thread.size();i++)
                {
                    ((Thread) thread.get(i)).join();
                }
                for(int i=0;i<peerswithfiles.size();i++)
                {
                    a=((LeafNode)peerswithfiles.get(i)).getarray();
                    for(int j=0;j<a.length;j++)
                    {	if(a[j]==0)
                        break;
                        peersArray_list[countofpeers++]=a[j];
                    }
                }
            }
            oos.writeObject(peersArray_list);

        }
        catch(Exception e)
        {

            e.printStackTrace();

        }
    }
}