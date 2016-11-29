import java.io.*;
import java.net.*;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentSkipListMap;

public class ServerUDP {
    public static SortedMap<Integer, byte[]> slidingWindow = new ConcurrentSkipListMap<Integer, byte[]>();
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
            serverSocket.receive(receivePacket);
            String requestedFile=  new String(receiveData).trim();
            System.out.println("Got message: " + requestedFile);
            RandomAccessFile fileToSend = new RandomAccessFile(fileMap.get(requestedFile),"r");
            if (fileToSend != null) {
                // Determine specified file if it exists
                int fileSize = (int) fileToSend.length();
                loadWindow(fileToSend);
                //Iterator<Map.Entry<Integer,byte[]>> iter = slidingWindow.entrySet().iterator();
                for(Map.Entry<Integer,byte[]> entry : slidingWindow.entrySet()){
                    //Map.Entry<Integer, byte[]> entry = iter.next();
                    byte[] tempBytes = new byte[1024];
                    tempBytes = slidingWindow.get(entry.getKey());
                    DatagramPacket sendPacket = new DatagramPacket(tempBytes,tempBytes.length,receivePacket.getAddress(),receivePacket.getPort());
                    serverSocket.send(sendPacket);
                    slidingWindow.remove(entry.getKey());
                    //slidingWindow.remove(entry.getKey());
                    loadWindow(fileToSend);
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
    private static void loadWindow(RandomAccessFile sendingFile) throws IOException {
        int fileSize = (int)sendingFile.length();
        //return -1 if last packet worth of data reached
        byte[] currentBytes;
        while(lastInWindow < fileSize && slidingWindow.size() < 5) {
            currentBytes = new byte[1024];
            //Maybe 1015? for length
            int readBytes = sendingFile.read(currentBytes,8, 1016);
            if(readBytes == 1016){
                //Read full packet worth of data
                System.arraycopy(ByteBuffer.allocate(8).putInt(0).putInt(lastInWindow).array(),0,currentBytes,0,8);
            }
            else{
                System.arraycopy(ByteBuffer.allocate(8).putInt(-1).putInt(lastInWindow).array(),0,currentBytes,0,8);
            }
            slidingWindow.put(lastInWindow,currentBytes);
            lastInWindow = lastInWindow + 1016;
            sendingFile.seek(lastInWindow);
        }
    }
    private byte[] loadBuffer(int offset,int fileSize, RandomAccessFile sendingFile) throws IOException {
        //Fill first 4 bytes with offest (index of current file)
        byte[] toSend = ByteBuffer.allocate(1024).putInt(0,offset).putInt(4,fileSize).array();
        //Offset file's current position
        sendingFile.seek(offset);
        //Load 1016 bytes of file into byte array (leaving first 8 bytes as headers)
        sendingFile.read(toSend,8,1016);
        return toSend;
    }

}
