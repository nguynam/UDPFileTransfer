import java.io.*;
import java.net.*;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class ServerUDP {
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
            String requestedFile=  new String(receiveData);
            System.out.println("Got message: " + requestedFile);

            if (fileMap.containsKey(requestedFile)) {
                File fileToSend = fileMap.get(requestedFile);
                // Determine specified file if it exists
                int fileSize = (int) fileToSend.length();
                byte[] byteArray = ByteBuffer.allocate(fileSize + 4).putInt(0, fileSize).array();
                // Create byteArray and allocate enough size for file + 4
                // bytes to send fileSize.
                BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(fileToSend));
                fileIn.read(byteArray, 4, fileSize);
                DatagramPacket sendPacket = new DatagramPacket(byteArray,byteArray.length);
                serverSocket.send(sendPacket);
                fileIn.close();
                // Close file InputStream.
            }
            else {
                byte[] byteArray = ByteBuffer.allocate(4).putInt(-1).array();
                // Signal an error occurred (file not found).
                // Send byteArray (with error).
                System.out.println("File not found.");
            }

        }
    }

}
