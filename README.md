![Logo](/assets/logo_w100.png) 
### Social Signal Processing for Android

SSJ is an extensible android framework for social signal processing in an ouț of lab envirnoment. It packages common signal processing tools in a flexible, mobile friendly Java library which can be easily integrated into Android Apps.

<img align="right" src="/assets/screen_ssj_vsmall.png"/>
### Features
* Realtime signal processing using independent components as processing steps in a pipeline
* Synchornized data streams
* Support for most standard android sensors 
  * e.g. Camera, Microphone, Acceleration, GPS
* Support for external sensors via bluetooth 
  * e.g. Microsoft Band 2, Myo, Angel Sensor, Empatica
* I/O functionality: local storage, sockets, bluetooth
* Basic GUI elements: graphs (using [GraphView](https://github.com/hcmlab/GraphView) library), camera painter
* <b>[NEW]</b> SSJ Creator: Android App for building, editing and runnning SSJ pipelines without writing a single line of code

### Download
* You can download the [latest binaries](../../releases/latest) for both libssj and SSJCreator from the [releases section](../../releases)

### Documentation
* Api (Javadoc): http://hcmlab.github.io/ssj/api

### Getting started
We do not yet have a formal tutorial, however you can have a look at the "demo" module which implements a simple but functional SSJ pipeline.

### About
The Social Signal Processing for Java/Android (SSJ) framework is beeing developed at the Lab for Human Centered Multimedia of the University of Augsburg. The authors or the framework are: <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/damian/">Ionut Damian</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/dietz/">Michael Dietz</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/_students/gaibler/">Frank Gaibler</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/_formerstudents/langerenken/">Daniel Langerenken</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/flutura/">Simon Flutura</a>.

SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a one-to-one port of SSI to Java, it is an approximation. Nevertheless, it borrows a lot of programming patterns from SSI and preserves the same vision for signal processing which makes SSI great. It than packages everything in a flexible, mobile friendly Java library which can be easily integrated into Android Apps.

If you use SSJ for a research project, please consider referencing the following paper:
<ul>
  <li>I. Damian, E. André, <i>Exploring the Potential of Realtime Haptic Feedback during Social Interactions</i>, In Proceedings of Tangible, Embedded and Embodied Interaction (TEI), ACM, 2016<br/>
  <a href="http://dl.acm.org/citation.cfm?id=2856519">dl.acm.org</a> / <a href="http://dl.acm.org/downformats.cfm?id=2856519&parent_id=2839462&expformat=bibtex">BibTex</a></li>
</ul>

### License
This library is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
