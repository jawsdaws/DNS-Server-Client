// Homework 1: DNS Server UDP
// Student Name: James Daws
// Course: CS401, Fall 2017
// Instructor: Dr. Poonam Dharam
// Date finished: 09/08/2017
// Program description: This program is the DNS server side that uses UDP.
//
// Programmer Notes: UDP is a poor protocol for this application. UDP is unreliable!!!!
//                   Sometimes the output is correct, other times it is not.  UDP also
//                   is connectionless, so threads are not needed.
//                   Most of the processing is done in the main while loop, and the processFile
//                   function handles the file work.

package edu.svsu;

import java.io.*;
import java.net.*;

public class HW1_Server_UDP {

    private static InetAddress hostAddress = null;
    private static DatagramSocket datagramSocket = null;

    /*
        Main.  Not much else to say.
    */
    public static void main(String[] args) {

        //Setup block.  Mostly used for debugging.
        int port = 8019;
        String hostname = "localhost";
        processFile(0);
        System.out.println("Welcome to the DNS server.");
        System.out.println("The hostname is " + hostname);

        try {
            hostAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            System.out.println("Unable to lookup ip address.");
        }

        System.out.println("The ip is " + hostAddress.getHostAddress());

        try {
            datagramSocket = new DatagramSocket(port);
        } catch (Exception e) {
            System.out.println("Error listening on " + port);
        }

        //The primary while loop.  Listen. Rx a packet. Process it,
        // and send it back.  Last, count a connection.

        while (true) {
            System.out.println("Listening for clients.");
            try {
                byte[] rxData = new byte[4096];
                DatagramPacket rxPacket = new DatagramPacket(rxData, rxData.length);
                datagramSocket.receive(rxPacket);
                processFile(1);
                ConnectionHandle connectionhandle = new ConnectionHandle(rxPacket, rxData);
                Thread thread = new Thread(connectionhandle);
                thread.start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("FUCK!!!");
            }
        }
    }

    /**
     * The thread portion of the UDP connection so that this server can have multiple clients.
     */
    private static class ConnectionHandle implements Runnable {

        private DatagramPacket rxPacket;
        private byte[] rxData;

        private ConnectionHandle(DatagramPacket rxPacket, byte[] rxData) {
            this.rxData = rxData;
            this.rxPacket = rxPacket;
        }

        @Override
        public void run() {

            byte[] sendData = new byte[4096];
            String returnIP;

            try {

                rxData = rxPacket.getData();
                InetAddress returnAddress = rxPacket.getAddress();
                int returnPort = rxPacket.getPort();
                ByteArrayInputStream byteInput = new ByteArrayInputStream(rxData);
                ObjectInputStream inputFromClient = new ObjectInputStream(byteInput);
                IPData inData = (IPData) inputFromClient.readObject();

                try {
                    InetAddress lookupAddress = InetAddress.getByName(inData.getStringData());
                    returnIP = lookupAddress.getHostAddress();
                } catch (UnknownHostException ue) {
                    ue.printStackTrace();
                    returnIP = "Unknown host";
                }

                ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(returnIP.getBytes().length);
                ObjectOutputStream outputToClient = new ObjectOutputStream(new BufferedOutputStream(outByteStream));
                IPData outData = new IPData(returnIP, processFile(0));
                outputToClient.flush();
                outputToClient.writeObject(outData);
                outputToClient.flush();
                sendData = outByteStream.toByteArray();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, returnAddress, returnPort);
                datagramSocket.send(sendPacket);

            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }

            /*try {
                ObjectOutputStream outputToClient = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputFromClient = new ObjectInputStream(socket.getInputStream());


                while (true) {
                    IPData input = (IPData) inputFromClient.readObject();
                    System.out.println(input.getStringData());
                    try {
                        lookupAddress = InetAddress.getByName(input.getStringData());
                        IPData outData = new IPData(lookupAddress.getHostAddress(), processFile(0));
                        outputToClient.writeObject(outData);
                    } catch (UnknownHostException e) {
                        System.out.println("Unable to lookup ip address.");
                        IPData outData = new IPData("Unable to lookup ip address.", processFile(0));
                        outputToClient.writeObject(outData);
                    }
                }
            } catch (EOFException e) {
                System.out.println("Client disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }*/

        }
    }

    /**
     * This function reads the count file and updates the count based on the int param.
     * @param count The amount to add to the count read from the file.
     *            Use -1 will decrement.
     */
    private static int processFile(int count) {

        File file = new File("countfile.txt");

        //Create a new file if one does not exist.
        if (count == 0 && !file.exists()) {
            try {
                file.createNewFile();
                BufferedWriter bw;
                FileWriter fw;
                fw = new FileWriter(file);
                bw = new BufferedWriter(fw);
                bw.write("0");
                bw.close();
                return 0;
            } catch (IOException e) {

            }
        //If there is anything other than zero add it to the total.(negatives decrement)
        } else {
            try {
                BufferedReader br;
                br = new BufferedReader(new FileReader(file));
                String line = br.readLine();
                br.close();
                BufferedWriter bw;
                FileWriter fw;
                fw = new FileWriter(file);
                bw = new BufferedWriter(fw);
                bw.write(Integer.toString(Integer.parseInt(line) + count));
                bw.close();
                return Integer.parseInt(line);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } return 0;
    }
}