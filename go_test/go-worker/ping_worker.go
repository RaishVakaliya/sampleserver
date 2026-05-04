package main

import (
	"fmt"
	"net/http"
	"sync"
	"time"
)

// Service represents a monitored endpoint
type Service struct {
	ID       string
	Name     string
	URL      string
	Interval time.Duration
}

// PingResult represents the outcome of a ping operation
type PingResult struct {
	ServiceID string
	Status    string // "online" or "offline"
	Latency   time.Duration
	CheckedAt time.Time
	Error     error
}

// PingWorker handles the core monitoring logic
type PingWorker struct {
	services []Service
	results  chan PingResult
	wg       sync.WaitGroup
}

// NewPingWorker initializes a new worker
func NewPingWorker(services []Service) *PingWorker {
	return &PingWorker{
		services: services,
		results:  make(chan PingResult, len(services)),
	}
}

// Ping performs an HTTP GET request to the service URL
func (pw *PingWorker) Ping(service Service) {
	defer pw.wg.Done()

	start := time.Now()
	client := http.Client{
		Timeout: 10 * time.Second,
	}

	resp, err := http.Get(service.URL)
	latency := time.Since(start)

	result := PingResult{
		ServiceID: service.ID,
		CheckedAt: time.Now(),
		Latency:   latency,
	}

	if err != nil || resp.StatusCode >= 400 {
		result.Status = "offline"
		result.Error = err
		if err == nil {
			result.Error = fmt.Errorf("HTTP %d", resp.StatusCode)
		}
	} else {
		result.Status = "online"
		resp.Body.Close()
	}

	pw.results <- result
}

// Run starts the monitoring loop
func (pw *PingWorker) Run() {
	fmt.Printf("🚀 PulsePing Go Worker started. Monitoring %d services...\n", len(pw.services))

	for {
		pw.wg.Add(len(pw.services))
		for _, service := range pw.services {
			go pw.Ping(service)
		}

		// Wait for all pings in this cycle to complete
		pw.wg.Wait()

		// Process results (this would normally send data to a database/API)
		for i := 0; i < len(pw.services); i++ {
			res := <-pw.results
			fmt.Printf("[%s] %s: %s (%v)\n", res.CheckedAt.Format("15:04:05"), res.ServiceID, res.Status, res.Latency)
		}

		fmt.Println("--------------------------------------------------")
		time.Sleep(15 * time.Second) // Base interval for this demo
	}
}

func main() {
	// Sample services to monitor
	services := []Service{
		{ID: "api-gateway", Name: "API Gateway", URL: "https://api.github.com", Interval: 5 * time.Minute},
		{ID: "web-frontend", Name: "Web Frontend", URL: "https://google.com", Interval: 15 * time.Minute},
		{ID: "auth-service", Name: "Auth Service", URL: "https://invalid-url-test.com", Interval: 1 * time.Minute},
	}

	worker := NewPingWorker(services)
	worker.Run()
}
