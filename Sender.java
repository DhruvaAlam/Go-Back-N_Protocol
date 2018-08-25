//good copy

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.Iterator;

public class Sender {
    //init variables
    public static String hostAddressEmulator;
    public static int udpPortEmulator;
    public static int udpPortSenderACK;
    public static DatagramSocket clientSocket;
    public static InetAddress emulatorIpAddress;

    // Files and log stuff
    public static File fileToSend;
    public static String fileName;
    public static BufferedWriter seqNumLog;
    public static BufferedWriter ackLog;

    //concurrency control
    public static Semaphore semaphore = new Semaphore(1, true);

    //Go Back N Variables
    public static int base = 0;
    public static int nextSeqNum = 0;
    public static int multiple = 0;
    public static int windowSize = 10;
    public static int maxDataLength = 500;
    public static int timeout = 5000; //5 seconds
    
    //list of already send out but non-acked packets
    public static LinkedList<packet> nonackedPackets = new LinkedList<packet>();
    public static Date startingTime;


    public static boolean eotSent = false;


    // all packets to send
    public static ArrayList <packet> packetList = new ArrayList<packet>();
    public static int number = 0;



    public void initiate(String a, String b, String c, String d) throws Exception{
        hostAddressEmulator = a;
        udpPortEmulator = Integer.parseInt(b);
        udpPortSenderACK = Integer.parseInt(c);
        fileToSend = new File(d);
        fileName = d;

        clientSocket = new DatagramSocket(udpPortSenderACK);
        clientSocket.setSoTimeout(timeout);
        emulatorIpAddress = InetAddress.getByName(hostAddressEmulator);

        seqNumLog =
                new BufferedWriter(new FileWriter("seqnum.log"));
        ackLog =
                new BufferedWriter(new FileWriter("ack.log"));
    }

    public void sendPacket(packet p) throws java.io.IOException{
        if (p.getType() != 2){
            seqNumLog.write(String.valueOf(p.getSeqNum()));
            seqNumLog.newLine();
        }

        byte [] data = p.getUDPdata();
        DatagramPacket packetToSend =
                new DatagramPacket(data,
                        data.length,
                        emulatorIpAddress,
                        udpPortEmulator);
        clientSocket.send(packetToSend);
    }

    public static boolean isWindowFull(){
        if (base + windowSize < 32){
            if (nextSeqNum >= base && nextSeqNum < (base + 10)){
                return false;
            } else {
                return true;
            }
        } else if ((nextSeqNum >= base) && (nextSeqNum < 32)){
            return false;
        } else if ((nextSeqNum < ((base + 10) % 32)) && (nextSeqNum >= 0)){
            return false;
        }
        return true;
    }

    public void resendPacketsInWindow() throws Exception{
        // System.out.println();
        // System.out.print("timeout: ");
        for (packet p: nonackedPackets){
            // System.out.print(p.getSeqNum() + " ");
            sendPacket(p);
        }
        // System.out.println();

    }
    public void convertFileToPacketList() throws java.lang.Exception{
        BufferedReader bufferedFile;
        try {
            bufferedFile = new BufferedReader(new FileReader(fileName));

        } catch (FileNotFoundException ex){
            return;
        }
        int i =0;
        while(true){
            char[] temp = new char[maxDataLength];
            int numOfCharsRead =
                bufferedFile.read(temp, 0, maxDataLength);
             if (numOfCharsRead == -1){
                 break;
             }
             packetList.add(packet.createPacket(i,new String(temp, 0, numOfCharsRead)));
             ++i;
        }
    }
    // Get and ack or eot from the receiver
    public packet getResponse() throws Exception{
        byte[] responseData = new byte[1024];
        DatagramPacket response =
                new DatagramPacket(responseData, responseData.length);
        clientSocket.receive(response);
        return packet.parseUDPdata(responseData);

    }

    // Send EOT packet
    public void endTransmission(){
        try {
            packet eot = packet.createEOT(nextSeqNum);
            byte[] eotData = new byte[1024];
            eotData = eot.getUDPdata();
            DatagramPacket eotDatagram = new DatagramPacket(eotData, eotData.length, emulatorIpAddress, udpPortEmulator);
            clientSocket.send(eotDatagram);
            nonackedPackets.add(eot);
        } catch (Exception ex){

        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Not enough arguments");
            System.exit(-1);
        }

        Sender udpSender = new Sender();
        udpSender.initiate(args[0], args[1], args[2], args[3]);
        udpSender.convertFileToPacketList();

        SendPackets sendThread = new SendPackets(udpSender);
        ReceivePackets receiverThread = new ReceivePackets(udpSender);

        udpSender.startingTime = new Date();
        sendThread.start();
        receiverThread.start();

        sendThread.join();
        receiverThread.join();
    }


}

