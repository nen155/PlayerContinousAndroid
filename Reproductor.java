
import hebras.HMplayer;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.SeekBar;
import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by Emilio Chica Jiménez on 03/08/2016.
 */
public class Reproductor {
    private MediaPlayer mp;
	//Listado de pistas de audio
    private String[] tracks;
    private int currentTrack = 0;
    private Context context;
	//Guarda el milisegundo por el que se ha pausado una pista
    private int pauseMoment;
    private boolean pause=false;
	//Si se avanza con el seekbar guarda la posicion por donde esta
    private int currentPosition=0;
	//Listado de duraciones de las pistas
    private int[] durations;
	//Variable para seguir el patrón Singleton
    private static Reproductor reproductor=null;
	//Manejador para actualizar la seekbar
    private Handler mHandler = new Handler();
    private SeekBar progressBar =null;
	//El total de la duracion de las pistas
    private int durationTracks=0;
	//Sirve para comprobar si te están llamando y pausar la reproducción
    private TelephonyManager mgr;

    /**
     * Constructor privado, patron Singleton
     */
    private Reproductor(){
    }

    /**
     * Instancia del patron Singleton
     * @return
     */
    public static Reproductor getInstance(){
        if(reproductor==null){
            reproductor = new Reproductor();
        }else {

        }
        return reproductor;
    }

    public Context getContext() {
        return context;
    }

    /**
     * Es obligatorio establecer el contexto del Reproductor antes de usar nada
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    public String[] getTracks() {
        return tracks;
    }

    /**
     * Para el reproductor y establece las pistas y su duracion
     * @param tracks
     */
    public void setTracks(String[] tracks){
        stop();
        this.tracks = tracks;
        this.durations = new int[tracks.length];
        durationTracks = getDurationList();
    }

    /**
     * Devuelve la duración de las pistas en un total
     * @return
     */
    private int getDurationList(){
        int duration=0;

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();

       for(int i=0;i<tracks.length;++i) {
            Uri mediaPath = Uri.parse(tracks[i]);
            mmr.setDataSource(context,mediaPath);
            durations[i] = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            duration += durations[i];

        }
        durationTracks=duration;
        return duration;
    }

    /**
     * Devuelve de la pista por la que se encuentra el milisegundo por el que se encuentra
     * @return
     */
    public int currentPositionInList(){
        int current =0;
        if(mp!=null) {
            for (int i = 0; i < currentTrack; ++i)
                current += durations[i];
            currentPosition = current + mp.getCurrentPosition();
        }
        return currentPosition;
    }

    public int getDurationTracks() {
        return durationTracks;
    }

    public int[] getDurations() {
        return durations;
    }

    public void setDurations(int[] durations) {
        this.durations = durations;
    }

    public int getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(int currentTrack) {
        this.currentTrack = currentTrack;
    }

    public boolean isPlaying(){
        boolean flag=false;
        if(mp!=null)
            flag=mp.isPlaying();
        return flag;
    }

