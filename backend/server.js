const express = require("express");
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware to detect and log NoSnore pings specifically
app.use((req, res, next) => {
  const userAgent = req.headers["user-agent"] || "Unknown";
  const time = new Date().toLocaleTimeString();

  if (userAgent.includes("NoSnore-Pinger")) {
    // Green text in terminal/Render logs for NoSnore pings
    console.log(
      `\x1b[32m[${time}] 🚀 NOSNORE WAKE-UP PING: ${req.method} ${req.url}\x1b[0m`,
    );
  } else {
    console.log(`[${time}] Standard Request: ${req.method} ${req.url}`);
  }
  next();
});

// Root Route
app.get("/", (req, res) => {
  res.json({
    status: "online",
    message: "Backend is active",
    last_ping: new Date().toISOString(),
  });
});

// Health Endpoint (Point NoSnore here for best performance)
app.get("/health", (req, res) => {
  res.status(200).send("OK");
});

// Start the server
app.listen(PORT, () => {
  console.log("===========================================");
  console.log(`✅ Server is live at http://localhost:${PORT}`);
  console.log("===========================================");
});
