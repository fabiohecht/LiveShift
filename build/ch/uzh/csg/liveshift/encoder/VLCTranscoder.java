package net.liveshift.encoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VLCTranscoder {

	/*
	 * This options allows to specify the codec the video tracks of the input stream should be transcoded to
	 */
	private String vcodec;
	
	private Map<String, String> availableVCodecs = new HashMap<String, String>();

	public Map<String, String> getAvailableVCodecs() {
		return availableVCodecs;
	}


	public Map<String, String> getAvailableACodecs() {
		return availableACodecs;
	}


	/*
	 * This option allows to set the bitrate of the transcoded video stream, in kbit/s
	 */
	private float vb;

	/*
	 * This allows to set the encoder to use to encode the videos stream
	 * 
	 * ffmpeg: this is the libavcodec encoding module. It handles a large variety of different codecs (the list can be found on the streaming features page.
	 * Item options are: keyint=<number of frames> allows to set the maximal amount of frames between 2 key frames, hurry-up allows the encoder to decrease the quality of the stream if the CPU can't keep up with the encoding rate, interlace allows to improve the quality of the encoding of interlaced streams, noise-reduction=<noise reduction factor> enables a noise reduction algorithm (will decrease required bitrate at the cost of details in the image), vt=<bitrate tolerance in kbit/s> allows to set a tolerance for the bitrate of the outputted video stream, bframes=<amount of frames> allows to set the amount of B frames between 2 key frames, qmin=<quantizer> allows to set the minimum quantizer scale, qmax=<quantizer> allows to set the maximum quantizer scale, qscale=<quantizer scale> allows to specify a fixed quantizer scale for VBR encodings, i-quant-factor=<quantization factor> allows to set the quantization factor of I frames, compared to P frames, hq=<quality> allows to choose the quality level for the encoding of the motion vectors (arguments are simple, rd or bits, default is simple *FIXME*), strict=<level of compliance> allows to force a stricter standard compliance (possible values are -1, 0 and 1, default is 0), strict-rc enables a strict rate control algorithm, rc-buffer-size=<size of the buffer in bits> allows to choose the size of the buffer used for rate control (bigger means more efficient rate control), rc-buffer-aggressivity=<float representing the aggressiveness> allows to set the rate control buffer aggressiveness *FIXME*, pre-me allows to enable pre motion estimation, mpeg4-matrix enable use of the MPEG4 quantization matrix with MPEG2 streams, improving quality while keeping compatibility with MPEG2 decoders, trellis enables trelli quantization (better quality, but slower processing).
	 * theora: The Xiph.org theora encoder. The module is used to produce theora streams. Theora is a free patent and royalties free video codec.
	 * 		The only available item option is quality=<quality level>. This option allows to create a VBR stream, overriding vb setting. the quality level must be an integer between 1 and 10. Higher is better.
	 * x264. x264 is a free open-source h264 encoder. h264 (or MPEG4-AVC) is a quite recent high quality video codec.
	 * 		Item options are: keyint=<number of frames> allows to set the maximal amount of frames between 2 key frames, idrint=<number of frames> allows to set the maximal amount of frames between 2 IDR frames, bframes=<amount of frames> allows to set the amount of B frames between an I and a P frame, qp=<quantizer parameter> allows to specify a fixed quantizer (between 1 and 51), qp-max=<quantizer parameter> allows to set the maximum value for the quantizer, qp-min=<quantizer parameter> allows to set the minimum value for the quantizer, cabac enables the CABAC (Context-Adaptive Binary Arithmetic Coding) algorithm (slower, but enhances quality), loopfilter enables deblocking loop filter, analyse enables the analyze mode, frameref=<amount of frames> allows to set the number of previous frames used as predictors, scenecut=<sensibility< allows to control how aggressively the encoder should insert extra I-frame, on scene change.
	 */
	private String venc;

	/*
	 * This options allows to set the framerate of the transcoded video, in frame per second. reducing the framerate of a video can help decreasing its bitrate
	 */
	private int fps;

	/*
	 * This option allows to enable deinterlacing of interlaced video streams before encoding 
	 */
	private boolean deinterlace;

	/*
	 * This option allows to crop the upper part of the source video while transcoding. The argument is the number of lines the video should be cropped 
	 */
	private int croptop;

	/*
	 * This option allows to crop the lower part of the source video. The argument is the Y coordinate of the first line to be cropped 
	 */
	private int cropbottom;

	/*
	 * This option allows to crop the left part of the source video while transcoding. The argument is the number of columns the video should be cropped
	 */
	private int cropleft;

	/*
	 * This option allows to crop the right part of the source video. The argument is the X coordinate of the first column to be cropped 
	 */
	private int cropright;

	/*
	 * This option allows the give the ratio from which the video should be rescaled while being transcoded. This option can be particularly useful to help reduce the bitrate of a stream 
	 */
	private double scale;

	/*
	 * This options allows to give the width of the transcoded video in pixels.
	 */
	private int width;

	/*
	 * This options allows to give the height of the transcoded video, in pixels. 
	 */
	private int height;

	/*
	 * This options allows to specify the codec the audio tracks of the input stream should be transcoded to. 
	 */
	private String acodec;
	
	private Map<String, String> availableACodecs = new HashMap<String, String>();

	/*
	 * This option allows to set the bitrate of the transcoded audio stream, in kbit/s 
	 */
	private float ab;

	/*
	 * This allows to set the encoder to use to encode the audio stream. Available options are:
	 * 
	 * ffmpeg: this is the libavcodec encoding module. It handles a large variety of different codecs (the list can be found on the streaming features page.
	 * vorbis. This module uses the vorbis encoder from the Xiph.org project. Vorbis is a free, open, license-free lossy audio codec.
	 * Item options are: quality=<quality level> allows to use VBR (variable bitrate) encoding instead of the default CBR (constant bitrate), and to set the quality level (between 1 and 10, higher is better), max-bitrate=<bitrate in kbit/s> allows to set the maximum bitrate, for vbr encoding, min-bitrate=<bitrate in kbit/s> allows to set the minimum bitrate, for vbr encoding, cbr allows to force cbr encoding.
	 * speex. This module uses the speex encoder from the Xiph.org project. Speex is a lossy audio codec, best fit for very low bitrates (around 10 kbit/s) and particularly video conference.
	 */
	private String aenc;

	/*
	 * This option allows to set the samplerate of the transcoded audio stream, in Hz. Reducing the samplerate is be a way to lower the bitrate of the resulting audio stream.
	 */
	private float samplerate;

	/*
	 * This option allows to set the number of channels of the resulting audio stream. This is useful for codecs that don't have support for more than 2 channels, of to lower the bitrate of an audio stream.
	 */
	private int channels;

	/*
	 * This options allows to specify subtitle format the subtitles tracks of the input stream should be converted to.
	 */
	private String scodec;

	/*
	 * This allows to set the converter to use to encode the subtitle stream.
	 * The only subtitle encoder we have at this time is dvbsub.
	 */
	private String senc;

	/*
	 * This option allow to render subtitles directly on the video, while transcoding it. Do not confuse this option with senc/scodec that transcode the subtitles and streams them
	 */
	private String soverlay;

	/*
	 * This option allows to render some images generated by a so called subpicture filter (e.g. a logo, a text string, etc) on top of the video.
	 */
	private String sfilter;		


	public VLCTranscoder(){
		this.availableVCodecs.put("mp1v", "MPEG-1 Video - recommended for portability");
		this.availableVCodecs.put("mp2v", "MPEG-2 Video - used in DVDs");
		this.availableVCodecs.put("mp4v", "MPEG-4 Video");
		this.availableVCodecs.put("SVQ1", "Sorenson Video v1");
		this.availableVCodecs.put("SVQ3", "Sorenson Video v3");
		this.availableVCodecs.put("DVDv", "VOB Video - used in DVDs");
		this.availableVCodecs.put("WMV1", "Windows Media Video v1");
		this.availableVCodecs.put("WMV2", "Windows Media Video v2");
		this.availableVCodecs.put("WMV3", "Windows Media Video v3, also called Windows Media 9 (unsupported)");
		this.availableVCodecs.put("DVSD", "Digital Video");
		this.availableVCodecs.put("MJPG", "MJPEG");
		this.availableVCodecs.put("H263", "H263");
		this.availableVCodecs.put("h264", "H264");
		this.availableVCodecs.put("theo", "Theora");
		this.availableVCodecs.put("IV20", "Indeo Video");
		this.availableVCodecs.put("IV40", "Indeo Video version 4 or later (unsupported)");
		this.availableVCodecs.put("RV10", "Real Media Video");
		this.availableVCodecs.put("cvid", "Cinepak");
		this.availableVCodecs.put("VP31", "On2 VP");
		this.availableVCodecs.put("FLV1", "Flash Video");
		this.availableVCodecs.put("CYUV", "Creative YUV");
		this.availableVCodecs.put("HFYU", "Huffman YUV");
		this.availableVCodecs.put("MSVC", "Microsoft Video v1");
		this.availableVCodecs.put("MRLE", "Microsoft RLE Video");
		this.availableVCodecs.put("AASC", "Autodesc RLE Video");
		this.availableVCodecs.put("FLIC", "FLIC video");
		this.availableVCodecs.put("QPEG", "QPEG Video");
		this.availableVCodecs.put("VP8", "VP8 Video");
		
		this.availableACodecs.put("mpga", "MPEG audio (recommended for portability)");
		this.availableACodecs.put("mp3", "MPEG Layer 3 audio");
		this.availableACodecs.put("mp4a", "MP4 audio");
		this.availableACodecs.put("a52", "Dolby Digital (A52 or AC3)");
		this.availableACodecs.put("vorb", "Vorbis");
		this.availableACodecs.put("spx", "Speex");
		this.availableACodecs.put("flac", "FLAC");
		this.availableACodecs.put("fl32", "FLAC");
	}
	
	
	public String getTranscodeArg(){
		
		List<String> args = new ArrayList<String>();
		
		if(this.deinterlace)
			args.add("deinterlace");
		if(this.ab != 0.0)
			args.add("ab=" + this.ab);
		if(this.acodec != null)
			args.add("acodec=" + this.acodec);
		if(this.aenc != null)
			args.add("aenc=" + this.aenc);
		if(this.channels != 0)
			args.add("channels=" + this.channels);
		if(this.cropbottom != 0)
			args.add("cropbottom=" + this.cropbottom);
		if(this.cropleft != 0)
			args.add("cropleft=" + this.cropleft);
		if(this.cropright != 0)
			args.add("cropright=" + this.cropright);
		if(this.croptop != 0)
			args.add("croptop=" + this.croptop);
		if(this.fps != 0)
			args.add("fps=" + this.fps);
		if(this.height != 0)
			args.add("height=" + this.height);
		if(this.samplerate != 0.0)
			args.add("samplerate=" + this.samplerate);
		if(this.scale != 0.0)
			args.add("scale=" + this.scale);
		if(this.scodec != null)
			args.add("scodec=" + this.scodec);
		if(this.senc != null)
			args.add("senc=" + this.senc);
		if(this.sfilter != null)
			args.add("sfilter=" + this.sfilter);
		if(this.soverlay != null)
			args.add("soverlay=" + this.soverlay);
		if(this.vb != 0.0)
			args.add("vb=" + this.vb);
		if(this.vcodec != null)
			args.add("vcodec=" + this.vcodec);
		if(this.venc != null)
			args.add("venc=" + this.venc);
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i < args.size();i++){
			if(i == 0)
				sb.append(args.get(i));
			else
				sb.append(", " + args.get(i));
		}
		return sb.toString();
	}
	
	public VLCTranscoder getDefault(){
		VLCTranscoder transcoder = new VLCTranscoder();
		transcoder.vcodec = "mp2v";
		transcoder.vb=0;
		transcoder.acodec = "mp4a";
		transcoder.ab = 128;
		transcoder.channels = 2;
		transcoder.samplerate = 44100;
		transcoder.scale=0;
		return transcoder;
	}
	
	
	public void setVcodec(String vcodec) {
		this.vcodec = vcodec;
	}


	public void setVb(float vb) {
		this.vb = vb;
	}


	public void setVenc(String venc) {
		this.venc = venc;
	}


	public void setFps(int fps) {
		this.fps = fps;
	}


	public void setDeinterlace(boolean deinterlace) {
		this.deinterlace = deinterlace;
	}


	public void setScale(double scale) {
		this.scale = scale;
	}


	public void setWidth(int width) {
		this.width = width;
	}


	public void setHeight(int height) {
		this.height = height;
	}


	public void setSamplerate(float samplerate) {
		this.samplerate = samplerate;
	}


	public void setScodec(String scodec) {
		this.scodec = scodec;
	}


	public void setSenc(String senc) {
		this.senc = senc;
	}


	public void setSoverlay(String soverlay) {
		this.soverlay = soverlay;
	}


	public void setSfilter(String sfilter) {
		this.sfilter = sfilter;
	}


	public String getVcodec() {
		return vcodec;
	}


	public float getVb() {
		return vb;
	}


	public String getVenc() {
		return venc;
	}


	public int getFps() {
		return fps;
	}


	public boolean isDeinterlace() {
		return deinterlace;
	}


	public int getCroptop() {
		return croptop;
	}


	public int getCropbottom() {
		return cropbottom;
	}


	public int getCropleft() {
		return cropleft;
	}


	public int getCropright() {
		return cropright;
	}


	public double getScale() {
		return scale;
	}


	public int getWidth() {
		return width;
	}


	public int getHeight() {
		return height;
	}


	public String getAcodec() {
		return acodec;
	}


	public float getAb() {
		return ab;
	}


	public String getAenc() {
		return aenc;
	}


	public float getSamplerate() {
		return samplerate;
	}


	public int getChannels() {
		return channels;
	}


	public String getScodec() {
		return scodec;
	}


	public String getSenc() {
		return senc;
	}


	public String getSoverlay() {
		return soverlay;
	}


	public String getSfilter() {
		return sfilter;
	}
	
	
}
