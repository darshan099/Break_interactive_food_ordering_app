package com.example.darshan.abreak;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.VoiceInteractor;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Message;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,GestureDetector.OnDoubleTapListener,RecognitionListener,TextToSpeech.OnUtteranceCompletedListener,TextToSpeech.OnInitListener {

    /*
    author: darshan099

    Online Database: google spreadsheet

    The qr code generated can only to scanned using mybreakclient app (check repo)

    Implements used:
    1. gesture detection : to detect double tapping of screen to call "speech-to-text" function
    2. recognizer intent : to enable "speech-to-text"
    3. text to speech :  to enable "text-to-speech"

    Dependecies used:
    1. 'com.squareup.okhttp:okhttp:2.4.0' : efficient http client
    2. 'com.squareup.okhttp:okhttp-urlconnection:2.2.0'
    3. 'me.dm7.barcodescanner:zxing:1.9' :  to create qr code
    4. 'com.squareup.retrofit2:retrofit:2.0.0-beta3'

    Permission needed:
    1) Microphone :  to record audio
    2) stable internet connection

    Program index:
    1) text-to-speech function : line=500
    2) speech-to-text function : line=546
    3) qr code generation : line=154
    4) brain of the app: line=201
    5) speech-to-text error handline function: line=205
    6) check connection establishment to google spreadsheet : line=483

    Overview:
    this app enables speech interaction with your device to order your food.

    working:
    1) the app asks the user about food preference (breakfast or snacks) and takes order.
    1.a) take all the orders first
    1.b) take all the order quantity for each order
    1.c) concatinate all the order list and order quantity
    2) the order is then stored to google spreadsheet with the following parameters, timestamp, id, <order quantities>, validity
    3) the id of your order is then encoded into a qr code.
    4) the qr code then will be scanned by another app, mybreakclient (in my repo) which will get all the orders from the spreadsheet

    possible issues/bugs:
    1) text-to-speech active even after the app is closed.
    2) difficult navigation
    3) UI
    4) total order amount ( exercise your brain to calculate it :) )

    I did not add any payment gateway ( paytm, paypal etc ) since it might reqire certain personal information.

     */

    //initialization section
    public SpeechRecognizer speech;
    public TextToSpeech t1;
    public ProgressDialog progressDialog;
    public int init,initorder=1,initerror=0,takeorder=0,takeqty=0,retake=0,speakorder=0,placeorder=0,placeorderfinal=0;
    public int intidli=-1,intpongal=-1,intdosa=-1,intburger=-1,intsandwich=-1,intpizza=-1;
    public int orderlist=0,orderqty=0;
    public TextView txtapp,txtuser,txttemp;
    public final static int QRcodeWidth=500;
    Bitmap bitmap;
    public RelativeLayout rlayout;
    public ImageView imageView,statusimg;
    public HashMap<String,String> params=new HashMap<String,String>();
    public String time,name="DA",count;
    public String idly,dosa,pongal,burger,pizza,sandwich;
    public Intent recognizerIntent;
    public String LOG_TAG="VoiceRecognitionActivity";
    GestureDetector gestureDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        rlayout=(RelativeLayout)findViewById(R.id.relayout);
        statusimg=(ImageView)findViewById(R.id.imageView2);
        txtapp=(TextView)findViewById(R.id.textView);
        imageView=(ImageView)findViewById(R.id.imageView);
        txtuser=(TextView)findViewById(R.id.textView2);
        txttemp=(TextView)findViewById(R.id.textView3);
        gestureDetector=new GestureDetector(MainActivity.this,MainActivity.this);
        String query="";

        if(getIntent().getAction()!=null && getIntent().getAction().equals("com.google.android.gms.actions.SEARCH_ACTION"))
        {
            query=getIntent().getStringExtra(SearchManager.QUERY);
        }

        //to check whether microphone permission is given
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},100);
        }
        init=1;

        //fire "init" to enable text-to-speech and downlooad the required data
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"uniqueid");
        Intent checkIntent=new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent,1234);
    }

    //creation of qr code
    public void createqrcode()
    {
        String EditTextValue=time;
        try {
            bitmap=TextToImageEncode(EditTextValue);
            statusimg.setAlpha(0);
            imageView.setImageBitmap(bitmap);
            progressDialog.dismiss();

        }
        catch (WriterException e)
        {
            e.printStackTrace();
        }
    }

    //bitmap encoding. to encode the order id into a qr code
    Bitmap TextToImageEncode(String value) throws WriterException
    {
        BitMatrix bitMatrix;
        try{
            bitMatrix=new MultiFormatWriter().encode(value, BarcodeFormat.DATA_MATRIX.QR_CODE,QRcodeWidth,QRcodeWidth,null);

        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
        int bitMatrixWidth=bitMatrix.getWidth();
        int bitMatrixHeight=bitMatrix.getHeight();
        int[] pixels=new int[bitMatrixWidth*bitMatrixHeight];
        for(int y=0;y<bitMatrixHeight;y++)
        {
            int offset=y*bitMatrixWidth;
            for(int x=0;x<bitMatrixWidth;x++)
            {
                pixels[offset+x]=bitMatrix.get(x,y)?getResources().getColor(R.color.QRCodeBlackColor):getResources().getColor(R.color.QRCodeWhiteColor);

            }
        }
        Bitmap bitmap=Bitmap.createBitmap(bitMatrixWidth,bitMatrixHeight,Bitmap.Config.ARGB_4444);
        bitmap.setPixels(pixels,0,500,0,0,bitMatrixWidth,bitMatrixHeight);
        return bitmap;
    }

    //brain of the app. to determine the next question according to user.
    public void airesult(String s)
    {
        //if there is an error
        if(initerror==1) {
            //error handling function
            initerror = 0;
            statusimg.setImageResource(R.drawable.button);
            Toast.makeText(MainActivity.this, "Double tap to speak!", Toast.LENGTH_LONG).show();

        }

        //if there is no error
        else {
            if(placeorder==1)
            {
                //to place your final given order/ change your order/ repeat your order
                placeorder=0;
                placeorderfinal=1;
                SystemClock.sleep(1000);
                txtuser.setText("");
                txtapp.setText("say yes to confirm your order. say repeat to repeat your order! Say change to again place your order. double tap your screen when you are sure");
                textspeak2("say yes to confirm your order. say repeat to repeat your order! Say change to again place your order. Double tap your screen when you are sure.");
                statusimg.setImageResource(R.drawable.button);

            }
            if(speakorder==1)
            {

                //to speak the placed order
                speakorder=0;
                placeorder=1;
                txttemp.setText("your order: ");
                if(intdosa==0)
                {
                    txttemp.append(dosa + " dosa ");
                }
                if(intidli==0)
                {
                    txttemp.append(idly + " idli ");
                }
                if(intpongal==0)
                {
                    txttemp.append(pongal+" pongal ");
                }
                if(intpizza==0)
                {
                    txttemp.append(pizza+" pizza ");
                }
                if(intburger==0)
                {
                    txttemp.append(burger+" burger ");
                }
                if(intsandwich==0)
                {
                    txttemp.append(sandwich+" sandwich ");
                }
                String ss=txttemp.getText().toString();
                textspeak1(ss);
            }

            if (takeqty == 1) {

                //to take quantity of the given food orders
                String ss = txtuser.getText().toString();
                String words[] = ss.split(" ");
                int len = words.length;
                if (len == 0) {
                    takeqty = 0;
                    retake = 1;
                    txtapp.setText("do did not order anything? do you want to order anything?");
                    textspeak("do did not order anything? do you want to order anything?");
                } else if (orderqty == len) {
                    takeqty = 0;
                    speakorder=1;
                    if (words[orderqty-1].contains("idli"))
                    {
                        idly=s;
                    }
                    else if(words[orderqty-1].contains("dosa"))
                    {
                        dosa= s;
                    }
                    else if(words[orderqty-1].contains("pongal"))
                    {
                        pongal=s;
                    }
                    else if(words[orderqty-1].contains("burger"))
                    {
                        burger= s;
                    }
                    else if(words[orderqty-1].contains("pizza"))
                    {
                        pizza= s;
                    }
                    else if(words[orderqty-1].contains("sandwich"))
                    {
                        sandwich= s;
                    }
                    txttemp.append(s + " ");
                    textspeak1("here is your order!");
                } else {
                    if(orderqty!=0) {
                        txttemp.append(s + " ");
                        if (words[orderqty-1].contains("idli"))
                        {
                            idly=s;
                        }
                        else if(words[orderqty-1].contains("dosa"))
                        {
                            dosa= s;
                        }
                        else if(words[orderqty-1].contains("pongal"))
                        {
                            pongal=s;
                        }
                        else if(words[orderqty-1].contains("burger"))
                        {
                            burger= s;
                        }
                        else if(words[orderqty-1].contains("pizza"))
                        {
                            pizza= s;
                        }
                        else if(words[orderqty-1].contains("sandwich"))
                        {
                            sandwich= s;
                        }
                    }
                    textspeak("enter quantity of " + words[orderqty]);
                    orderqty++;
                }

            }
            if (takeorder == 1) {

                //to take all the orders first, one by one.
                if (s.contains("done ")) {
                    takeorder = 0;
                    takeqty = 1;
                    airesult("");
                } else if (orderlist <= 3)

                {
                    if (s.contains("dosa ") || s.contains("idli") || s.contains("pongal") || s.contains("burger") || s.contains("pizza") || s.contains("sandwich ")) {
                        if(s.contains("dosa "))
                        {
                            intdosa=0;
                            txtuser.append("dosa" + " ");
                        }
                        else if(s.contains("idli "))
                        {
                            intidli=0;
                            txtuser.append("idli" + " ");
                        }
                        else if(s.contains("pongal "))
                        {
                            intpongal=0;
                            txtuser.append("pongal" + " ");
                        }
                        else if(s.contains("burger "))
                        {
                            intburger=0;
                            txtuser.append("burger" + " ");
                        }
                        else if(s.contains("pizza "))
                        {
                            intpizza=0;
                            txtuser.append("pizza" + " ");
                        }
                        else if(s.contains("sandwich "))
                        {
                            intsandwich=0;
                            txtuser.append("sandwich" + " ");
                        }

                        orderlist++;
                    }
                    txtapp.setText("what would you like to order next? say done when you have completed your order");
                    textspeak("what would you like to order next? say done when you have completed your order");
                } else {
                    takeorder = 0;
                    takeqty = 1;
                    txtapp.setText("you exhausted our menu!! lets take your quantity now");
                    textspeak1("you exhausted our menu!! lets take your quantity now");
                }
            }

            if (s.contains("yes ") || s.contains("ya ")) {

                //to take breakfast or snacks
                if (initorder == 1) {
                    txtapp.setText("would you like breakfast or snacks?");
                    textspeak("would you like breakfast or snacks?");
                } else if (retake == 1) {
                    txtapp.setText("would you like breakfast or snacks?");
                    textspeak("would you like breakfast or snacks?");
                    initorder=1;
                }
                else if(placeorderfinal==1)
                {

                    //final order. call qr code function
                    placeorderfinal=0;
                    textspeak2("Scan the code to get your order. thank you! enjoy your meal. ");
                    long timex=System.currentTimeMillis();
                    time= String.valueOf(timex);
                    count="1";
                    progressDialog =new ProgressDialog(MainActivity.this);
                    progressDialog.setMessage("breathe in.. out..");
                    progressDialog.setTitle("Getting QR Code");
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.show();
                    //build retrofit. pass on all parameter data to google spreadsheet using http client (retrofit)
                    Retrofit retrofit=new Retrofit.Builder().baseUrl("https://docs.google.com/forms/d/e/")
                            .build();
                    final QuestionSpreadsheetWebService spreadsheetWebService=retrofit.create(QuestionSpreadsheetWebService.class);
                    Call<Void> completeQuestionnaireCall=spreadsheetWebService.coompleteQuestionnaire(time,name,dosa,idly,pongal,burger,pizza,sandwich,count);
                    completeQuestionnaireCall.enqueue(callCallback);
                }
            }
            if(s.contains("repeat "))
            {
                //repeat order.
                speakorder=1;
                airesult("");
            }
            if(s.contains("change "))
            {
                //change the order and reinitialize all the valiables
                initorder=1;
                txtuser.setText("");
                txtapp.setText("");
                txttemp.setText("");
                intidli=intdosa=intpongal=intpizza=intburger=intsandwich=-1;
                orderqty=0;
                orderlist=0;
                airesult("yes ");
            }
            if(s.contains("menu "))
            {
                initorder=1;
                txtapp.setText("idli rupees 5!\n" +
                        "dosa rupees 10!\n" +
                        "pongal rupees 15!\n"+
                        "burger rupees 30!\n"+
                        "pizza rupees 30!\n"  +
                        "sandwich rupees 15!\n" +
                        "to enter your order! say, yes");
                textspeak("idli rupees 5!" +
                        "dosa rupees 10!" +
                        "pongal rupees 15!" +
                        "burger rupees 30!" +
                        "pizza rupees 30!" +
                        "sandwich rupees 15!" +
                        "to enter your order! say, yes");
            }
            if (s.contains("breakfast ")) {

                //breakfast orders.
                if (initorder == 1) {
                    initorder = 0;
                    takeorder = 1;
                    txtapp.setText("here is your menu. Idly, Dosa, Pongal. say your order one by one");
                    textspeak("here is your menu. Idly, Dosa, Pongal. say your order one by one");

                }
            }
            if (s.contains("snack ") || s.contains("snacks ")) {
                //snacks order
                if (initorder == 1) {
                    initorder = 0;
                    takeorder = 1;
                    txtapp.setText("here is your menu. Burger, pizza, sandwitch. say your order one by one");
                    textspeak("here is your menu. Burger, pizza, sandwitch. say your order one by one");

                }
            }
        }


    }
    private final Callback<Void> callCallback=new Callback<Void>() {

        //callback. to get the response on successful connection to spreadsheet. if connection is established, create qr code
        @Override
        public void onResponse(Response<Void> response) {
            createqrcode();
            Log.d("XXX", String.valueOf(response));
        }

        @Override
        public void onFailure(Throwable t) {
            // if connection failed. show order failed.
            txtapp.setText("sorry your order failed!");
            progressDialog.dismiss();
            Log.d("XXX","failed",t );
        }

    };


    //text-to-speak function with calling speech-to-text function after completion
    public void textspeak(String str) {

        HashMap<String,String> params=new HashMap<String,String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"uniqueid");
        t1.speak(str,TextToSpeech.QUEUE_FLUSH,null,null);
        //shut down all process until text-to-speech is enabled
        while (t1.isSpeaking())
        {
            SystemClock.sleep(1000);
        }
        while (t1.isSpeaking())
        {
            SystemClock.sleep(1000);
        }
        //after text-to-speech is done. call the speech-to-text function
        promptSpeechInput();

    }


    //text-to-speech function with calling the brain of the app after completion.
    public void textspeak1(String s) {
        t1.speak(s, TextToSpeech.QUEUE_FLUSH, null, null);
        while (t1.isSpeaking())
        {
            SystemClock.sleep(1000);
        }
        while (t1.isSpeaking())
        {
            SystemClock.sleep(1000);
        }

        airesult("");
    }

    //text-to-speech function without calling any functions after completion
    public void textspeak2(String s) {
        t1.speak(s, TextToSpeech.QUEUE_FLUSH, null, null);
        while (t1.isSpeaking())
        {
            SystemClock.sleep(1000);
        }

    }

    //speech-to-text function
    private void promptSpeechInput()
    {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            speech.startListening(recognizerIntent);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
    }

    //speech-to-text implement fuctions. Used to cover all runtime changes
    @Override
    public void onResume()
    {
        super.onResume();
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if(speech!=null)
        {
            speech.destroy();
            Log.i(LOG_TAG,"destroy");
        }
    }
    @Override
    public void onReadyForSpeech(Bundle bundle) {
    }

    @Override
    public void onBeginningOfSpeech() {
        statusimg.setImageResource(R.drawable.d620bfd17e706a3eb5443eb986a477f4);
    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {

        statusimg.setImageResource(R.drawable.wait);
    }

    @Override
    public void onError(int i) {
        initerror=1;
        airesult("");
    }
    //main result from speech.
    @Override
    public void onResults(Bundle bundle) {
        ArrayList<String> match= bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text="";
        if(match!=null) {
                text = match.get(0);
        }
        //checking few unwanted results
        if(text.equals("to") || text.equals("too"))
        {
            airesult("2"+" ");
        }
        else if(text.equals("for") || text.equals("hey"))
        {
            airesult("4"+" ");
        }
        else if(text.equals("tree"))
        {
            airesult("3"+" ");
        }
        else
        {
            airesult(text.toLowerCase()+" ");
        }
        SystemClock.sleep(500);

    }

    @Override
    public void onPartialResults(Bundle bundle) {
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
    }

    //text-to-speech implement function.
    @Override
    public void onInit(int i) {

        if(i==TextToSpeech.SUCCESS)
        {
            String xx=txtapp.getText().toString();
            textspeak2(xx);
        }
    }


    //on activity result function.
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data)
    {
        switch (requestCode)
        {
            case 1234:
            {
                if(resultCode==TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
                {
                    t1=new TextToSpeech(this,this);
                }
                else
                {
                    Intent install=new Intent();
                    install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(install);
                }
                break;
            }

        }

    }
    @Override
    public void onUtteranceCompleted(String s) {
        promptSpeechInput();
    }


    //guesture control implement fuctions.
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    //double tap to call speech-to-text function
    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        promptSpeechInput();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }
}
