/*
 * Copyright (C) 2011 Virginia Tech Department of Computer Science
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sofia.audio;

import sofia.app.internal.LifecycleInjection;
import sofia.app.internal.ScreenMixin;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemClock;
import android.util.SparseIntArray;

import java.io.IOException;
import java.util.HashMap;

//-------------------------------------------------------------------------
/**
 * This class provides a simple interface for playing basic sounds (such as
 * notification chimes or game play effects) in a Sofia application.
 *  
 * @author Ellen Boyd, Tony Allevato
 */
public class SoundPlayer 
{
	//~ Fields ................................................................

	private static final int LOOP_INDEFINITELY = -1;
	private static final float DEFAULT_RATE = 1.0f;
	
	private static final String[] ASSET_EXTENSIONS = {
		".ogg", ".OGG", ".mp3", ".MP3", ".wav", ".WAV"
	};
	
	private Context context;
	private SoundPool soundPool;

	// A cache that maps sound names to their sound pool integer IDs, so that
	// sounds can always be referred to by name for simplicity.
	private HashMap<String, Integer> soundNamesToPoolIds;
	
	// Maps sound pool IDs to currently playing stream IDs.
	private SparseIntArray poolIdsToStreamIds;


	//~ Constructors ..........................................................

    // ----------------------------------------------------------
	/**
	 * Creates a new SoundPlayer with given context (which is an activity or
	 * screen).
	 * 
	 * @param context the context
	 */
	public SoundPlayer(Context context) 
	{
		this.context = context;
		
		soundNamesToPoolIds = new HashMap<String, Integer>();
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		poolIdsToStreamIds = new SparseIntArray();
		
		ScreenMixin mixin = ScreenMixin.getMixin(context);
		if (mixin != null)
		{
			mixin.addLifecycleInjection(injection);
		}		
	}
	

	//~ Methods ...............................................................

    // ----------------------------------------------------------
	/**
	 * Plays the sound with the specified name once.
	 * 
	 * @param name the name of the sound to play
	 */
	public void play(String name)
	{
		play(name, 0);		
	}
	

    // ----------------------------------------------------------
	/**
	 * Plays the sound with the specified name, repeating it a given number of
	 * times.
	 * 
	 * @param name the name of the sound to play
	 * @param loopCount the number of times to repeat the sound
	 */
	public void play(String name, int loopCount) 
	{
		if (!isLoaded(name))
		{
			loadSound(name);
		}

		int soundId = soundNamesToPoolIds.get(name);
		int stream = playHelper(soundId, loopCount);
		poolIdsToStreamIds.put(soundId, stream);
	}


    // ----------------------------------------------------------
	/**
	 * Plays the sound with the specified name, repeating it forever (until a
	 * method such as {@link #pause(String)} or {@link #stop(String)} is
	 * called).
	 * 
	 * @param name the name of the sound to play
	 */
	public void playForever(String name)
	{
		play(name, LOOP_INDEFINITELY);
	}
	
	
    // ----------------------------------------------------------
	/**
	 * Stops the sound with the specified name, if it is currently playing. If
	 * the sound is not currently playing, nothing happens.
	 * 
	 * @param name the name of the sound to stop
	 */
	public void stop(String name)
	{
		if (soundNamesToPoolIds.containsKey(name))
		{
			soundPool.stop(
					poolIdsToStreamIds.get(soundNamesToPoolIds.get(name)));
		}
	}
	

	// ----------------------------------------------------------
	/**
	 * Resumes the sound with the specified name, if it is paused. If the sound
	 * is not paused (or was never loaded), nothing happens.
	 * 
	 * @param name the name of the sound to resume
	 */
	public void resume(String name)
	{
		if (soundNamesToPoolIds.containsKey(name))
		{
			soundPool.resume(
					poolIdsToStreamIds.get(soundNamesToPoolIds.get(name)));
		}
	}
	

    // ----------------------------------------------------------
	/**
	 * Pauses the sound with the specified name, if it is playing. If the sound
	 * is not playing, nothing happens.
	 * 
	 * @param name the name of the sound to pause
	 */
	public void pause(String name)
	{
		if (soundNamesToPoolIds.containsKey(name))
		{
			soundPool.pause(
					poolIdsToStreamIds.get(soundNamesToPoolIds.get(name)));
		}
	}

	
	// ----------------------------------------------------------
	/**
	 * Pauses all currently playing sounds. Call {@link #resumeAll()} to start
	 * them again where they left off.
	 */
	public void pauseAll()
	{
		soundPool.autoPause();
	}


