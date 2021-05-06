package com.example.p2pmessagingapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    EditText receivePortEditText, targetPortEditText, messageEditText, targetIPEditText;
    TextView chatText1, chatText2;
    Button startServerButton,connectButton,sendButton;
    LinearLayout linearLayout,linearLayoutPort,linearLayoutTargetIp,linearLayoutTargetPort;
    LinearLayout linearLayoutScrollView;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;
    String savedMsg = "";

    String textFromFile= "";
    static final int MESSAGE_READ = 1;
    static final String TAG = "MyTag";

    int mDefaultColor;
    private static String FILE_NAME = "SavedText";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //Message handling
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    //Background color handling
                    if (tempMsg.charAt(0) == '#') {
                        tempMsg = tempMsg.substring(1);
                        linearLayout.setBackgroundColor(Integer.parseInt(tempMsg));
                    }
                    //File handling
                    else if (tempMsg.charAt(0) == '@') {
                        tempMsg = tempMsg.replace("@", "");
                        String fileText = tempMsg;
                        writeToFile("file", fileText, false);

                        tempMsg = "Friend : Sent a file named file " ;
                        chatText1.setText(tempMsg);
                    }
                    //Text message handling
                    else {
                        tempMsg = tempMsg.substring(1);
                        tempMsg = "Friend : " + tempMsg;
                       // FriendMsg+=tempMsg;
                        chatText1.setText(tempMsg);
                        savedMsg += tempMsg+"\n";
                    }
            }

            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        receivePortEditText = findViewById(R.id.serverPortEditText);
        targetPortEditText = findViewById(R.id.targetPortEditText);
        messageEditText = findViewById(R.id.editTextId);
        targetIPEditText = findViewById(R.id.targetIPEditText);
        chatText1 = findViewById(R.id.chatText1Id);
        chatText2 = findViewById(R.id.chatText2Id);

        mDefaultColor = ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);
        linearLayout = findViewById(R.id.linearLayoutId);
        linearLayoutPort =findViewById(R.id.linearLayoutPort);
        linearLayoutTargetIp =findViewById(R.id.linearLayoutTargetIP);
        linearLayoutTargetPort =findViewById(R.id.linearLayoutTargetPort);
        linearLayoutScrollView =findViewById(R.id.linearLayoutScrollView);

        startServerButton = findViewById(R.id.hostButton);
        connectButton = findViewById(R.id.connectButton);
        sendButton = findViewById(R.id.sendButton);

        verifyDataFolder();
        verifyStoragePermissions();
    }

   //"START SERVER" button  activity
    public void onStartClicked(View v) {
        String port = receivePortEditText.getText().toString();
        //Create and start server
        serverClass = new ServerClass(Integer.parseInt(port));
        serverClass.start();
        Toast.makeText(this, "The server is started.", Toast.LENGTH_SHORT).show();
    }

    //"CONNECT" button  activity
    public void onConnectClicked(View v) {
        String port = targetPortEditText.getText().toString();
        String ip = targetIPEditText.getText().toString();
        clientClass = new ClientClass(targetIPEditText.getText().toString(), Integer.parseInt(port));
        clientClass.start();
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        setElement();
    }

    //"SEND" button  activity
    public void onSendClicked(View v) {

       String msg = messageEditText.getText().toString();
        chatText2.setText("Me : "+msg);
        savedMsg += "Me : " + msg ;
        msg = "$" + msg;
        sendReceive.write(msg.getBytes());

        messageEditText.setText("");
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;
        int port;

        public ServerClass(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                Log.d(TAG, "Waiting for client...");
                socket = serverSocket.accept();
                Log.d(TAG, "Connection established from server");
                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "ERROR/n" + e);
            }
        }
    }

    public class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(final byte[] bytes) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

    }

    public class ClientClass extends Thread {
        Socket socket;
        String hostAdd;
        int port;

        public ClientClass(String hostAddress, int port) {
            this.port = port;
            this.hostAdd = hostAddress;
        }

        @Override
        public void run() {
            try {

                socket = new Socket(hostAdd, port);
                Log.d(TAG, "Client is connected to server");

                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Can't connect from client/n" + e);
            }
        }

    }
    //Create menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_layout, menu);
        return super.onCreateOptionsMenu(menu);
    }
    //Menu item activity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.fileMenu:
                generateFileManagerWindow();
                return true;
            case R.id.backgroundMenu:
                openColorPicker();
                return true;
            case R.id.saveMenu:
                writeToFile(FILE_NAME, savedMsg,true);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
//open color picker window to select color
    public void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, mDefaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mDefaultColor = color;
                //Change background color
                linearLayout.setBackgroundColor(mDefaultColor);
                String msg1 = "#" + Integer.toString(mDefaultColor);
                //send the color
                sendReceive.write(msg1.getBytes());
            }
        });
        colorPicker.show();
    }
    //Write text to the .txt file
    private void writeToFile(String fileName, String data, boolean timeStamp) {
        Long time= System.currentTimeMillis();
        String timeMill = " "+time.toString();
        String path = Environment.getExternalStorageDirectory().toString();
        File file = null;
        if(timeStamp)
            file = new File(path+"/P2P/SavedConversations", fileName+timeMill+".txt");
        else
            file = new File(path+"/P2P/Received txt files", fileName+timeMill+".txt");

        FileOutputStream stream;

        try {
            stream = new FileOutputStream(file, false);
            stream.write(data.getBytes());
            Toast.makeText(this, "file saved in: "+file.getPath(), Toast.LENGTH_SHORT).show();
            stream.close();


        } catch (FileNotFoundException e) {
            Log.d(TAG, e.toString());
            Toast.makeText(this, "file not found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
    }

//to select text file
    private void generateFileManagerWindow() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 7);
    }
    //result activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub

        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK) {

            Uri uri = data.getData();
            String fileText = getTextFromUri(uri);
            textFromFile = "@"+fileText;

            new Thread(new Runnable() {
                @Override
                public void run() {


                    //String msg = "124@@@"+fileText;

                    Log.d(TAG, textFromFile);

                    sendReceive.write(textFromFile.getBytes());
                }
            }).start();

        }


    }
    //derive text from file
    public String getTextFromUri(Uri uri){
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = "";

            while ((line = reader.readLine()) != null) {
                builder.append("\n"+line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return builder.toString();
    }


    public void setElement(){
        linearLayout.setWeightSum(7);
        linearLayoutPort.setVisibility(View.GONE);
        startServerButton.setVisibility(View.GONE);
        linearLayoutTargetIp.setVisibility(View.GONE);
        linearLayoutTargetPort.setVisibility(View.GONE);
        connectButton.setVisibility(View.GONE);

        linearLayoutScrollView.setVisibility(View.VISIBLE);
        messageEditText.setVisibility(View.VISIBLE);

        sendButton.setVisibility(View.VISIBLE);


    }
    //verify storage permission

    public void verifyStoragePermissions() {
        // Check if we have write permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
    }
    //verify and create folder
    private void verifyDataFolder() {
        File folder = new File(Environment.getExternalStorageDirectory() + "/P2P");
        File folder1 = new File(folder.getPath() + "/SavedConversations");
        File folder2 = new File(folder.getPath() + "/Received txt files");
        if(!folder.exists() || !folder.isDirectory()) {
            folder.mkdir();
            folder1.mkdir();
            folder2.mkdir();
        }
        else if(!folder1.exists())
            folder1.mkdir();
        else if(!folder2.exists())
            folder2.mkdir();
    }
}
