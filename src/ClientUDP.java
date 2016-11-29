import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;


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

            int endOfFile = 0;
            //FileOutputStream fileIn = new FileOutputStream(fileName);
            RandomAccessFile fileIn = new RandomAccessFile(fileName,"rw");
            while (endOfFile != -1) {
                byte[] receiveData = new byte[1024];
                byte[] endIndicatorBuf = new byte[4];
                byte[] recievedByteIndex = new byte[4];
                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                clientSocket.receive(receivePacket);

                System.arraycopy(receiveData,0,endIndicatorBuf,0,4);
                System.arraycopy(receiveData,4,recievedByteIndex,0,4);

                endOfFile = ByteBuffer.wrap(endIndicatorBuf).getInt();
                int recievedIndex = ByteBuffer.wrap(recievedByteIndex).getInt();

                fileIn.seek(recievedIndex);
                fileIn.write(receiveData,8,1016);
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