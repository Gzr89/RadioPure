// 播放逻辑与 RadioPureShared/RadioPlayer.swift 对齐。

using CommunityToolkit.Mvvm.ComponentModel;
using LibVLCSharp.Shared;
using Microsoft.UI.Dispatching;
using RadioPureWindows.Models;

namespace RadioPureWindows.Player;

public partial class RadioPlayer : ObservableObject, IDisposable
{
    private readonly LibVLC _libVlc;
    private readonly DispatcherQueue _dispatcher;

    private MediaPlayer? _mediaPlayer;
    private Media? _media;
    private string? _resolvedStreamUrl;
    private bool _didTryFallbackForCurrentStation;
    private RadioStation? _pendingStationForFallback;

    [ObservableProperty]
    private bool _isPlaying;

    [ObservableProperty]
    private bool _isError;

    [ObservableProperty]
    private float _volume = 0.7f;

    [ObservableProperty]
    private RadioStation? _currentStation;

    public RadioPlayer(DispatcherQueue dispatcher)
    {
        _dispatcher = dispatcher;
        _libVlc = new LibVLC("--no-video");
    }

    partial void OnVolumeChanged(float value)
    {
        if (_mediaPlayer is not null)
        {
            _mediaPlayer.Volume = (int)(Math.Clamp(value, 0f, 1f) * 100);
        }
    }

    public void Play(RadioStation station)
    {
        if (CurrentStation?.Id == station.Id)
        {
            if (IsPlaying)
            {
                Pause();
            }
            else
            {
                Resume();
            }

            return;
        }

        StopAndRelease(resettingFallback: true);
        CurrentStation = station;
        IsError = false;

        var primary = StreamingUrl(station, primary: true);
        if (primary is null)
        {
            return;
        }

        _resolvedStreamUrl = primary;
        AttachPlayer(station, primary);
    }

    public void Pause()
    {
        _mediaPlayer?.Pause();
        IsPlaying = false;
    }

    public void Resume()
    {
        _mediaPlayer?.Play();
        IsPlaying = true;
        IsError = false;
    }

    public void TogglePlayPause()
    {
        if (IsPlaying)
        {
            Pause();
        }
        else
        {
            Resume();
        }
    }

    private static string? StreamingUrl(RadioStation station, bool primary)
    {
        return primary ? station.Url : station.FallbackUrl;
    }

    private void AttachPlayer(RadioStation station, string streamUrl)
    {
        ReleaseMediaPlayer();

        _pendingStationForFallback = station;
        _mediaPlayer = new MediaPlayer(_libVlc);
        _mediaPlayer.Volume = (int)(Volume * 100);
        _mediaPlayer.EncounteredError += OnEncounteredError;
        _mediaPlayer.EndReached += OnEndReached;

        _media = new Media(_libVlc, streamUrl, FromType.FromLocation);
        _mediaPlayer.Media = _media;
        _mediaPlayer.Play();

        IsPlaying = true;
        IsError = false;
    }

    private void OnEncounteredError(object? sender, EventArgs e)
    {
        DispatchHandlePlaybackFailure();
    }

    private void OnEndReached(object? sender, EventArgs e)
    {
        // 直播流异常结束时也尝试 fallback。
        if (IsPlaying)
        {
            DispatchHandlePlaybackFailure();
        }
    }

    private void DispatchHandlePlaybackFailure()
    {
        _dispatcher.TryEnqueue(() =>
        {
            var station = _pendingStationForFallback ?? CurrentStation;
            if (station is not null)
            {
                HandlePlaybackFailure(station);
            }
        });
    }

    private void HandlePlaybackFailure(RadioStation station)
    {
        var fallback = station.FallbackUrl;
        if (fallback is not null &&
            !_didTryFallbackForCurrentStation &&
            _resolvedStreamUrl != fallback)
        {
            _didTryFallbackForCurrentStation = true;
            IsError = false;
            _resolvedStreamUrl = fallback;
            AttachPlayer(station, fallback);
            return;
        }

        IsPlaying = false;
        IsError = true;
        _pendingStationForFallback = null;
    }

    private void StopAndRelease(bool resettingFallback = false)
    {
        ReleaseMediaPlayer();
        IsPlaying = false;
        IsError = false;
        _resolvedStreamUrl = null;

        if (resettingFallback)
        {
            _didTryFallbackForCurrentStation = false;
            _pendingStationForFallback = null;
        }
    }

    private void ReleaseMediaPlayer()
    {
        if (_mediaPlayer is not null)
        {
            _mediaPlayer.EncounteredError -= OnEncounteredError;
            _mediaPlayer.EndReached -= OnEndReached;
            _mediaPlayer.Stop();
            _mediaPlayer.Dispose();
            _mediaPlayer = null;
        }

        _media?.Dispose();
        _media = null;
    }

    public void Dispose()
    {
        StopAndRelease(resettingFallback: true);
        _libVlc.Dispose();
        GC.SuppressFinalize(this);
    }
}
