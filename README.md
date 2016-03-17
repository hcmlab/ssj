![Logo](/assets/logo_w100.png) 
### Social Signal Interpretation for Java/Android

Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken

The Social Signal Interpretation for Java/Android (SSJ) framework is beeing developed at the Lab for Human Centered Multimedia of the University of Augsburg.
SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a one-to-one port of SSI to Java, it is an approximation. Nevertheless, it borrows a lot of programming patterns from SSI and preserves the same vision for signal processing which makes SSI great. It than packages everything in a flexible, mobile friendly Java library which can be easily integrated into Android Apps.

### Features
* Realtime signal processing using independent components as processing steps in a pipeline
* Synchornized data streams
* Support for most standard android sensors (Camera, Microphone, Acceleration, ...)
* Support for external sensors via bluetooth (e.g. Myo, Empatica)
* I/O functionality: local storage, sockets, bluetooth
* Basic GUI elements: graphs (using [GraphView](https://github.com/hcmlab/GraphView) library), camera painter

### Documentation
* Api (Javadoc): http://hcmlab.github.io/ssj/api

### License
This library is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

### External Libraries
* [GraphView](https://github.com/hcmlab/GraphView) ([License](https://github.com/hcmlab/GraphView/blob/master/license.txt))
* [TarsosDSP](https://github.com/JorenSix/TarsosDSP) ([License](https://github.com/JorenSix/TarsosDSP/blob/master/license.txt))
* [PRAAT](http://www.fon.hum.uva.nl/praat/) ([License](http://www.fon.hum.uva.nl/praat/GNU_General_Public_License.txt))
