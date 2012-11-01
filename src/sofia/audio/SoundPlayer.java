package sofia.audio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.SystemClock;
import android.util.Log;

/**
 * Class that provides a simple API for playing sounds in an android project. 
 * @author bellen08
 *
 */
public class SoundPlayer 
{
	public String TAG = "SoundPlayer:";
	private static final int DEFAULT_LOOP = 0;
	private static final float DEFAULT_RATE = 1.0f;
	
	private HashMap<String, Integer> soundMap;
	
	//maps soundIds to streamIds
	private HashMap<Integer, Integer> streamIds;
	
	private SoundPool soundPool;
	private Context context;
	
	//~ Constructors ..........................................................

    // ----------------------------------------------------------
	/**
	 * Creates a new SoundPlayer with given context
	 * @param context
	 */
	public SoundPlayer(Context context) 
	{
		this.context = context;
		soundMap = new HashMap<String, Integer>();
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		streamIds = new HashMap<Integer, Integer>();
	}
	
	
    // ----------------------------------------------------------
	/**
	 * Method specifying a sound to loop infinitely.
	 * @param sound the sound to repeat
	 */
	public void playOnRepeat(String sound)
	{
		play(sound, -1);
	}
	
	
	//public void load_resources

    // ----------------------------------------------------------
	/**
	 * Method specifying the sound to play, doesn't loop by default.
	 * @param sound
	 */
	public int play(String sound)
	{
		return play(sound, DEFAULT_LOOP);		
	}
	
    // ----------------------------------------------------------
	/**
	 * Overloaded play specifying the number of times to loop the sound
	 * @param sound
	 * @param loopCount
	 * @throws IllegalArgumentException 
	 */
	public int play(String sound, int loopCount) 
	{
		if (!isLoaded(sound))
			loadSound(sound);
		int soundid = soundMap.get(sound);
		int stream = playHelper(soundid, loopCount);
		streamIds.put(soundid, stream);
		return stream;
	}
	
    // ----------------------------------------------------------
	/**
	 * Stops the current stream from playing 
	 */
	public void stop(String sound)
	{
		if (soundMap.containsKey(sound))
			soundPool.stop(streamIds.get(soundMap.get(sound)));
	}
	
	
    // ----------------------------------------------------------
	/**
	 * Pauses the given file if it's playing, else ignores
	 * @return
	 */
	public void pause(String sound)
	{
		if (soundMap.containsKey(sound))
			soundPool.pause(streamIds.get(soundMap.get(sound)));
		Log.i(TAG, "pause called on sound: " + sound);
	}
	
	public void pauseAllSounds()
	{
		soundPool.autoPause();
	}
	
	/**
	 * Resumes playing the sound, assuming the sound:
	 * 
	 * A.exists
	 * B.currently paused
	 * @param sound
	 */
	public void resume(String sound)
	{
		if (soundMap.containsKey(sound))
			soundPool.resume(streamIds.get(soundMap.get(sound)));
	}
	
    // ----------------------------------------------------------
	/**
	 * Returns the mapping of sound file names to their
	 * unique soundId -- mostly for testing purposes.
	 * 
	 * ****To do: remove***
	 * @return
	 */
	public HashMap<String, Integer> getSoundMap()
	{
		return soundMap;
	}
	
    // ----------------------------------------------------------
	/**
	 * Returns the mapping soundIds to streamId
	 * 
	 * ****To do: remove***
	 * @return
	 */
	public HashMap<Integer, Integer> getStreamMap()
	{
		return streamIds;
	}
	
    // ----------------------------------------------------------
	/**
	 * Currently being called in the onDestroy method, 
	 * releases all memory associated with given pool.
	 */
	public void clear()
	{
		soundPool.release();
	}
	
	//public pauseAll
    // ----------------------------------------------------------
	/**
	 * Attempts to load the sound file with the given name by first checking for the file
	 * in R.raw, then in the assets/sounds folder.
	 * @param filename name of the sound file
	 * @throws IllegalArgumentException if a sound_file with given name cannot be located
	 */
	public void loadSound(String filename) throws IllegalArgumentException
	{
		boolean didLoad = addSoundFromResources(filename) || addSoundFromAssets(filename);
		if (!didLoad)
			throw new IllegalArgumentException("Could not find specified sound resource.");
	}
	
    // ----------------------------------------------------------
	/**
	 * Attempts to load a sound file from resources (in R.raw sub folder)
	 * @param sound the soundfile name (no extension)
	 * @return true if succesfully loaded
	 */
	private boolean addSoundFromResources(String sound)
	{
		int res_id = context.getResources().getIdentifier(sound, "raw", context.getPackageName());
		if (res_id != 0)
		{
			int id = soundPool.load(context, res_id, 1);
			soundMap.put(sound, id);
			Log.i("addSoundFromResources", "successfully added " + sound + " from R.raw.");
			return true;
		}
		else return false;
	}
	
    // ----------------------------------------------------------
	/**
	 * Attempts to load a sound file from the assets folder (under sounds/ directory)
	 * Returns indication of load success
	 * @param sound filename	
	 * @return true if successfully loaded from assets/sounds/
	 */
	private boolean addSoundFromAssets(String sound)
	{
		AssetManager assets = context.getAssets();
		AssetFileDescriptor assetDescriptor;
		try{
			assetDescriptor = assets.openFd("sounds" + "/" + sound);
			int soundId = soundPool.load(assetDescriptor, 1);
			soundMap.put(sound, soundId);
			Log.i("addSoundFromAssets", "successfully added " + sound + " from assets/sounds.");
		}catch(IOException e){
			return false;}	
		return true;
	}
	
    // ----------------------------------------------------------
	/**
	 * Helper method that plays the given sound a certain amount of times as specified
	 * by the caller.
	 * @param soundId unique sound id
	 * @param loopCount number of times to repeat the sound
	 */
	private int playHelper(int soundId, int loopCount)
	{
		int streamId = 0;
		int waitLimit = 1000;
		int waitCounter = 0;
		int throttle = 10;
		do
		{
			streamId = soundPool.play(soundId, 0.5f, 0.5f, 
					1, loopCount, DEFAULT_RATE);
			waitCounter ++;
			SystemClock.sleep(throttle);
		}while(streamId == 0 && waitCounter < waitLimit);
		if (streamId == 0)
			throw new RuntimeException("Failed to load sound file.");
		return streamId;
	}

    // ----------------------------------------------------------
	/**
	 * Helper method determining whether or not a sound file has been loaded
	 * to the pool
	 * @param soundName
	 * @return
	 */
	private boolean isLoaded(String soundName)
	{
		return soundMap.containsKey(soundName);
	}
	


	public String toString()
	{
		String out = "Total number of sound files loaded: " + soundMap.size() + "\n";
		out += "-------------------------------------------------------------------\n";
		
		Iterator<Entry<String, Integer>> it = soundMap.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry nameId = (Map.Entry)it.next();
			out += "soundfile: " + nameId.getKey() + "     soundId: " + nameId.getValue() + "     streamId: " + streamIds.get(nameId.getValue()) + "\n";
		}
		out += "-------------------------------------------------------------------\n";
		return out;
	}

	
}
