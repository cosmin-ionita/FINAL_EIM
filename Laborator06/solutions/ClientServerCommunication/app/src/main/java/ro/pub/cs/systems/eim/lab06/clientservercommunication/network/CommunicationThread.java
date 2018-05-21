package ro.pub.cs.systems.eim.lab06.clientservercommunication.network;

import android.util.Log;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import ro.pub.cs.systems.eim.lab06.clientservercommunication.general.Constants;
import ro.pub.cs.systems.eim.lab06.clientservercommunication.general.Utilities;

public class CommunicationThread extends Thread {

    private Socket socket;
    private EditText serverTextEditText;

    private static final HashMap<String, String> alarms = new HashMap<>();
    private static final HashMap<String, String> activeAlarms = new HashMap<>();

    public CommunicationThread(Socket socket, EditText serverTextEditText) {
        this.socket = socket;
        this.serverTextEditText = serverTextEditText;
    }

    @Override
    public void run() {
        try {
            Log.v(Constants.TAG, "Connection opened with " + socket.getInetAddress() + ":" + socket.getLocalPort());

            BufferedReader bufferedReader = Utilities.getReader(socket);

            String command = bufferedReader.readLine();

            String[] commands;

            String result = "";

            if(command.contains(",")) {
                commands = command.split(",");

                if(commands[0].equals("set")) {

                    CommunicationThread.alarms.put(socket.getInetAddress().toString(), commands[1] + "," + commands[2]);

                    result = "the alarm was set!";
                }
            }
            else if(command.equals("reset")) {
                CommunicationThread.alarms.put(socket.getInetAddress().toString(), "");

                result = "the alarm was RESET!";

                activeAlarms.clear();

            } else if(command.equals("poll")) {

                String key = this.socket.getInetAddress().toString();

                if(!activeAlarms.containsKey(key)) {

                    Socket socket = new Socket("utcnist.colorado.edu", 13);
                    BufferedReader reader = Utilities.getReader(socket);
                    reader.readLine();

                    String dayTimeProtocol = reader.readLine();

                    socket.close();

                    int receivedHour = Integer.parseInt(dayTimeProtocol.split(" ")[2].split(":")[0]);
                    int receivedMinute = Integer.parseInt(dayTimeProtocol.split(" ")[2].split(":")[1]);

                    key = this.socket.getInetAddress().toString();

                    if(!CommunicationThread.alarms.containsKey(key)) {
                        result = "none";
                    }
                    else {
                        String storedAlarm = alarms.get(key);

                        int h = Integer.parseInt(storedAlarm.split(",")[0]);
                        int m = Integer.parseInt(storedAlarm.split(",")[1]);

                        result = "inactive";

                        if(receivedHour > h) {
                            result = "active";
                            activeAlarms.put(key, "");
                        } else if(receivedHour == h) {
                            if(receivedMinute > m) {
                                result = "active";
                                activeAlarms.put(key, "");
                            }
                        }
                    }
                    }
                    else {
                        result = "active";
                        }
                }


            // take action, send response
            PrintWriter printWriter = Utilities.getWriter(socket);
            printWriter.println(result);
            socket.close();
            Log.v(Constants.TAG, "Conenction closed");
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        }
    }

}
