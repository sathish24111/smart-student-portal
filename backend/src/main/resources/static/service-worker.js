const CACHE_NAME = "geoportal-web-cache-v3";
const ASSETS = [
    "index.html",
    "dashboard.html",
    "style.css?v=2.5.0",
    "script.js?v=2.5.0",
    "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css",
    "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
];

self.addEventListener("install", (e) => {
    e.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(ASSETS);
        }).then(() => self.skipWaiting())
    );
});

self.addEventListener("activate", (e) => {
    e.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.map((key) => {
                    if (key !== CACHE_NAME) {
                        return caches.delete(key);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (e) => {
    // Skip non-GET requests or requests to backend API for normal caching
    if (e.request.method !== "GET" || e.request.url.includes("/api/")) {
        return;
    }

    e.respondWith(
        caches.match(e.request).then((cachedResponse) => {
            if (cachedResponse) return cachedResponse;
            return fetch(e.request).catch(() => {
                if (e.request.mode === "navigate") {
                    return caches.match("index.html");
                }
            });
        })
    );
});

// Background Sync for offline attendance
self.addEventListener("sync", (e) => {
    if (e.tag === "sync-attendance") {
        e.waitUntil(syncPendingAttendance());
    }
});

async function syncPendingAttendance() {
    return new Promise((resolve, reject) => {
        const req = indexedDB.open("GeoPortalOfflineDB", 1);
        req.onsuccess = (e) => {
            const db = e.target.result;
            if (!db.objectStoreNames.contains("attendanceQueue")) {
                resolve();
                return;
            }
            const tx = db.transaction("attendanceQueue", "readwrite");
            const store = tx.objectStore("attendanceQueue");
            const getAllReq = store.getAll();
            
            getAllReq.onsuccess = async () => {
                const items = getAllReq.result;
                if (!items || items.length === 0) {
                    resolve();
                    return;
                }
                
                for (const item of items) {
                    try {
                        const res = await fetch("https://smart-student-portal-hbxw.onrender.com/api/attendance/mark", {
                            method: "POST",
                            headers: {
                                "Authorization": `Bearer ${item.token}`,
                                "Content-Type": "application/json"
                            },
                            body: JSON.stringify({
                                sessionId: item.sessionId,
                                latitude: item.latitude,
                                longitude: item.longitude
                            })
                        });
                        if (res.ok) {
                            const deleteTx = db.transaction("attendanceQueue", "readwrite");
                            deleteTx.objectStore("attendanceQueue").delete(item.id);
                            console.log("Background sync: Synced offline attendance item", item.id);
                        }
                    } catch (err) {
                        console.error("Background sync: Failed for item", item.id, err);
                    }
                }
                resolve();
            };
            getAllReq.onerror = () => reject(getAllReq.error);
        };
        req.onerror = () => reject(req.error);
    });
}
