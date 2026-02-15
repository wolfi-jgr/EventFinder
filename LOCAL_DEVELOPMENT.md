# 🏠 Local Development Guide

This guide explains how to run EventFinder locally for development and testing.

---

## 🚀 Quick Start - Local Development

### Option 1: Docker Compose (Recommended)

**Start the app:**
```powershell
docker-compose up
```

**Access the app:**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- Database: localhost:5432

**Stop the app:**
```powershell
# Press Ctrl+C, then:
docker-compose down
```

---

## 🧪 Testing Production Build Locally

Before deploying to cloud, test the production build:

```powershell
docker-compose -f docker-compose.prod.yml up --build
```

**Access the production build:**
- Frontend: http://localhost:3000 (note different port!)
- Backend: http://localhost:8080

This uses the same Dockerfiles that will be deployed to Railway/Render.

**Clean up:**
```powershell
docker-compose -f docker-compose.prod.yml down
```

---

## 📱 Testing on Mobile (Same WiFi)

To test the PWA on your phone while developing locally:

### Step 1: Find Your Computer's IP
```powershell
ipconfig
```
Look for your WiFi adapter's IPv4 Address (e.g., `192.168.1.105`)

### Step 2: Temporary Change for Mobile Testing

**Option A: Environment Variable (Easiest)**
```powershell
# Set environment variable before starting
$env:VITE_API_BASE="http://192.168.1.105:8080"
docker-compose up
```

**Option B: Edit docker-compose.yml**
```yaml
frontend:
  environment:
    VITE_API_BASE: http://192.168.1.105:8080  # Your actual IP
```

### Step 3: Access from Phone
Open Safari/Chrome on your phone: `http://192.168.1.105:5173`

**Don't forget to change it back to `localhost` after testing!**

---

## 🔧 Development Modes Explained

### Development Mode (docker-compose.yml)
- **Frontend**: Vite dev server with hot reload
- **Backend**: Maven with spring-boot:run (auto-restart on changes)
- **Fast**: Changes reflect immediately
- **Use for**: Daily development

### Production Mode (docker-compose.prod.yml)
- **Frontend**: Optimized production build
- **Backend**: Compiled JAR with JRE
- **Smaller**: Multi-stage Docker builds
- **Use for**: Testing before deployment

---

## 🗂️ Project Structure

```
eventfinder/
├── docker-compose.yml           # Development mode
├── docker-compose.prod.yml      # Production testing mode
├── backend/
│   ├── Dockerfile              # Production backend image
│   ├── src/                    # Java source code
│   └── pom.xml                 # Maven dependencies
├── frontend/
│   ├── Dockerfile              # Production frontend image
│   ├── src/
│   │   ├── config.js          # API configuration (handles dev/prod)
│   │   ├── App.jsx            # Main app component
│   │   └── ...
│   ├── public/
│   │   ├── manifest.json      # PWA manifest
│   │   └── service-worker.js  # PWA service worker
│   └── package.json
└── ...
```

---

## 🔄 How API Configuration Works

The app automatically detects the environment:

### In Development (npm run dev):
```javascript
// config.js automatically uses:
API_BASE = "http://localhost:8080"
```

### In Docker Compose:
```javascript
// config.js uses VITE_API_BASE from environment:
API_BASE = process.env.VITE_API_BASE  // e.g., http://localhost:8080
```

### In Production (Railway/Render):
```javascript
// config.js uses VITE_API_BASE set during build:
API_BASE = "https://your-backend.railway.app"
```

**You don't need to change anything!** The [config.js](frontend/src/config.js) handles it automatically.

---

## 📝 Common Development Tasks

### View Backend Logs
```powershell
docker logs -f eventfinder_backend
```

### View Frontend Logs
```powershell
docker logs -f eventfinder_frontend
```

### Restart a Single Service
```powershell
docker-compose restart backend
# or
docker-compose restart frontend
```

### Rebuild After Dependency Changes
```powershell
docker-compose up --build
```

### Clean Everything and Start Fresh
```powershell
docker-compose down -v  # -v removes volumes (deletes database!)
docker-compose up --build
```

### Access Database Directly
```powershell
docker exec -it eventfinder_db psql -U eventfinder -d eventfinder
```

---

## 🐛 Troubleshooting

### Port Already in Use
```powershell
# Find what's using the port
netstat -ano | findstr :8080
# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

### Frontend Can't Reach Backend
1. Check backend is running: http://localhost:8080/api/events
2. Check CORS settings in backend application.yml
3. Verify VITE_API_BASE in docker-compose.yml

### Database Connection Errors
```powershell
# Check if database is ready
docker exec eventfinder_db pg_isready -U eventfinder
```

### Hot Reload Not Working
- Make sure volumes are mounted correctly in docker-compose.yml
- Try restarting the container: `docker-compose restart frontend`

### PWA Not Installing
- PWA requires HTTPS (or localhost)
- Check manifest.json is accessible: http://localhost:5173/manifest.json
- Check service worker: Open DevTools → Application → Service Workers

---

## 🌐 Environment Variables

### Backend (.env or docker-compose.yml)
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/eventfinder
SPRING_DATASOURCE_USERNAME: eventfinder
SPRING_DATASOURCE_PASSWORD: eventfinder
APP_CORS_ORIGINS: "*"  # Allow all origins for local dev
```

### Frontend (.env.local or docker-compose.yml)
```yaml
VITE_API_BASE: http://localhost:8080
```

**Never commit `.env` files with real credentials!**

---

## 🧹 Cleanup

### Remove All Containers and Images
```powershell
# Stop and remove containers
docker-compose down

# Remove all EventFinder images
docker rmi $(docker images -q eventfinder*)

# Remove dangling images
docker image prune -f
```

### Free Up Disk Space
```powershell
# Remove unused Docker data
docker system prune -a --volumes
```

---

## 🚀 Next Steps

- **Ready to deploy?** See [DEPLOYMENT.md](DEPLOYMENT.md)
- **Want mobile access?** See [MOBILE_DEPLOYMENT.md](MOBILE_DEPLOYMENT.md)
- **Adding features?** Edit code in `frontend/src` or `backend/src`

---

## 📚 Useful Commands Cheat Sheet

```powershell
# Start development
docker-compose up

# Start in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop everything
docker-compose down

# Rebuild and start
docker-compose up --build

# Test production build
docker-compose -f docker-compose.prod.yml up --build

# Clean restart
docker-compose down -v && docker-compose up --build
```

---

**Happy coding! 🎉**
