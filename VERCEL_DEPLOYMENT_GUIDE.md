# Vercel Deployment & Architecture Guide

> **Project:** API Health, Observability & Latency Optimization Platform  
> **Backend Stack:** Spring Boot 3.3.5 (Java 21), PostgreSQL 16, Redis 7, Flyway, ThreadPoolExecutor Health Check Scheduler  

---

## 1. Important Architectural Reality Check

Before deploying, it is essential to understand **how Vercel fits into this application's architecture**:

| Component | Vercel Native Support? | Explanation |
| :--- | :--- | :--- |
| **Frontend SPA / Next.js / React** | **Yes (Ideal)** | Vercel is the industry gold standard for hosting frontend single-page applications, static assets, and Edge/Serverless routing. |
| **Spring Boot 3 (This Backend)** | ❌ **No (Not Supported)** | Vercel is a **serverless** platform without persistent JVM runtimes. It terminates or freezes processes after each HTTP request. |
| **Scheduled Health Checks** | ❌ **No** | This platform relies on a long-running `ThreadPoolExecutor` and background scheduled tasks to ping APIs at set intervals. Serverless environments cannot run background daemons. |
| **PostgreSQL & Redis** | ❌ **No** | Vercel does not host relational databases or Redis instances natively. |

### The Standard Production Architecture

```
┌─────────────────────────────────┐
│     Vercel (Frontend Tier)      │
│  React / Next.js / Vite SPA     │
│  URL: https://my-app.vercel.app │
└────────────────┬────────────────┘
                 │ HTTPS (REST API calls with JWT)
                 ▼
┌─────────────────────────────────┐        ┌──────────────────────────────┐
│ Backend PaaS (Render / Railway) │───────▶│ Managed PostgreSQL (Neon/DB) │
│ Dockerized Spring Boot 3 Engine │        └──────────────────────────────┘
│ └── Scheduled Health Checks     │        ┌──────────────────────────────┐
│ └── L1/L2 Tiered Cache          │───────▶│ Managed Redis (Upstash)      │
└─────────────────────────────────┘        └──────────────────────────────┘
```

The recommended industry architecture for this project is:
1. **Frontend** deployed on **Vercel**.
2. **Backend (Spring Boot)** deployed on a container PaaS like **Render** or **Railway** (or Fly.io / AWS EC2).
3. **Database & Cache** provisioned via **Neon** (Postgres) and **Upstash** (Redis) or through Render/Railway add-ons.

---

## 2. Deploying the Spring Boot Backend (Render / Railway)

### Option A: Deploy on Render (Recommended & Free Tier Friendly)