	// ----------------------------------------------------------
	/**
	 * Resumes all sounds that were playing when {@link #pauseAll()} was
	 * called.
	 */
	public void resumeAll()
	{
		soundPool.autoResume();
	}


	// ----------------------------------------------------------
	/**
	 * Attempts to load the sound file with the given name by first checking
	 * for a resource with the matching name in res/raw, and if it is not found
	 * there then it is looked up in the assets/sounds folder.
	 * 
	 * @param name the name of the sound file, without the file extension
	 * 
	 * @throws IllegalArgumentException if a sound with the given name cannot
	 *     be located
	 */
	public void loadSound(String name)
	{
		boolean didLoad =
				loadSoundFromResources(name) || loadSoundFromAssets(name);

		if (!didLoad)
		{
			throw new IllegalArgumentException(
					"Could not find an audio file named \"" + name +
					"\" in assets or in res/raw.");
		}
	}


    // ----------------------------------------------------------
	/**
	 * Attempts to load a sound from a resource stored in the res/raw folder.
	 * 
	 * @param name the name of the sound file, without the file extension
	 * @return true if the sound was found and loaded successfully
	 */
	private boolean loadSoundFromResources(String name)
	{
		int resId = context.getResources().getIdentifier(
				name, "raw", context.getPackageName());

		if (resId != 0)
		{
			int soundId = soundPool.load(context, resId, 1);
			soundNamesToPoolIds.put(name, soundId);

			return true;
		}
		else
		{
			return false;
		}
	}


    // ----------------------------------------------------------
	/**
	 * Attempts to load a sound from a resource stored in the assets/sounds
	 * folder.
	 * 
	 * @param name the name of the sound file, without the file extension
	 * @return true if the sound was found and loaded successfully
	 */
	private boolean loadSoundFromAssets(String name)
	{
		AssetManager assets = context.getAssets();
		AssetFileDescriptor fd = null;

		for (String extension : ASSET_EXTENSIONS)
		{
			try
			{
				fd = assets.openFd("sounds/" + name + extension);
			}
			catch (IOException e)
			{
				// Do nothing.
			}
		}
		
		if (fd != null)
		{
			int soundId = soundPool.load(fd, 1);
			soundNamesToPoolIds.put(name, soundId);

			try
			{
				fd.close();
			}
			catch (IOException e)
			{
				// Do nothing.
			}

			return true;
		}
		else
		{
			return false;
		}
	}


    // ----------------------------------------------------------
	/**
	 * Helper method that plays the given sound a certain amount of times as
	 * specified by the caller.
	 * 
	 * @param soundId unique sound id
	 * @param loopCount number of times to repeat the sound
	 */
	private int playHelper(int soundId, int loopCount)
	{
		// FIXME We need to replace this with a background thread that uses
		// the sound pool's load-listener to wait for a sound to be loaded
		// before it is played.

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
		} while(streamId == 0 && waitCounter < waitLimit);
		
		if (streamId == 0)
		{
			throw new RuntimeException("Failed to load sound file.");
		}

		return streamId;
	}


    // ----------------------------------------------------------
	/**
	 * Returns true if a sound with the specified name has been loaded into the
	 * sound pool.
	 * 
	 * @param name the name of the sound
	 * @return true if the specified sound has been loaded, otherwise false
	 */
	private boolean isLoaded(String name)
	{
		return soundNamesToPoolIds.containsKey(name);
	}
	
	
	//~ Inner classes .........................................................

	// ----------------------------------------------------------
	/**
	 * This object is injected into the owning screen's lifecycle so that
	 * sounds will be automatically stopped when the screen is paused.
	 */
	private final LifecycleInjection injection = new LifecycleInjection()
	{
		// ----------------------------------------------------------
		@Override
		public void pause()
		{
			pauseAll();
		}

		
		// ----------------------------------------------------------
		@Override
		public void resume()
		{
			resumeAll();
		}

		
		// ----------------------------------------------------------
		@Override
		public void destroy()
		{
			soundPool.release();
		}
	};
}
