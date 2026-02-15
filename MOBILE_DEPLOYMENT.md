# 📱 Deploy EventFinder PWA to iPhone

Your EventFinder app is now PWA-enabled and ready to install on your iPhone! Here's how to do it:

## Prerequisites
- Your iPhone and computer must be on the **same WiFi network**
- Docker Desktop running on your computer
- iPhone with Safari browser

## Step 1: Find Your Computer's Local IP Address

Open PowerShell and run:
```powershell
ipconfig
```

Look for your WiFi adapter and find the **IPv4 Address**. It will look like:
- `192.168.1.xxx` or
- `192.168.0.xxx` or  
- `10.0.0.xxx`

**Example output:**
```
Wireless LAN adapter Wi-Fi:
   IPv4 Address. . . . . . . . . : 192.168.1.105
```

In this example, your IP is **192.168.1.105**. **Write this down!**

## Step 2: Update Frontend Environment

Open `docker-compose.yml` and update the VITE_API_BASE in the frontend service:

```yaml
frontend:
  # ... other settings ...
  environment:
    VITE_API_BASE: http://YOUR_IP_HERE:8080  # Replace with your actual IP!
```

**Example:**
```yaml
VITE_API_BASE: http://192.168.1.105:8080
```

## Step 3: Start the Application

In your project directory, run:

```powershell
docker-compose up --build
```

Wait until you see:
- ✅ Backend: "Started EventFinderApplication"
- ✅ Frontend: "Local: http://localhost:5173/"
- ✅ Database: "database system is ready to accept connections"

## Step 4: Access from iPhone

1. **Open Safari** on your iPhone (must be Safari for PWA install!)
2. **Enter the URL**: `http://YOUR_IP:5173` (e.g., `http://192.168.1.105:5173`)
3. The EventFinder app should load!

## Step 5: Install as App on iPhone

Once the app loads in Safari:

1. Tap the **Share button** (square with arrow pointing up) at the bottom of Safari
2. Scroll down and tap **"Add to Home Screen"**
3. You'll see:
   - App name: "EventFinder"
   - Icon: Calendar with events
4. Tap **"Add"** in the top right

🎉 **Done!** You now have EventFinder as an app icon on your iPhone home screen!

## Using the App

- **Tap the icon** to open (looks like a native app!)
- **Works offline** after first load (thanks to service worker)
- **No browser UI** - full screen app experience
- **Can be in your dock** - just like any other app

## Troubleshooting

### ❌ Can't connect from iPhone

1. **Check WiFi**: Ensure both devices are on the same network
2. **Check Firewall**: Windows Firewall might be blocking:
   ```powershell
   # Allow ports 5173 and 8080
   netsh advfirewall firewall add rule name="EventFinder Frontend" dir=in action=allow protocol=TCP localport=5173
   netsh advfirewall firewall add rule name="EventFinder Backend" dir=in action=allow protocol=TCP localport=8080
   ```
3. **Verify Docker is running**: Check Docker Desktop
4. **Test from computer first**: Open `http://localhost:5173` to verify it works

### ❌ App loads but can't fetch data

- Check that `VITE_API_BASE` in docker-compose.yml uses your **actual IP address** (not localhost!)
- Restart Docker Compose after changing the environment variable

### ❌ "Add to Home Screen" option missing

- Make sure you're using **Safari** (not Chrome or other browsers)
- Make sure you're at the root URL (not a subpage)

### ❌ Icon not showing correctly

The app uses an SVG icon which works on most devices. For better compatibility:

1. Go to https://www.svgtopng.com/
2. Upload `frontend/public/icons/icon.svg`
3. Download as:
   - `icon-192.png` (192x192 pixels)
   - `icon-512.png` (512x512 pixels)
4. Place both in `frontend/public/icons/`
5. Rebuild: `docker-compose up --build`

## Advanced: Access from Outside Your Home

⚠️ **Not recommended without proper security!**

If you want to access from outside your WiFi:
1. Set up port forwarding on your router (ports 5173 and 8080)
2. Use your public IP address
3. **Better option**: Use ngrok or a proper hosting solution
4. **Best option**: Deploy to a cloud service (Heroku, Railway, etc.)

## Features of Your PWA

✅ Installable on home screen  
✅ Works offline (after first visit)  
✅ Full screen mode (no browser UI)  
✅ App-like experience  
✅ Fast loading with service worker caching  
✅ iOS and Android compatible  

## Next Steps

- **Customize the icon**: Edit `frontend/public/icons/icon.svg`
- **Change theme color**: Edit `theme_color` in `frontend/public/manifest.json`
- **Update app name**: Edit `name` in `frontend/public/manifest.json`
- **Add push notifications**: Implement push notification service (advanced)

---

**Need help?** Check the main README.md for general application documentation.
