package com.iit;

import java.io.*;
import java.net.*;

public class ServerDownload extends Thread{

    int portno;
    String FileDirectory;
    ServerSocket serverSocket;
    Socket socket;
    ServerDownload(int portno,String FileDirectory)
    {
        this.portno=portno;
        this.FileDirectory=FileDirectory;
    }
    public void run()
    {
        try{
            serverSocket=new ServerSocket(portno);
        }
        catch(IOException io)
        {
            io.printStackTrace();
        }
        try{
            socket=serverSocket.accept();

        }catch(IOException io)
        {
            io.printStackTrace();
        }
        new Downloading(socket,portno,FileDirectory).start();
    }
}

class Downloading extends Thread
{

    int portno;
    String sharedDirectory;
    Socket socket;
    String filename;
    Downloading(Socket socket,int portno,String FileDir)
    {
        this.socket=socket;
        this.portno=portno;
        this.sharedDirectory=FileDir;
    }

    public void run()
    {
        try{

            InputStream is=socket.getInputStream();
            ObjectInputStream ois=new ObjectInputStream(is);
            OutputStream os=socket.getOutputStream();
            ObjectOutputStream oos=new ObjectOutputStream(os);
            filename=(String)ois.readObject();
            String FileLocation;
            if(filename.startsWith("Invalied File"))
            {
                System.out.println(filename+"  Modified by server...");
            }
            else
            {
                while(true)
                {
                    File myFile = new File(sharedDirectory+"/"+filename);
                    long length = myFile.length();
                    byte [] mybytearray = new byte[(int)length];
                    oos.writeObject((int)myFile.length());
                    oos.flush();
                    FileInputStream fileInSt=new FileInputStream(myFile);
                    BufferedInputStream objBufInStream = new BufferedInputStream(fileInSt);
                    objBufInStream.read(mybytearray,0,(int)myFile.length());
                    System.out.println("sending file of " +mybytearray.length+ " bytes");
                    oos.write(mybytearray,0,mybytearray.length);
                    oos.flush();
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}