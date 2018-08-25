import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver {
    public String hostAddressEmulator;
    public int udpPortEmulator;
    public int udpPortReceiver;
    public String fileName;

    public DatagramSocket receiverSocket;
    public InetAddress emulatorIpAddress;

    public BufferedWriter file;
    public BufferedWriter logFile;

    public void initiate(String a, String b, String c, String d) throws Exception{
        hostAddressEmulator = a;
        udpPortEmulator = Integer.parseInt(b);
        udpPortReceiver = Integer.parseInt(c);
        fileName = d;

        receiverSocket = new DatagramSocket(udpPortReceiver);
        emulatorIpAddress = InetAddress.getByName(hostAddressEmulator);
        file = new BufferedWriter(new FileWriter(fileName));
        logFile = new BufferedWriter(new FileWriter("arrival.log"));
    }

    // get a packet from the sender
    public packet getPacket() throws Exception{
        byte[] packetData = new byte[1024];
        DatagramPacket response =
                new DatagramPacket(packetData, packetData.length);
        receiverSocket.receive(response);
        return packet.parseUDPdata(packetData);
    }

    // send and ack or an eot
    public void sendPacket(int type, int seqNum) throws Exception {
        byte [] data;

        if (type == 2){
            packet eotResponse = packet.createEOT(seqNum);
            data = eotResponse.getUDPdata();

        } else {
            packet ackPacket = packet.createACK(seqNum);
            data = ackPacket.getUDPdata();
        }

        DatagramPacket packetToSend =
                new DatagramPacket(data,
                        data.length,
                        emulatorIpAddress,
                        udpPortEmulator);
        receiverSocket.send(packetToSend);
    }

    public static void main(String[] args) throws Exception{
        if (args.length != 4) {
            System.out.println("Not enough arguments");
            System.exit(-1);
        }
        Receiver rec = new Receiver();
        rec.initiate(args[0], args[1], args[2], args[3]);

        int expectedSeqNum = 0;

        while(true){
            packet tempPacket;
            // Keep trying to get acks
            try{
                tempPacket = rec.getPacket();
            } catch (Exception e){
                continue;
            }
            // send back an eot if we get an eot
            if (tempPacket.getType() == 2){
                //System.out.println("got eot ");
                rec.sendPacket(2, expectedSeqNum);
                break;
            }

            //record into log file
            int seqNum = tempPacket.getSeqNum();
            rec.logFile.write(String.valueOf(seqNum));
            rec.logFile.newLine();

            if (seqNum == expectedSeqNum){
                //System.out.print("d ");
                //System.out.print("R: " + seqNum + " ");
                String packetData = new String(tempPacket.getData());
                rec.file.write(packetData);
                rec.sendPacket(0,expectedSeqNum);
                expectedSeqNum += 1;
                expectedSeqNum %= 32;

            } else { //drop older packets and resend same ack
                //System.out.print("e ");
                int lastSeqNum = expectedSeqNum - 1;
                lastSeqNum %= 32;

                if (lastSeqNum < 0){
                    lastSeqNum += 32;
                }
                rec.sendPacket(0,lastSeqNum);
            }
            //System.out.println();

        }
        rec.receiverSocket.close();
        rec.file.close();
        rec.logFile.close();


    }
}
