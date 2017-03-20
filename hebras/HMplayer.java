import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;


/**
 * Created by Emilio Chica Jiménez on 14/01/2016.
 */
public class HMplayer extends AsyncTask<Void, Integer, Integer> {
        Context context;
    MediaPlayer mPlayer;

    public HMplayer(Context context, MediaPlayer mPlayer){
        this.context = context;
        this.mPlayer = mPlayer;


    }

    @Override
    protected Integer doInBackground(Void... params) {
        mPlayer.start();
        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        // durante la ejecucion -- para la barra
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onPostExecute(Integer result) {

    }

    @Override
    protected void onCancelled() {

    }

}

