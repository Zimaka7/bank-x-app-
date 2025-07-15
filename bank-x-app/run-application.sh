#!/bin/bash

# Bank X Application Management Script

APP_NAME="Bank X Application"
APP_PORT=9090
APP_URL="http://41.76.107.45:9090"
PID_FILE="/tmp/bankx-app.pid"
LOG_FILE="application.log"

# Function to check if application is running
check_status() {
    # First check if PID file exists and process is running
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "$APP_NAME is running (PID: $PID)"
            echo "Application URL: $APP_URL"
            return 0
        else
            echo "$APP_NAME is not running (stale PID file)"
            rm -f "$PID_FILE"
            return 1
        fi
    fi
    
    # Simple check if something is running on port 9090
    if netstat -tlnp 2>/dev/null | grep -q ":$APP_PORT "; then
        echo "$APP_NAME may be running on port $APP_PORT"
        echo "Application URL: $APP_URL"
        return 0
    fi
    
    echo "$APP_NAME is not running"
    return 1
}

# Function to stop the application
stop_app() {
    echo "Stopping $APP_NAME..."
    
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            echo "Stopping process $PID..."
            kill $PID
            sleep 5
            
            # Force kill if still running
            if ps -p $PID > /dev/null 2>&1; then
                echo "Force killing process $PID..."
                kill -9 $PID
            fi
        fi
        rm -f "$PID_FILE"
    fi
    
    # Kill any process on port 9090
    if lsof -ti:$APP_PORT &> /dev/null; then
        echo "Killing process on port $APP_PORT..."
        kill -9 $(lsof -ti:$APP_PORT) 2>/dev/null || true
        sleep 2
    fi
    
    # Wait a bit more and verify everything is stopped
    sleep 3
    
    # Clean up any remaining processes
    if lsof -ti:$APP_PORT &> /dev/null; then
        echo "Force killing remaining processes on port $APP_PORT..."
        kill -9 $(lsof -ti:$APP_PORT) 2>/dev/null || true
        sleep 2
    fi
    
    echo "$APP_NAME stopped"
}

# Function to start the application
start_app() {
    echo "=== $APP_NAME Startup ==="
    echo ""
    
    # Check if already running
    if check_status > /dev/null 2>&1; then
        echo "Error: $APP_NAME is already running!"
        echo "Use 'restart' to restart or 'stop' to stop first."
        exit 1
    fi
    
    # Set JAVA_HOME if not set and Java is available
    if [ -z "$JAVA_HOME" ]; then
        # Try to find Java installation
        if [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
            export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
            echo "Setting JAVA_HOME to: $JAVA_HOME"
        elif [ -f "/usr/bin/java" ]; then
            # Fallback to detecting from /usr/bin/java
            JAVA_REAL_PATH=$(readlink -f /usr/bin/java)
            export JAVA_HOME=$(dirname $(dirname "$JAVA_REAL_PATH"))
            echo "Setting JAVA_HOME to: $JAVA_HOME"
        fi
    fi
    
    # Add Java to PATH - prioritize JAVA_HOME/bin if set, otherwise use /usr/bin
    if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME/bin" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
        echo "Added $JAVA_HOME/bin to PATH"
    elif [ -f "/usr/bin/java" ]; then
        export PATH="/usr/bin:$PATH"
        echo "Added /usr/bin to PATH"
    fi
    
    # Debug: Show current PATH and test Java
    echo "Current PATH: $PATH"
    echo "Testing Java command..."
    
    # Check if Java is installed - use direct path if command -v fails
    if ! command -v java &> /dev/null; then
        if [ -f "$JAVA_HOME/bin/java" ]; then
            echo "Java found at: $JAVA_HOME/bin/java"
            # Use direct path for Java commands
            JAVA_CMD="$JAVA_HOME/bin/java"
        elif [ -f "/usr/bin/java" ]; then
            echo "Java found at: /usr/bin/java"
            JAVA_CMD="/usr/bin/java"
        else
            echo "Error: Java is not installed or not in PATH"
            echo "Available Java locations:"
            find /usr -name "java" -type f 2>/dev/null | head -5
            exit 1
        fi
    else
        JAVA_CMD="java"
        echo "Java found at: $(which java)"
    fi
    
    # Check Java version
    JAVA_VERSION_OUTPUT=$($JAVA_CMD -version 2>&1 | head -n 1)
    echo "Java version: $JAVA_VERSION_OUTPUT"
    
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        if [ -f "/usr/bin/mvn" ]; then
            echo "Maven found at: /usr/bin/mvn"
            MVN_CMD="/usr/bin/mvn"
        elif [ -f "/usr/share/maven/bin/mvn" ]; then
            echo "Maven found at: /usr/share/maven/bin/mvn"
            MVN_CMD="/usr/share/maven/bin/mvn"
        else
            echo "Error: Maven is not installed or not in PATH"
            echo "Please install Maven 3.6 or higher"
            exit 1
        fi
    else
        MVN_CMD="mvn"
        echo "Maven found at: $(which mvn)"
    fi
    
    # Check if PostgreSQL is running on the correct port (5433)
    if command -v pg_isready &> /dev/null; then
        if ! pg_isready -h localhost -p 5433 &> /dev/null; then
            echo "Warning: PostgreSQL is not running on localhost:5433"
            echo "Please ensure PostgreSQL is running and the database is set up"
            echo ""
        else
            echo "PostgreSQL is running on port 5433"
        fi
    else
        echo "Warning: pg_isready not found, skipping PostgreSQL check"
    fi
    
    echo ""
    echo "Starting $APP_NAME..."
    echo "Building and running with Maven..."
    echo "Application will be available at: $APP_URL"
    echo "Logs will be written to: $LOG_FILE"
    echo ""
    
    # Start the application in background (skip tests and test compilation)
    nohup $MVN_CMD clean spring-boot:run -Dmaven.test.skip=true > "$LOG_FILE" 2>&1 &
    APP_PID=$!
    
    # Save PID
    echo $APP_PID > "$PID_FILE"
    
    echo "$APP_NAME started with PID: $APP_PID"
    echo "Waiting for application to start..."
    
    # Wait for application to start (check for up to 60 seconds)
    for i in {1..60}; do
        if curl -s "$APP_URL" > /dev/null 2>&1; then
            echo ""
            echo "✅ $APP_NAME is now running!"
            echo "🌐 Access the application at: $APP_URL"
            echo "👤 Admin login: admin / admin123"
            echo "📝 Customer registration: $APP_URL/register"
            echo "📊 Health check: $APP_URL/actuator/health"
            echo "📋 View logs: tail -f $LOG_FILE"
            return 0
        fi
        echo -n "."
        sleep 1
    done
    
    echo ""
    echo "⚠️  Application may still be starting. Check logs: tail -f $LOG_FILE"
    echo "🌐 Application URL: $APP_URL"
}

# Function to restart the application
restart_app() {
    echo "Restarting $APP_NAME..."
    stop_app
    sleep 2
    start_app
}

# Function to show usage
show_usage() {
    echo "Usage: $0 {start|stop|restart|status}"
    echo ""
    echo "Commands:"
    echo "  start   - Start the $APP_NAME"
    echo "  stop    - Stop the $APP_NAME"
    echo "  restart - Restart the $APP_NAME"
    echo "  status  - Check if $APP_NAME is running"
    echo ""
    echo "Application URL: $APP_URL"
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
        check_status
        ;;
    "")
        # Default action is start
        start_app
        ;;
    *)
        show_usage
        exit 1
        ;;
esac