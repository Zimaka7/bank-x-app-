#!/bin/bash

# Bank X Server Management Script
# Usage: ./bankx.sh {start|stop|restart|status}

APP_NAME="Bank X"
APP_DIR="/opt/absa/bank-x-app"
PID_FILE="/tmp/bankx.pid"
LOG_FILE="/opt/absa/bank-x-app/bankx.log"
PORT=9090

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    printf "${BLUE}[INFO]${NC} %s\n" "$1"
}

print_success() {
    printf "${GREEN}[SUCCESS]${NC} %s\n" "$1"
}

print_warning() {
    printf "${YELLOW}[WARNING]${NC} %s\n" "$1"
}

print_error() {
    printf "${RED}[ERROR]${NC} %s\n" "$1"
}

# Function to check if app is running
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            return 0
        else
            rm -f "$PID_FILE"
            return 1
        fi
    fi
    return 1
}

# Function to get process using port
get_port_process() {
    lsof -ti:$PORT 2>/dev/null
}

# Function to start the application
start_app() {
    print_status "Starting $APP_NAME..."
    
    # Check if already running
    if is_running; then
        PID=$(cat "$PID_FILE")
        print_warning "$APP_NAME is already running (PID: $PID)"
        print_status "Access at: http://localhost:$PORT"
        return 0
    fi
    
    # Check if port is in use
    PORT_PID=$(get_port_process)
    if [ ! -z "$PORT_PID" ]; then
        print_warning "Port $PORT is already in use by process $PORT_PID"
        print_status "Killing process on port $PORT..."
        kill -9 $PORT_PID 2>/dev/null
        sleep 2
    fi
    
    # Change to app directory
    cd "$APP_DIR" || {
        print_error "Cannot change to directory $APP_DIR"
        exit 1
    }
    
    # Check if Java is available - use command -v instead of just java -version
    if ! command -v java >/dev/null 2>&1; then
        print_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Maven is available
    if ! command -v mvn >/dev/null 2>&1; then
        print_error "Maven is not installed or not in PATH"
        exit 1
    fi
    
    print_status "Java version: $(java -version 2>&1 | head -n 1)"
    print_status "Maven version: $(mvn -version 2>&1 | head -n 1)"
    
    print_status "Building and starting $APP_NAME..."
    print_status "Logs will be written to: $LOG_FILE"
    
    # Start the application
    nohup mvn spring-boot:run -Dmaven.test.skip=true > "$LOG_FILE" 2>&1 &
    APP_PID=$!
    
    # Save PID
    echo $APP_PID > "$PID_FILE"
    
    print_status "$APP_NAME started with PID: $APP_PID"
    print_status "Waiting for application to start..."
    
    # Wait for application to start (check for up to 60 seconds)
    for i in $(seq 1 60); do
        if curl -s "http://localhost:$PORT" > /dev/null 2>&1; then
            print_success "$APP_NAME is now running!"
            print_success "Access the application at: http://localhost:$PORT"
            print_status "Admin login: admin / admin123"
            print_status "Customer registration: http://localhost:$PORT/register"
            print_status "Health check: http://localhost:$PORT/actuator/health"
            print_status "View logs: tail -f $LOG_FILE"
            return 0
        fi
        printf "."
        sleep 1
    done
    
    print_warning "Application may still be starting. Check logs: tail -f $LOG_FILE"
    print_status "Application URL: http://localhost:$PORT"
}

# Function to stop the application
stop_app() {
    print_status "Stopping $APP_NAME..."
    
    # Stop using PID file
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            print_status "Stopping process $PID..."
            kill $PID
            sleep 5
            
            # Force kill if still running
            if ps -p $PID > /dev/null 2>&1; then
                print_status "Force killing process $PID..."
                kill -9 $PID
            fi
        fi
        rm -f "$PID_FILE"
    fi
    
    # Kill any process on the port
    PORT_PID=$(get_port_process)
    if [ ! -z "$PORT_PID" ]; then
        print_status "Killing process on port $PORT..."
        kill -9 $PORT_PID 2>/dev/null
        sleep 2
    fi
    
    print_success "$APP_NAME stopped"
}

# Function to show status
show_status() {
    if is_running; then
        PID=$(cat "$PID_FILE")
        print_success "$APP_NAME is running (PID: $PID)"
        print_status "Application URL: http://localhost:$PORT"
        
        # Test if responding
        if curl -s "http://localhost:$PORT" > /dev/null 2>&1; then
            print_success "Application is responding"
        else
            print_warning "Application is not responding"
        fi
    else
        print_error "$APP_NAME is not running"
        
        # Check if something else is on the port
        PORT_PID=$(get_port_process)
        if [ ! -z "$PORT_PID" ]; then
            print_warning "Port $PORT is in use by process $PORT_PID"
        fi
    fi
}

# Function to restart the application
restart_app() {
    print_status "Restarting $APP_NAME..."
    stop_app
    sleep 3
    start_app
}

# Function to show usage
show_usage() {
    echo "Usage: $0 {start|stop|restart|status}"
    echo ""
    echo "Commands:"
    echo "  start   - Start $APP_NAME"
    echo "  stop    - Stop $APP_NAME"
    echo "  restart - Restart $APP_NAME"
    echo "  status  - Check if $APP_NAME is running"
    echo ""
    echo "Application will run on: http://localhost:$PORT"
}

# Main script logic
case "$1" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        restart_app
        ;;
    status)
        show_status
        ;;
    *)
        show_usage
        exit 1
        ;;
esac