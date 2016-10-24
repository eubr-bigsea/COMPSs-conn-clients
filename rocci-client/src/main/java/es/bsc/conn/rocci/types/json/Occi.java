package es.bsc.conn.rocci.types.json;


import javax.annotation.Generated;
import com.google.gson.annotations.Expose;

@Generated("org.jsonschema2pojo")
public class Occi {

    @Expose
    private Core core;
    @Expose
    private Compute compute;
    @Expose
    private Networkinterface networkinterface;

    public Core getCore() {
        return core;
    }

    public void setCore(Core core) {
        this.core = core;
    }

    public Compute getCompute() {
        return compute;
    }

    public void setCompute(Compute compute) {
        this.compute = compute;
    }

    public Networkinterface getNetworkinterface() {
        return networkinterface;
    }

    public void setNetworkinterface(Networkinterface networkinterface) {
        this.networkinterface = networkinterface;
    }

}
