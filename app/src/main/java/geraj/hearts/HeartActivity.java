package geraj.hearts;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import course.labs.graphicslab.R;

public class HeartActivity extends Activity {

    // These variables are for testing purposes, do not modify
    private final static int RANDOM = 0;
    private final static int SINGLE = 1;
    private final static int STILL = 2;
    private static int speedMode = RANDOM;

    private static final String TAG = "Lab-Graphics";

    // The Main view
    private RelativeLayout mFrame;
    private ImageView mImageView;
    // Bubble image's bitmap
    private Bitmap mBitmap;

    // Display dimensions
    private int mDisplayWidth, mDisplayHeight;

    // Sound variables

    // AudioManager
    private AudioManager mAudioManager;
    // SoundPool
    private SoundPool mSoundPool;
    // ID for the bubble popping sound
    private int[] mSoundID = new int[4];
    // Audio volume
    private float mStreamVolume;
    private ArrayList<Drawable> mDrawables = new ArrayList<Drawable>();

    // Gesture Detector
    private GestureDetector mGestureDetector;

    private ScheduledFuture<?> mGenerateHeartExecutor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Set up user interface
        mFrame = (RelativeLayout) findViewById(R.id.frame);
        mImageView = (ImageView) findViewById(R.id.image_view);

        // Load basic bubble Bitmap
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.heartopac);

        mDrawables.add(getResources().getDrawable(R.drawable.one));
        mDrawables.add(getResources().getDrawable(R.drawable.two));
        mDrawables.add(getResources().getDrawable(R.drawable.three));
        mDrawables.add(getResources().getDrawable(R.drawable.four));
        mDrawables.add(getResources().getDrawable(R.drawable.five));

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Manage bubble popping sound
        // Use AudioManager.STREAM_MUSIC as stream type

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        mStreamVolume = (float) mAudioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC)
                / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        // make a new SoundPool, allowing up to 10 streams
        mSoundPool = new SoundPool(10, 0, AudioManager.STREAM_MUSIC);


        // set a SoundPool OnLoadCompletedListener that calls setupGestureDetector()
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                setupGestureDetector();
                Random r = new Random();
                for (int i = 0; i < 4; i++) {
                    HeartView heartView = new HeartView(getApplicationContext(), r.nextInt(mDisplayWidth + 1), r.nextInt(mDisplayHeight + 1), speedMode);
                    heartView.start();
                    mFrame.addView(heartView);
                }
            }
        });

        mSoundID[0] = mSoundPool.load(this, R.raw.recording1, 1);
        mSoundID[1] = mSoundPool.load(this, R.raw.recording2, 1);
        mSoundID[2] = mSoundPool.load(this, R.raw.recording3, 1);
        mSoundID[3] = mSoundPool.load(this, R.raw.recording4, 1);

        ScheduledExecutorService executor = Executors
                .newScheduledThreadPool(1);


        /**
         * Add a heart every 2 secconds
         */
        mGenerateHeartExecutor = executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {


                mFrame.post(new Runnable() {
                    @Override
                    public void run() {
                        Random r = new Random();
                        HeartView heartView = new HeartView(getApplicationContext(), r.nextInt(mDisplayWidth + 1), r.nextInt(mDisplayHeight + 1), r.nextInt(3));
                        heartView.start();
                        mFrame.addView(heartView);
                    }
                });
            }

            ;
        }, 0, 2000, TimeUnit.MILLISECONDS);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {

            // Get the size of the display so this View knows where borders are
            mDisplayWidth = mFrame.getWidth();
            mDisplayHeight = mFrame.getHeight();
        }
    }

    // Set up GestureDetector
    private void setupGestureDetector() {

        mGestureDetector = new GestureDetector(this,

                new GestureDetector.SimpleOnGestureListener() {

                    // If a fling gesture starts on a heart view then change the
                    // HeartView's velocity and change the background picture

                    @Override
                    public boolean onFling(MotionEvent event1, MotionEvent event2,
                                           float velocityX, float velocityY) {

                        HeartView heartView = getBubleOnMotion(event1);
                        if (heartView != null) {
                            heartView.deflect(velocityX, velocityY);
                            mFrame.post(new Runnable() {
                                @Override
                                public void run() {
                                    Random r = new Random();
                                    mImageView.setImageDrawable(mDrawables.get(r.nextInt(5)));
                                }
                            });
                            return true;
                        }
                        return false;


                    }

                    // If a single tap intersects a HeartView, then pop the HeartView
                    // Otherwise, create a new HeartView at the tap's location and add
                    //

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent event) {

                        HeartView heartView = getBubleOnMotion(event);
                        if (heartView != null) {
                            heartView.stop(true);
                            return true;
                        }
                        heartView = new HeartView(getApplicationContext(), event.getX(), event.getY(), speedMode);
                        heartView.start();
                        mFrame.addView(heartView);
                        return false;
                    }
                });
    }

    /**
     * Get intersecting bubble view
     *
     * @param event1
     * @return bubbleview or null if no interscting view found
     */
    public HeartView getBubleOnMotion(MotionEvent event1) {
        Log.i(TAG, "Event " + event1.getX() + " " + event1.getY());
        int nr = mFrame.getChildCount();
        for (int i = 0; i < nr; i++) {
            View view = mFrame.getChildAt(i);
            if (view instanceof HeartView) {
                HeartView heartView = (HeartView) view;
                if (heartView.intersects(event1.getX(), event1.getY())) {
                    return heartView;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mGestureDetector.onTouchEvent(event);


        return false;

    }

    @Override
    protected void onPause() {

        mSoundPool.release();
        mGenerateHeartExecutor.cancel(true);

        super.onPause();
    }

    // HeartView is a View that displays a heart.
    // This class handles animating, drawing, and popping amongst other actions.
    // A new HeartView is created for each bubble on the display

    public class HeartView extends View {

        private static final int BITMAP_SIZE = 64;
        private static final int REFRESH_RATE = 40;
        private final Paint mPainter = new Paint();
        private ScheduledFuture<?> mMoverFuture;
        private int mScaledBitmapWidth;
        private Bitmap mScaledBitmap;

        // location, speed and direction of the bubble
        private float mXPos, mYPos, mDx, mDy, mRadius, mRadiusSquared;
        private long mRotate, mDRotate;

        HeartView(Context context, float x, float y, int speedModeToUse) {
            super(context);

            // Create a new random number generator to
            // randomize size, rotation, speed and direction
            Random r = new Random();

            // Creates the bubble bitmap for this BubbleView
            createScaledBitmap(r);

            // Radius of the Bitmap
            mRadius = mScaledBitmapWidth / 2;
            mRadiusSquared = mRadius * mRadius;

            // Adjust position to center the bubble under user's finger
            mXPos = x;
            mYPos = y;

            Log.i(TAG, "BubbleView: " + mXPos + " " + mYPos);

            // Set the BubbleView's speed and direction
            setSpeedAndDirection(r, speedModeToUse);

            // Set the BubbleView's rotation
            setRotation(r);

            mPainter.setAntiAlias(true);

        }

        private void setRotation(Random r) {

            if (speedMode == RANDOM) {
                mDRotate = r.nextInt(3) + 1;
            } else {
                mDRotate = 0;
            }
        }

        private void setSpeedAndDirection(Random r, int speedMode) {

            // Used by test cases
            switch (speedMode) {

                case SINGLE:

                    mDx = 20;
                    mDy = 20;
                    break;

                case STILL:

                    // No speed
                    mDx = 0;
                    mDy = 0;
                    break;

                default:
                    mDx = r.nextInt(7) - 3;
                    mDy = r.nextInt(7) - 3;

            }
        }

        private void createScaledBitmap(Random r) {


            //set scaled bitmap size in range [1..3] * BITMAP_SIZE
            mScaledBitmapWidth = (r.nextInt(3) + 1) * BITMAP_SIZE;

            // create the scaled bitmap using size set above
            mScaledBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.heartopac), mScaledBitmapWidth, mScaledBitmapWidth, false);
        }

        // Start moving the HeartView & updating the display
        private void start() {

            // Creates a WorkerThread
            ScheduledExecutorService executor = Executors
                    .newScheduledThreadPool(1);

            // Execute the run() in Worker Thread every REFRESH_RATE
            // milliseconds
            // Save reference to this job in mMoverFuture
            mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {

                    // Movement logic.
                    // Each time this method is run the BubbleView should
                    // move one step. If the BubbleView exits the display,
                    // stop the BubbleView's Worker Thread.
                    // Otherwise, request that the BubbleView be redrawn.
                    if (moveWhileOnScreen()) {
                        postInvalidate();
                    } else {
                        stop(false);
                    }
                }
            }, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
        }

        // Returns true if the BubbleView intersects position (x,y)
        private synchronized boolean intersects(float x, float y) {

            // TODO - Return true if the BubbleView intersects position (x,y)
            if (x > mXPos - mRadius &&
                    x < mXPos + mRadius
                    && y < mYPos + mRadius
                    && y > mYPos - mRadius) {
                return true;
            }


            return false;
        }

        // Cancel the heart's movement
        // Remove heart from mFrame
        // Play pop sound if the heartView was popped

        private void stop(final boolean wasPopped) {

            if (null != mMoverFuture && !mMoverFuture.isDone()) {
                mMoverFuture.cancel(true);
            }

            // This work will be performed on the UI Thread
            mFrame.post(new Runnable() {
                @Override
                public void run() {
                    mFrame.removeView(HeartView.this);
                    // If the heart was popped by user,
                    // play the random sound
                    if (wasPopped) {
                        Random r = new Random();
                        mSoundPool.play(mSoundID[r.nextInt(4)], 1, 1, 0, 0, 1);
                    }
                }
            });
        }

        // Change the heart's speed and direction
        private synchronized void deflect(float velocityX, float velocityY) {

            mDx = velocityX / REFRESH_RATE;
            mDy = velocityY / REFRESH_RATE;

        }

        // Draw the heart at its current location
        @Override
        protected synchronized void onDraw(Canvas canvas) {
            canvas.save();
            mRotate += mDRotate;
            canvas.rotate(mRotate, mXPos, mYPos);
            //Log.i(TAG, "Draw BM @ top left" + (mXPos - mRadius) + " y " + (mYPos + mRadius));
            canvas.drawBitmap(mScaledBitmap, mXPos - mRadius, mYPos - mRadius, mPainter);
            canvas.restore();

        }

        // Returns true if the heartView is still on the screen after the move
        // operation
        private synchronized boolean moveWhileOnScreen() {

            mXPos += mDx;
            mYPos += mDy;
            if (isOutOfView()) {
                return false;
            }
            return true;
        }

        // Return true if the heartView is off the screen after the move
        // operation
        private boolean isOutOfView() {

            // the move operation
            if (mYPos > mDisplayHeight || mYPos < 0 || mXPos > mDisplayWidth || mXPos < 0) {
                return true;
            }
            return false;
        }
    }

    // Do not modify below here

    @Override
    public void onBackPressed() {
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_still_mode:
                speedMode = STILL;
                return true;
            case R.id.menu_single_speed:
                speedMode = SINGLE;
                return true;
            case R.id.menu_random_mode:
                speedMode = RANDOM;
                return true;
            case R.id.quit:
                exitRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void exitRequested() {
        super.onBackPressed();
    }
}