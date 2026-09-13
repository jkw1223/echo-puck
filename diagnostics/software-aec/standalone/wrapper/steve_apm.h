#ifndef STEVE_APM_H_
#define STEVE_APM_H_
#include <stddef.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
typedef struct SteveApm SteveApm;
enum { STEVE_APM_OK = 0, STEVE_APM_INVALID_ARGUMENT = -1, STEVE_APM_INVALID_FRAMES = -2, STEVE_APM_CREATE_FAILED = -3, STEVE_APM_PROCESS_FAILED = -4 };
SteveApm* steve_apm_create(int sample_rate_hz, int channels);
void steve_apm_destroy(SteveApm* apm);
int steve_apm_process_reverse(SteveApm* apm, const int16_t* speaker_pcm, size_t frames);
int steve_apm_process_capture(SteveApm* apm, const int16_t* mic_pcm, int16_t* output_pcm, size_t frames);
int steve_apm_set_stream_delay_ms(SteveApm* apm, int delay_ms);
#ifdef __cplusplus
}
#endif
#endif
