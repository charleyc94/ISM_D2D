package com.research.itsl.ism_d2d;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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


    //CHARLEY: File containing data, should be placed in ISM_D2D folder if folder exists
    private final String dataFileName = "SideChannelData.txt";

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
                //CHARLEY: Convert a file placed within ISM_D2D folder in phone to bytes, or if file does not exist, simply send a simple message
                buf = convertFileToBytes();
                while (srcPos < buf.length ) {
                    try {
                        DatagramPacket packet;
                        InetAddress address;
                        //CHARLEY: Get the IP address from the MAC address of the target device, The OS seems to be off by hexadecimal value of 8 in the 4th to last digit
                        String receiveIpAddress = getIPFromMac(this.receiveAddress);
                        //CHARLEY: If we can't find the IP address(receiveIpAddress == null), just used the groupOwnerAddress
                        if(receiveIpAddress==null)
                            address = InetAddress.getByName(groupOwnerAddress);
                        else
                            address = InetAddress.getByName(receiveIpAddress);
                        //CHARLEY: If the last packet is less than 64 bytes only send how many bytes are left
                        if(buf.length- srcPos - 1 < mainActivity.PACKET_LENGTH){
                            packet = new DatagramPacket(buf,srcPos,  buf.length - srcPos, address, 8988);
                        }else{
                            packet = new DatagramPacket(buf,srcPos, mainActivity.PACKET_LENGTH, address, 8988);
                        }

                        sendSocket.send(packet);

                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Move a PACKET LENGTH down the buffer to get the next packet
                    srcPos += mainActivity.PACKET_LENGTH;
                }
            }
            else if( transferAction.equals(TRANSFER_RECEIVE)){
                try{
                    DatagramSocket receiveSocket = new DatagramSocket(8988);

                    //CHARLEY: Run for TIMEOUT_SECONDS long before timing out for the receiving of packets
                    for (long stop=System.nanoTime()+TimeUnit.SECONDS.toNanos(mainActivity.TIMEOUT_SECONDS);stop>System.nanoTime();) {
                        packet_buf = new byte[mainActivity.PACKET_LENGTH];
                        DatagramPacket packet = new DatagramPacket(packet_buf, mainActivity.PACKET_LENGTH);
                        receiveSocket.receive(packet);
                        //CHARLEY: Parse the received packet into a string and concatenate it to our result string so we can display our data
                        byte[] data = packet.getData();
                        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(data), Charset.forName("UTF-8"));
                        StringBuilder str = new StringBuilder();
                        for (int value; (value = input.read()) != -1; )
                            str.append((char) value);
                        mainActivity.receivedDataString+=(str.toString())+"\n";
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

    //CHARLEY: Convert file stored in ISM_D2D into byte array, Otherwise send a simple message including from and to MAC addresses
    private byte[] convertFileToBytes(){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            File file = new File(Environment.getExternalStorageDirectory(), "ISM_D2D");
            if (!file.exists()) {
                if (file.mkdirs()) {
                    File[] dirFiles = file.listFiles();
                    if(dirFiles.length != 0){
                        for(int i =0; i< dirFiles.length; i++){
                            if(dirFiles[i].getName().equals(dataFileName)){
                                try{
                                    FileInputStream fileInputStream = new FileInputStream(dirFiles[i]);
                                    fileInputStream.close();
                                    return convertStreamToString(fileInputStream).getBytes();
                                }catch (FileNotFoundException fn){
                                    Log.v("Unable to find file",dataFileName);
                                }catch (Exception e){}
                            }
                        }
                    }
                }else{
                    return ("Sending data from "+ this.sendAddress+ " to "+ this.receiveAddress + ". Data can now be used for signal processing ").getBytes();
                }
            }else{
                File[] dirFiles = file.listFiles();
                if(dirFiles.length != 0){
                    for(int i =0; i< dirFiles.length; i++){
                        if(dirFiles[i].getName().equals(dataFileName)){
                            try{
                                FileInputStream fileInputStream = new FileInputStream(dirFiles[i]);
                                byte[] resultByteArray = convertStreamToString(fileInputStream).getBytes();
                                fileInputStream.close();
                                return resultByteArray;
                            }catch (FileNotFoundException fn){
                                Log.v("Unable to find file",dataFileName);
                            }catch (Exception e){}
                        }
                    }
                }
            }
        }
        return ("Sending data from "+ this.sendAddress+ " to "+ this.receiveAddress + ". Data can now be used for signal processing ").getBytes();
    }

    //CHARLEY: Convert file input stream into a string
    private String convertStreamToString(InputStream inputStream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        sb.append("From: "+this.sendAddress+ " To: "+ this.receiveAddress);
        int initalHeaderLength = sb.length();
        for(int i = 0; i < (this.mainActivity.PACKET_LENGTH - initalHeaderLength); i++){
            sb.append("-");
        }
        sb.append("\n");
         String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    //CHARLEY: Get the IP address from the given MAC address so that we can send data to the selected device
    public static String getIPFromMac(String MAC) {
        BufferedReader br = null;
        try {
            //CHARLEY: /proc/net/arp stores all the networks and devices under each network we have seen, we can find our target device by MAC through this table
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                //CHARLEY: split words on regex, so " +" is one or more spaces, So we split each ARP entry into array of multiple strings using space as delimiter
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String mac = splitted[3];
                    //CHARLEY: The first condition is to make sure we are not looing at ARP header and we are actually looking at a valid MAC address
                    //          We skip the 13th entry in the string or 9th digit since the phone OS seems to corrupt the 9th digit
                    if (mac.matches("..:..:..:..:..:..") && mac.substring(0,11).equals(MAC.substring(0,11)) && mac.substring(13).equals(MAC.substring(13))) {
                        //Log.v("ORIGINAL-DERIVED MAC ADDRESS-IP ADDRESS DESIRED",MAC+" "+splitted[3]+" "+splitted[0]);
                        return splitted[0];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //CHARLEY: If we could not find the IP address, return null
        return null;
    }

}

