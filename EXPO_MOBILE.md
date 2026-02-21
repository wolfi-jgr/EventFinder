# Expo Mobile Setup (separate from web app)

This project now includes a separate Expo client in `mobile-expo/`.
Your existing web frontend in `frontend/` is unchanged.

## 1) Start backend
Use your existing backend (local Docker or cloud).

- Local backend default: `http://localhost:8080`
- For real device testing with friends: use a hosted backend URL (recommended)

## 2) Configure API base URL
Create a `.env` file in `mobile-expo/` from `.env.example`.

Example:

```bash
EXPO_PUBLIC_API_BASE=https://your-backend-url.example.com
```

If you don't set this value, the app tries to auto-detect your PC's LAN IP from Expo and uses `http://<detected-ip>:8080` on physical devices.

If this variable is missing, defaults are:
- Android emulator: `http://10.0.2.2:8080`
- Physical device (Expo Go): tries `http://<expo-lan-ip>:8080` first, then fallback candidates
- iOS simulator / Expo web fallback: `http://localhost:8080`

## Quick connectivity check (phone)
1. Find your PC IPv4 (for example `192.168.1.105`).
2. Open on phone browser: `http://<PC_IP>:8080/api/health`
3. If it returns `{"status":"ok"}`, network path is fine and Expo should work.

If it does not open, this is a network/firewall issue between phone and PC. In that case, easiest workaround is using a hosted backend URL in `EXPO_PUBLIC_API_BASE`.

## 3) Run Expo app

```bash
cd mobile-expo
npm install
npm run start
```

Then:
- Scan the QR code with **Expo Go** on iPhone/Android
- Or press `a`/`i` for emulator/simulator

## 4) Friend testing (easy mode)
Use a hosted backend and Expo sharing.

- In Expo CLI press `s` and choose **Tunnel** if needed
- Share the Expo link/QR with friends
- Friends install **Expo Go** and open your project

## 5) Local backend without firewall changes (localtunnel)
If your phone cannot reach your PC IP directly, use localtunnel for backend traffic.

1. Start backend locally and verify:
	```bash
	http://localhost:8080/api/health
	```
2. Start tunnel (any terminal, any folder):
	```bash
	npx localtunnel --port 8080
	```
3. Copy the generated URL (example: `https://xxxxx.loca.lt`).
4. Set mobile API base in `mobile-expo/.env`:
	```bash
	EXPO_PUBLIC_API_BASE=https://xxxxx.loca.lt
	```
5. Restart Expo with clean cache:
	```bash
	npx expo start --clear
	```
6. On phone browser, open:
	```bash
	https://xxxxx.loca.lt/api/health
	```
	If prompted by localtunnel access page, enter the tunnel password (usually your public IP shown by localtunnel).

### Important localtunnel note
- This project's mobile API client already sends `bypass-tunnel-reminder: true` header for requests.
- Even with this header, opening the URL once in phone browser can help confirm the tunnel is alive.
- Tunnel URLs are temporary. If localtunnel restarts, update `.env` with the new URL.

## 6) Debugging in app
The mobile app shows runtime API debug values:
- `Current base`: active API base URL
- `Last attempt`: exact last endpoint called
- `Last success`: exact last successful endpoint

Use these lines to quickly identify whether failure is base URL, endpoint, or connectivity.

## 7) Common failure patterns
- `Backend unreachable ...`:
  - API base not reachable from phone, tunnel down, or wrong URL.
- `Partial data loaded ...`:
  - One endpoint failed but the other worked.
- `... /api/events failed ...` with places working:
  - Backend route/data issue for events specifically.
- Tunnel works in browser but not app:
  - Restart Expo with `--clear` and verify `EXPO_PUBLIC_API_BASE` value.

## Included mobile features
- Load events from `/api/events`
- Load places from `/api/places?lat=48.2082&lon=16.3738`
- Trigger scraper with `/api/scraping/run`
- Open event/place source URLs in browser

## Notes
- No Xcode required for Expo Go testing.
- A standalone App Store build still requires Apple signing/build workflow (can be done with EAS cloud builds).
