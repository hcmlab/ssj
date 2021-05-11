// Default input (cExternalAudioSource) and output (cExternalSink) components

[componentInstances:cComponentManager]
instance[dataMemory].type=cDataMemory
instance[externalAudioSource].type = cExternalAudioSource
instance[featureConcat].type=cVectorConcat
instance[externalSink].type=cExternalSink
// Detail for data memory information (0 is no information, 5 is maximum detail)
printLevelStats = 0
profiling = 0
nThreads = 1

// External audio source will receive audio input from SSJ pipeline

[externalAudioSource:cExternalAudioSource]
writer.dmLevel = wave
sampleRate = 16000
channels = 1
nBits = 16
nBPS = 0
blocksize = 25600
fieldName = pcm

// External audio sink will provide openSMILE features to SSJ pipeline

[externalSink:cExternalSink]
reader.dmLevel = features