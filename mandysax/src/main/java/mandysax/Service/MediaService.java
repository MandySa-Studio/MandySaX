package mandysax.Service;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.os.*;
import java.io.*;
import java.util.*;
import mandysax.Lifecycle.LiveData.*;
import mandysax.Service.StateEvent.*;

import mandysax.Lifecycle.LiveData.Observer;

public abstract class MediaService extends Service
{

	public abstract Class<?> getActvity()

	public abstract int getSinger()

	public abstract int getSmallIcon()

	public abstract int getLastSong()

	public abstract int getPlayOfPause()

	public abstract int getNextSong()

	public abstract String getUrl()

	private final String[] GROUP={"a"};
	private final String[] DITCH={"aa"};

    public final static int SEEK=0,NEXT=1,LAST=2,ERROR=3,LOADMUSIC=4,PLAY=5,PAUSE=6;

	private NotificationManager manager;

	private Notification.Builder builder;

	private MusicBinder music_bind;

    @Override
    public IBinder onBind(Intent p1)
    {
        return music_bind = new MusicBinder();
    }

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		music_bind.onDestroy();
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		builder = new Notification.Builder(this);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
		{
            NotificationChannelGroup channelGroup = new NotificationChannelGroup(GROUP[0], "媒体");
			manager.createNotificationChannelGroup(channelGroup);
			NotificationChannel notificationChannel = new NotificationChannel(DITCH[0], "音乐控制", NotificationManager.IMPORTANCE_NONE);
			notificationChannel.enableLights(false);
			notificationChannel.enableVibration(false);
			notificationChannel.setGroup(GROUP[0]);
			manager.createNotificationChannel(notificationChannel);
            builder .setChannelId(DITCH[0]);
        } 
        builder.setSmallIcon(getSmallIcon())
			.setContentTitle("没有音乐")
			.setContentText("没有音乐")
			.setBadgeIconType(getSinger())
			.setLargeIcon(BitmapFactory.decodeResource(getResources(), getSinger()))
			.addAction(getLastSong(), "", null)
            .addAction(getPlayOfPause(), "", null)
            .addAction(getNextSong(), "", null)
            .setAutoCancel(false)
		    .setCategory(Notification.CATEGORY_SERVICE)
			.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
			.setOngoing(true)
			.setWhen(0)
	    	.setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, getActvity()), PendingIntent.FLAG_UPDATE_CURRENT));	
		Notification notification = builder.build();
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
		notification.sound = null;
		startForeground(1, notification);
	}

	private void setContent(String song, String singer)
	{
		builder.setContentTitle(song);
		builder.setContentText(singer);
		manager.notify(1, builder.getNotification());
	}

	private void setAlbum(Bitmap album)
	{
		if(album!=null){
		builder.setLargeIcon(album);
		manager.notify(1, builder.getNotification());
		}
	}

    public class MusicBinder extends Binder
    {  

        private MusicItem music_TAG;

        private boolean reset=false;

        private onChange change;

        private final List<MusicItem> list=new ArrayList<MusicItem>();

		private final AudioManager audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		private final MediaPlayer mMediaPlayer = new MediaPlayer();

        private final MediaPlayer.OnPreparedListener prepared=new MediaPlayer.OnPreparedListener(){

            @Override
            public void onPrepared(MediaPlayer p1)
            {
				if (change != null)
					change.onEvent(MusicBinder.this, PLAY);
                play();
            }
        };

        private final MediaPlayer.OnErrorListener error =new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1)
            {
				if (change != null)
					change.onEvent(MusicBinder.this, ERROR);
				pause();
                return false;
            }
        };

        private final MediaPlayer.OnCompletionListener completion=new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer)
            {
                nextSong();
            }
        };

		private final AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
			@Override
			public void onAudioFocusChange(int focusChange)
			{
				switch (focusChange)
				{
					case AudioManager.AUDIOFOCUS_LOSS:
						pause();
						audio_manager.abandonAudioFocus(mAudioFocusChange);
						break;
					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
						pause();
						break;
					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
						break;
					case AudioManager.AUDIOFOCUS_GAIN:
						play();
						break;
				}
			}
		};

        public MusicBinder()
        {
			audio_manager.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnPreparedListener(prepared);
            mMediaPlayer.setOnErrorListener(error);
            mMediaPlayer.setOnCompletionListener(completion);
        }

        private void addList(List<MusicItem> list)
        {
            this.list.addAll(list);
        }

        private void emptyLisr()
        {
            list.clear();
        }

        public void playMusic(List<MusicItem> list, int p1)
        {  
            emptyLisr();
            addList(list);
            playMusic(p1);
        }

        public void playMusic(int p1)
        {  
		    if (p1 >= list.size())return;
            music_TAG = list.get(p1);
			setContent(getTitle(), getSinger().get(0).getName());	
			music_TAG.album.observeForever(new Observer<Bitmap>(){

					@Override
					public void onChanged(Bitmap p1)
					{
						MediaService.this.setAlbum(p1);
					}
				});
			try
            {
                if (reset)mMediaPlayer.reset();
                mMediaPlayer.setDataSource(getUrl() + list.get(p1).getId());
                mMediaPlayer.prepareAsync();
                reset = true;
				if (change != null)
					change.onEvent(MusicBinder.this, LOADMUSIC);
            }
            catch (IllegalArgumentException e)
            {}
            catch (IOException e)
            {}
            catch (IllegalStateException e)
            {}
            catch (SecurityException e)
            {}    
        }

        public void setOnChange(onChange change)
        {
            this.change = change;
        }

        public void nextSong()
        {
            if (list.indexOf(music_TAG) < list.size())
            {
                playMusic(list.indexOf(music_TAG) + 1);
				if (change != null)
					change.onEvent(this, NEXT);
            }
        }

        public void lastSong()
        {         
            if (list.indexOf(music_TAG) < list.size() && list.indexOf(music_TAG) >= 1)
            {
                playMusic(list.indexOf(music_TAG) - 1);
				if (change != null)
					change.onEvent(this, LAST);
            }
        }

		public void playOrPause()
		{
			if (reset)
				if (mMediaPlayer.isPlaying())
				{
					pause();
				}
				else play();
		}

        public void play()
        {
            mMediaPlayer.start();
			if (change != null)
				change.onEvent(this, PLAY);
        }

        public void pause()
        {
            mMediaPlayer.pause();
			if (change != null)
				change.onEvent(this, PAUSE);
        }

        public int getDuration()
        {
            return mMediaPlayer.getDuration();
        }

        public int getCurrenPostion()
        {
            return mMediaPlayer.getCurrentPosition();
        }

        public void seekTo(int mesc)
        {
            mMediaPlayer.seekTo(mesc);
			if (change != null)
				change.onEvent(this, SEEK);
        }

        public String getTitle()
        {
            return music_TAG.getTitle();
        }

        public List<SingerItem> getSinger()
        {
            return music_TAG.getSinger();
        }

		public void setAlbum(Bitmap album){
			music_TAG.setAlbum(album);
		}
			
		public MusicItem getMusicItem(){
			return music_TAG;
		}
		
        public Bitmap getAlbum()
        {
            return music_TAG.album.getValue();
        }

		private   void onDestroy()
        {
			stopForeground(true);
            if (reset)
            {
				mMediaPlayer.stop();
				mMediaPlayer.release();
            }      
        }

	}
}
