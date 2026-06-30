using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using RadioPureWindows.Controls;
using RadioPureWindows.Models;
using RadioPureWindows.ViewModels;
using Windows.UI;

namespace RadioPureWindows;

public sealed partial class MainWindow : Window
{
    private readonly MainViewModel _viewModel = App.ViewModel;

    public MainWindow()
    {
        InitializeComponent();
        Title = "RadioPure";

        ConfigureWindow();
        BindUi();
        SubscribeToPlayerChanges();
        Closed += (_, _) => _viewModel.Player.Dispose();
    }

    private void ConfigureWindow()
    {
        AppWindow.Resize(new Windows.Graphics.SizeInt32(400, 640));

        if (AppWindow.Presenter is OverlappedPresenter presenter)
        {
            presenter.IsResizable = false;
            presenter.IsMaximizable = false;
        }
    }

    private void BindUi()
    {
        StationList.ItemsSource = _viewModel.Stations;
        VolumeSlider.Value = _viewModel.Player.Volume;
        UpdateHeader();
        UpdatePlayButton();
    }

    private void SubscribeToPlayerChanges()
    {
        _viewModel.Player.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName is nameof(_viewModel.Player.CurrentStation) or
                nameof(_viewModel.Player.IsPlaying) or
                nameof(_viewModel.Player.IsError) or
                nameof(_viewModel.Player.Volume))
            {
                UpdateHeader();
                UpdatePlayButton();
                RefreshStationRows();
            }
        };
    }

    private void UpdateHeader()
    {
        var player = _viewModel.Player;
        if (player.CurrentStation is { } station)
        {
            CurrentStationPanel.Visibility = Visibility.Visible;
            PlaceholderText.Visibility = Visibility.Collapsed;
            CurrentStationName.Text = station.Name;
            CurrentStationName.Opacity = player.IsError ? 0.6 : 1.0;

            if (player.IsError)
            {
                StatusDot.Fill = new SolidColorBrush(Colors.Red);
            }
            else if (player.IsPlaying)
            {
                StatusDot.Fill = new SolidColorBrush(Color.FromArgb(255, 0, 200, 0));
            }
            else
            {
                StatusDot.Visibility = Visibility.Collapsed;
                return;
            }

            StatusDot.Visibility = Visibility.Visible;
        }
        else
        {
            CurrentStationPanel.Visibility = Visibility.Collapsed;
            PlaceholderText.Visibility = Visibility.Visible;
        }
    }

    private void UpdatePlayButton()
    {
        var player = _viewModel.Player;
        PlayPauseButton.IsEnabled = player.CurrentStation is not null;

        if (player.CurrentStation is not null)
        {
            PlayButtonCircle.Fill = new SolidColorBrush(Colors.White);
            PlayPauseIcon.Foreground = new SolidColorBrush(Color.FromArgb(255, 20, 20, 26));
        }
        else
        {
            PlayButtonCircle.Fill = new SolidColorBrush(Color.FromArgb(51, 255, 255, 255));
            PlayPauseIcon.Foreground = new SolidColorBrush(Color.FromArgb(102, 255, 255, 255));
        }

        PlayPauseIcon.Glyph = player.IsPlaying ? "\uE769" : "\uE768";
    }

    private void RefreshStationRows()
    {
        var offset = StationList.VerticalOffset;
        StationList.ItemsSource = null;
        StationList.ItemsSource = _viewModel.Stations;
        StationList.UpdateLayout();
        StationList.ChangeView(null, offset, null);
    }

    private void StationList_ContainerContentChanging(ListViewBase sender, ContainerContentChangingEventArgs args)
    {
        if (args.InRecycleQueue)
        {
            return;
        }

        args.RegisterUpdateCallback(UpdateStationRow);
    }

    private void UpdateStationRow(ListViewBase sender, ContainerContentChangingEventArgs args)
    {
        if (args.Item is not RadioStation station)
        {
            return;
        }

        if (args.ItemContainer.ContentTemplateRoot is StationRow row)
        {
            row.Station = station;
            row.IsSelected = _viewModel.IsSelected(station);
            row.IsPlaying = _viewModel.IsPlayingStation(station);
            row.IsError = _viewModel.IsErrorStation(station);
        }
    }

    private void StationRow_Tapped(object sender, TappedRoutedEventArgs e)
    {
        if (sender is StationRow { Station: { } station })
        {
            _viewModel.Play(station);
        }
    }

    private void PlayPauseButton_Click(object sender, RoutedEventArgs e)
    {
        _viewModel.TogglePlayPause();
    }

    private void VolumeSlider_ValueChanged(object sender, Microsoft.UI.Xaml.Controls.Primitives.RangeBaseValueChangedEventArgs e)
    {
        if (_viewModel.Player.Volume != (float)e.NewValue)
        {
            _viewModel.SetVolume((float)e.NewValue);
        }
    }
}
