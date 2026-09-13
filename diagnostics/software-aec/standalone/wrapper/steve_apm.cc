#include "steve_apm.h"
#include <new>
#include <vector>
#include "api/audio/audio_processing.h"
struct SteveApm { rtc::scoped_refptr<webrtc::AudioProcessing> impl; webrtc::StreamConfig stream; std::vector<int16_t> scratch; SteveApm(int rate,int ch):stream(rate,ch),scratch(stream.num_samples()) {} };
extern "C" SteveApm* steve_apm_create(int rate,int channels) {
  if (rate < 8000 || rate > 384000 || channels < 1 || channels > 2 || webrtc::AudioProcessing::GetFrameSize(rate) <= 0) return nullptr;
  try { auto* a = new SteveApm(rate,channels); webrtc::AudioProcessing::Config c; c.echo_canceller.enabled=true; c.echo_canceller.mobile_mode=false; c.echo_canceller.enforce_high_pass_filtering=false; c.high_pass_filter.enabled=false; c.noise_suppression.enabled=true; c.gain_controller1.enabled=false; c.gain_controller2.enabled=false; a->impl=webrtc::AudioProcessingBuilder().SetConfig(c).Create(); if (!a->impl) { delete a; return nullptr; } webrtc::ProcessingConfig pc; pc.input_stream()=a->stream; pc.output_stream()=a->stream; pc.reverse_input_stream()=a->stream; pc.reverse_output_stream()=a->stream; if (a->impl->Initialize(pc)!=0) { delete a; return nullptr; } return a; } catch (...) { return nullptr; }
}
extern "C" void steve_apm_destroy(SteveApm* a) { delete a; }
extern "C" int steve_apm_process_reverse(SteveApm* a,const int16_t* in,size_t frames) { if(!a||!in)return STEVE_APM_INVALID_ARGUMENT; if(frames!=a->stream.num_frames())return STEVE_APM_INVALID_FRAMES; return a->impl->ProcessReverseStream(in,a->stream,a->stream,a->scratch.data())==0?0:STEVE_APM_PROCESS_FAILED; }
extern "C" int steve_apm_process_capture(SteveApm* a,const int16_t* in,int16_t* out,size_t frames) { if(!a||!in||!out)return STEVE_APM_INVALID_ARGUMENT; if(frames!=a->stream.num_frames())return STEVE_APM_INVALID_FRAMES; return a->impl->ProcessStream(in,a->stream,a->stream,out)==0?0:STEVE_APM_PROCESS_FAILED; }
extern "C" int steve_apm_set_stream_delay_ms(SteveApm* a,int d) { if(!a)return STEVE_APM_INVALID_ARGUMENT; return a->impl->set_stream_delay_ms(d); }
