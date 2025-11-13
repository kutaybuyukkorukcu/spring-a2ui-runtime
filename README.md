# FogData

Full-stack application with .NET Web API backend and React frontend.

# FogData

Full-stack application with .NET Web API backend and React frontend.

## Project Structure

```
FogData/
├── Controllers/          # API Controllers
├── Models/              # DTOs and data models
├── Services/            # Business logic layer
├── Repositories/        # Data access layer
├── Middleware/          # Custom middleware
├── Extensions/          # Service extensions
├── client/              # React TypeScript frontend
│   ├── src/
│   ├── public/
│   └── vite.config.ts
├── wwwroot/             # Built React app (production)
├── Program.cs           # App entry point
└── FogData.csproj

```

## Development

### Prerequisites
- .NET 9.0 SDK
- Node.js 20.19+ or 22.12+ (for React development)
- Docker and Docker Compose

### Running Locally (Without Docker)

**Backend:**
```bash
dotnet run
```
API will be available at `https://localhost:5000` (or check console output)

**Frontend (Development):**
```bash
cd client
npm install  # if not already installed
npm run dev
```
React dev server runs at `http://localhost:5173` with API proxy configured

### Running with Docker (Recommended)

**Production Build:**
```bash
docker-compose up --build
```
- Backend: http://localhost:5001
- Frontend: http://localhost:3001

**Development with Hot Reload:**
```bash
docker-compose up --build
```
- Backend: http://localhost:5001 (auto-restarts on code changes)
- Frontend: http://localhost:5173 (hot module reload)

**Stop containers:**
```bash
docker-compose down
```

**View logs:**
```bash
docker-compose logs -f [service-name]
```

## API Endpoints

- `GET /api/weatherforecast` - Get weather forecast data

## Docker Development Features

### Hot Reload Setup
- **Backend**: Uses `dotnet watch run` with volume mounts
- **Frontend**: Uses Vite HMR with volume mounts
- **Automatic**: Code changes trigger rebuilds/restarts

### Development vs Production
- `docker-compose.yml` - Production configuration
- `docker-compose.override.yml` - Development overrides (auto-loaded)

### Networking
- Services communicate via `fogdata-network`
- Frontend proxies `/api/*` to backend container

## Notes

- CORS enabled in development for React app
- Production serves React static files from `wwwroot/`
- Vite proxy configured in `vite.config.ts` for development
- Use record types for DTOs (available since .NET 5)
- Interfaces required for services/repositories, NOT for models

## Development

### Prerequisites
- .NET 9.0 SDK
- Node.js 20.19+ or 22.12+ (for React development)

### Running the Backend
```bash
dotnet run
```
API will be available at `https://localhost:5000` (or check console output)

### Running the React Frontend (Development)
```bash
cd client
npm install  # if not already installed
npm run dev
```
React dev server runs at `http://localhost:5173` with API proxy configured

### Building for Production
```bash
dotnet publish -c Release
```
This will:
1. Build the .NET API
2. Install npm dependencies
3. Build React app to `wwwroot/`
4. Package everything for deployment

## API Endpoints

- `GET /api/weatherforecast` - Get weather forecast data

## Notes

- CORS is enabled in development for React app
- Production serves React static files from `wwwroot/`
- Vite proxy forwards `/api/*` requests to backend in development
