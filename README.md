![Logo](/assets/logo_w100.png) 
### Social Signal Processing for Android

SSJ is an extensible android framework for social signal processing in an out of lab envirnoment. It packages common signal processing tools in a flexible, mobile friendly Java library which can be easily integrated into Android Apps.

<img align="right" width="220" src="/assets/screen_ssj.png"/>
### Features
* Realtime signal processing using independent components as processing steps in a pipeline
* Synchronized data streams
* Support for most standard android sensors e.g. Camera, Microphone, Acceleration, GPS
* Support for external sensors via bluetooth e.g. Microsoft Band 2, Myo, Angel Sensor, Empatica
* Advanced signal processing functionality, including machine learning approaches (NaiveBayes, SVM)
* I/O functionality: local storage, sockets, bluetooth
* Energy efficient processing thanks to advanced sleep state management and support for discrete data propagation
* Basic GUI elements: graphs (using [GraphView](https://github.com/hcmlab/GraphView) library), camera painter
* SSJ Creator: Android App for building, editing and running SSJ pipelines without writing a single line of code


### Download
 * To use libssj in your own application, simply add the gradle dependency:
```
compile 'com.github.hcmlab:libssj:0.4.4.11'
```
* You can also download the [latest binaries](../../releases/latest) from the [releases section](../../releases)
<a href='https://play.google.com/store/apps/details?id=hcm.ssj.creator&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height="70" align="right"/></a>
* SSJ Creator can be downloaded from the <a href="https://play.google.com/store/apps/details?id=hcm.ssj.creator">play store</a>

### Documentation
* Api (Javadoc): http://hcmlab.github.io/ssj/api

### Getting started
We do not yet have a formal tutorial, however you can have a look at the "demo" module which implements a simple but functional SSJ pipeline.

### About
The Social Signal Processing for Java/Android (SSJ) framework is being developed at the Lab for Human Centered Multimedia of the University of Augsburg. The authors of the framework are: <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/damian/">Ionut Damian</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/dietz/">Michael Dietz</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/_students/gaibler/">Frank Gaibler</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/_formerstudents/langerenken/">Daniel Langerenken</a>, <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/staff/flutura/">Simon Flutura</a>.

SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a one-to-one port of SSI to Java, it is an approximation. Nevertheless, it borrows a lot of programming patterns from SSI and preserves the same vision for signal processing which makes SSI great. It than packages everything in a flexible, mobile friendly Java library which can be easily integrated into Android Apps.

If you use SSJ for a research project, please reference the following paper:
<ul>
  <li>Ionut Damian, Tobias Baur, Elisabeth André, <i>Measuring the Impact of Multimodal Behavioural Feedback Loops on Social Interactions</i>, In Proceedings of International Conference on Multimodal Interaction (ICMI), ACM, 2016<br/>
  <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/publications/2016-ICMI-Damian/">paper</a> / <a href="https://www.informatik.uni-augsburg.de/lehrstuehle/hcm/publications/2016-ICMI-Damian/Damian-BehaviouralFeedbackLoops-2016-bib.txt">BibTex</a> 
  </li>
</ul>

### License
This library is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
