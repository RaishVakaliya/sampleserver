require 'net/http'
require 'uri'
require 'json'
require 'webrick'
require 'thread'

# Service model
class Service
  attr_accessor :id, :name, :url, :interval
  
  def initialize(id, name, url, interval)
    @id = id
    @name = name
    @url = url
    @interval = interval
  end
end

# The Ping Worker
class PingWorker
  def initialize(services)
    @services = services
  end

  def ping(service)
    start_time = Time.now
    uri = URI.parse(service.url)
    
    begin
      response = Net::HTTP.get_response(uri)
      latency = (Time.now - start_time) * 1000 # convert to ms
      
      if response.is_a?(Net::HTTPSuccess)
        puts "[#{Time.now.strftime('%H:%M:%S')}] #{service.id}: online (#{latency.round(2)}ms)"
      else
        puts "[#{Time.now.strftime('%H:%M:%S')}] #{service.id}: offline (HTTP #{response.code})"
      end
    rescue => e
      puts "[#{Time.now.strftime('%H:%M:%S')}] #{service.id}: offline (Error: #{e.message})"
    end
  end

  def run
    puts "🚀 PulsePing Ruby Worker started. Monitoring #{@services.length} services..."
    loop do
      threads = @services.map do |service|
        Thread.new { ping(service) }
      end
      
      threads.each(&:join)
      puts "--------------------------------------------------"
      sleep 15 # Check every 15 seconds for this demo
    end
  end
end

# Sample Data
services = [
  Service.new("api-gateway", "API Gateway", "https://api.github.com", 300),
  Service.new("web-frontend", "Web Frontend", "https://google.com", 900)
]

worker = PingWorker.new(services)

# Start Monitoring in a Background Thread
Thread.new { worker.run }

# Start a simple WEBrick server for Render Health Checks
port = ENV['PORT'] || 8080
server = WEBrick::HTTPServer.new(Port: port, AccessLog: [], Logger: WEBrick::Log.new(File::NULL))

server.mount_proc '/' do |req, res|
  res.body = "PulsePing Ruby Worker is active!"
end

server.mount_proc '/health' do |req, res|
  res.status = 200
  res.body = "OK"
end

trap('INT') { server.shutdown }

puts "🌍 Web server started on port #{port}"
server.start
