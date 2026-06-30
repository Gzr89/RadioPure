using LibVLCSharp.Shared;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using RadioPureWindows.ViewModels;
using Windows.UI;

namespace RadioPureWindows;

public partial class App : Application
{
    public static MainViewModel ViewModel { get; private set; } = null!;

    public App()
    {
        Core.Initialize();
        InitializeComponent();
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        ViewModel = new MainViewModel(DispatcherQueue.GetForCurrentThread());

        var window = new MainWindow();
        window.Activate();
    }
}
