package com.example.snakegamewithbt;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.Random;
import java.util.UUID;

public class GameActivity extends Activity
{
    /*
    *  The game software written here is not entirely original source code.
    *  Original code was added and written by Anna Dawidowska and Joel Degner-Budd.
    *  The game supports Bluetooth connection of external controller
    *  and only works using external controller.
    *  Much of the code was edited to support our project, though much of the original source code
    *  remains intact from the previous author.
    * */
    private static final String TAG = "GameActivity";

    Canvas canvas;
    SnakeView snakeView;

    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;

    //Snake movement
    //0 = up, 1 = right, 2 = down, 3= left
    int directionOfTravel = 0;

    //Holds screen size values
    int screenWidth;
    int screenHeight;
    int topGap;

    //Game statistics
    long lastFrameTime;
    int fps;
    int score;
    int hi;

    //Game objects
    int[] snakeX;
    int[] snakeY;
    int snakeLength;
    int appleX;
    int appleY;

    //The size in pixels of a place on the game board
    int blockSize;
    int numBlocksWidth;
    int numBlocksHeight;

    //-----BLUETOOTH COMPONENTS-----

    BluetoothConnectionService mBluetoothConnection;

    //UUID
    static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //VARIABLE RECEIVED FROM ARDUINO
    StringBuilder direction;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);

        //GETTING DATA FROM ARDUINO
        direction = new StringBuilder();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

    }

    //Written by Anna Dawidowska, Edited by Joel Degner-Budd
    BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int num = 0; //used to pass direction value
            String text = intent.getStringExtra("theMessage");
            direction.append(text + "\n"); //append direction to String

            Log.d(TAG, "Received direction from arduino: " + direction);
            String[] temp = direction.toString().split("\\s"); //remove whitespace from String

            try
            {
                num = Integer.parseInt(temp[0]);//Parse data and store as an int value
                ControlInput(num);//pass to control method
            }
            catch(NumberFormatException e)
            {
                Toast.makeText(GameActivity.this, "Could not parse " + e, Toast.LENGTH_SHORT).show();
            }

            direction.setLength(0);//reset the direction value
        }
    };

    //Return to previous activity with game score value
    public void sendScore()
    {
        Intent goBack = new Intent (GameActivity.this, MainActivity.class);
        String scoreString = Integer.toString(score);
        goBack.putExtra("SCORE", scoreString);
        startActivity(goBack);

    }

    //Keypad control method - Written By Joel Degner-Budd 40430615
    private void ControlInput(int i)
    {
        //if the key pressed is equal to the value 3
        if(i == 3)
        {
            //increment direction of travel i.e. turn right
            directionOfTravel++;
            //direction of travel cannot exceed 4 and is reset to 0
            if(directionOfTravel == 4)
            {
                directionOfTravel = 0;
            }
        }
        //if the key pressed is equal to the value 1
        else if(i == 1)
        {
            //increment direction of travel i.e. turn left
            directionOfTravel--;
            //direction of travel cannot exceed -1 and is reset to 3
            if(directionOfTravel == -1)
            {
                directionOfTravel = 3;
            }
        }
    }

    class SnakeView extends SurfaceView implements Runnable
    {
        Thread gameThread = null; //start new game thread
        SurfaceHolder gameHolder; //create new game holder
        volatile boolean playingSnake; //playing snake game boolean
        Paint paint; //new paint object for drawing canvas

        public SnakeView(Context context)
        {
            super(context);
            gameHolder = getHolder();
            paint = new Paint();

            //Maximum possible snake length 200
            snakeX = new int[200];
            snakeY = new int[200];

            //Get new starting Snake object
            getSnake();
            //Get new starting apple object
            getApple();
        }

        public void getSnake()
        {
            //Initial starting length of snake
            snakeLength = 3;
            //Start snake head in the middle of screen
            snakeX[0] = numBlocksWidth / 2;
            snakeY[0] = numBlocksHeight / 2;

            //Followed by placing the body
            snakeX[1] = snakeX[0] - 1;
            snakeY[1] = snakeY[0];

            //And then finally placing the tail
            snakeX[1] = snakeX[1] - 1;
            snakeY[1] = snakeY[0];
        }

        //create new apple object and randomly place it on the game grid
        public void getApple()
        {
            Random random = new Random();
            appleX = random.nextInt(numBlocksWidth - 1) + 1;
            appleY = random.nextInt(numBlocksHeight - 1) + 1;
        }

        @Override
        public void run()
        {
            while (playingSnake)
            {
                updateGame();
                drawGame();
                controlFPS();
            }

        }

        public void updateGame()
        {
            //If the player gets the apple object
            if (snakeX[0] == appleX && snakeY[0] == appleY)
            {
                //Increase Snake length
                snakeLength++;
                //Get new apple object
                getApple();
                //Add to the score
                score = score + snakeLength;
                Log.d(TAG, "Score: " + score);
            }

            //move the body - starting at the back
            //moves snake related objects across the axial grid
            for (int i = snakeLength; i > 0; i--)
            {
                snakeX[i] = snakeX[i - 1];
                snakeY[i] = snakeY[i - 1];
            }

            //Move the head in the appropriate direction
            switch (directionOfTravel)
            {
                case 0://up
                    snakeY[0]--;
                    break;

                case 1://right
                    snakeX[0]++;
                    break;

                case 2://down
                    snakeY[0]++;
                    break;

                case 3://left
                    snakeX[0]--;
                    break;
            }

            //If border collision occurs
            boolean dead = false;
            if (snakeX[0] == -1) dead = true;
            if (snakeX[0] >= numBlocksWidth) dead = true;
            if (snakeY[0] == -1) dead = true;
            if (snakeY[0] == numBlocksHeight) dead = true;

            //If a body collision occurs
            for (int i = snakeLength - 1; i > 0; i--)
            {
                if ((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i]))
                {
                    dead = true;
                }
            }

            //if dead end game and send score
            if (dead)
            {
                //Go to main activity and send the high score to arduino
                //if dead close socket
                sendScore();
            }

        }

        public void drawGame()
        {

            if (gameHolder.getSurface().isValid())
            {
                canvas = gameHolder.lockCanvas();
                canvas.drawColor(Color.WHITE);//the background
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(topGap / 2);
                canvas.drawText("Score:" + score + "  Hi:" + hi+" Dir: "+directionOfTravel, 10, topGap - 6, paint);

                //draw a border - 4 lines, top right, bottom , left
                paint.setStrokeWidth(3);//4 pixel border
                canvas.drawLine(1, topGap, screenWidth - 1, topGap, paint);
                canvas.drawLine(screenWidth - 1, topGap, screenWidth - 1, topGap + (numBlocksHeight * blockSize), paint);
                canvas.drawLine(screenWidth - 1, topGap + (numBlocksHeight * blockSize), 1, topGap + (numBlocksHeight * blockSize), paint);
                canvas.drawLine(1, topGap, 1, topGap + (numBlocksHeight * blockSize), paint);

                //Draw the snake
                canvas.drawBitmap(headBitmap, snakeX[0] * blockSize, (snakeY[0] * blockSize) + topGap, paint);
                //Draw the body
                for (int i = 1; i < snakeLength - 1; i++) {
                    canvas.drawBitmap(bodyBitmap, snakeX[i] * blockSize, (snakeY[i] * blockSize) + topGap, paint);
                }
                //Draw the tail
                canvas.drawBitmap(tailBitmap, snakeX[snakeLength - 1] * blockSize, (snakeY[snakeLength - 1] * blockSize) + topGap, paint);

                //Draw the apple
                canvas.drawBitmap(appleBitmap, appleX * blockSize, (appleY * blockSize) + topGap, paint);

                gameHolder.unlockCanvasAndPost(canvas);
            }

        }

        public void controlFPS()
        {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 100 - timeThisFrame;
            if (timeThisFrame > 0)
            {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0)
            {

                try
                {
                    gameThread.sleep(timeToSleep);
                }
                catch (InterruptedException e)
                {
                    //Print an error message to the console
                    Log.e("error", "failed to load");
                }

            }
            lastFrameTime = System.currentTimeMillis();
        }


        public void pause()
        {
            playingSnake = false;
            try
            {
                gameThread.join();
            }
            catch (InterruptedException e)
            {
                Log.e("error", "failed to load");
            }

        }

        public void resume()
        {
            playingSnake = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    //Edits made by Anna Dawidowska and Joel Degner-Budd
    @Override
    protected void onStop()
    {
        super.onStop();

        while (true)
        {
            snakeView.pause();
            break;
        }
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        snakeView.resume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        snakeView.pause();
    }

    //Edits made by Anna Dawidowska and Joel Degner-Budd
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            snakeView.pause();

            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }

    //Edits made by Anna Dawidowska and Joel Degner-Budd
    public void configureDisplay()
    {
        //find out the width and height of the screen
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y-50;
        topGap = screenHeight / 14;

        //Determine the size of each block/place on the game board
        blockSize = screenWidth / 40;

        //Determine how many game blocks will fit into the height and width
        //Leave one block for the score at the top
        numBlocksWidth = 40;
        numBlocksHeight = ((screenHeight - topGap)) / blockSize;

        //Load and scale bitmaps
        headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.head2);
        bodyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.body2);
        tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tail2);
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple2);

        //scale the bitmaps to match the block size
        headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize, blockSize, false);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap, blockSize, blockSize, false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);
    }
}