    public boolean isPause() {
        return pause;
    }
    /**
     * Reproduce toda la lista en una hebra
     */
    public void play(){
        if(mp==null) {
            try {
               mgr = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
                if(mgr != null) {
                    mgr.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
                }
                Uri file = Uri.parse(tracks[this.currentTrack]);
                mp = new MediaPlayer();
                mp.setDataSource(context, file);
                mp.prepare();
                new HMplayer(context, mp).execute();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        currentTrack = (currentTrack + 1);
                        if (currentTrack < tracks.length) {
                            Uri nextTrack = Uri.parse(tracks[currentTrack]);
                            try {
                                mp.reset();
                                mp.setDataSource(context, nextTrack);
                                mp.prepare();
                                new HMplayer(context, mp).execute();
                                pause = false;
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }else {
                            stop();
                        }

                    }

                });
                pause = false;
                updateSeekBar();
            } catch (Exception e) {
                Log.e("Reproducir", "Player failed", e);
            }
        }
    }

    /**
     * Pausa la reproducción si hay algo reproduciendose
     */
    public void pause(){
        if(mp !=null && mp.isPlaying()) {
            pauseMoment = mp.getCurrentPosition();
            mp.pause();
            pause=true;
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
    }

    /**
     * Para la reproducción y libera el reproductor
     */
    public void stop(){
        if(mp !=null) {
            mp.setOnCompletionListener(null);
            mp.release();
            mp = null;
            pause=false;
            currentTrack=0;
            mHandler.removeCallbacks(mUpdateTimeTask);
            progressBar.setProgress(0);
            if(mgr != null) {
                mgr.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
            }
        }
    }

    /**
     * Reanuda la reproducción por donde se pausó
     */
    public void resume() {
        if(mp !=null&& !mp.isPlaying()) {
            mp.seekTo(pauseMoment);
            new HMplayer(context, mp).execute();
            pause=false;
            updateSeekBar();
        }
    }

    /**
     * Comprueba si la lista de pistas pasada actual es diferente a la que tiene
     * @param list
     * @return
     */
    public boolean compareList(String[] list){
        boolean flag=true;
            if(tracks!=null) {
                if (list.length != tracks.length)
                    flag = false;
                for (int i = 0; i < list.length && flag; ++i) {
                    if (tracks[i].compareTo(list[i]) != 0)
                        flag = false;
                }
            }else
                flag=false;
        return flag;
    }

    /**
     * Devuelve la barra de progreso
     * return
     * */
    public SeekBar getProgressBar() {
        return progressBar;
    }
    /**
     * Cambia el progreso a tiempo en milisegundos
     * @param progress -
     * @param totalDuration
     * returns current duration in milliseconds
     * */
    private int progressToTimer(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration = (int) (totalDuration / 1000);
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);

        // return current duration in milliseconds
        return currentDuration * 1000;
    }

    /**
     * Establece la barra de progreso y el escuchador para actualizar su valor
     * @param progressBar
     */
    public void setProgressBar(SeekBar progressBar) {
        this.progressBar = progressBar;

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // remove message Handler from updating progress bar
                if(mHandler!=null && mUpdateTimeTask!=null)
                    mHandler.removeCallbacks(mUpdateTimeTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(mHandler!=null && mUpdateTimeTask!=null)
                    mHandler.removeCallbacks(mUpdateTimeTask);
                if(mp!=null) {
                    int totalDuration = mp.getDuration();
                    int currentPosition = progressToTimer(seekBar.getProgress(), totalDuration);

                    // forward or backward to certain seconds
                    if (mp != null && mp.isPlaying())
                        mp.seekTo(currentPosition);

                    // update timer progress again
                    updateSeekBar();
                }
            }
        });
    }

    /**
     * Tarea que actualiza el valor del progressBar cada 100 milisegundos
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            progressBar.setProgress((int)(((double)reproductor.currentPositionInList()/durationTracks)*100));
            mHandler.postDelayed(this, 100);
        }
    };

    /**
     * Ejecuta la tarea de actualizar el progressBar a los 100 milisegundos
     */
    public void updateSeekBar(){
        if(mHandler!=null && mUpdateTimeTask!=null)
            mHandler.postDelayed(mUpdateTimeTask, 100);
    }



    /**
     * Sirve para controlar si alguien te esta llamando y pausar la reproduccion o volver a reproduccir cuando acabe
     */
    private PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING: {
                        Reproductor reproductor = Reproductor.getInstance();
                        reproductor.pause();
                        break;
                    }
                    case TelephonyManager.CALL_STATE_OFFHOOK: {
                        break;
                    }
                    case TelephonyManager.CALL_STATE_IDLE: {
                        //PLAY
                        Reproductor reproductor = Reproductor.getInstance();
                        reproductor.resume();
                        break;
                    }
                    default: { }
                }
            } catch (Exception ex) {

            }
        }
    };
}
