///////////////////////////////////////////////////////////////////////////////////////
///////// > openSMILE configuration file for speech prosody features //////////////////
/////////   pitch and intensity                                      //////////////////
/////////                                                            //////////////////
///////// (c) 2014-2016 audEERING.                                   //////////////////
/////////     All rights reserved. See file COPYING for details.     //////////////////
///////////////////////////////////////////////////////////////////////////////////////

;;;;;;; component list ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

[componentInstances:cComponentManager]
instance[frame].type=cFramer
instance[int].type=cIntensity
instance[win].type=cWindower
instance[fft].type=cTransformFFT
instance[fftmp].type=cFFTmagphase
instance[scale].type=cSpecScale
instance[shs].type=cPitchShs
instance[smooth].type=cPitchSmoother
instance[smo].type=cContourSmoother
printLevelStats=0

;;;;;;;;;;;;;;;;;;;;;;;;;;;; main section ;;;;;;;;;;;;;;;;;;;;;;;;;;;

[frame:cFramer]
reader.dmLevel=wave
writer.dmLevel=outp
frameSize = 0.050
frameStep = 0.010
frameCenterSpecial = left

[int:cIntensity]
reader.dmLevel = outp
writer.dmLevel = intens
// nameAppend =
copyInputName = 1
processArrayFields = 1
intensity = 0
loudness = 1

[win:cWindower]
reader.dmLevel=outp
writer.dmLevel=win
winFunc=gauss
gain=1.0
sigma=0.4

[fft:cTransformFFT]
reader.dmLevel=win
writer.dmLevel=fftc
 ; for compatibility with 2.2.0 and older versions
zeroPadSymmetric = 0

[fftmp:cFFTmagphase]
reader.dmLevel=fftc
writer.dmLevel=fftmag

[scale:cSpecScale]
reader.dmLevel=fftmag
writer.dmLevel=hps
// nameAppend =
copyInputName = 1
processArrayFields = 0
scale=octave
sourceScale = lin
// logScaleBase = 2
// logSourceScaleBase = 2
// firstNote = 55
interpMethod = spline
minF = 25
maxF = -1
nPointsTarget = 0
specSmooth = 1
specEnhance = 1
auditoryWeighting = 1 

[shs:cPitchShs]
reader.dmLevel=hps
writer.dmLevel=pitchShs
// nameAppend =
copyInputName = 1
processArrayFields = 0
maxPitch = 620
minPitch = 52
nCandidates = 4
scores = 1
voicing = 1
F0C1 = 0
voicingC1 = 0
F0raw = 1
voicingClip = 1
voicingCutoff = 0.700000
inputFieldSearch = Mag_logScale
octaveCorrection = 0
nHarmonics = 15
compressionFactor = 0.850000

[smooth:cPitchSmoother]
reader.dmLevel=pitchShs
writer.dmLevel=pitch
// nameAppend =
copyInputName = 1
processArrayFields = 0
medianFilter0 = 0
postSmoothing = 0
postSmoothingMethod = simple
 ; note: octave correction is too agressive, thus we disable it..
octaveCorrection = 0
F0final = 1
F0finalEnv = 0
no0f0 = 0
voicingFinalClipped = 0
voicingFinalUnclipped = 1
F0raw = 0
voicingC1 = 0
voicingClip = 0

[smo:cContourSmoother]
reader.dmLevel = pitch;intens
writer.dmLevel = lld
nameAppend = sma
copyInputName = 1
noPostEOIprocessing = 0
smaWin = 3
