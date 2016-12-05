import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class ClientUDP {
    public static void main(String args[]) throws Exception{
        DatagramSocket clientSocket = new DatagramSocket();
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter address: ");
        String address = inFromUser.readLine();
        System.out.println("Enter port number: ");
        int port = Integer.parseInt(inFromUser.readLine());
        boolean on = true;

        while(on){
            System.out.println("Enter Filename: ");
            String fileName = inFromUser.readLine();
            byte[] sendData = fileName.getBytes();
            InetAddress IPAddress = InetAddress.getByName(address);
            DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,IPAddress,port);
            clientSocket.send(sendPacket);

            byte endOfFile = 0;
            //FileOutputStream fileIn = new FileOutputStream(fileName);
            RandomAccessFile fileIn = new RandomAccessFile(fileName,"rw");
            while (endOfFile != -1) {
                byte[] receiveData = new byte[1024];
                byte[] endIndicatorBuf = new byte[1];
                byte[] recievedByteIndex = new byte[4];
                byte[] checksumBytes = new byte[8];
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                clientSocket.receive(receivePacket);
                Checksum checksum = new CRC32();
                checksum.update(receiveData,8,1016);
                long calculatedCheck = checksum.getValue();
                System.arraycopy(receiveData,0,checksumBytes,0,8);
                System.arraycopy(receiveData,8,endIndicatorBuf,0,1);
                System.arraycopy(receiveData, 9, recievedByteIndex, 0, 4);
                long recievedCheck = ByteBuffer.wrap(checksumBytes).getLong();
                if(recievedCheck != calculatedCheck) continue; //Checksum not correct stop handling packet

                endOfFile = ByteBuffer.wrap(endIndicatorBuf).get();
                int recievedIndex = ByteBuffer.wrap(recievedByteIndex).getInt();

                fileIn.seek(recievedIndex);
                fileIn.write(receiveData,13,1011);

                //Send Ack
                byte[] ackData = ByteBuffer.allocate(1024).putInt(recievedIndex).array();
                DatagramPacket ackPacket = new DatagramPacket(ackData,ackData.length,IPAddress,port);
                clientSocket.send(ackPacket);
            }
            fileIn.close();

            String exit = "Exit";
            if(fileName.equals(exit)){
                on = false;
                break;
            }
        }
    }
}