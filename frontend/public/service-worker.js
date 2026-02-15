const CACHE_NAME = 'eventfinder-v2';  // Increment version to force update
const urlsToCache = [
  '/',
  '/index.html',
  '/manifest.json'
];

// Install event - cache assets
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Opened cache');
        return cache.addAll(urlsToCache);
      })
      .catch((err) => {
        console.log('Cache installation failed:', err);
      })
  );
  self.skipWaiting();
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// Fetch event - smart caching strategy
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  
  // NEVER cache API requests - always fetch fresh data
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request)
        .catch(() => {
          // If offline and API fails, return error response
          return new Response(
            JSON.stringify({ error: 'Offline - API unavailable' }),
            { 
              status: 503,
              headers: { 'Content-Type': 'application/json' }
            }
          );
        })
    );
    return;
  }

  // For static assets: cache-first strategy
  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        // Cache hit - return cached response
        if (response) {
          return response;
        }

        // Not in cache - fetch from network
        return fetch(event.request).then(
          (response) => {
            // Check if valid response
            if (!response || response.status !== 200) {
              return response;
            }

            // Only cache successful GET requests
            if (event.request.method === 'GET') {
              const responseToCache = response.clone();
              
              caches.open(CACHE_NAME)
                .then((cache) => {
                  cache.put(event.request, responseToCache);
                });
            }

            return response;
          }
        );
      })
      .catch(() => {
        // Return offline page if you have one
        return caches.match('/index.html');
      })
  );
});
