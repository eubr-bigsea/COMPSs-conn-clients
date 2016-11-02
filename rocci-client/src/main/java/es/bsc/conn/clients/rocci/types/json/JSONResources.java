package es.bsc.conn.clients.rocci.types.json;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;


public class JSONResources {

    @Expose
    private List<Resource> resources = new ArrayList<>();


    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

}
