# Connecting Spotify (optional)

SmartEQ Pro never bundles a Spotify Client Secret — mobile apps can't hold
one safely. Instead it uses Spotify's **Authorization Code with PKCE**
flow, which only needs a **Client ID** that you (or whoever owns this
build) generate for free.

## 1. Create a Spotify Developer app
1. Go to https://developer.spotify.com/dashboard and log in.
2. Click **Create app**.
3. App name / description: anything (e.g. "SmartEQ Pro — personal").
4. **Redirect URI** — this is the important part. Add exactly:
   ```
   smarteqpro://spotify-callback
   ```
   (This matches `SpotifyAuthManager.REDIRECT_URI` and the `<intent-filter>`
   in `AndroidManifest.xml` — if you ever rename the app's package ID or
   change that scheme, update both places together.)
5. Under **Which API/SDKs are you planning to use?**, check **Web API**.
6. Save. Copy the **Client ID** shown on the app's dashboard page.

## 2. Enter it in SmartEQ Pro
Settings > Web Connections > Spotify > paste the Client ID > Save.
Then tap **Connect Spotify** and log in through the Spotify page that
opens. Nothing is sent anywhere until you do this — the feature is fully
inert until a Client ID is entered.

## 3. Scopes requested
`user-read-private`, `user-top-read`, `user-read-currently-playing`,
`user-read-playback-state`, `playlist-read-private`,
`playlist-read-collaborative` — matching exactly what the Auto-Tune and
Spotify panel features need (currently-playing track, top artists,
playlists). No playback-control or write scopes are requested.

## Note on Spotify Developer Mode / app review
Newly created Spotify apps start in **Development Mode**, which limits
usage to 25 explicitly-added user accounts (added under the app's Users
tab in the dashboard) unless you request **Extension/Quota** from Spotify
for a public release. Fine for personal use and testing; required before
distributing this build publicly with your own Spotify integration.
