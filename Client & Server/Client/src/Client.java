import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    private Socket socket = null;
    private String FILE_INDICATOR = "file";
    private String FILE_CONTENT_INDICATOR = "-";


    public Client(String ip, int port) {
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        //Thread reading from Console and sending to server
        Runnable listeningThread = createListeningThread();

        //Thread reading message from Server and writing to console
        Runnable writingThread = createWritingThread();

        //Start threads
        new Thread(listeningThread).start();
        new Thread(writingThread).start();


    }

    /**
     * Creates a thread that is used to send messages to the server
     *
     * @return thread
     */ 
    private Runnable createWritingThread() {
        return new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        //Listen for incoming messages
                        InputStreamReader reader = new InputStreamReader(socket.getInputStream());
                        BufferedReader br = new BufferedReader(reader);
                        String message = br.readLine();
                        //Check if message is text or file
                        if (isFile(message)) {
                            //When done writing file
                            print(message.substring(0, message.indexOf(FILE_CONTENT_INDICATOR)));

                            //extract teh file part from the message
                            String file = getFileFromMessage(message);
                            String fileName = getFileNameFromMessage(message);

                            //The file that you wish to write
                            System.out.println(Paths.get(fileName));
                            Path path = Paths.get(fileName);

                            //Convert string to bytearray to File
                            FileUtils.writeByteArrayToFile(path.toFile(), Base64.decodeBase64(file));


                        } else {

                            //If just a normal message
                            System.out.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };
    }

    /**
     * Creates a thread to listen for inoming messages from server
     *
     * @return
     */
    private Runnable createListeningThread() {
        return new Runnable() {

            @Override
            public void run() {
                while (true) {

                    try {
                        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                        //get message from console
                        String line = userInput.readLine();

                        //Check if the message wants to send a file
                        if (isFile(line)) {
                            //Get file name
                            String fileName = getFileNameFromMessage(line);
                            //generate message
                            String message = getFile(fileName);
                            //send file
                            if(fileName.contains("\\")){         
                                String s = line.substring(0,line.indexOf("\""));
                                String[] ss = line.split("\\\\");
                                String name = ss[ss.length-1];
                                if(name.contains("\"")){
                                	name = name.substring(0,name.indexOf("\"")+1);
                                }
                                writer.write(s.trim() + "\"" + name + FILE_CONTENT_INDICATOR + message + "\n");

                            }else{
                            	writer.write(line + FILE_CONTENT_INDICATOR + message + "\n");
                            }
                            
                        }
                        //if message is not sending file
                        else {
                            writer.write(line + "\n");
                        }
                        //send message
                        print("Message Sent");
                        writer.flush();

                    } catch (Exception e) {
                    	e.printStackTrace();
                        System.out.println(e.getMessage());
                    }
                }
            }


        };
    }

    /**
     * Extracts the file name from the message
     *
     * @param line
     * @return
     */
    private String getFileNameFromMessage(String line) {
        int a = line.indexOf('\"');
        int b = line.indexOf('\"', a + 1);
        return line.substring(a + 1, b);
    }

    /**
     * Takes as input a message recieved from the server
     * Removes any information that is unnecessary
     * And extracts actual file from the message
     *
     * @param message message from server
     * @return the String coresponding to the file in messsage
     */
    private String getFileFromMessage(String message) {
        int index = message.indexOf(FILE_CONTENT_INDICATOR);
        return message.substring(index + 1, message.length());
    }

    /**
     * If message contains the file flag
     *
     * @param message
     * @return
     */
    private boolean isFile(String message) {
        if (message.contains(FILE_INDICATOR) || message.contains(FILE_CONTENT_INDICATOR))
            return true;
        return false;
    }


    /**
     * Print method for the lazy programmer
     *
     * @param o
     */
    public static void print(Object o) {
        System.out.println(o.toString());
    }

    /**
     * Gets the file from the local directory and converts into a String representation
     * The file is a base64 encoded String
     *
     * @param path path to file
     * @return String represetnation of file
     * @throws IOException
     */
    public static String getFile(String path) {

        try {
        	String p = Paths.get("").toAbsolutePath().toString()+Paths.get("\\"+path);
            byte[] encoded = java.nio.file.Files.readAllBytes(Paths.get(p));
            return Base64.encodeBase64String(encoded);
        } catch (IOException e) {
            System.err.println("ERROR");
            System.exit(1);
        }
        return null;
    }


    public static void main(String args[]) {
        //Start Client
        new Client("0.0.0.0", 9800);


    }
}