// This thread class is responsible for sending packets
class SendPackets extends Thread{
    Sender sender;
    public SendPackets(Sender sender){
        this.sender = sender;

    }

    public void run(){
        try {
            while(true){
                sender.semaphore.acquire();

                //if we've already sent everything then break
                if (sender.eotSent && sender.nonackedPackets.size() == 0){
                    sender.semaphore.release();
                    break;
                }

                Date currentTime = new Date();

                //handle timeout
                if (currentTime.getTime() - sender.startingTime.getTime() > 10000 && sender.nonackedPackets.size() != 0){
                    sender.resendPacketsInWindow();
                    sender.startingTime = currentTime;
                }

                //send a packet
                if (!sender.isWindowFull() && !sender.eotSent){

                    //if there is nothing else to send, wait for nonackedPackets
                    // to be empty to we can break out then send eot
                    if (sender.number == sender.packetList.size()){
                        sender.eotSent = true;
                    } else {
                        //get an unsent packet and send it
                        sender.nonackedPackets.add(sender.packetList.get(sender.number));
                        sender.sendPacket(sender.packetList.get(sender.number));
                        ++sender.number;
                    }
                    // reset the timer
                    if (sender.base == sender.nextSeqNum){
                        sender.startingTime = currentTime;
                    }
                    sender.nextSeqNum = (sender.nextSeqNum + 1) % 32;
                }
                sender.semaphore.release();
            }
            //now send eot and keep sending it until we've received an eot from
            // the receiver. endTransmission() will add the eot packet we're
            // sending to the window
            sender.endTransmission();
            sender.startingTime = new Date();
            while(true){
                sender.semaphore.acquire();
                if (sender.nonackedPackets.size() == 0){
                    sender.semaphore.release();
                    break;
                }
                Date currentTime = new Date();

                //resend the eot
                if (currentTime.getTime() - sender.startingTime.getTime() > 10000){
                    sender.resendPacketsInWindow();
                    sender.startingTime = currentTime;
                }
                sender.semaphore.release();
            }


        } catch (Exception ex){
            ex.printStackTrace();
            sender.semaphore.release();
        }

    }
}

//this thread class is responsible for receiving acks and eot
class ReceivePackets extends Thread{
    Sender sender;
    public ReceivePackets(Sender sender){
        this.sender = sender;

    }
    public void run(){
        try {
            while(true){
                sender.semaphore.acquire();

                //get an ack or eot
                packet senderResponse;
                try{
                    senderResponse = sender.getResponse();
                } catch (Exception e){
                    sender.semaphore.release();
                    continue;
                }
                int ackdSequence = senderResponse.getSeqNum();
                if(senderResponse.getType() != 2){ // if not EOT
                    sender.ackLog.write(String.valueOf(ackdSequence));
                    sender.ackLog.newLine();
                } else {
                    sender.nonackedPackets.clear();
                    sender.semaphore.release();
                    break;
                }
                sender.base = (ackdSequence + 1) % 32;
                //drop the ack if its out of the range of the window
                boolean valid = false;
                for (packet p: sender.nonackedPackets){
                    if (p.getSeqNum() == ackdSequence){
                        valid = true;
                        break;
                    }
                }
                if (!valid){
                    sender.semaphore.release();
                    continue;
                }
                // System.out.println("R:" + ackdSequence + " ");

                // the ack's seqNum is cumulative so ack cumulatively
                Iterator<packet> it = sender.nonackedPackets.iterator();
                while(it.hasNext()){
                    packet p = it.next();
                    if (p.getSeqNum() != sender.base){
                        it.remove();
                    } else {
                        break;
                    }
                }
                //reset timer
                if (sender.base != sender.nextSeqNum){
                    sender.startingTime = new Date();
                } else if (senderResponse.getType() == 2) {
                    sender.nonackedPackets.clear();
                    sender.semaphore.release();
                    break;
                }
                sender.semaphore.release();
            }
            sender.clientSocket.close();
            sender.ackLog.close();
            sender.seqNumLog.close();
        } catch (Exception e){
            e.printStackTrace();
            sender.semaphore.release();
        }

    }
}
