/*
 * TimeBuffer.java
 * Copyright (c) 2018
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.core;

import java.util.Arrays;

/**
 * Created by Johnny on 16.03.2015.
 */
public class TimeBuffer {

    public final static int STATUS_SUCCESS = 0;
    public final static int STATUS_INPUT_ARRAY_TOO_SMALL = -1;
    public final static int STATUS_DATA_EXCEEDS_BUFFER_SIZE = -2;
    public final static int STATUS_DATA_NOT_IN_BUFFER_YET = -3;
    public final static int STATUS_DATA_NOT_IN_BUFFER_ANYMORE = -4;
    public final static int STATUS_DURATION_TOO_SMALL = -5;
    public final static int STATUS_DURATION_TOO_LARGE = -6;
    public final static int STATUS_UNKNOWN_DATA = -7;
    public final static int STATUS_ERROR = -9; //unknown error, buffer is probably closed

    private byte[] _buffer;
    private long _position;

    private final Object _lock = new Object();
    private boolean _terminate = false;

    private double _sr;
    private int _dim;
    private int _bytesPerValue;
    private Cons.Type _type;

    private int _capacitySamples;
    private int _bytesPerSample;
    private double _sampleDuration;

    private int _offsetSamples;
    private int _lastAccessedSample;

    private Provider _owner;

    public TimeBuffer(double capacity, double sr, int dim, int bytesPerValue, Cons.Type type, Provider owner)
    {
        _owner = owner;

        _sr = sr;
        _dim = dim;
        _bytesPerValue = bytesPerValue;
        _type = type;

        _capacitySamples = (int)(capacity * sr);
        _bytesPerSample = bytesPerValue * dim;

        _sampleDuration = 1.0 / _sr;

        _buffer = new byte[_capacitySamples * _bytesPerSample];

        reset();
    }

    public void reset()
    {
        _position = 0;
        _offsetSamples = 0;
        _lastAccessedSample = 0;

        _terminate = false;
    }

    public void close()
    {
        _terminate = true;

        synchronized (_lock) {
            _lock.notifyAll();
        }
    }

    public void push(Object data, int numBytes)
    {
        synchronized (_lock) {
            //compute actual position of data within buffer
            int pos_mod = (int)(_position % _buffer.length);

            copy(data, 0, _buffer, pos_mod, numBytes);

            _position += numBytes;
            _lock.notifyAll();
        }
    }

    private void copy(Object src, int srcpos, byte[] dst, int dstpos, int numBytes)
    {
        if (dstpos + numBytes <= dst.length) {
            // end of buffer not reached
            // copy data in one step
            Util.arraycopy(src, srcpos, dst, dstpos, numBytes);
        } else {
            // end of buffer reached
            // copy data in two steps:
            // 1. copy everything until the end of the buffer is reached
            // 2. copy remaining part from the beginning
            int size_until_end = dst.length - dstpos;
            int size_remaining = numBytes - size_until_end;
            Util.arraycopy(src, srcpos, dst, dstpos, size_until_end);
            copy(src, size_until_end, dst, 0, size_remaining);
        }
    }

    public void pushZeroes(int numBytes)
    {
        Log.w(_owner.getComponentName(), "pushing " + numBytes + " bytes of zeroes");

        synchronized (_lock) {
            //compute actual position of data within buffer
            int pos_mod = (int)(_position % _buffer.length);

            fillZero(_buffer, pos_mod, numBytes);

            _position += numBytes;
            _lock.notifyAll();
        }
    }

    private void fillZero(byte[] buffer, int pos, int num)
    {
        if (pos + num <= buffer.length)
            Arrays.fill(buffer, pos, pos + num, (byte)0);
        else
        {
            // end of buffer reached
            // copy data in two steps:
            // 1. copy everything until the end of the buffer is reached
            // 2. copy remaining part from the beginning
            int size_until_end = buffer.length - pos;
            int size_remaining = num - size_until_end;
            Arrays.fill(buffer, pos, pos + size_until_end, (byte)0);
            fillZero(buffer, 0, size_remaining);
        }
    }

    private boolean get_(Object dst, long pos, int len)
    {
        synchronized (_lock) {
            //wait for requested data to become available
            while (pos + len > _position && !_terminate) {
                try {
                    _lock.wait();
                } catch (InterruptedException e) {
                    Log.w("thread interrupt");
                }
            }

            if(_terminate)
                return false;

            //compute actual position of data within buffer
            int pos_mod = (int)(pos % _buffer.length);

            if (pos_mod + len <= _buffer.length) {
                // end of buffer not reached
                // copy data in one step
                Util.arraycopy(_buffer, pos_mod, dst, 0, len);
            } else {
                // end of buffer reached
                // copy data in two steps:
                // 1. copy everything until the end of the buffer is reached
                // 2. copy remaining part from the beginning
                int size_until_end = _buffer.length - pos_mod;
                int size_remaining = len - size_until_end;
                Util.arraycopy(_buffer, pos_mod, dst, 0, size_until_end);
                Util.arraycopy(_buffer, 0, dst, size_until_end, size_remaining);
            }
        }

        return true;
    }

    public int get(Object dst, int startSample, int numSamples)
    {
        //correct position for sync
        startSample -= _offsetSamples;

        // check if requested duration is too small
        if (numSamples == 0) {
            return STATUS_DURATION_TOO_SMALL;
        }

        // check if requested duration is too large
        if (numSamples > _capacitySamples) {
            return STATUS_DURATION_TOO_LARGE;
        }

        if (startSample < 0) {
            return STATUS_UNKNOWN_DATA;
        }

        // Check if requested data is still available (checks if startSample is older than whole buffer size of samples before current position)
        if (startSample + _capacitySamples < _position / _bytesPerSample || startSample < 0) {
            return STATUS_DATA_NOT_IN_BUFFER_ANYMORE;
        }

        boolean ok = get_(dst, (long)startSample * _bytesPerSample, numSamples * _bytesPerSample);

        _lastAccessedSample = startSample + numSamples - 1;

        if(ok) return STATUS_SUCCESS;
        else return STATUS_ERROR;
    }

    public int get(Object dst, double start_time, double duration)
    {
        int pos = (int)(start_time * _sr + 0.5);
        int pos_stop = (int)((start_time + duration) * _sr + 0.5);
        int len = pos_stop - pos;

        return get(dst, pos, len);
    }

    public void sync(double time)
    {
        setReadTime(time);
    }

    public double getReadTime()
    {
        long positionSamples = _position / _bytesPerSample;
        return (_offsetSamples + positionSamples) * _sampleDuration;
    }

    public void setReadTime(double time)
    {
        double delta = getReadTime() - time;
        _offsetSamples -= (int)(delta * _sr + 0.5);
    }

    public long getPositionAbs()
    {
        return _position;
    }

    public int getCapacity()
    {
        return _buffer.length;
    }

    public double getLastAccessedSampleTime ()
    {
        return (_offsetSamples + _lastAccessedSample) * _sampleDuration;
    }

    public double getLastWrittenSampleTime ()
    {
        return (_offsetSamples + (_position / _bytesPerSample)) * _sampleDuration;
    }

    public double getSampleRate ()
    {
        return _sr;
    }
    public int getBytesPerSample ()
    {
        return _bytesPerSample;
    }
    public int getBytesPerValue ()
    {
        return _bytesPerValue;
    }

    public Provider getOwner()
    {
        return _owner;
    }

    public int getOffsetSamples()
    {
        return _offsetSamples;
    }
}
