from fastapi import FastAPI, Request
import time
import uvicorn
from datetime import datetime

app = FastAPI()

# Middleware to log NoSnore Pings
@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    user_agent = request.headers.get("user-agent", "Unknown")
    timestamp = datetime.now().strftime("%H:%M:%S")
    
    # Check if the request is from NoSnore
    if "NoSnore-Pinger" in user_agent:
        print(f"\033[92m[{timestamp}] 🚀 PYTHON WAKE-UP PING RECEIVED! (FastAPI)\033[0m")
    else:
        print(f"[{timestamp}] Incoming Request: {request.method} {request.url.path}")
        
    response = await call_next(request)
    return response

@app.get("/")
async def root():
    return {
        "status": "online",
        "language": "Python 🐍",
        "framework": "FastAPI",
        "message": "This Python backend is kept awake by NoSnore!"
    }

@app.get("/health")
async def health():
    return "OK"

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
