package com.example.tjuhi.testclip;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    boolean clicked;
    int cnt=0;
    private MediaRecorder myRecorder;  //used for recording
    private String outputFile = null;
    String nameOfFolder="test";
    String nameOfFile;
    ProgressDialog dialog;
    CountDownTimer t;
     EditText url;
    private AlarmReceiver receiver;
int pd;
    TextView interval;
    int inter;
    TextView period;
    String SERVER_URL;
    String site;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button startBtn,stopBtn;
        startBtn=(Button)findViewById(R.id.startbtn);
        stopBtn=(Button)findViewById(R.id.stopbtn);
        TextView id=(TextView)findViewById(R.id.id_text);
  url=(EditText) findViewById(R.id.url_text);

        interval=(TextView)findViewById(R.id.interval_text);

        period=(TextView)findViewById(R.id.time_text);


        nameOfFile=id.getText().toString();


        startBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                site=url.getText().toString();
                String interv=interval.getText().toString();
                inter=Integer.parseInt(interv);
                pd=Integer.parseInt(period.getText().toString());
                Toast.makeText(getApplicationContext(), site, Toast.LENGTH_SHORT).show();
                 t = new CountDownTimer( Long.MAX_VALUE , inter) {


                    public void onTick(long millisUntilFinished) {
                        Log.d("test","Timer tick");
                        record();
                    }

                    public void onFinish() {
                        Log.d("test","Timer last tick");

                    }
                }.start();

            }









        });


        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                t.cancel();
            }
        });


    }

    public void record(){
        String filePath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+nameOfFolder;
        String currentDateAndTime=getCurrentDateAndTime();
        File dir=new File(filePath);
        if(!dir.exists()){
            dir.mkdirs();
        }

        outputFile = filePath+"/"+nameOfFile+"_"+currentDateAndTime+".mp4";

        myRecorder = new MediaRecorder();
        myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);  // setting the audio source as MIC
        myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);  //set audio format 3gpp
        myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); //setting audio encoder
        myRecorder.setOutputFile(outputFile);   // sets the output path.

        // record part  -- START'S THE RECORDING
        try {
            myRecorder.prepare();
            myRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // prepare() fails
            e.printStackTrace();
        }


        //STOPS the record after 10 sec.
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //STOP
                try {
                    myRecorder.stop();
                    myRecorder.release();
                   Toast.makeText(getApplicationContext(), "Saved"+outputFile, Toast.LENGTH_SHORT).show();
                    upload();
                    myRecorder = null;
                   /* AlarmManager alarmManager=(AlarmManager) MainActivity.this.getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),30000,
                            pendingIntent);*/

                } catch (IllegalStateException e) {
                    // it is called before start()
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    // no valid audio/video data has been received
                    e.printStackTrace();
                }
            }
        }, pd);
    }
    public String getCurrentDateAndTime(){
        Calendar c=Calendar.getInstance();
        SimpleDateFormat df= new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String formattedDate=df.format(c.getTime());
        return formattedDate;
    }
  private void upload(){
      if (outputFile != null) {
          dialog = ProgressDialog.show(MainActivity.this, "", "Uploading File...", true);

          new Thread(new Runnable() {
              @Override
              public void run() {

                  try {
                      //creating new thread to handle Http Operations
                      uploadFile(outputFile);
                  } catch (OutOfMemoryError e) {

                      runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              Toast.makeText(MainActivity.this, "Insufficient Memory!", Toast.LENGTH_SHORT).show();
                          }
                      });
                      dialog.dismiss();
                  }

              }
          }).start();
      } else {
          Toast.makeText(MainActivity.this, "Please choose a File First", Toast.LENGTH_SHORT).show();
      }

  }

    public int uploadFile(String outputFile) {

        int serverResponseCode = 0;

        HttpURLConnection connection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";


        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File selectedFile = new File(outputFile);


        String[] parts = outputFile.split("/");
        final String fileName = parts[parts.length - 1];

        if (!selectedFile.isFile()) {
            dialog.dismiss();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
            return 0;
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                URL url = new URL(site);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);//Allow Inputs
                connection.setDoOutput(true);//Allow Outputs
                connection.setUseCaches(false);//Don't use a cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty(
                        "Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file",outputFile);

                //creating new dataoutputstream
                dataOutputStream = new DataOutputStream(connection.getOutputStream());

                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + outputFile + "\"" + lineEnd);

                dataOutputStream.writeBytes(lineEnd);

                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];

                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);


                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {

                    try {

                        //write the bytes read from inputstream
                        dataOutputStream.write(buffer, 0, bufferSize);
                    } catch (OutOfMemoryError e) {
                        Toast.makeText(MainActivity.this, "Insufficient Memory!", Toast.LENGTH_SHORT).show();
                    }
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                try{
                    serverResponseCode = connection.getResponseCode();
                }catch (OutOfMemoryError e){
                    Toast.makeText(MainActivity.this, "Memory Insufficient!", Toast.LENGTH_SHORT).show();
                }
                String serverResponseMessage = connection.getResponseMessage();

                Log.i("YEAHHH", "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                //response code of 200 indicates the server status OK
                if (serverResponseCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Done!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();



            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "File Not Found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "URL Error!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Cannot Read/Write File", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            dialog.dismiss();
            return serverResponseCode;
        }

    }





}
