# break_interactive_food_ordering_app
An interactive food ordering app with hands-off experience

    author: darshan099
    
    Online Database: google spreadsheet
    
    The qr code generated can only to scanned using break_client app (check repo)
    
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
    4) the qr code then will be scanned by another app, break_client (in my repo) which will get all the orders from the spreadsheet
    
    possible issues/bugs:
    
    1) text-to-speech active even after the app is closed.
    2) difficult navigation
    3) UI
    4) total order amount ( exercise your brain to calculate it :) )
    
    I did not add any payment gateway ( paytm, paypal etc ) since it might reqire certain personal information.
