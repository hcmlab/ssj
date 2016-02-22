package hcm.ssj.mobileSSI;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.stream.FloatStream;
import hcm.ssj.core.stream.Stream;

import static java.lang.System.loadLibrary;

/**
 * File writer for SSJ.<br>
 * Created by Frank Gaibler on 20.08.2015.
 */
public class MobileSSIConsumer extends Consumer
{
    /**
     *
     */
    public class Options
    {

    }

    public Options options = new Options();

    public MobileSSIConsumer()
    {
        _name = "SSJ_consumer_" + this.getClass().getSimpleName();
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void enter(Stream[] stream_in)
    {
        if (stream_in.length > 1 || stream_in.length < 1)
        {
            Log.e(_name, "stream count not supported");
            return;
        }
        if (stream_in[0].type == Cons.Type.CUSTOM || stream_in[0].type == Cons.Type.UNDEF)
        {
            Log.e(_name, "stream type not supported");
            return;
        }
        start(stream_in[0]);
    }

    /**
     * @param stream Stream
     */
    private void start(Stream stream)
    {

    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    protected final void consume(Stream[] stream_in)
    {
        float[] floats = stream_in[0].ptrF();
            stream_in[0].ptrF()[0]=0.5f;
        //ssi_ssj_sensor
			pushData(stream_in[0], getId());
    }

    /**
     * @param stream_in Stream[]
     */
    @Override
    public final void flush(Stream stream_in[])
    {

    }

    @Override
    public final void init(Stream stream_in[])
    {
        int t=0;
        if(stream_in[0].type== Cons.Type.FLOAT)
            t=9;
        setStreamOptions(getId(), stream_in[0].num, stream_in[0].dim, t, stream_in[0].sr);
    }


    public void setId(int _id)
    {
        id=_id;
    }
    public int getId()
    {return id;}

static{
    loadLibrary("ssiframe");
    loadLibrary("ssievent");
    loadLibrary("ssiioput");
    loadLibrary("ssiandroidsensors");
    loadLibrary("ssimodel");
    loadLibrary("ssisignal");
    loadLibrary("ssissjSensor");
    loadLibrary("android_xmlpipe");
}
    public native void setSensor(Object sensor, Stream[] stream, int id);
    public native void pushData(Stream stream, int id);
    public native void setStreamOptions(int id, int num, int dim, int type, double sr);

    private int id=0;



}