Render natively supports the existing [`Dockerfile`](file:///c:/final%20year%20project/Api%20health%20App%20backend/Dockerfile).

#### 1. Create PostgreSQL & Redis on Render:
1. Go to the [Render Dashboard](https://dashboard.render.com).
2. Click **New +** > **PostgreSQL**:
   - Name: `apihealth-db`
   - Database: `apihealth`
   - User: `apihealth`
   - Copy the **Internal Connection String** or **External Database URL**.
3. Click **New +** > **Redis** (or create a free Redis on [Upstash](https://upstash.com)):
   - Note the **Host**, **Port**, and **Password**.

#### 2. Create the Backend Web Service:
1. Click **New +** > **Web Service**.
2. Connect your GitHub repository (`Api health App backend`).
3. Choose **Docker** as the runtime (Render will automatically detect the [`Dockerfile`](file:///c:/final%20year%20project/Api%20health%20App%20backend/Dockerfile)).
4. Under **Environment Variables**, add:
   ```env
   SPRING_PROFILES_ACTIVE=prod
   DB_URL=jdbc:postgresql://<RENDER_POSTGRES_HOST>:5432/apihealth
   DB_USERNAME=<RENDER_POSTGRES_USER>
   DB_PASSWORD=<RENDER_POSTGRES_PASSWORD>
   REDIS_HOST=<REDIS_HOST>
   REDIS_PORT=<REDIS_PORT>
   REDIS_PASSWORD=<REDIS_PASSWORD>
   JWT_SECRET=production-secret-key-that-is-at-least-64-characters-long-for-hmac-sha512
   SERVER_PORT=8080
   CORS_ORIGINS=https://your-frontend-project.vercel.app,http://localhost:3000
   ```
5. Click **Create Web Service**. Render will build the Docker container and start your Spring Boot application. Note down your backend URL (e.g. `https://apihealth-backend.onrender.com`).

---

### Option B: Deploy on Railway

1. Go to [Railway.app](https://railway.app).
2. Create a **New Project** > **Provision PostgreSQL**.
3. In the same project, click **New** > **Database** > **Redis**.
4. Click **New** > **GitHub Repo** and select this backend repository.
5. Railway will automatically build using the [`Dockerfile`](file:///c:/final%20year%20project/Api%20health%20App%20backend/Dockerfile).
6. Set the environment variables in Railway referencing Postgres and Redis variables:
   ```env
   SPRING_PROFILES_ACTIVE=prod
   DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   DB_USERNAME=${{Postgres.PGUSER}}
   DB_PASSWORD=${{Postgres.PGPASSWORD}}
   REDIS_HOST=${{Redis.REDISHOST}}
   REDIS_PORT=${{Redis.REDISPORT}}
   REDIS_PASSWORD=${{Redis.REDISPASSWORD}}
   JWT_SECRET=your-secure-random-64-character-jwt-key
   SERVER_PORT=8080
   CORS_ORIGINS=https://your-project.vercel.app
   ```
7. Generate a public domain under **Settings** > **Networking** (e.g., `https://apihealth-production.up.railway.app`).

---

## 3. Deploying the Frontend to Vercel

If you have a React, Next.js, or Vite frontend for this project, deploy it directly to Vercel:

### Step 1: Push Frontend to GitHub
Make sure your frontend project is committed to a GitHub repository.

### Step 2: Import into Vercel
1. Log in to [Vercel](https://vercel.com).
2. Click **Add New...** > **Project**.
3. Select your GitHub repository.
4. Framework preset will be auto-detected (e.g., **Vite**, **Next.js**, or **Create React App**).

### Step 3: Configure Environment Variables in Vercel
In the Vercel project configuration, expand **Environment Variables** and add your backend API URL:

- For **Vite**:
  ```env
  VITE_API_BASE_URL=https://apihealth-backend.onrender.com
  ```
- For **Next.js**:
  ```env
  NEXT_PUBLIC_API_BASE_URL=https://apihealth-backend.onrender.com
  ```
- For **Create React App**:
  ```env
  REACT_APP_API_BASE_URL=https://apihealth-backend.onrender.com
  ```

### Step 4: Click Deploy
Vercel will build the frontend and provide you with a production URL (e.g., `https://apihealth-app.vercel.app`).

---

## 4. Configuring Reverse Proxy in Vercel (`vercel.json`)

To prevent CORS issues and cookies/HTTPS mixed-content warnings, you can proxy API calls from Vercel directly to your Spring Boot backend:

Create a `vercel.json` in the **root of your frontend project**:

```json
{
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://apihealth-backend.onrender.com/api/:path*"
    },
    {
      "source": "/actuator/:path*",
      "destination": "https://apihealth-backend.onrender.com/actuator/:path*"
    }
  ]
}
```

With this rewrite rule:
- Any call from the frontend to `/api/auth/login` or `/api/monitored-apis` is securely proxied through Vercel's edge network to your Spring Boot backend.
- The browser views requests as same-origin, completely eliminating CORS errors.

---

## 5. Syncing Backend CORS Configuration

Once your Vercel deployment URL is generated (e.g., `https://my-health-app.vercel.app`):

1. Go back to your backend hosting dashboard (Render / Railway / Docker).
2. Update the `CORS_ORIGINS` environment variable:
   ```env
   CORS_ORIGINS=https://my-health-app.vercel.app,http://localhost:3000,http://localhost:5173
   ```
3. Restart or redeploy the backend service.

---

## 6. Verification & Health Check Checklist

After completing the setup, verify the entire pipeline:

1. **Database Migration Verification**:
   Check backend logs for successful Flyway execution:
   ```text
   Successfully applied 3 migrations to schema 'apihealth'
   ```
2. **Backend Actuator Health Check**:
   Visit `https://<YOUR-BACKEND-URL>/actuator/health`. Expected response:
   ```json
   {
     "status": "UP"
   }
   ```
3. **Frontend Connection**:
   Open your Vercel URL `https://<YOUR-APP>.vercel.app`, sign in or register, and inspect network requests in the DevTools console to verify `200 OK` or `201 Created` responses from the backend.
