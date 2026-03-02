# Cycle Nest - Orchestrator Service


This project is part of a coursework by building RESTful web services to manage inventory, routing, and user request. This coursework/project is inspired from this [website](https://www.libraryofthings.co.uk/heatmap/nottingham), the sustainable practice of borrowing useful items within your local community. 

**Prerequisite:** A MongoDB Atlas cluster is required to run this project. You can find the dataset in `items.json` to upload to your cluster.

---

## 1. RESTful Web Services
The application exposes 4 key endpoints:

1.  **Inventory Service** (`GET /resources/inventory`)
    *   **Purpose:** Fetches the list of items with server-side pagination.
    *   **Logic:** Enforces a strict limit of 16 items per page to ensure low latency.

2.  **Item Search Service** (`GET /resources/item-search`)
    *   **Purpose:** Allows filtering inventory by name, category, or price range.

3.  **Request Status Service** (`GET/POST /resources/status`)
    *   **Purpose:** Manages the lifecycle of user requests (Toggle Pending/Cancelled), updates item status database.

4.  **Route Service** (`GET /resources/route-service`)
    *   **Purpose:** Calculates driving distances between the user and an item via OSRM.

---

## 2. QoS Optimization & Performance Analysis

### 1. Problem Identification
Initial Quality of Service (QoS) testing revealed severe scalability issues when interacting with the 10,000 items dataset.

- **Symptom:** The '/inventory' endpoint high latency (>4.9s) and massive bandwidth consumption.
- **Root Cause:** The endpoint was performing a full collection scan and returning all documents in a single HTTP response (~5.6MB).
- **Impact:** This "Unbounded Result Set" anti-pattern led to blocking I/O, increased memory pressure on the JVM, and network saturation.

### 2. Testing Methodology & Evidence
Used JMeter to test performance baseline.

**A. Baseline (Pre-Fix)**
- Configuration: 5 concurrent users, 10 loops.
- Average Latency: 4903 ms
- Payload Size: 5.6 MB
- Throughput: 0.9 req/sec
- Evidence File: `evidence/results_metrics.csv`

**B. Verification (Post-Fix)**
- Configuration: Identical load profile.
- Average Latency: 376 ms (92% Reduction)
- Payload Size: 4.6 KB (99.9% Reduction)
- Throughput: 9.3 req/sec (10x Increase)
- Evidence File: `evidence/results_metrics_after_solution.csv`

### 3. Technical Solution
The solution involved refactoring the `InventoryResource()` to implement server-side pagination.

- **Backend (InventoryResource.java):**
Integrated 'page' and 'limit' query parameters. Utilized MongoDB's native 'skip()' and 'limit()' cursors to restrict data retrieval at the database level. This moves the filtering logic to the database engine, rather than transferring all data to the application layer.

- **Frontend (index.html):**
Updated the UI to support page-by-page navigation while maintaining search functionality.

### 4. Cloud Cost
If deployed to a cloud environment (e.g., AWS EC2 + MongoDB Atlas):

- **Original State:** 5.6MB per refresh. 1,000 users = 5.6 GB of data transfer.
- **Optimized State:** 50KB per refresh. 1,000 users = 50 MB of data transfer.
- **Impact:** Significant reduction in Data Transfer Out (DTO) charges. Additionally, the lower memory footprint per request allows the application to run on smaller, cheaper container instances (e.g., reducing vCPU/RAM requirements).

### 5. Research & Justification
The implemented solution is grounded in recent research regarding Sustainable Software Engineering and API performance optimization.

**A. Application of Green API Design**
Diallo establishes that "Low Latency" APIs are a critical component of Green Computing, as they reduce the processing time and energy consumption per request. By reducing our payload size by 99.9% (from 5.6MB to 4.6KB), we adhered to these principles, minimizing the energy required for data transmission and serialization on both the client and server. Diallo (2024).

**B. Strategic Choice of Pagination**
Verma argues that implementing pagination is essential for avoiding "response timeout" and "server overload" in data-heavy applications. While Cursor-based pagination is efficient for real-time streams, Verma highlights that Offset-based (Page/Limit) pagination remains a standard best practice for inventory systems requiring "random access" navigation. Selected Offset-based pagination to provide the "Jump to Page" functionality balancing performance improvements with User Experience. Verma (2023).

### 6. References (From 2023 and later)
* Diallo, T., 2024. *Green API Design [3/5]: Low Latency API Optimized* [online]. Medium. Available at: <https://medium.com/just-tech-it-now/green-api-design-3-5-low-latency-api-optimized-f04ecbaada31> [Accessed 11 January 2026].
* Verma, P., 2023. *Unlocking the Power of API Pagination: Best Practices and Strategies* [online]. Dev.to. Available at: <https://dev.to/pragativerma18/unlocking-the-power-of-api-pagination-best-practices-and-strategies-4b49> [Accessed 11 January 2026].

---

## 3. Environment Variables

To keep credentials secure, this project uses environment variables for database connections. Create a `.env` file in the root directory and fill in your credentials:

```env
MONGO_CONNECTION_STRING=mongodb+srv://user:pass@cluster.mongodb.net/?retryWrites=true&w=majority
MONGO_DATABASE=items
```

---

## 4. Docker

The service is fully containerized using a multi-stage Dockerfile.

### Prerequisite
- Docker Desktop / Engine installed.

### Build & Run Instructions

Open cmd and cd within the project directory.

1.  **CD into project directory:**
    ```bash
    cd orchestrator-service/
    ```
2.  **Build the Image:**
    ```bash
    docker build -t orchestrator-service:latest .
    ```
3.  **Start the Container (providing environment variables):**
    ```bash
    docker run -p 8080:8080 --env-file .env orchestrator-service:latest
    ```
    Alternatively, if you don't have a `.env` file:
    ```bash
    docker run -p 8080:8080 -e MONGO_CONNECTION_STRING="your-mongodb-connection-string" -e MONGO_DATABASE="items" orchestrator-service:latest
    ```
4.  **Access the App:**
    - `http://localhost:8080/mavenproject1/`

### Cloud Readiness Features
- **Statelessness:** Connections to external MongoDB allow for horizontal scaling.
- **Port Mapping:** Exposes port 8080 for easy mapping by cloud load balancers.
- **Multi-Stage Build:** Ensures a lean production image containing only the necessary runtime components.
