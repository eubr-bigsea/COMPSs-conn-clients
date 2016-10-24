package es.bsc.conn.rocci.types.json;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;


public class JSONResources {

    @Expose
    private List<Resource> resources = new ArrayList<Resource>();


    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

}
