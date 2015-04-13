package com.research.itsl.ism_d2d;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class DataTransferAsyncTask extends AsyncTask<Void, Void, String> {

    private MainActivity mainActivity;
    private String transferAction;
    private String sendAddress;
    private String receiveAddress;
    private String groupOwnerAddress;



    private long TIMEOUT_SECONDS = 5;
    private int PACKET_LENGTH = 64;
    public final static String TRANSFER_SEND = "com.research.itsl.ism_d2d.SEND";
    public final static String TRANSFER_RECEIVE = "com.research.itsl.ism_d2d.RECEIVE";


    public DataTransferAsyncTask(MainActivity activity, String transferType, String sendingAddress, String receivingAddress, String groupOwner) {
        this.mainActivity = activity;
        this.transferAction = transferType;
        this.sendAddress = sendingAddress;
        this.groupOwnerAddress = groupOwner;
        this.receiveAddress = receivingAddress;
    }

    @Override
    protected String doInBackground(Void... params) {
        try{
            //CHARLEY: Create a socket to send data
            DatagramSocket sendSocket = new DatagramSocket();
            sendSocket.setBroadcast(true);
            byte[] packet_buf = new byte[1024];

            //CHARLEY: We are using port 8988 for our socket and sending to the group owner address. Each packet is at most 64 bytes of data.
            if( transferAction.equals(TRANSFER_SEND)){
                int srcPos = 0;
                byte[] buf;
                buf = ("Sending data from "+ this.sendAddress+ " to"+ this.receiveAddress + ". Data can now be used for signal processing ").getBytes();
                while (srcPos < buf.length) {
                    try {
                        DatagramPacket packet;
                        InetAddress address = InetAddress.getByName(groupOwnerAddress);
                        //CHARLEY: If the last packet is less than 64 bytes only send how many bytes are left
                        if(buf.length- srcPos < PACKET_LENGTH){
                            packet = new DatagramPacket(buf,srcPos,  buf.length - srcPos, address, 8988);
                        }else{
                            packet = new DatagramPacket(buf,srcPos, PACKET_LENGTH, address, 8988);
                        }

                        sendSocket.send(packet);

                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    srcPos += PACKET_LENGTH;
                }
            }
            else if( transferAction.equals(TRANSFER_RECEIVE)){
                try{
                    DatagramSocket receiveSocket = new DatagramSocket(8988);

                    for (long stop=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);stop>System.nanoTime();) {
                        packet_buf = new byte[PACKET_LENGTH];
                        DatagramPacket packet = new DatagramPacket(packet_buf, PACKET_LENGTH);
                        receiveSocket.receive(packet);
                        //CHARLEY: Parse the received packet into a string and concatenate it to our result string so we can display our data
                        byte[] data = packet.getData();
                        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(data), Charset.forName("UTF-8"));
                        StringBuilder str = new StringBuilder();
                        for (int value; (value = input.read()) != -1; )
                            str.append((char) value);
                        mainActivity.receivedDataString+=(str.toString());
                    }

                    receiveSocket.close();
                    return mainActivity.receivedDataString;
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
            sendSocket.close();

            return null;
        }catch(SocketException exep){
            Log.v("SOCKET ERROR:", "Unable to create socket");
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if(transferAction == this.TRANSFER_RECEIVE){
            //CHARLEY: Update our view in our Main Activity so that we can see the received data.
            if (mainActivity.receivedDataString != null) {
                TextView dataReceived=(TextView) this.mainActivity.findViewById(R.id.received_data_textview);
                if(dataReceived != null)
                    dataReceived.setText(mainActivity.receivedDataString);
            }
        }

    }

}

