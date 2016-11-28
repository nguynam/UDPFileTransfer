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
            FileOutputStream fileIn = new FileOutputStream(fileName);

            while (endOfFile != -1) {
                byte[] receiveData = new byte[1024];
                byte[] sizeBuffer = new byte[4];
                System.arraycopy(sizeBuffer, 0, receiveData, 0, 4);
                endOfFile = ByteBuffer.wrap(sizeBuffer).getInt();

                DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
                clientSocket.receive(receivePacket);

                fileIn.write(receiveData, 4, receiveData.length);
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
