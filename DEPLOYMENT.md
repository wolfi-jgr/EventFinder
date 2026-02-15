# 🚀 Deploy EventFinder to the Cloud

This guide will help you deploy EventFinder to Railway or Render, making it accessible to anyone via a public URL. Users can then install it as a PWA on their phones!

---

## 🎯 What You'll Get

After deployment:
- **Public URL**: `https://your-app.railway.app` (or similar)
- **Anyone can access**: Share the link with friends
- **PWA Installation**: Users open in Safari/Chrome → Install to home screen
- **One database**: All users share the same event data
- **Always online**: No need to run Docker on your computer

---

## 📋 Prerequisites

1. **Git** installed on your computer
2. **GitHub account** (free)
3. **Railway or Render account** (free tier available)

---

## Option 1: Deploy to Railway (Recommended)

Railway offers the easiest deployment experience with a generous free tier.

### Step 1: Push Code to GitHub

1. **Initialize Git** (if not already done):
   ```powershell
   git init
   git add .
   git commit -m "Initial commit - EventFinder app"
   ```

2. **Create a GitHub repository**:
   - Go to https://github.com/new
   - Name it `eventfinder`
   - Don't initialize with README (we already have code)
   - Click "Create repository"

3. **Push your code**:
   ```powershell
   git remote add origin https://github.com/YOUR_USERNAME/eventfinder.git
   git branch -M main
   git push -u origin main
   ```

### Step 2: Sign Up for Railway

1. Go to https://railway.app
2. Click "Login" and sign up with GitHub
3. Authorize Railway to access your repositories

### Step 3: Create New Project

1. Click "**New Project**"
2. Select "**Deploy from GitHub repo**"
3. Choose your `eventfinder` repository
4. Railway will detect your app structure

### Step 4: Add Services

You need to create **3 services**:

#### 4.1: PostgreSQL Database

1. Click "**+ New**" → "**Database**" → "**PostgreSQL**"
2. Railway will provision a database
3. Note: This creates `DATABASE_URL` environment variable automatically

#### 4.2: Backend Service

1. Click "**+ New**" → "**GitHub Repo**" → Select your repo
2. **Settings**:
   - **Name**: `backend`
   - **Root Directory**: `/backend`
   - **Dockerfile Path**: `Dockerfile`
3. **Environment Variables** (Add these):
   ```
   SPRING_DATASOURCE_URL=${{Postgres.DATABASE_URL}}
   SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
   SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
   APP_CORS_ORIGINS=*
   ```
4. **Deploy**

#### 4.3: Frontend Service

1. Click "**+ New**" → "**GitHub Repo**" → Select your repo
2. **Settings**:
   - **Name**: `frontend`
   - **Root Directory**: `/frontend`
   - **Dockerfile Path**: `Dockerfile`
3. **Environment Variables**:
   ```
   VITE_API_BASE=https://${{backend.RAILWAY_PUBLIC_DOMAIN}}
   ```
4. **Generate Domain** (click button to get public URL)
5. **Deploy**

### Step 5: Access Your App

1. Wait for all services to deploy (1-3 minutes)
2. Frontend service will have a URL like: `https://eventfinder-production.up.railway.app`
3. Open it in your browser - your app is live! 🎉

---

## Option 2: Deploy to Render

Render is another excellent free option.

### Step 1: Push to GitHub

(Same as Railway Step 1 above)

### Step 2: Sign Up for Render

1. Go to https://render.com
2. Click "Get Started" and sign up with GitHub

### Step 3: Create Services

#### 3.1: Create PostgreSQL Database

