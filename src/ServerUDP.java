import java.io.*;
import java.net.*;
import java.util.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/*
TO-DO:
Acknowledgments
Checksum
Exact file transfer - send packet data size instead of "last packet" indicator so client can avoid reading empty bytes.
 */

public class ServerUDP {
    public static SortedMap<Integer, Packet> slidingWindow = new ConcurrentSkipListMap<>();
    public static int lastInWindow = 0;

    private static Map<String, File> scanFolder(String dir) {
        Map<String, File> folderMap = new HashMap<String, File>();
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                folderMap.put(file.getName(), file);
            }
        }
        return folderMap;
    }

    public static void main(String args[]) throws Exception{
        DatagramSocket serverSocket = new DatagramSocket(9876);
        final String DIRECTORY = "src";
        Map<String, File> fileMap = scanFolder(DIRECTORY);

        while(true){
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);
            //Remove timeout while waiting for new file to be requested (waiting for user)
            serverSocket.setSoTimeout(0);
            serverSocket.receive(receivePacket);
            String requestedFile = new String(receiveData).trim();
            System.out.println("Client requested file: " + requestedFile);
            RandomAccessFile fileToSend = new RandomAccessFile(fileMap.get(requestedFile),"r");
            slidingWindow.clear();
            lastInWindow = 0;
            if (fileToSend != null) {
                // Determine specified file if it exists
                loadWindow(fileToSend);
                while(slidingWindow.isEmpty() == false){
                    //Send whole window (unless packet is acked).
                    for(Map.Entry<Integer,Packet> entry : slidingWindow.entrySet()){
                        Packet tempPacket;
                        tempPacket = slidingWindow.get(entry.getKey());
                        //Send packet if it is not acked
                        if(tempPacket.isAcknowledged() == false){
                            DatagramPacket sendPacket = new DatagramPacket(tempPacket.getBytes(),tempPacket.getBytes().length,receivePacket.getAddress(),receivePacket.getPort());
                            serverSocket.send(sendPacket);
                        }
                    }
                    slideWindow(serverSocket,fileToSend);
                }
                fileToSend.close();
                // Close file InputStream.
            }
            else {
                byte[] byteArray = ByteBuffer.allocate(4).putInt(-1).array();
                DatagramPacket sendPacket = new DatagramPacket(byteArray, byteArray.length,receivePacket.getAddress(),receivePacket.getPort());
                serverSocket.send(sendPacket);
                // Signal an error occurred (file not found).
                // Send byteArray (with error).
                System.out.println("File not found.");
            }

        }
    }
    private static void slideWindow(DatagramSocket receiveSocket, RandomAccessFile sendingFile) throws IOException {
        byte[] recieveData = new byte[1024];
        DatagramPacket recievePacket = new DatagramPacket(recieveData,recieveData.length);
        receiveSocket.setSoTimeout(50);
        while(true) {
            try {
                receiveSocket.receive(recievePacket);
                int ackedPacket = ByteBuffer.wrap(recieveData).getInt();
                slidingWindow.get(ackedPacket).setAcknowledged(true);

            } catch (SocketTimeoutException t) {
                //Socket timed out - assuming no further data for now
                //Remove acked packets that are not surrounded by unacked packets.
                for(Map.Entry<Integer, Packet> entry : slidingWindow.entrySet()){
                    if(entry.getValue().isAcknowledged()){
                        slidingWindow.remove(entry.getKey());
                    }else{
                        //Current packet is not acked so window cannot be slid further
                        break;
                    }
                }
                loadWindow(sendingFile);
                return;
            }
        }
    }
    private static void loadWindow(RandomAccessFile sendingFile) throws IOException {
        int fileSize = (int)sendingFile.length();
        byte[] currentBytes;
        Packet packet;
        while(lastInWindow < fileSize && slidingWindow.size() < 5) {
            packet = new Packet();
            currentBytes = new byte[1024];
            //Maybe 1015? for length
            int readBytes = sendingFile.read(currentBytes,13, 1011);
            if(readBytes == 1011){
                //Read full packet worth of data
                System.arraycopy(ByteBuffer.allocate(13).put(8,(byte)0).putInt(9,lastInWindow).array(),0,currentBytes,0,13);
                System.arraycopy(ByteBuffer.allocate(13).put(8,(byte)0).putInt(9,lastInWindow).array(),0,currentBytes,0,13);
                Checksum checksum = new CRC32();
                checksum.update(currentBytes,8,1016);
                long checkValue = checksum.getValue();
                ByteBuffer.wrap(currentBytes).putLong(0,checkValue);
            }
            else{
                System.arraycopy(ByteBuffer.allocate(13).put(8,(byte)-1).putInt(9, lastInWindow).array(),0,currentBytes,0,13);
                Checksum checksum = new CRC32();
                checksum.update(currentBytes,8,1016);
                long checkValue = checksum.getValue();
                ByteBuffer.wrap(currentBytes).putLong(0,checkValue);
            }
            packet.setBytes(currentBytes);
            slidingWindow.put(lastInWindow,packet);
            lastInWindow = lastInWindow + 1011;
            sendingFile.seek(lastInWindow);
        }
    }
}