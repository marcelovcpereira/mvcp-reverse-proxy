# Adobe Reverse Proxy Test

Implementarion and automation of a Reverse Proxy.

## Components
**Entrypoint**
Central controller responsible for intercepting all http requests done to the proxy.
It processes the request, forwards it to the ReverseProxy, then returns the result to the caller. 

**ReverseProxy**
The heart of the flow, the proxy contains the list of the Services that are attached to it. It is responsible for caching 
mechanisms, circuit breaker features and load balancing.
After initialized, the ReverseProxy starts polling its service's endpoints each 10 seconds for re-evaluating their health. 
It marks them as Suspended or Active, depending on the result.

**CacheManager**
Manages HTTP Cache Control logic (Not 100% compliant).
Current implemented properties: no-cache, no-store, private, max-age.
Both the client (request) and the service (response) can return Cache Control headers.
Below goes the Reverse Proxy behavior when each of them uses one of the implemented headers.
  
Behaviors on request:
 * no-cache: Skips cache, execute query and then cache Response
 * no-store: Skips cache, execute query and then cache Response
 * private: Skips cache, execute query and then cache Response
 * max-age: Validate if cached object is not older than max-age in seconds
 
Behaviors on response:
 * no-cache: Does not store Response on cache.
 * no-store: Does not store Response on cache.
 * private: Does not store Response on cache.
 
**Service**
Represents a group of Endpoints that are responding as replicas of an Application.
Each service can have its own load balancing strategies for routing the requests. 

**Endpoint**
Represents a server host & port configuration that is responding for a certain Service.
Each Endpoint has a status value to represent its health:
>*PENDING* - All newly initialized Endpoints

>*ACTIVE* - All endpoints that have a successful last request

>*SUSPENDED* - All endpoints that have a failed last request

>*BLOCKED* - Black listed 

**Balancer**
Responsible for trying to fulfil a request using one of the available Endpoints. It tries all available Endpoints until
some of them fulfils the request or all fail. The strategy of electing which Endpoint should be the next candidate for
attempting the request depends on the routing implementation of the subclasses.

**RoundRobinLoadBalancer**
Balancer that implements the circular strategy for electing the Endpoints.

**RandomLoadBalancer**
Balancer that implements the random strategy for electing the Endpoints.

**HttpForwarder**
Helper for executing HTTP requests in remote hosts.


## Deploy Reverse Proxy in a Kubernetes Cluster via Helm Chart:
>After cloning, notice that inside the project folder there is a "mvcp-adobe-reverse-proxy/src/main/resources/devops" directory containing the necessary files for launching the application in a kubernetes cluster as shown below:
```bash
git clone https://github.com/marcelovcpereira/mvcp-adobe-reverse-proxy.git
helm install --name marcelo-adobe-reverse-proxy --namespace marcelo-test -f ./mvcp-adobe-reverse-proxy/src/main/resources/devops/values.yaml ./mvcp-adobe-reverse-proxy/src/main/resources/devops
```
The above command will deploy the Reverse Proxy, Prometheus, Grafana, Redis, ServiceA & Service B based on the configuration found on file:
>./mvcp-adobe-reverse-proxy/src/main/resources/devops/values.yaml


## Playing with the Reverse Proxy:

### Using K8s Port-forward + cUrl
>Important: Using Postman, you cannot send restricted HTTP headers like "Host". Install Postman Interceptor for it or use cUrl shown below.
```bash
kubectl port-forward -n marcelo-test svc/marcelo-adobe-reverse-proxy 9999:9999
curl -XGET -H "Host: a.my-services.com" http://localhost:9999/marcelo/test/15
curl -XGET -H "Host: b.my-services.com" http://localhost:9999/marcelo/serviceb/15
curl -H "Host: a.my-services.com" -H "Cache-Control: max-age=100" localhost:9999/marcelo/test/12345
curl -H "Host: a.my-services.com" -H "Cache-Control: no-cache" localhost:9999/marcelo/test/12345
```

Example response:
>{"idServiceA": 15}


## Monitoring SLIs

### For verifying overal performance of the application, you can use Prometheus & Grafana:

#### Visiting Prometheus dashboard:
```bash
kubectl port-forward -n marcelo-test svc/prometheus-service 7777:80
```
After the port forward, access in your browser: http://localhost:9999


#### Visiting Grafana dashboard for metrics visualization:
```bash
kubectl port-forward -n marcelo-test svc/marcelo-adobe-grafana 9898:9898
```
Then, access in your browser: http://localhost:9898
>User: (configurable on values.yaml:monitoring.adminUser)
>Password: (configurable on values.yaml:monitoring.adminPassword)

Open the dashboard: Reverse Proxy Visualization
You can change the timeframe to last 5 minutes and activate refreshing every 5s.

With the visualization opened you can then access terminal and execute several requests or maybe use siege as shown below:

##### Firing 10 req/s (600/min) during 5 minutes to Mock Service A without never touching cache
```bash
siege -c 10 -i -t 5M -f /Users/marcelopereira/Documents/workspace/mvcp-adobe-reverse-proxy/siege_urls_a.txt -H "Host: a.my-services.com" -H "Cache-Control: no-cache"
```


##### Firing 5 req/s (300/min) during 3 minutes to Mock Service B without never touching cache
```bash
siege -c 5 -i -t 3M -f /Users/marcelopereira/Documents/workspace/mvcp-adobe-reverse-proxy/siege_urls_b.txt -H "Host: b.my-services.com" -H "Cache-Control: no-cache"
```

### Calculation:
```bash
Requests Per Minute: rate(http_server_requests_seconds_count[5m])*60
Memory Usage: jvm_memory_used_bytes{area="heap",job="reverse-proxy"}
Real time CPU Usage: system_cpu_usage*100
Average Latency/min (ms): rate(http_server_requests_seconds_max{uri!~"/actuator/.*"}[5m])*1000*60
```
###

#### To clean helm installations:
```bash
helm del --purge marcelo-adobe-reverse-proxy
```

PS: If you use MacOS & your docker container needs to access a local service/port, do not bind to localhost or 127.0.0.1, instead use internal docker DNS, e.g:
>docker.for.mac.host.internal:8080


## Improvements:
- Create tests for Cache Control 
- Implement more Cache Control headers
- Make Cache Control 100% compliant to specification
- Configure credentials for acessing the Cache
- Generate and expose metrics of Cache (miss/hists/uptime/etc)
- Implement cluster version of Redis for better scalability
- Externalize a toggle for enabling/disabling Cache deployment
- Implement more Strategies of load balancing
- Increase test coverage
- Externalize the configuration "polling interval" (currently: 10s)
- Implement dynamic black list of endpoints for being used with BLOCKED status feature
- Implement persistent volumes for storing Prometheus + Grafana data
- Configure Prometheus Alert Manager
- Generate and expose metrics from attached Services (availability, latency, etc)
- Improve marshalling/unmarshalling of message in proxy & cache


# References:
>https://helm.sh/docs/chart_best_practices/
>https://github.com/helm/charts/tree/master/stable/grafana
>https://github.com/helm/charts/tree/master/stable/prometheus
>https://github.com/helm/charts/tree/master/stable/redis
>https://stackoverflow.com/questions/47668793/helm-generate-comma-separated-list
>https://itnext.io/simple-redis-cache-on-kubernetes-with-prometheus-metrics-8667baceab6b
>https://groups.google.com/forum/#!topic/prometheus-users/MkyxLiVsJz0
>https://blog.softwaremill.com/practical-monitoring-with-prometheus-ee09a1dd5527
>https://www.joedog.org/siege-manual/