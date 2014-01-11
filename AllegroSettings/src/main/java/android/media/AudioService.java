package android.media;

/**
 * Created by Renard on 31.10.13.
 */
public class AudioService {
    public int getRemoteStreamMaxVolume() {
        return 0;
    }

    public int getRemoteStreamVolume() {
        return 0;
    }

    public void setRemoteStreamVolume(int i)
    {}

    public boolean isStreamAffectedByRingerMode(int i)
    {
        return false;
    }

    public void disableSafeMediaVolume() {}
}
