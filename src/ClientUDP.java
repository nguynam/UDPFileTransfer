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

            byte[] receiveData = new byte[50000];
            byte[] sizeBuffer = new byte[4];
            System.arraycopy(sizeBuffer, 0, receiveData, 0, 4);
            int sizeCheck = ByteBuffer.wrap(sizeBuffer).getInt();

            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
            clientSocket.receive(receivePacket);

            if (sizeCheck != -1) {
                byte[] buffer = new byte[1024];
                FileOutputStream fileIn = new FileOutputStream(fileName);
                int totalCount = 0;
                while (totalCount < sizeCheck - 1) {
                    //Not sure what to do here
                    int localCount = clientSocket.getReceiveBufferSize();
                    totalCount = totalCount + localCount;
                    fileIn.write(buffer, 0, localCount);
                }
                // fill buffer with 1024 bytes and save to
                // file until total bytes read equals file size.

                fileIn.close();
            } else {
                System.out.println("File not found");

            }


            String messageBack = new String(receiveData);
            String exit = "Exit";
            if(fileName.equals(exit)){
                on = false;
                break;
            }
            System.out.println("Server sent back message: " + messageBack);
        }
    }
}
