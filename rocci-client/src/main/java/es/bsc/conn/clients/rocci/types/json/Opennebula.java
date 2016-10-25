package es.bsc.conn.clients.rocci.types.json;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;

@Generated("org.jsonschema2pojo")
public class Opennebula {

    @Expose
    private Compute compute;

    public Compute getCompute() {
        return compute;
    }

    public void setCompute(Compute compute) {
        this.compute = compute;
    }

}
