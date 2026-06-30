using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using RadioPureWindows.Models;
using Windows.UI;

namespace RadioPureWindows.Controls;

public sealed partial class StationRow : UserControl
{
    public static readonly DependencyProperty StationProperty =
        DependencyProperty.Register(nameof(Station), typeof(RadioStation), typeof(StationRow), new PropertyMetadata(null, OnVisualPropertyChanged));

    public static readonly DependencyProperty IsSelectedProperty =
        DependencyProperty.Register(nameof(IsSelected), typeof(bool), typeof(StationRow), new PropertyMetadata(false, OnVisualPropertyChanged));

    public static readonly DependencyProperty IsPlayingProperty =
        DependencyProperty.Register(nameof(IsPlaying), typeof(bool), typeof(StationRow), new PropertyMetadata(false, OnVisualPropertyChanged));

    public static readonly DependencyProperty IsErrorProperty =
        DependencyProperty.Register(nameof(IsError), typeof(bool), typeof(StationRow), new PropertyMetadata(false, OnVisualPropertyChanged));

    public RadioStation? Station
    {
        get => (RadioStation?)GetValue(StationProperty);
        set => SetValue(StationProperty, value);
    }

    public bool IsSelected
    {
        get => (bool)GetValue(IsSelectedProperty);
        set => SetValue(IsSelectedProperty, value);
    }

    public bool IsPlaying
    {
        get => (bool)GetValue(IsPlayingProperty);
        set => SetValue(IsPlayingProperty, value);
    }

    public bool IsError
    {
        get => (bool)GetValue(IsErrorProperty);
        set => SetValue(IsErrorProperty, value);
    }

    public StationRow()
    {
        InitializeComponent();
        Loaded += (_, _) => UpdateVisuals();
        PointerEntered += (_, _) => HoverBackground.Opacity = 1;
        PointerExited += (_, _) => HoverBackground.Opacity = 0;
    }

    private static void OnVisualPropertyChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
    {
        if (d is StationRow row)
        {
            row.UpdateVisuals();
        }
    }

    private void UpdateVisuals()
    {
        if (Station is null)
        {
            return;
        }

        EmojiText.Text = Station.Emoji;
        NameText.Text = Station.Name;
        NameText.FontWeight = IsSelected ? Windows.UI.Text.FontWeights.SemiBold : Windows.UI.Text.FontWeights.Normal;
        NameText.Opacity = IsSelected ? 1.0 : 0.75;

        IconBackground.Fill = new SolidColorBrush(Color.FromArgb((byte)((IsSelected ? 0.15 : 0.06) * 255), 255, 255, 255));

        if (IsError)
        {
            SubtitleText.Text = "加载失败，请重试";
            SubtitleText.Foreground = new SolidColorBrush(Color.FromArgb(204, 255, 0, 0));
            StatusIcon.Glyph = "\uE783";
            StatusIcon.Foreground = new SolidColorBrush(Color.FromArgb(179, 255, 0, 0));
            StatusIcon.Visibility = Visibility.Visible;
            ChevronIcon.Visibility = Visibility.Collapsed;
        }
        else if (IsPlaying)
        {
            SubtitleText.Text = "正在播放";
            SubtitleText.Foreground = new SolidColorBrush(Color.FromArgb(255, 0, 200, 0));
            StatusIcon.Glyph = "\uE9D9";
            StatusIcon.Foreground = new SolidColorBrush(Color.FromArgb(255, 0, 200, 0));
            StatusIcon.Visibility = Visibility.Visible;
            ChevronIcon.Visibility = Visibility.Collapsed;
        }
        else
        {
            SubtitleText.Text = "点击收听";
            SubtitleText.Foreground = new SolidColorBrush(Color.FromArgb(77, 255, 255, 255));
            if (IsSelected)
            {
                StatusIcon.Glyph = "\uE769";
                StatusIcon.Foreground = new SolidColorBrush(Color.FromArgb(102, 255, 255, 255));
                StatusIcon.Visibility = Visibility.Visible;
                ChevronIcon.Visibility = Visibility.Collapsed;
            }
            else
            {
                StatusIcon.Visibility = Visibility.Collapsed;
                ChevronIcon.Visibility = Visibility.Visible;
            }
        }
    }
}
