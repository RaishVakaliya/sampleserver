use axum::{
    routing::get,
    http::HeaderMap,
    Router,
};
use std::net::SocketAddr;
use chrono::Local;

#[tokio::main]
async fn main() {
    // build our application with a route
    let app = Router::new()
        .route("/", get(root))
        .route("/health", get(health));

    // run it
    let addr = SocketAddr::from(([0, 0, 0, 0], 3000));
    println!("-------------------------------------------");
    println!("✅ Rust Axum Server is live at {}", addr);
    println!("-------------------------------------------");

    axum::Server::bind(&addr)
        .serve(app.into_make_service())
        .await
        .unwrap();
}

async fn root(headers: HeaderMap) -> &'static str {
    let timestamp = Local::now().format("%H:%M:%S").to_string();
    let user_agent = headers.get("user-agent")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("Unknown");

    if user_agent.contains("NoSnore-Pinger") {
        // Print in Green for NoSnore pings
        println!("\x1b[32m[{}] 🚀 RUST WAKE-UP PING RECEIVED! (Axum)\x1b[0m", timestamp);
    } else {
        println!("[{}] Incoming Request: GET /", timestamp);
    }

    "Rust Backend is Online 🦀"
}

async fn health() -> &'static str {
    "OK"
}
