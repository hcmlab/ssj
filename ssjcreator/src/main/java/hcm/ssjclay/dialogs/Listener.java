package hcm.ssjclay.dialogs;

/**
 * Created by Frank Gaibler on 21.03.2016.
 */
public abstract class Listener
{
    public abstract void onPositiveEvent(Object[] o);

    public abstract void onNegativeEvent(Object[] o);
}
