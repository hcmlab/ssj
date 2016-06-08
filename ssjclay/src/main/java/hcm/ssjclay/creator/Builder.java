package hcm.ssjclay.creator;

import hcm.ssj.androidSensor.AndroidSensor;
import hcm.ssj.androidSensor.AndroidSensorProvider;
import hcm.ssj.androidSensor.transformer.AvgVar;
import hcm.ssj.androidSensor.transformer.Count;
import hcm.ssj.androidSensor.transformer.Distance;
import hcm.ssj.androidSensor.transformer.Median;
import hcm.ssj.androidSensor.transformer.MinMax;
import hcm.ssj.androidSensor.transformer.Progress;
import hcm.ssj.audio.AudioConvert;
import hcm.ssj.audio.AudioProvider;
import hcm.ssj.audio.AudioWriter;
import hcm.ssj.audio.Energy;
import hcm.ssj.audio.Microphone;
import hcm.ssj.audio.Pitch;
import hcm.ssj.audio.SpeechRate;
import hcm.ssj.biosig.GSRArousalCombination;
import hcm.ssj.biosig.GSRArousalEstimation;
import hcm.ssj.body.Activity;
import hcm.ssj.camera.CameraPainter;
import hcm.ssj.camera.CameraProvider;
import hcm.ssj.camera.CameraSensor;
import hcm.ssj.camera.CameraWriter;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.EventConsumer;
import hcm.ssj.core.Sensor;
import hcm.ssj.core.SensorProvider;
import hcm.ssj.core.Transformer;
import hcm.ssj.empatica.AccelerationProvider;
import hcm.ssj.empatica.BVPProvider;
import hcm.ssj.empatica.BatteryProvider;
import hcm.ssj.empatica.Empatica;
import hcm.ssj.empatica.GSRProvider;
import hcm.ssj.empatica.IBIProvider;
import hcm.ssj.empatica.TemperatureProvider;
import hcm.ssj.evaluator.Cull;
import hcm.ssj.evaluator.NaiveBayes;
import hcm.ssj.event.FloatSegmentEventSender;
import hcm.ssj.event.FloatsEventSender;
import hcm.ssj.event.ThresholdEventSender;
import hcm.ssj.file.SimpleFileReader;
import hcm.ssj.file.SimpleFileReaderProvider;
import hcm.ssj.file.SimpleFileWriter;
import hcm.ssj.glass.InfraredProvider;
import hcm.ssj.glass.InfraredSensor;
import hcm.ssj.glass.transformer.BlinkDetection;
import hcm.ssj.graphic.SignalPainter;
import hcm.ssj.ioput.BluetoothProvider;
import hcm.ssj.ioput.BluetoothReader;
import hcm.ssj.ioput.BluetoothWriter;
import hcm.ssj.ioput.SocketWriter;
import hcm.ssj.mobileSSI.MobileSSIConsumer;
import hcm.ssj.myo.DynAccelerationProvider;
import hcm.ssj.myo.EMGProvider;
import hcm.ssj.myo.Myo;
import hcm.ssj.praat.Intensity;
import hcm.ssj.signal.Avg;
import hcm.ssj.signal.Butfilt;
import hcm.ssj.signal.Envelope;
import hcm.ssj.signal.IIR;
import hcm.ssj.signal.MvgAvgVar;
import hcm.ssj.signal.MvgMinMax;
import hcm.ssj.signal.MvgNorm;
import hcm.ssj.signal.Selector;
import hcm.ssj.signal.Serializer;
import hcm.ssj.test.Logger;

/**
 * Builds pipelines.<br>
 * Created by Frank Gaibler on 09.03.2016.
 */
public class Builder
{
    public static final Class<Sensor>[] sensors = new Class[]{
            AndroidSensor.class, Microphone.class, CameraSensor.class, Empatica.class,
            SimpleFileReader.class, InfraredSensor.class, BluetoothReader.class, Myo.class,
    };
    public static final Class<SensorProvider>[] sensorProviders = new Class[]{
            AndroidSensorProvider.class, AudioProvider.class, CameraProvider.class,
            AccelerationProvider.class, BatteryProvider.class, BVPProvider.class,
            GSRProvider.class, IBIProvider.class, TemperatureProvider.class,
            SimpleFileReaderProvider.class, InfraredProvider.class, BluetoothProvider.class,
            hcm.ssj.myo.AccelerationProvider.class, DynAccelerationProvider.class, EMGProvider.class,
    };
    public static final Class<Transformer>[] transformers = new Class[]{
            AvgVar.class, Count.class, Distance.class, Median.class, MinMax.class, Progress.class,
            AudioConvert.class, Energy.class, Pitch.class, SpeechRate.class,
            GSRArousalCombination.class, GSRArousalEstimation.class, Activity.class,
            Cull.class, NaiveBayes.class, BlinkDetection.class, Intensity.class, Avg.class,
            Butfilt.class, Envelope.class, IIR.class, MvgAvgVar.class, MvgMinMax.class,
            MvgNorm.class, Selector.class, Serializer.class,
    };
    public static final Class<Consumer>[] consumers = new Class[]{
            AudioWriter.class, CameraPainter.class, CameraWriter.class, FloatsEventSender.class,
            SimpleFileWriter.class, SignalPainter.class, BluetoothWriter.class,
            SocketWriter.class, MobileSSIConsumer.class, Logger.class,
    };
    public static final Class<EventConsumer>[] eventConsumers = new Class[]{
            FloatSegmentEventSender.class, ThresholdEventSender.class,
    };

    /**
     * @param clazz Class
     * @return Object
     */
    public static Object instantiate(Class clazz)
    {
        try
        {
            return clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
