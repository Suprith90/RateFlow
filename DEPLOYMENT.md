# RateFlow Docker Deployment Guide

This guide explains how to deploy the RateFlow application using Docker Compose for local development and testing.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose V2
- Git (to clone the repository)

## Quick Start

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd demo
   ```

2. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

3. (Optional) Edit the `.env` file to customize configuration:
   ```bash
   nano .env
   ```

4. Start the application stack:
   ```bash
   docker compose up -d
   ```

5. Verify the services are running:
   ```bash
   docker compose ps
   ```

6. Access the application:
   - API: http://localhost:8080
   - Actuator endpoints: http://localhost:8080/actuator
   - Health check: http://localhost:8080/actuator/health

## Service Overview

The Docker Compose file defines the following services:

- **api**: Spring Boot application exposing REST APIs on port 8080
- **postgres**: PostgreSQL 16 database for persistent storage
- **redis**: Redis 7 instance for Token Bucket rate limiting
- **kafka**: Apache Kafka 7.6 for event streaming
- **zookeeper**: Zookeeper ensemble for Kafka coordination
- **notification-worker**: (Optional) Separate worker for processing notifications

## Environment Variables

The application uses environment variables for configuration. Copy `.env.example` to `.env` and modify as needed:

```
# Application Settings
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8080

# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/rateflow
SPRING_DATASOURCE_USERNAME=rateflow_user
SPRING_DATASOURCE_PASSWORD=rateflow_pass

# Redis Configuration
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# Kafka Configuration
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_KAFKA_CONSUMER_GROUP_ID=rateflow-group
```

## Volumes for Persistent Data

The following named volumes are used to persist data between container restarts:

- `postgres-data`: PostgreSQL database files
- `redis-data`: Redis append-only file
- `kafka-data`: Kafka message logs
- `api-data`: API application data (if any)
- `worker-data`: Notification worker data (if any)

## Health Checks

Each service includes health checks to ensure proper startup ordering:

- **API**: Checks `/actuator/health` endpoint
- **PostgreSQL**: Uses `pg_isready` command
- **Redis**: Uses `redis-cli ping` command
- **Kafka**: Checks port 9092 connectivity
- **Zookeeper**: Checks port 2181 connectivity

## Dependency Ordering

Services start in the correct order using `depends_on` with health checks:
1. Zookeeper → Kafka
2. PostgreSQL, Redis, Kafka → API and Notification Worker

## Stopping and Cleaning Up

To stop all services:
```bash
docker compose down
```

To stop services and remove all data:
```bash
docker compose down -v
```

To view logs:
```bash
docker compose logs -f
```

To view logs for a specific service:
```bash
docker compose logs -f api
```

## Troubleshooting

### Common Issues

1. **Port Conflicts**
   - If ports 8080, 5432, 6379, or 9092 are already in use, either:
     - Stop the conflicting services
     - Modify the port mappings in docker-compose.yml
     - Use `docker compose ps` to see what's running

2. **Database Connection Issues**
   - Ensure PostgreSQL health check passes before API starts
   - Check credentials in .env match those in docker-compose.yml
   - Verify the database name is correct

3. **Kafka Connection Issues**
   - Ensure Zookeeper is healthy before Kafka starts
   - Verify `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092` matches the service name
   - Check that advertised listeners are correctly configured

4. **Redis Connection Issues**
   - Verify Redis is healthy before API starts
   - Check host and port configuration

5. **Application Fails to Start**
   - Check application logs: `docker compose logs api`
   - Look for bean creation errors or configuration issues
   - Verify all required environment variables are set

### Debugging Tips

1. **Check Service Status**
   ```bash
   docker compose ps
   ```

2. **View Real-time Logs**
   ```bash
   docker compose logs -f
   ```

3. **Execute Commands in Containers**
   ```bash
   docker compose exec api bash
   docker compose exec postgres psql -U rateflow_user -d rateflow
   docker compose exec redis redis-cli ping
   docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
   ```

4. **Recreate Containers**
   ```bash
   docker compose up -d --force-recreate
   ```

5. **Rebuild Images**
   ```bash
   docker compose up -d --build
   ```

## Production Considerations

For production deployments, consider:

1. **Image Tags**
   - Use specific version tags instead of `latest` or `alpine`
   - Example: `postgres:16.3-alpine` instead of `postgres:16-alpine`

2. **Resource Limits**
   - Add CPU and memory limits to each service
   ```bash
   deploy:
     resources:
       limits:
         cpus: "0.5"
         memory: 512M
       reservations:
         cpus: "0.25"
         memory: 256M
   ```

3. **Security**
   - Use Docker secrets for sensitive configuration
   - Run containers with minimal privileges
   - Regularly update base images

4. **Monitoring**
   - Add Prometheus and Grafana for metrics collection
   - Use ELK stack for log aggregation
   - Implement distributed tracing

## Maintenance

### Backing Up Data

To backup PostgreSQL data:
```bash
docker exec rateflow-postgres pg_dump -U rateflow_user rateflow > backup.sql
```

To restore PostgreSQL data:
```bash
cat backup.sql | docker exec -i rateflow-postgres psql -U rateflow_user rateflow
```

### Updating Services

To update a specific service image:
```bash
docker compose pull <service-name>
docker compose up -d <service-name>
```

To update all services:
```bash
docker compose pull
docker compose up -d
```

## Verification

After deployment, verify the following endpoints are accessible:

- API Health: http://localhost:8080/actuator/health
- API Info: http://localhost:8080/actuator/info
- Kafka Topics: Use `docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list`
- Redis Info: `docker compose exec redis redis-cli info`
- PostgreSQL Connection: `docker compose exec postgres psql -U rateflow_user -d rateflow -c "\dt"`

## Notes

- The first startup may take a minute as services initialize and health checks pass
- The API will automatically apply database migrations on startup
- Kafka topics are created automatically by the application
- Rate limiting uses Redis Token Bucket algorithm with Lua scripts for atomic operations
- Notification events are published to Kafka and processed asynchronously