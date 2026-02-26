
#M2=$(HOME)/.m2/repository
#mmbase=1.9-SNAPSHOT

#export CLASSPATH = target/classes:$(M2)/org/mmbase/mmbase/$(mmbase)/mmbase-$(mmbase).jar:$(M2)/org/mmbase/mmbase-media/$(mmbase)/mmbase-media-$(mmbase)-classes.jar:target/mmbase-streams-$(mmbase)-classes.jar:$(M2)/net/sf/fmj/fmj-nojmf/2009-04-16/fmj-nojmf-2009-04-16.jar:$(M2)/javax/media/jmf/2.1.1e/jmf-2.1.1e.jar




.PHONY:
samples: samples/basic.mpg samples/basic.mov samples/basic.mp3 samples/basic.mp4 samples/basic.png samples/basic.flv samples/basic.jpg samples/basic.wav \
	samples/basic2.mpg \
	samples/unknown.wav \
	samples/unsupported.rm


# 	samples/testbestanden/audio/wav_logic_trans.wav \
# 	samples/testbestanden/audio/m4a_itunes_trans.m4a \
# 	samples/testbestanden/audio/ogg_audacity_trans.ogg \
# 	samples/testbestanden/audio/m4a_logic_trans.m4a \
# 	samples/testbestanden/audio/mp3_logic_trans.mp3 \
# 	samples/testbestanden/audio/new/aiff_tvis_trans.aiff \
# 	samples/testbestanden/audio/mp3_audacity_trans.mp3 \
# 	samples/testbestanden/audio/mp3_itunes_trans.mp3 \
# 	samples/testbestanden/audio/m4a_itunes_org.m4a \
# 	samples/testbestanden/audio/flac_ccmixter_org.flac \
# 	samples/testbestanden/audio/mp3_jamendo_trans.mp3 \
# 	samples/testbestanden/audio/mp3_simuze_trans.mp3 \
# 	samples/testbestanden/audio/mp3_ccmixter_trans.mp3 \
# 	samples/testbestanden/audio/m4a_iphone_org.m4a \
# 	samples/testbestanden/audio/m4a_itunes_trans2.m4a \
# 	samples/testbestanden/audio/aiff_logic_trans.aif \
# 	samples/testbestanden/afbeeldingen/jpg_photobooth_org.jpg \
# 	samples/testbestanden/afbeeldingen/tiff_canon_trans2.TIF \
# 	samples/testbestanden/afbeeldingen/new/gif_paint_trans.GIF \
# 	samples/testbestanden/afbeeldingen/new/jpg_paint_trans.JPG \
# 	samples/testbestanden/afbeeldingen/new/png_paint_trans.PNG \
# 	samples/testbestanden/afbeeldingen/new/bmp_paint_org.bmp \
# 	samples/testbestanden/afbeeldingen/new/tiff_paint_trans.TIF \
# 	samples/testbestanden/afbeeldingen/jpg_iphone_org.jpg \
# 	samples/testbestanden/afbeeldingen/tiff_canon_trans1.TIF \
# 	samples/testbestanden/afbeeldingen/jpg_canon_trans.jpg \
# 	samples/testbestanden/afbeeldingen/jpg_canon_org.JPG \
# 	samples/testbestanden/afbeeldingen/bmp_preview_trans.bmp \
# 	samples/testbestanden/afbeeldingen/jpg_sony_org.JPG \
# 	samples/testbestanden/afbeeldingen/png_preview_trans.png \
# 	samples/testbestanden/afbeeldingen/tiff_preview_trans.tiff \
# 	samples/testbestanden/afbeeldingen/jpg_flick_trans.jpg \
# 	samples/testbestanden/video/.flv_youtube_trans.flv.jGKona \
# 	samples/testbestanden/video/flv_blip_trans.flv



samples/%:
	(mkdir -p samples; cd samples ; curl --create-dirs -o $* -L https://www.mmbase.org/samples/$*)



samples/mahnamahna.broken: samples/mahnamahna.mpg
	cp samples/mahnamahna.mpg samples/mahnamahna.broken


.PHONY:
test.theora: samples/mahnamahna.mpg
	mkdir -p results
	java -cp $(CLASSPATH) org.mmbase.streams.transcoders.FFMpeg2TheoraTranscoder samples/mahnamahna.mpg  results/mahnamahna.ogv

test.avi: samples/mahnamahna.mpg
	mkdir -p results
	java org.mmbase.streams.transcoders.FFMpegTranscoder samples/mahnamahna.mpg  results/mahnamahna.avi

test.h264: samples/mahnamahna.mpg
	mkdir -p results
	java org.mmbase.streams.transcoders.FFMpegTranscoder samples/mahnamahna.mpg  results/mahnamahna.h264

test.recognize: samples/mahnamahna.mpg samples/mahnamahna.broken
#	java org.mmbase.streams.transcoders.FFMpegRecognizer samples/mahnamahna.mpg
#	java org.mmbase.streams.transcoders.FFMpegRecognizer samples/test.mp3
#	java org.mmbase.streams.transcoders.FFMpegRecognizer samples/test.jpeg
#	java org.mmbase.streams.transcoders.FFMpegRecognizer samples/mahnamahna.broken
#	java org.mmbase.streams.transcoders.FFMpegRecognizer samples/odd.ogg
	java org.mmbase.streams.transcoders.FFMpegRecognizer audio:samples/testaudio.ogg


transcode:
	java Transcode samples/mahnamahna.mpg  -o results/mahnamahna.avi

clean:
	rm -r results
