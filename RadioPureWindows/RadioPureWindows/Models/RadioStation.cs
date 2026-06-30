namespace RadioPureWindows.Models;

public sealed record RadioStation(string Name, string Url, string? FallbackUrl, string Emoji)
{
    public string Id => Name;
}
