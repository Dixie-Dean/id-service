# Unique ID Generator Service

## Installation

1. Ensure you have Docker installed on your system.
2. Clone the repository and navigate to the project directory.
3. Run the following command to start the Redis container:

   ```
   docker-compose up -d
   ```

   This will start the Redis container with the necessary configuration.

## Usage

The Unique ID Generator Service provides an API to generate and retrieve unique IDs. The service uses Redis as the underlying data store to store and retrieve the IDs.

### Generating a Unique ID

To generate a unique ID, make a GET request to the `/id/generate` endpoint:

```
GET /id/generate
```

The service will respond with a unique 8-character ID.

### Retrieving a Unique ID

The service also listens to a Kafka topic named `request-id-topic` for requests to retrieve a unique ID. When a message is received on this topic, the service will retrieve a unique ID from the Redis store and send it to the `provide-id-topic` Kafka topic.

## API

The Unique ID Generator Service exposes the following API endpoints:

- `GET /id/generate`: Generates a new unique ID and returns it.

## Testing

The Unique ID Generator Service includes a comprehensive test suite to ensure the correctness of the implementation. The tests cover the following aspects:

- Generating a unique ID
- Retrieving a unique ID from the Redis store
- Handling cases where the Redis store is empty or has a low batch size
- Sending and receiving messages on the Kafka topics

To run the tests, use the following command:

```
./gradlew test
```

This will execute all the tests and report the results.