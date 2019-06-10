package mvcp.adobe.entities;
/**
 * Represents a server host & port configuration that is responding for a certain Service.
 * Each Endpoint has a status value to represent its health:
 * 
 * PENDING - All newly initialized Endpoints
 * ACTIVE - All endpoints that have a successful last request
 * SUSPENDED - All endpoints that have a failed last request
 * BLOCKED - Black listed 
 */
public class Endpoint {

    private String ip;
    private int port;
    private EndpointStatus status = EndpointStatus.PENDING;

    public Endpoint(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public String toJsonString() {
        return "{ \"ip\": \"" + this.ip + "\", \"port\": " + this.port + ", \"status\": \"" + this.status.toString() + "\"}";
    }
    public String toString() {
        return this.ip + ":" + this.port;
    }

    public EndpointStatus getStatus() {
        return status;
    }

    public void setStatus(EndpointStatus status) {
        this.status = status;
    }

    //Code reference: https://www.geeksforgeeks.org/equals-hashcode-methods-java/
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
            return true;
        if(obj == null || obj.getClass()!= this.getClass())
            return false;
        Endpoint endpoint = (Endpoint) obj;
        return endpoint.getIp().equalsIgnoreCase(this.getIp())
                && endpoint.getPort() == this.getPort()
                && endpoint.getStatus().equals(this.status);
    }

}
