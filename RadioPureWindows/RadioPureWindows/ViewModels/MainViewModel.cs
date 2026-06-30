using CommunityToolkit.Mvvm.ComponentModel;
using Microsoft.UI.Dispatching;
using RadioPureWindows.Catalog;
using RadioPureWindows.Models;
using RadioPureWindows.Player;

namespace RadioPureWindows.ViewModels;

public partial class MainViewModel : ObservableObject
{
    public RadioPlayer Player { get; }

    public IReadOnlyList<RadioStation> Stations { get; } = RadioStationCatalog.All;

    public MainViewModel(DispatcherQueue dispatcher)
    {
        Player = new RadioPlayer(dispatcher);
        Player.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName is nameof(RadioPlayer.CurrentStation) or nameof(RadioPlayer.IsPlaying) or nameof(RadioPlayer.IsError))
            {
                OnPropertyChanged(nameof(Stations));
            }
        };
    }

    public void Play(RadioStation station) => Player.Play(station);

    public void TogglePlayPause() => Player.TogglePlayPause();

    public void SetVolume(float volume) => Player.Volume = volume;

    public bool IsSelected(RadioStation station) => Player.CurrentStation?.Id == station.Id;

    public bool IsPlayingStation(RadioStation station) =>
        Player.IsPlaying && Player.CurrentStation?.Id == station.Id;

    public bool IsErrorStation(RadioStation station) =>
        Player.IsError && Player.CurrentStation?.Id == station.Id;
}