1. Click "**New +**" → "**PostgreSQL**"
2. **Name**: `eventfinder-db`
3. **Database**: `eventfinder`
4. **User**: `eventfinder`
5. Select **Free** tier
6. Click "**Create Database**"
7. **Copy** the "Internal Database URL" (you'll need this)

#### 3.2: Deploy Backend

1. Click "**New +**" → "**Web Service**"
2. Connect your GitHub repository
3. **Settings**:
   - **Name**: `eventfinder-backend`
   - **Root Directory**: `backend`
   - **Environment**: `Docker`
   - **Dockerfile Path**: `backend/Dockerfile`
   - **Plan**: Free
4. **Environment Variables**:
   ```
   SPRING_DATASOURCE_URL=[paste Internal Database URL from step 3.1]
   SPRING_DATASOURCE_USERNAME=eventfinder
   SPRING_DATASOURCE_PASSWORD=[your database password]
   APP_CORS_ORIGINS=*
   ```
5. Click "**Create Web Service**"
6. Copy the service URL (e.g., `https://eventfinder-backend.onrender.com`)

#### 3.3: Deploy Frontend

1. Click "**New +**" → "**Web Service**"
2. Connect your GitHub repository
3. **Settings**:
   - **Name**: `eventfinder-frontend`
   - **Root Directory**: `frontend`
   - **Environment**: `Docker`
   - **Dockerfile Path**: `frontend/Dockerfile`
   - **Plan**: Free
4. **Environment Variables**:
   ```
   VITE_API_BASE=[paste backend URL from step 3.2]
   ```
5. Click "**Create Web Service**"

### Step 4: Access Your App

1. Frontend service URL: `https://eventfinder-frontend.onrender.com`
2. Open it - your app is live! 🎉

**Note**: Free tier services on Render "sleep" after 15 minutes of inactivity. First load may take 30-60 seconds.

---

## 📱 Installing as PWA on Phones

Once deployed, share your app URL with others. They can install it:

### On iPhone (Safari)

1. Open the URL in Safari
2. Tap the **Share** button (square with arrow)
3. Scroll down and tap "**Add to Home Screen**"
4. Tap "**Add**"
5. The app icon appears on the home screen!

### On Android (Chrome)

1. Open the URL in Chrome
2. Tap the menu (⋮) → "**Install app**" or "**Add to Home Screen**"
3. Tap "**Install**"
4. The app icon appears on the home screen!

---

## 🔧 Configuration Tips

### Update CORS for Production

Once deployed, update the backend CORS to be more restrictive:

In Railway/Render environment variables:
```
APP_CORS_ORIGINS=https://your-frontend-url.com
```

### Custom Domain (Optional)

Both Railway and Render allow you to add custom domains:
- Buy domain from Namecheap, Google Domains, etc.
- Add CNAME record pointing to your app
- Configure in Railway/Render dashboard

---

## 🐛 Troubleshooting

### Backend Can't Connect to Database

- Check that `SPRING_DATASOURCE_URL` is correctly set
- Verify database service is running
- Check service logs in Railway/Render dashboard

### Frontend Can't Reach Backend

- Verify `VITE_API_BASE` points to correct backend URL
- Check backend service is deployed and running
- Look at browser console for errors (F12)

### App Loads But Shows Errors

- Check browser console (F12)
- Check backend logs in Railway/Render
- Verify all environment variables are set

### Railway/Render Commands

View logs:
```powershell
# Railway CLI
railway logs

# Render
# View logs in the dashboard
```

---

## 💰 Pricing

### Railway Free Tier
- $5 credit per month
- Typically enough for small apps
- No credit card required to start

### Render Free Tier
- Unlimited static sites
- 750 hours per month for web services
- Services sleep after 15 min inactivity

---

## 🚀 Next Steps

1. **Share your URL** with friends - they can install the PWA
2. **Custom Icon** - Update `frontend/public/icons/icon.svg`
3. **Theme Colors** - Edit `frontend/public/manifest.json`
4. **Analytics** - Add Google Analytics or Plausible
5. **Push Notifications** - Implement for event reminders (advanced)

---

## 📚 Additional Resources

- Railway Docs: https://docs.railway.app
- Render Docs: https://render.com/docs
- PWA Guide: https://web.dev/progressive-web-apps/

---

**Need help?** Open an issue on GitHub or check the console logs!

🎉 **Congratulations!** Your EventFinder app is now accessible to everyone!
